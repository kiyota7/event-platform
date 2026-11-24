import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'

function RequireAuth({ children }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <p className="center">読み込み中...</p>
  return user ? children : <Navigate to="/login" replace state={{ from: location.pathname }} />
}

function Header() {
  const { user, logout } = useAuth()
  return (
    <header className="header">
      <Link to="/" className="brand">Event Platform</Link>
      <nav>
        {user ? (
          <>
            <span className="user">{user.displayName}</span>
            <button className="link" onClick={logout}>ログアウト</button>
          </>
        ) : (
          <>
            <Link to="/login">ログイン</Link>
            <Link to="/register">新規登録</Link>
          </>
        )}
      </nav>
    </header>
  )
}

export default function App() {
  return (
    <>
      <Header />
      <main className="main">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/me" element={<RequireAuth><Home /></RequireAuth>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}
