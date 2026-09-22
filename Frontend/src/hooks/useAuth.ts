import { useCallback, useEffect, useState } from 'react'
import { login as apiLogin, logout as apiLogout, me, subscribeAuthExpired } from '../api/client'
import type { AuthUser } from '../api/client'

export type AuthStatus = 'checking' | 'authenticated' | 'anonymous'

export interface AuthApi {
  status: AuthStatus
  user: AuthUser | null
  /** Set only when `status === 'checking'` failed outright (network down), not for a plain "not logged in". */
  checkError: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

/**
 * Gates the whole console: nothing renders until this resolves. The session cookie from a previous
 * visit is checked once on mount via `GET /api/auth/me`, so a refresh doesn't bounce a signed-in
 * user back to the login screen.
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
        setCheckError(err instanceof Error ? err.message : 'Could not reach the server')
        setStatus('anonymous')
      })
    return () => {
      cancelled = true
    }
  }, [])

  // A session that dies mid-visit (server restarted, timed out) surfaces as a 401 on whatever
  // call happened to be in flight — from deep inside PlaygroundPage, DocumentsController, anywhere.
  // Catching it centrally here is what sends the person back to the login screen instead of
  // leaving the console silently running on simulated replies.
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
    // Whatever the server call does — succeeds, 401s because the session was already gone, or
    // can't be reached at all — the UI has one job afterward: stop showing the console.
    await apiLogout().catch(() => undefined)
    setUser(null)
    setStatus('anonymous')
  }, [])

  return { status, user, checkError, login, logout }
}
