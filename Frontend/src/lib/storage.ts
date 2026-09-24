/** localStorage helpers that never throw (private mode, blocked storage, quota). */

/**
 * Parsed JSON for `key`, or `undefined` when it is missing or unparsable. Deliberately `unknown`:
 * stored data can be stale or hand-edited, so callers validate its shape before using it.
 */
export function loadJson(key: string): unknown {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : undefined
  } catch {
    return undefined
  }
}

export function saveJson(key: string, value: unknown): void {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // ignore — storage is a convenience, not a requirement
  }
}

export function loadString(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

export function saveString(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch {
    // ignore
  }
}

export const STORAGE_KEYS = {
  conversations: 'maap.conversations',
  theme: 'maap.theme',
  apiBase: 'maap.apiBase',
  simulate: 'maap.simulate',
  sidebar: 'maap.sidebar',
  inspector: 'maap.inspector',
  rememberUser: 'maap.rememberUser',
} as const
