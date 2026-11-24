import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { browserZone, formatInZone } from '../time.js'

export default function EventList() {
  const { user } = useAuth()
  const zone = user?.timeZone || browserZone
  const [filters, setFilters] = useState({ q: '', categoryId: '', tag: '', availableOnly: false })
  const [applied, setApplied] = useState(filters)
  const [page, setPage] = useState(0)
  const [categories, setCategories] = useState([])
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => { api('/categories', { auth: false }).then(setCategories).catch(() => {}) }, [])

  useEffect(() => {
    const p = new URLSearchParams({ page, size: 10 })
    if (applied.q) p.set('q', applied.q)
    if (applied.categoryId) p.set('categoryId', applied.categoryId)
    applied.tag.split(/[,\s]+/).filter(Boolean).forEach((t) => p.append('tag', t))
    if (applied.availableOnly) p.set('availableOnly', 'true')
    setError('')
    api(`/events?${p}`, { auth: false }).then(setData).catch(() => setError('イベントの取得に失敗しました'))
  }, [applied, page])

  const set = (k) => (e) => setFilters({ ...filters, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })
  const search = (e) => { e.preventDefault(); setPage(0); setApplied(filters) }

  return (
    <section>
      <h1>イベント一覧</h1>
      <form className="filters" onSubmit={search}>
        <input placeholder="キーワード" value={filters.q} onChange={set('q')} />
        <select value={filters.categoryId} onChange={set('categoryId')}>
          <option value="">すべてのカテゴリ</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input placeholder="タグ(スペース区切り)" value={filters.tag} onChange={set('tag')} />
        <label className="inline"><input type="checkbox" checked={filters.availableOnly} onChange={set('availableOnly')} />空きあり</label>
        <button>検索</button>
      </form>
      {error && <p className="error" role="alert">{error}</p>}
      {data && data.content.length === 0 && <p className="muted">該当するイベントがありません。</p>}
      <ul className="events">
        {data?.content.map((e) => (
          <li key={e.id} className="event-item">
            <Link to={`/events/${e.id}`}><strong>{e.title}</strong></Link>
            <div className="muted">{formatInZone(e.startAt, zone)} 〜 ({zone}) {e.location && `/ ${e.location}`}</div>
            <div>
              {e.categoryName && <span className="badge">{e.categoryName}</span>}
              {e.tags.map((t) => <span key={t} className="badge tag">#{t}</span>)}
              <span className={e.confirmedCount >= e.capacity ? 'full' : 'seats'}>
                {e.confirmedCount >= e.capacity ? '満席(キャンセル待ち)' : `残り${e.capacity - e.confirmedCount}席`}
              </span>
            </div>
          </li>
        ))}
      </ul>
      {data && data.totalPages > 1 && (
        <div className="pager">
          <button disabled={data.first} onClick={() => setPage(page - 1)}>前へ</button>
          <span>{page + 1} / {data.totalPages}</span>
          <button disabled={data.last} onClick={() => setPage(page + 1)}>次へ</button>
        </div>
      )}
    </section>
  )
}
