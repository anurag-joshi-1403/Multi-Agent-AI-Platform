/** Stand-ins for browser APIs and backend answers, shared by the unit tests. */

/** An in-memory localStorage. */
export function memoryStorage(): Storage {
  const items = new Map<string, string>()
  return {
    get length() {
      return items.size
    },
    clear: () => items.clear(),
    getItem: (key) => items.get(key) ?? null,
    key: (index) => [...items.keys()][index] ?? null,
    removeItem: (key) => void items.delete(key),
    setItem: (key, value) => void items.set(key, String(value)),
  }
}

/** localStorage when the browser blocks it (private mode, disabled cookies): every call throws. */
export function blockedStorage(): Storage {
  const blocked = () => {
    throw new DOMException('The operation is insecure.', 'SecurityError')
  }
  return { length: 0, clear: blocked, getItem: blocked, key: blocked, removeItem: blocked, setItem: blocked }
}

/** A JSON answer, as the backend sends it. */
export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

/** An RFC 9457 error, as the backend's ApiExceptionHandler sends it. */
export function problem(status: number, title: string, detail: string): Response {
  return new Response(JSON.stringify({ type: `https://multi-agent-ai-platform/problems/${status}`, title, status, detail }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

/** What the Vite dev proxy answers when nothing listens on :8080: a 502 with an empty body. */
export function proxyDown(): Response {
  return new Response(null, { status: 502, statusText: 'Bad Gateway' })
}
