import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, tokenStore } from './api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!tokenStore.get())

  // 起動時に保存済みトークンでログイン状態を復元
  useEffect(() => {
    if (!tokenStore.get()) return
    api('/auth/me')
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const onUnauthorized = () => setUser(null)
    window.addEventListener('auth:unauthorized', onUnauthorized)
    return () => window.removeEventListener('auth:unauthorized', onUnauthorized)
  }, [])

  const handleAuth = useCallback((res) => {
    tokenStore.set(res.token)
    setUser(res.user)
  }, [])

  const login = useCallback(
    async (email, password) =>
      handleAuth(await api('/auth/login', { method: 'POST', body: { email, password }, auth: false })),
    [handleAuth],
  )
  const register = useCallback(
    async (form) => handleAuth(await api('/auth/register', { method: 'POST', body: form, auth: false })),
    [handleAuth],
  )
  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>{children}</AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
