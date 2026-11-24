import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import CalendarView from './pages/CalendarView.jsx'
import EventDetail from './pages/EventDetail.jsx'
import EventForm from './pages/EventForm.jsx'
import EventList from './pages/EventList.jsx'
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
        <Link to="/">イベント</Link>
        <Link to="/calendar">カレンダー</Link>
        {user ? (
          <>
            <Link to="/events/new">イベント作成</Link>
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
          <Route path="/" element={<EventList />} />
          <Route path="/calendar" element={<CalendarView />} />
          <Route path="/events/new" element={<RequireAuth><EventForm /></RequireAuth>} />
          <Route path="/events/:id" element={<EventDetail />} />
          <Route path="/events/:id/edit" element={<RequireAuth><EventForm /></RequireAuth>} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}
