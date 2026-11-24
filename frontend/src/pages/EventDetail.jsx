import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { browserZone, formatInZone } from '../time.js'

const STATUS_LABEL = { CONFIRMED: '参加確定', WAITLISTED: 'キャンセル待ち' }

export default function EventDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const zone = user?.timeZone || browserZone
  const [event, setEvent] = useState(null)
  const [reg, setReg] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = useCallback(async () => {
    try {
      setEvent(await api(`/events/${id}`, { auth: false }))
      if (user) {
        try {
          const r = await api(`/events/${id}/registrations/me`)
          setReg(r.status === 'CANCELED' ? null : r)
        } catch { setReg(null) }
      } else setReg(null)
    } catch (e) { setError(e.status === 404 ? 'イベントが見つかりません' : '取得に失敗しました') }
  }, [id, user])

  useEffect(() => { load() }, [load])

  async function act(fn, okErrors = {}) {
    setBusy(true); setError('')
    try { await fn(); await load() }
    catch (e) { setError(okErrors[e.status] || '操作に失敗しました。時間をおいて再度お試しください') }
    finally { setBusy(false) }
  }

  const register = () => act(() => api(`/events/${id}/registrations`, { method: 'POST' }),
    { 409: '既に申込済み、または混み合っています。再度お試しください', 400: '開始済みのイベントには申込できません' })
  const cancel = () => window.confirm('申込をキャンセルしますか?') && act(() => api(`/events/${id}/registrations`, { method: 'DELETE' }))
  async function remove() {
    if (!window.confirm('このイベントを削除しますか?(元に戻せません)')) return
    try { await api(`/events/${id}`, { method: 'DELETE' }); navigate('/') } catch { setError('削除に失敗しました') }
  }

  if (!event) return error ? <p className="error" role="alert">{error}</p> : <p className="center">読み込み中...</p>
  const isOrganizer = user && user.id === event.organizerId
  const full = event.confirmedCount >= event.capacity
  const started = new Date(event.startAt) <= new Date()

  return (
    <section className="detail">
      <h1>{event.title}</h1>
      {error && <p className="error" role="alert">{error}</p>}
      <dl>
        <dt>日時({zone})</dt><dd>{formatInZone(event.startAt, zone)} 〜 {formatInZone(event.endAt, zone)}</dd>
        {event.timeZone !== zone && (<><dt>現地時間({event.timeZone})</dt><dd>{formatInZone(event.startAt, event.timeZone)} 〜 {formatInZone(event.endAt, event.timeZone)}</dd></>)}
        {event.location && (<><dt>場所</dt><dd>{event.location}</dd></>)}
        <dt>定員</dt><dd>{event.confirmedCount} / {event.capacity} 人{full && '(満席)'}</dd>
        {event.categoryName && (<><dt>カテゴリ</dt><dd>{event.categoryName}</dd></>)}
        {event.tags.length > 0 && (<><dt>タグ</dt><dd>{event.tags.map((t) => <span key={t} className="badge tag">#{t}</span>)}</dd></>)}
      </dl>
      {event.description && <p className="desc">{event.description}</p>}
      <div className="actions">
        {!user && <Link to="/login" state={{ from: `/events/${id}` }}>ログインして申し込む</Link>}
        {user && reg && (<><span className="badge status">{STATUS_LABEL[reg.status]}</span><button className="secondary" disabled={busy} onClick={cancel}>キャンセルする</button></>)}
        {user && !reg && !started && <button disabled={busy} onClick={register}>{full ? 'キャンセル待ちに申し込む' : '参加申込する'}</button>}
        {started && !reg && <span className="muted">受付は終了しました</span>}
        {isOrganizer && (<><Link to={`/events/${id}/edit`}>編集</Link><button className="danger" onClick={remove}>削除</button></>)}
      </div>
    </section>
  )
}
