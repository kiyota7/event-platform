import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { browserZone, dateKey, formatInZone } from '../time.js'

const WEEK = ['日', '月', '火', '水', '木', '金', '土']

export default function CalendarView() {
  const { user } = useAuth()
  const zone = user?.timeZone || browserZone
  const now = new Date()
  const [ym, setYm] = useState({ y: now.getFullYear(), m: now.getMonth() }) // m: 0-11
  const [events, setEvents] = useState([])
  const [error, setError] = useState('')

  // 月の前後1日の余裕を持って取得し、表示はユーザーのTZの日付で振り分ける
  useEffect(() => {
    const from = new Date(Date.UTC(ym.y, ym.m, 1) - 36 * 3600 * 1000).toISOString()
    const to = new Date(Date.UTC(ym.y, ym.m + 1, 1) + 36 * 3600 * 1000).toISOString()
    setError('')
    api(`/events?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&size=100`, { auth: false })
      .then((d) => setEvents(d.content)).catch(() => setError('イベントの取得に失敗しました'))
  }, [ym])

  const byDay = useMemo(() => {
    const map = {}
    events.forEach((e) => { (map[dateKey(e.startAt, zone)] ||= []).push(e) })
    return map
  }, [events, zone])

  const first = new Date(Date.UTC(ym.y, ym.m, 1))
  const daysInMonth = new Date(Date.UTC(ym.y, ym.m + 1, 0)).getUTCDate()
  const cells = [...Array(first.getUTCDay()).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)]
  const todayKey = dateKey(new Date().toISOString(), zone)
  const move = (d) => { const t = new Date(Date.UTC(ym.y, ym.m + d, 1)); setYm({ y: t.getUTCFullYear(), m: t.getUTCMonth() }) }
  const key = (d) => `${ym.y}-${String(ym.m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`

  return (
    <section>
      <div className="cal-head">
        <button onClick={() => move(-1)}>‹</button>
        <h1>{ym.y}年{ym.m + 1}月</h1>
        <button onClick={() => move(1)}>›</button>
        <span className="muted">表示タイムゾーン: {zone}</span>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="cal">
        {WEEK.map((w) => <div key={w} className="cal-w">{w}</div>)}
        {cells.map((d, i) => (
          <div key={i} className={`cal-d${d && key(d) === todayKey ? ' today' : ''}${d ? '' : ' empty'}`}>
            {d && <span className="num">{d}</span>}
            {d && (byDay[key(d)] || []).map((e) => (
              <Link key={e.id} to={`/events/${e.id}`} className="cal-e" title={e.title}>
                {formatInZone(e.startAt, zone, { year: undefined, month: undefined, day: undefined })} {e.title}
              </Link>
            ))}
          </div>
        ))}
      </div>
    </section>
  )
}
