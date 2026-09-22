/** localStorage helpers that never throw (private mode, blocked storage, quota). */

export function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
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
