import type { AgentInfo, AgentRequest, AgentResponse, DocumentSummary, PlatformStatus, ProviderStatus } from '../types'
import { loadString, STORAGE_KEYS } from '../lib/storage'
import { isRecord, titleCase } from '../lib/util'
import { MOCK_AGENTS, MOCK_PLATFORM, mockDeleteDocument, mockRun, mockUploadDocument } from './mock'

/**
 * Backend contract (see Backend/README.md):
 *
 *   GET    /api/platform                 -> PlatformStatus
 *   GET    /api/agents                   -> AgentSummary[]
 *   POST   /api/agents/{id}/run          <- AgentRequest  -> RunAgentResponse
 *   GET    /api/documents                -> DocumentSummary[]
 *   POST   /api/documents (multipart)    -> DocumentSummary
 *   DELETE /api/documents/{id}
 *   DELETE /api/conversations/{id}       (forget server-side memory)
 *
 * Errors follow RFC 9457 problem+json.
 */

export type BackendStatus = 'checking' | 'online' | 'offline' | 'simulated'

export class ApiError extends Error {
  status: number
  title?: string
  /**
   * False when the error response carried no problem+json body. The backend always sends one, so
   * such a response came from whatever sits in front of it — e.g. the Vite dev proxy answers 502
   * with an empty body when nothing is listening on :8080.
   */
  fromBackend: boolean
  constructor(status: number, message: string, title?: string, fromBackend = true) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.fromBackend = fromBackend
  }
}

// --- tiny external store for backend status --------------------------------
let status: BackendStatus = 'checking'
const listeners = new Set<() => void>()

function setStatus(next: BackendStatus) {
  if (next === status) return
  status = next
  listeners.forEach((l) => l())
}

export function getBackendStatus(): BackendStatus {
  return status
}

export function subscribeBackendStatus(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

// --- config ----------------------------------------------------------------
/**
 * Same-origin by default: the Vite dev/preview server proxies `/api` to the backend, and a
 * deployment is expected to reverse-proxy it the same way (or set `VITE_API_BASE` at build time).
 * An absolute `http://localhost:8080/api` here would bypass that proxy — every call becomes
 * cross-origin and needs CORS on the backend, and a production bundle would point each visitor's
 * browser at their own machine.
 */
const DEFAULT_API_BASE = '/api'

/** What `apiBase()` resolves to when nothing is saved in Settings: `VITE_API_BASE`, else `/api`. */
export function defaultApiBase(): string {
  return (import.meta.env.VITE_API_BASE?.trim() || DEFAULT_API_BASE).replace(/\/+$/, '')
}

export function apiBase(): string {
  const stored = loadString(STORAGE_KEYS.apiBase)
  if (stored && stored.trim()) return stored.trim().replace(/\/+$/, '')
  return defaultApiBase()
}

/**
 * Accepts an empty value (use the default), a same-origin path like `/api`, or an absolute
 * http(s) URL. Anything else — e.g. `localhost:8080/api` without a scheme — would be resolved by
 * `fetch` as a path relative to the page and fail with a confusing 404.
 */
export function isValidApiBase(value: string): boolean {
  const v = value.trim()
  if (v === '') return true
  if (v.startsWith('/')) return !v.startsWith('//')
  try {
    const url = new URL(v)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

export function simulationForced(): boolean {
  return loadString(STORAGE_KEYS.simulate) === 'true'
}

/** True when calls should be answered by the in-browser simulation instead of the server. */
function simulated(): boolean {
  return status !== 'online'
}

// --- http ------------------------------------------------------------------
async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (init?.body && !(init.body instanceof FormData)) headers['Content-Type'] = 'application/json'
  const res = await fetch(apiBase() + path, {
    ...init,
    headers: { ...headers, ...(init?.headers ?? {}) },
  })
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`
    let title: string | undefined
    let fromBackend = false
    try {
      // RFC 9457 problem+json from the backend; anything else (proxy error page, empty body) keeps
      // the status line. Read defensively — only non-empty strings make it into the message.
      const problem: unknown = await res.json()
      if (isRecord(problem)) {
        fromBackend = true
        const text = (value: unknown) => (typeof value === 'string' && value.trim() ? value : undefined)
        detail = text(problem.detail) ?? text(problem.message) ?? text(problem.title) ?? detail
        title = text(problem.title)
      }
    } catch {
      // non-JSON body
    }
    if (!fromBackend && res.status >= 500) detail = `The backend could not be reached (HTTP ${res.status}).`
    throw new ApiError(res.status, detail, title, fromBackend)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

function normalizeAgent(raw: Partial<AgentInfo> & { id: string }): AgentInfo {
  return {
    id: raw.id,
    name: raw.name ?? `${titleCase(raw.id)} Agent`,
    description: raw.description ?? '',
    capabilities: raw.capabilities ?? [],
    parameters: raw.parameters ?? [],
  }
}

function invalidResponse(what: string): ApiError {
  return new ApiError(502, `The backend returned ${what} in an unexpected format.`, 'Invalid response')
}

/**
 * The run response is rendered as markdown and persisted to localStorage with the transcript, so a
 * malformed body is rejected here instead of crashing the thread (and every later page load).
 */
function normalizeRunResponse(raw: unknown, agentId: string): AgentResponse {
  if (!isRecord(raw) || typeof raw.content !== 'string') throw invalidResponse('a reply')
  return {
    agentId: typeof raw.agentId === 'string' ? raw.agentId : agentId,
    content: raw.content,
    metadata: isRecord(raw.metadata) ? raw.metadata : {},
    conversationId: typeof raw.conversationId === 'string' ? raw.conversationId : undefined,
    elapsedMs: typeof raw.elapsedMs === 'number' ? raw.elapsedMs : undefined,
  }
}

function isProviderStatus(value: unknown): value is ProviderStatus {
  return (
    isRecord(value) &&
    typeof value.provider === 'string' &&
    typeof value.providerName === 'string' &&
    typeof value.model === 'string' &&
    typeof value.active === 'boolean' &&
    typeof value.keyEnvVar === 'string'
  )
}

/** The backend evolves separately (`providers` is already optional), so read `/platform` defensively. */
function normalizePlatform(raw: unknown): PlatformStatus {
  if (!isRecord(raw) || !isRecord(raw.documents)) throw invalidResponse('platform details')
  const text = (value: unknown) => (typeof value === 'string' ? value : '')
  const count = (value: unknown) => (typeof value === 'number' ? value : 0)
  const docs = raw.documents
  return {
    provider: text(raw.provider),
    providerName: text(raw.providerName),
    model: text(raw.model),
    apiKeyConfigured: raw.apiKeyConfigured === true,
    keyEnvVar: text(raw.keyEnvVar),
    agents: count(raw.agents),
    memoryMaxMessages: count(raw.memoryMaxMessages),
    documents: { stored: count(docs.stored), maxStored: count(docs.maxStored), maxContextChars: count(docs.maxContextChars) },
    providers: Array.isArray(raw.providers) ? raw.providers.filter(isProviderStatus) : undefined,
  }
}

// --- agents ----------------------------------------------------------------
export async function listAgents(): Promise<AgentInfo[]> {
  if (simulationForced()) {
    setStatus('simulated')
    return MOCK_AGENTS
  }
  try {
    const agents = await http<Array<Partial<AgentInfo> & { id: string }>>('/agents')
    setStatus('online')
    return agents.map(normalizeAgent)
  } catch {
    // Either unreachable, or the server answered unhappily (e.g. 404 until the controller
    // exists). Fall back so the UI stays usable either way.
    setStatus('offline')
    return MOCK_AGENTS
  }
}

export async function runAgent(agentId: string, request: AgentRequest): Promise<AgentResponse> {
  // The page may have loaded (or lost the backend mid-visit) while the Spring Boot app was still
  // starting. Probe again before answering with a simulated reply, so a backend that has come up
  // since is used without the person having to refresh or press Retry.
  if (simulated() && !simulationForced()) await listAgents()
  if (simulated()) return mockRun(agentId, request)
  try {
    const raw = await http<unknown>(`/agents/${encodeURIComponent(agentId)}/run`, {
      method: 'POST',
      body: JSON.stringify(request),
    })
    return normalizeRunResponse(raw, agentId)
  } catch (err) {
    // Rethrow what the backend itself answered (bad key, rate limit, unknown agent, …). A 5xx with no
    // problem body came from the proxy in front of it, i.e. the backend is down: same as a network error.
    if (err instanceof ApiError && (err.fromBackend || err.status < 500)) throw err
    setStatus('offline')
    throw new Error('Backend unreachable. Switched to simulation mode — send again to get a simulated reply.', {
      cause: err,
    })
  }
}

/** Re-probe the backend (used by Settings and the status pill). */
export async function refreshBackend(): Promise<AgentInfo[]> {
  setStatus('checking')
  return listAgents()
}

// --- platform --------------------------------------------------------------
export async function getPlatform(): Promise<PlatformStatus> {
  // Copy `documents` too: the simulated store updates MOCK_PLATFORM.documents in place, which would
  // otherwise mutate a platform object React already holds in state.
  if (simulated()) return { ...MOCK_PLATFORM, documents: { ...MOCK_PLATFORM.documents }, agents: MOCK_AGENTS.length }
  return normalizePlatform(await http<unknown>('/platform'))
}

// --- documents -------------------------------------------------------------
export async function uploadDocument(file: File): Promise<DocumentSummary> {
  if (simulated()) return mockUploadDocument(file)
  const form = new FormData()
  form.append('file', file, file.name)
  return http<DocumentSummary>('/documents', { method: 'POST', body: form })
}

export async function deleteDocument(id: string): Promise<void> {
  if (simulated()) return mockDeleteDocument(id)
  await http<void>(`/documents/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

// --- conversations ---------------------------------------------------------
/** Forget a conversation's server-side memory. Best effort: never throws. */
export async function forgetConversation(id: string): Promise<void> {
  if (simulated()) return
  try {
    await http<void>(`/conversations/${encodeURIComponent(id)}`, { method: 'DELETE' })
  } catch {
    // the client-side transcript is already gone; a stale memory window is harmless
  }
}
