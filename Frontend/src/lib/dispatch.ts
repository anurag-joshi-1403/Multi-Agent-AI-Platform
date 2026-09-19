import { runAgent } from '../api/client'
import type { ConversationsApi } from '../hooks/useConversations'
import { uid } from './util'

export interface DispatchOptions {
  /** When set, edits this existing user message in place instead of appending a new one (edit-and-resend). */
  replaceMessageId?: string
}

/**
 * Append the user's message (or, with `opts.replaceMessageId`, edit an existing one in place), run
 * the agent, then append either the agent's reply (with measured round-trip latency) or an error
 * message. Never throws.
 */
export async function dispatchMessage(
  conversations: ConversationsApi,
  conversationId: string,
  agentId: string,
  text: string,
  attributes: Record<string, unknown>,
  opts?: DispatchOptions,
): Promise<void> {
  if (opts?.replaceMessageId) {
    conversations.updateMessage(conversationId, opts.replaceMessageId, { content: text, editedAt: Date.now() })
  } else {
    conversations.appendMessage(conversationId, {
      id: uid(),
      role: 'user',
      content: text,
      createdAt: Date.now(),
      attributes,
    })
  }

  const started = performance.now()
  try {
    const res = await runAgent(agentId, { conversationId, message: text, attributes })
    conversations.appendMessage(conversationId, {
      id: uid(),
      role: 'agent',
      agentId: res.agentId,
      content: res.content,
      metadata: res.metadata,
      elapsedMs: performance.now() - started,
      createdAt: Date.now(),
    })
  } catch (err) {
    conversations.appendMessage(conversationId, {
      id: uid(),
      role: 'error',
      agentId,
      content: err instanceof Error ? err.message : 'Request failed',
      createdAt: Date.now(),
    })
  }
}
