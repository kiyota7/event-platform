import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { browserZone, formatInZone } from '../time.js'
import { FillBar } from './Dashboard.jsx'

const STATUS = { CONFIRMED: '確定', WAITLISTED: 'キャンセル待ち' }

/** 日別の新規申込数(依存ライブラリなしの簡易棒グラフ) */
function TrendChart({ trend }) {
  if (trend.length === 0) return <p className="muted">まだ申込がありません。</p>
  const W = 560, H = 160, pad = 24
  const max = Math.max(...trend.map((t) => t.registrations))
  const bw = Math.min(40, (W - pad * 2) / trend.length - 6)
  const step = (W - pad * 2) / trend.length
  return (
    <svg viewBox={`0 0 ${W} ${H + 24}`} className="chart" role="img" aria-label="日別の新規申込数">
      <line x1={pad} y1={H} x2={W - pad} y2={H} stroke="#d1d5db" />
      {trend.map((t, i) => {
        const h = (t.registrations / max) * (H - 24)
        const x = pad + step * i + (step - bw) / 2
        return (
          <g key={t.date}>
            <rect x={x} y={H - h} width={bw} height={h} fill="#2563eb" rx="3"><title>{`${t.date}: ${t.registrations}件`}</title></rect>
            <text x={x + bw / 2} y={H - h - 4} textAnchor="middle" fontSize="11" fill="#374151">{t.registrations}</text>
            <text x={x + bw / 2} y={H + 14} textAnchor="middle" fontSize="10" fill="#6b7280">{t.date.slice(5)}</text>
          </g>
        )
      })}
    </svg>
  )
}

export default function DashboardEvent() {
  const { id } = useParams()
  const { user } = useAuth()
  const zone = user?.timeZone || browserZone
  const [d, setD] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api(`/dashboard/events/${id}`).then(setD).catch((e) =>
      setError(e.status === 403 ? 'このイベントの主催者ではありません' : e.status === 404 ? 'イベントが見つかりません' : '取得に失敗しました'))
  }, [id])

  if (error) return <p className="error" role="alert">{error}</p>
  if (!d) return <p className="center">読み込み中...</p>
  const s = d.stats

  return (
    <section>
      <p><Link to="/dashboard">← ダッシュボード</Link></p>
      <h1>{s.title}</h1>
      <p className="muted">開始: {formatInZone(s.startAt, zone)} ({zone}) / <Link to={`/events/${s.eventId}`}>イベントページ</Link></p>
      <div className="cards">
        <div className="stat"><div className="stat-v">{s.confirmed} / {s.capacity}</div><div className="muted">確定 / 定員</div></div>
        <div className="stat"><div className="stat-v">{s.waitlisted}</div><div className="muted">キャンセル待ち</div></div>
        <div className="stat"><div className="stat-v">{s.canceled}</div><div className="muted">キャンセル</div></div>
        <div className="stat"><FillBar rate={s.fillRate} /><div className="muted">充足率</div></div>
      </div>
      <h2>日別の新規申込数({s.timeZone})</h2>
      <TrendChart trend={d.trend} />
      <h2>参加者一覧({d.participants.length}人)</h2>
      {d.participants.length === 0 ? <p className="muted">参加者はいません。</p> : (
        <table className="table">
          <thead><tr><th>名前</th><th>メール</th><th>状態</th><th>申込日時({zone})</th></tr></thead>
          <tbody>
            {d.participants.map((p) => (
              <tr key={p.userId}><td>{p.displayName}</td><td>{p.email}</td>
                <td><span className={`badge ${p.status === 'CONFIRMED' ? 'status' : ''}`}>{STATUS[p.status]}</span></td>
                <td>{formatInZone(p.registeredAt, zone)}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
