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
  /** The one shared chat list from before chats were kept per account; see `conversationsKey`. */
  conversations: 'maap.conversations',
  theme: 'maap.theme',
  apiBase: 'maap.apiBase',
  simulate: 'maap.simulate',
  sidebar: 'maap.sidebar',
  inspector: 'maap.inspector',
  rememberUser: 'maap.rememberUser',
} as const

/**
 * Where one account's chats are saved, so two people on one browser each see only their own.
 * This keeps them apart in the console only: anyone with the browser's dev tools can read them all.
 */
export function conversationsKey(username: string): string {
  return `${STORAGE_KEYS.conversations}.${username}`
}

/** Delete every account's saved chats in this browser, and the old shared list. */
export function clearAllConversations(): void {
  try {
    const keys = Array.from({ length: localStorage.length }, (_, i) => localStorage.key(i))
    for (const key of keys) {
      if (key === STORAGE_KEYS.conversations || key?.startsWith(`${STORAGE_KEYS.conversations}.`)) {
        localStorage.removeItem(key)
      }
    }
  } catch {
    // ignore — nothing was readable, so nothing can be cleared either
  }
}
