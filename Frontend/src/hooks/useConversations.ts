import { useCallback, useEffect, useState } from 'react'
import { loadJson, saveJson, STORAGE_KEYS } from '../lib/storage'
import { truncate, uid } from '../lib/util'
import type { ChatMessage, Conversation } from '../types'

export interface ConversationsApi {
  conversations: Conversation[]
  create: (agentId: string) => Conversation
  remove: (id: string) => void
  rename: (id: string, title: string) => void
  setAgent: (id: string, agentId: string) => void
  appendMessage: (id: string, message: ChatMessage) => void
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

  const setAgent = useCallback(
    (id: string, agentId: string) => patch(id, (c) => ({ ...c, agentId })),
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

  const clearAll = useCallback(() => setConversations([]), [])

  return { conversations, create, remove, rename, setAgent, appendMessage, clearAll }
}
