import { runAgent } from '../api/client'
import type { ConversationsApi } from '../hooks/useConversations'
import { uid } from './util'

/**
 * Append the user's message, run the agent, then append either the agent's
 * reply (with measured round-trip latency) or an error message. Never throws.
 */
export async function dispatchMessage(
  conversations: ConversationsApi,
  conversationId: string,
  agentId: string,
  text: string,
  attributes: Record<string, unknown>,
): Promise<void> {
  conversations.appendMessage(conversationId, {
    id: uid(),
    role: 'user',
    content: text,
    createdAt: Date.now(),
    attributes,
  })

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
