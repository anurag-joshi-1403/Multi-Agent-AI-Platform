import { useCallback, useEffect, useState } from 'react'
import { login as apiLogin, logout as apiLogout, me, subscribeAuthExpired } from '../api/client'
import type { AuthUser } from '../api/client'

export type AuthStatus = 'checking' | 'authenticated' | 'anonymous'

export interface AuthApi {
  status: AuthStatus
  user: AuthUser | null
  /** Set only when the session check failed outright (backend unreachable), not for "not signed in". */
  checkError: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

/**
 * Gates the whole console: nothing else renders until this resolves. The session cookie from an
 * earlier visit is checked once on mount via `GET /api/auth/me`, so a refresh doesn't sign you out.
 */
export function useAuth(): AuthApi {
  const [status, setStatus] = useState<AuthStatus>('checking')
  const [user, setUser] = useState<AuthUser | null>(null)
  const [checkError, setCheckError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    me()
      .then((u) => {
        if (cancelled) return
        setUser(u)
        setStatus(u ? 'authenticated' : 'anonymous')
      })
      .catch((err: unknown) => {
        if (cancelled) return
        setCheckError(err instanceof Error ? err.message : 'Could not reach the backend')
        setStatus('anonymous')
      })
    return () => {
      cancelled = true
    }
  }, [])

  // A session that ends mid-visit shows up as a 401 on whatever call was in flight; any of them
  // sends the person back to the login page.
  useEffect(() => {
    return subscribeAuthExpired(() => {
      setUser(null)
      setStatus('anonymous')
    })
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const u = await apiLogin(username, password)
    setUser(u)
    setCheckError(null)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    // Whatever the server says (or if it can't be reached), the console closes.
    await apiLogout().catch(() => undefined)
    setUser(null)
    setStatus('anonymous')
  }, [])

  return { status, user, checkError, login, logout }
}
