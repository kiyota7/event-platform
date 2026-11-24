import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { browserZone, formatInZone } from '../time.js'

export function FillBar({ rate }) {
  return (
    <div className="fillbar" title={`${rate}%`} role="img" aria-label={`充足率 ${rate}%`}>
      <div style={{ width: `${Math.min(rate, 100)}%` }} />
      <span>{rate}%</span>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const zone = user?.timeZone || browserZone
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api('/dashboard').then(setData).catch(() => setError('ダッシュボードの取得に失敗しました'))
  }, [])

  if (error) return <p className="error" role="alert">{error}</p>
  if (!data) return <p className="center">読み込み中...</p>

  const cards = [
    ['主催イベント', data.totalEvents], ['開催前', data.upcomingEvents],
    ['確定参加者(合計)', data.totalConfirmed], ['キャンセル待ち(合計)', data.totalWaitlisted],
    ['平均充足率', `${data.averageFillRate}%`],
  ]
  return (
    <section>
      <h1>主催者ダッシュボード</h1>
      <div className="cards">
        {cards.map(([label, value]) => (
          <div key={label} className="stat"><div className="stat-v">{value}</div><div className="muted">{label}</div></div>
        ))}
      </div>
      {data.events.length === 0 ? (
        <p className="muted">主催イベントがありません。<Link to="/events/new">イベントを作成</Link></p>
      ) : (
        <table className="table">
          <thead><tr><th>イベント</th><th>開始({zone})</th><th>確定/定員</th><th>充足率</th><th>待ち</th><th>取消</th></tr></thead>
          <tbody>
            {data.events.map((e) => (
              <tr key={e.eventId}>
                <td><Link to={`/dashboard/events/${e.eventId}`}>{e.title}</Link></td>
                <td>{formatInZone(e.startAt, zone)}</td>
                <td>{e.confirmed} / {e.capacity}</td>
                <td><FillBar rate={e.fillRate} /></td>
                <td>{e.waitlisted}</td><td>{e.canceled}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
