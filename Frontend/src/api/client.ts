import type { AgentInfo, AgentRequest, AgentResponse, DocumentSummary, PlatformStatus } from '../types'
import { loadString, STORAGE_KEYS } from '../lib/storage'
import { titleCase } from '../lib/util'
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
  constructor(status: number, message: string, title?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = title
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
export function apiBase(): string {
  const stored = loadString(STORAGE_KEYS.apiBase)
  if (stored && stored.trim()) return stored.trim().replace(/\/+$/, '')
  const env = import.meta.env.VITE_API_BASE as string | undefined
  return (env ?? '/api').replace(/\/+$/, '')
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
  const res = await fetch(apiBase() + path, { ...init, headers: { ...headers, ...(init?.headers ?? {}) } })
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`
    let title: string | undefined
    try {
      const problem = (await res.json()) as { detail?: string; title?: string; message?: string }
      detail = problem.detail ?? problem.message ?? problem.title ?? detail
      title = problem.title
    } catch {
      // non-JSON body
    }
    throw new ApiError(res.status, detail, title)
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
    // Either unreachable, or the server answered unhappily (401 from Spring Security,
    // 404 until the controller exists). Fall back so the UI stays usable either way.
    setStatus('offline')
    return MOCK_AGENTS
  }
}

export async function runAgent(agentId: string, request: AgentRequest): Promise<AgentResponse> {
  if (simulated()) return mockRun(agentId, request)
  try {
    return await http<AgentResponse>(`/agents/${encodeURIComponent(agentId)}/run`, {
      method: 'POST',
      body: JSON.stringify(request),
    })
  } catch (err) {
    if (err instanceof ApiError) throw err
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
  if (simulated()) return { ...MOCK_PLATFORM, agents: MOCK_AGENTS.length }
  return http<PlatformStatus>('/platform')
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
