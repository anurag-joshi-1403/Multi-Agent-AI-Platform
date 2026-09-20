import { useCallback, useEffect, useState } from 'react'
import { loadJson, saveJson, STORAGE_KEYS } from '../lib/storage'
import { truncate, uid } from '../lib/util'
import type { ChatMessage, Conversation } from '../types'

export interface ConversationsApi {
  conversations: Conversation[]
  create: (agentId: string) => Conversation
  remove: (id: string) => void
  rename: (id: string, title: string) => void
  appendMessage: (id: string, message: ChatMessage) => void
  /** Replaces a message's content in place and stamps `editedAt` (used by edit-and-resend). */
  updateMessage: (id: string, messageId: string, patch: { content: string; editedAt: number }) => void
  /** Drops every message after (not including) `messageId` — clears the stale reply before a resend. */
  truncateAfter: (id: string, messageId: string) => void
  clearAll: () => void
}

function sortByUpdated(list: Conversation[]): Conversation[] {
  return [...list].sort((a, b) => b.updatedAt - a.updatedAt)
}

export function useConversations(): ConversationsApi {
  const [conversations, setConversations] = useState<Conversation[]>(() =>
    sortByUpdated(loadJson<Conversation[]>(STORAGE_KEYS.conversations, [])),
  )

  useEffect(() => {
    saveJson(STORAGE_KEYS.conversations, conversations)
  }, [conversations])

  const create = useCallback((agentId: string): Conversation => {
    const now = Date.now()
    const conv: Conversation = {
      id: uid(),
      title: 'New conversation',
      agentId,
      messages: [],
      createdAt: now,
      updatedAt: now,
    }
    setConversations((prev) => [conv, ...prev])
    return conv
  }, [])

  const remove = useCallback((id: string) => {
    setConversations((prev) => prev.filter((c) => c.id !== id))
  }, [])

  const patch = useCallback((id: string, fn: (c: Conversation) => Conversation) => {
    setConversations((prev) => sortByUpdated(prev.map((c) => (c.id === id ? fn(c) : c))))
  }, [])

  const rename = useCallback(
    (id: string, title: string) => patch(id, (c) => ({ ...c, title: title.trim() || c.title })),
    [patch],
  )

  const appendMessage = useCallback(
    (id: string, message: ChatMessage) =>
      patch(id, (c) => {
        const isFirstUserMessage = message.role === 'user' && !c.messages.some((m) => m.role === 'user')
        return {
          ...c,
          title: isFirstUserMessage ? truncate(message.content, 42) : c.title,
          messages: [...c.messages, message],
          updatedAt: message.createdAt,
        }
      }),
    [patch],
  )

  const updateMessage = useCallback(
    (id: string, messageId: string, messagePatch: { content: string; editedAt: number }) =>
      patch(id, (c) => ({
        ...c,
        messages: c.messages.map((m) => (m.id === messageId ? { ...m, ...messagePatch } : m)),
        updatedAt: messagePatch.editedAt,
      })),
    [patch],
  )

  const truncateAfter = useCallback(
    (id: string, messageId: string) =>
      patch(id, (c) => {
        const idx = c.messages.findIndex((m) => m.id === messageId)
        if (idx < 0) return c
        return { ...c, messages: c.messages.slice(0, idx + 1), updatedAt: Date.now() }
      }),
    [patch],
  )

  const clearAll = useCallback(() => setConversations([]), [])

  return { conversations, create, remove, rename, appendMessage, updateMessage, truncateAfter, clearAll }
}
