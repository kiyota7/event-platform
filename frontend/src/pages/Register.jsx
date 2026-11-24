import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext.jsx'

const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Tokyo'
const zones = typeof Intl.supportedValuesOf === 'function' ? Intl.supportedValuesOf('timeZone') : ['Asia/Tokyo']

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', displayName: '', timeZone: browserZone })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await register(form)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err.status === 409 ? 'このメールアドレスは既に登録されています' : '登録に失敗しました。入力内容を確認してください')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h1>新規登録</h1>
      {error && <p className="error" role="alert">{error}</p>}
      <label>表示名
        <input value={form.displayName} onChange={set('displayName')} required maxLength={50} />
      </label>
      <label>メールアドレス
        <input type="email" value={form.email} onChange={set('email')} required autoComplete="email" />
      </label>
      <label>パスワード(8文字以上)
        <input type="password" value={form.password} onChange={set('password')} required minLength={8} maxLength={72} autoComplete="new-password" />
      </label>
      <label>タイムゾーン
        <select value={form.timeZone} onChange={set('timeZone')}>
          {(zones.includes(form.timeZone) ? zones : [form.timeZone, ...zones]).map((z) => <option key={z}>{z}</option>)}
        </select>
      </label>
      <button disabled={busy}>{busy ? '送信中...' : '登録する'}</button>
      <p className="muted">登録済みの方は <Link to="/login">ログイン</Link></p>
    </form>
  )
}
