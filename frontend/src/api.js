const TOKEN_KEY = 'token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

/** JWTを付けてJSON APIを呼ぶ。401ならトークンを破棄してunauthorizedイベントを通知する */
export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (auth && token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 401 && auth && token) {
    tokenStore.clear()
    window.dispatchEvent(new Event('auth:unauthorized'))
  }
  if (!res.ok) {
    let message = res.statusText
    try {
      const data = await res.json()
      message = data.message || data.error || message
    } catch { /* bodyなし */ }
    throw new ApiError(res.status, message)
  }
  return res.status === 204 ? null : res.json()
}
