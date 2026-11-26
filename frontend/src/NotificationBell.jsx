import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from './api.js'
import { formatInZone } from './time.js'

const MAX_BACKOFF_MS = 30000
/** SSEがつながっていない間に、通知を取り直す間隔 */
const POLL_MS = 30000

/**
 * ヘッダーの通知ベル。SSEで新着を受信し、切断時は再接続する。
 * 本番(Render)では、静的サイトの中継がSSEを流さず、接続が開かないことがある。
 * そのため、SSEがつながっていない間は、一定間隔で通知を取り直して補う。
 */
export default function NotificationBell({ zone }) {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [live, setLive] = useState(false)
  const box = useRef(null)

  const reload = useCallback(async () => {
    try {
      const d = await api('/notifications')
      setItems(d.items)
      setUnread(d.unreadCount)
    } catch { /* 次回の更新で回復 */ }
  }, [])

  useEffect(() => {
    let es = null
    let timer = null
    let attempt = 0
    let cancelled = false

    async function connect() {
      try {
        // EventSourceはAuthorizationヘッダを付けられないため、使い捨てticketで接続する
        const { ticket } = await api('/notifications/stream-ticket', { method: 'POST' })
        if (cancelled) return
        es = new EventSource(`/api/notifications/stream?ticket=${encodeURIComponent(ticket)}`)
        es.addEventListener('connected', () => { attempt = 0; setLive(true); reload() }) // 切断中の取りこぼしを再取得
        es.addEventListener('notification', (ev) => {
          const n = JSON.parse(ev.data)
          setItems((prev) => (prev.some((p) => p.id === n.id) ? prev : [n, ...prev]))
          setUnread((u) => u + 1)
        })
        es.onerror = () => { es.close(); setLive(false); schedule() }
      } catch { schedule() }
    }
    function schedule() {
      if (cancelled) return
      attempt += 1
      timer = setTimeout(connect, Math.min(1000 * 2 ** attempt, MAX_BACKOFF_MS))
    }

    reload()
    connect()
    return () => { cancelled = true; clearTimeout(timer); es?.close() }
  }, [reload])

  // SSEがつながっていない間は、定期的に取り直す(つながっているときは、リアルタイムで届く)
  useEffect(() => {
    if (live) return undefined
    const id = setInterval(reload, POLL_MS)
    const onVisible = () => { if (document.visibilityState === 'visible') reload() }
    document.addEventListener('visibilitychange', onVisible)
    return () => { clearInterval(id); document.removeEventListener('visibilitychange', onVisible) }
  }, [live, reload])

  useEffect(() => {
    const onDoc = (e) => { if (box.current && !box.current.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [])

  async function openItem(n) {
    setOpen(false)
    if (!n.read) {
      setItems((prev) => prev.map((p) => (p.id === n.id ? { ...p, read: true } : p)))
      setUnread((u) => Math.max(0, u - 1))
      api(`/notifications/${n.id}/read`, { method: 'POST' }).catch(reload)
    }
    if (n.eventId) navigate(`/events/${n.eventId}`)
  }

  async function readAll() {
    setItems((prev) => prev.map((p) => ({ ...p, read: true })))
    setUnread(0)
    api('/notifications/read-all', { method: 'POST' }).catch(reload)
  }

  return (
    <div className="bell" ref={box}>
      <button className="bell-btn" onClick={() => { if (!open) reload(); setOpen(!open) }} aria-label={`通知 未読${unread}件`} aria-expanded={open}>
        🔔{unread > 0 && <span className="bell-badge">{unread > 99 ? '99+' : unread}</span>}
      </button>
      {open && (
        <div className="bell-panel" role="dialog" aria-label="通知">
          <div className="bell-head">
            <strong>通知</strong>
            <span className="muted">{live ? '● リアルタイム' : '○ 30秒ごとに更新'}</span>
            <button className="link" onClick={readAll} disabled={unread === 0}>すべて既読</button>
          </div>
          {items.length === 0 ? <p className="muted pad">通知はありません。</p> : (
            <ul>
              {items.map((n) => (
                <li key={n.id} className={n.read ? '' : 'unread'}>
                  <button className="bell-item" onClick={() => openItem(n)}>
                    <span>{n.message}</span>
                    <span className="muted">{formatInZone(n.createdAt, zone)}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
