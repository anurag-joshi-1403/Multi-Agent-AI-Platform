import { rewindConversation, runAgent } from '../api/client'
import type { ConversationsApi } from '../hooks/useConversations'
import type { Attachment, ChatMessage } from '../types'
import { uid } from './util'

export interface DispatchOptions {
  /** When set, edits this existing user message in place instead of appending a new one (edit-and-resend). */
  replaceMessageId?: string
  /** With `replaceMessageId`: how many turns the server forgets first (see `serverTurnsFrom`). */
  forgetTurns?: number
  /** Files attached to this turn. Their ids must already be in `attributes.attachments`. */
  attachments?: Attachment[]
}

/**
 * How many turns of the server's memory start at `messages[index]` or later: your messages that a
 * real backend reply followed. Failed and simulated runs never reached that memory, so they don't count.
 */
export function serverTurnsFrom(messages: ChatMessage[], index: number): number {
  let turns = 0
  for (let i = Math.max(0, index); i < messages.length - 1; i++) {
    const reply = messages[i + 1]
    if (messages[i].role === 'user' && reply.role === 'agent' && reply.metadata?.simulated !== true) turns++
  }
  return turns
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
      attachments: opts?.attachments?.length ? opts.attachments : undefined,
    })
  }
  // An edit replaces those turns, so the agent must not remember them when it answers
  if (opts?.forgetTurns) await rewindConversation(conversationId, opts.forgetTurns)

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
