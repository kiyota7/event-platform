import { useAuth } from '../AuthContext.jsx'

export default function Home() {
  const { user } = useAuth()
  return (
    <section>
      <h1>イベント一覧</h1>
      <p>{user ? `${user.displayName} さん、ようこそ。` : 'ログインするとイベントに参加できます。'}</p>
      <p className="muted">イベント一覧・カレンダーは次のIssueで実装します。</p>
    </section>
  )
}
