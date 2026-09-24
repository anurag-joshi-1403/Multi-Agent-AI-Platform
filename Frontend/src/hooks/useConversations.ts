import { useCallback, useEffect, useState } from 'react'
import { loadJson, saveJson, STORAGE_KEYS } from '../lib/storage'
import { isRecord, truncate, uid } from '../lib/util'
import type { Attachment, ChatMessage, Conversation, MessageRole } from '../types'

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

// --- validation of what localStorage hands back -----------------------------
// Stored data outlives the code that wrote it and can be edited by hand. Rendering assumes these
// shapes (`c.messages.filter`, `m.attachments.map`, …), so it is checked at load time rather than
// crashing the console on every start with no way to reach Settings > Delete all. Only what is
// actually broken is dropped: a bad optional field loses that field, not the message around it.
const ROLES: ReadonlySet<string> = new Set<MessageRole>(['user', 'agent', 'error'])
const isString = (v: unknown): v is string => typeof v === 'string'
const isNumber = (v: unknown): v is number => typeof v === 'number' && Number.isFinite(v)
const isRole = (v: unknown): v is MessageRole => isString(v) && ROLES.has(v)
const isAttachment = (v: unknown): v is Attachment => isRecord(v) && isString(v.id) && isString(v.name)

function toMessage(v: unknown): ChatMessage | null {
  if (!isRecord(v)) return null
  const { id, role, content, createdAt, agentId, metadata, attributes, elapsedMs, editedAt, attachments } = v
  if (!isString(id) || !isRole(role) || !isString(content) || !isNumber(createdAt)) return null
  const files = Array.isArray(attachments) ? attachments.filter(isAttachment) : []
  return {
    id,
    role,
    content,
    createdAt,
    agentId: isString(agentId) ? agentId : undefined,
    metadata: isRecord(metadata) ? metadata : undefined,
    attributes: isRecord(attributes) ? attributes : undefined,
    elapsedMs: isNumber(elapsedMs) ? elapsedMs : undefined,
    editedAt: isNumber(editedAt) ? editedAt : undefined,
    attachments: files.length ? files : undefined,
  }
}

function toConversation(v: unknown): Conversation | null {
  if (!isRecord(v)) return null
  const { id, title, agentId, messages, createdAt, updatedAt } = v
  if (!isString(id) || !isString(agentId) || !Array.isArray(messages)) return null
  const created = isNumber(createdAt) ? createdAt : 0
  return {
    id,
    title: isString(title) && title.trim() ? title : 'Untitled conversation',
    agentId,
    messages: messages.map(toMessage).filter((m): m is ChatMessage => m !== null),
    createdAt: created,
    updatedAt: isNumber(updatedAt) ? updatedAt : created,
  }
}

function loadConversations(): Conversation[] {
  const raw = loadJson(STORAGE_KEYS.conversations)
  if (!Array.isArray(raw)) return []
  return raw.map(toConversation).filter((c): c is Conversation => c !== null)
}

export function useConversations(): ConversationsApi {
  const [conversations, setConversations] = useState<Conversation[]>(() => sortByUpdated(loadConversations()))

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
