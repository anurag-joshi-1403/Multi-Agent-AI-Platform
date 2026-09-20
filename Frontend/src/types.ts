/** One request attribute an agent understands (mirrors `AgentParameter` on the backend). */
export interface AgentParameter {
  name: string
  label: string
  description: string
  type: 'STRING' | 'NUMBER' | 'SELECT'
  required: boolean
  options: string[]
  defaultValue: string | number | null
}

/** Mirrors the backend `AgentSummary`. */
export interface AgentInfo {
  id: string
  name: string
  description: string
  capabilities: string[]
  parameters: AgentParameter[]
}

/** Mirrors `AgentRequest` on the backend. */
export interface AgentRequest {
  conversationId: string | null
  message: string
  attributes: Record<string, unknown>
}

/** Mirrors `RunAgentResponse` on the backend (`conversationId`/`elapsedMs` are optional for older servers). */
export interface AgentResponse {
  agentId: string
  content: string
  metadata: Record<string, unknown>
  conversationId?: string
  elapsedMs?: number
}

/** Mirrors `DocumentSummary` on the backend. */
export interface DocumentSummary {
  id: string
  name: string
  mediaType: string
  chars: number
  pages: number | null
  uploadedAt: string
  preview: string
}

/** Mirrors `PlatformStatus` on the backend (`GET /api/platform`). */
export interface PlatformStatus {
  provider: string
  providerName: string
  model: string
  apiKeyConfigured: boolean
  keyEnvVar: string
  agents: number
  memoryMaxMessages: number
  documents: { stored: number; maxStored: number; maxContextChars: number }
}

/** A file uploaded to the backend and attached to a message. */
export interface Attachment {
  id: string
  name: string
}

export type MessageRole = 'user' | 'agent' | 'error'

export interface ChatMessage {
  id: string
  role: MessageRole
  content: string
  createdAt: number
  /** Which agent produced this (agent/error messages only). */
  agentId?: string
  metadata?: Record<string, unknown>
  elapsedMs?: number
  /** Attributes that were sent with a user message, if any. */
  attributes?: Record<string, unknown>
  /** Files attached to this user message; they stay in context for the rest of the conversation. */
  attachments?: Attachment[]
  /** Set when a user message has been edited-and-resent; shows an "(edited)" tag. */
  editedAt?: number
}

export interface Conversation {
  id: string
  title: string
  agentId: string
  messages: ChatMessage[]
  createdAt: number
  updatedAt: number
}
