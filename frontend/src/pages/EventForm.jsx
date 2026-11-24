import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../AuthContext.jsx'
import { browserZone, localToOffsetISO } from '../time.js'

const zones = typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : ['Asia/Tokyo']

export default function EventForm() {
  const { id } = useParams()
  const editing = !!id
  const { user } = useAuth()
  const navigate = useNavigate()
  const [categories, setCategories] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({
    title: '', description: '', location: '', start: '', end: '', timeZone: user?.timeZone || browserZone,
    capacity: 10, categoryId: '', tags: '',
  })
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  useEffect(() => { api('/categories', { auth: false }).then(setCategories).catch(() => {}) }, [])

  useEffect(() => {
    if (!editing) return
    api(`/events/${id}`, { auth: false }).then((e) => {
      if (e.organizerId !== user?.id) { navigate(`/events/${id}`, { replace: true }); return }
      setForm({
        title: e.title, description: e.description || '', location: e.location || '',
        start: e.startLocal.slice(0, 16), end: e.endLocal.slice(0, 16), timeZone: e.timeZone, // イベントのTZの壁時計
        capacity: e.capacity, categoryId: e.categoryId ?? '', tags: e.tags.join(' '),
      })
    }).catch(() => setError('イベントの取得に失敗しました'))
  }, [editing, id, user, navigate])

  async function submit(e) {
    e.preventDefault()
    setError('')
    if (form.end <= form.start) { setError('終了日時は開始日時より後にしてください'); return }
    setBusy(true)
    try {
      const body = {
        title: form.title, description: form.description || null, location: form.location || null,
        startAt: localToOffsetISO(form.start, form.timeZone), endAt: localToOffsetISO(form.end, form.timeZone),
        timeZone: form.timeZone, capacity: Number(form.capacity),
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        tags: form.tags.split(/[,\s]+/).filter(Boolean),
      }
      const saved = await api(editing ? `/events/${id}` : '/events', { method: editing ? 'PUT' : 'POST', body })
      navigate(`/events/${saved.id}`)
    } catch (err) {
      setError(err.status === 400 ? '入力内容を確認してください(定員は確定済み人数以上にしてください)' : '保存に失敗しました')
    } finally { setBusy(false) }
  }

  return (
    <form className="card wide" onSubmit={submit}>
      <h1>{editing ? 'イベントを編集' : 'イベントを作成'}</h1>
      {error && <p className="error" role="alert">{error}</p>}
      <label>タイトル<input value={form.title} onChange={set('title')} required maxLength={100} /></label>
      <label>説明<textarea rows={4} value={form.description} onChange={set('description')} maxLength={4000} /></label>
      <label>場所<input value={form.location} onChange={set('location')} maxLength={200} /></label>
      <label>タイムゾーン(開催地)
        <select value={form.timeZone} onChange={set('timeZone')}>
          {(zones.includes(form.timeZone) ? zones : [form.timeZone, ...zones]).map((z) => <option key={z}>{z}</option>)}
        </select>
      </label>
      <div className="row">
        <label>開始({form.timeZone})<input type="datetime-local" value={form.start} onChange={set('start')} required /></label>
        <label>終了({form.timeZone})<input type="datetime-local" value={form.end} onChange={set('end')} required /></label>
      </div>
      <div className="row">
        <label>定員<input type="number" min={1} max={100000} value={form.capacity} onChange={set('capacity')} required /></label>
        <label>カテゴリ
          <select value={form.categoryId} onChange={set('categoryId')}>
            <option value="">なし</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
      </div>
      <label>タグ(スペース区切り)<input value={form.tags} onChange={set('tags')} /></label>
      <button disabled={busy}>{busy ? '保存中...' : '保存'}</button>
    </form>
  )
}
