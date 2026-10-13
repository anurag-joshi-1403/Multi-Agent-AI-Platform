import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { conversationsKey } from '../lib/storage'
import { memoryStorage } from '../test/fakes'
import { loadConversations } from './useConversations'

const chat = (id: string, title: string) => ({
  id,
  title,
  agentId: 'general',
  messages: [{ id: `${id}-m1`, role: 'user', content: title, createdAt: 1 }],
  createdAt: 1,
  updatedAt: 1,
})

let storage: Storage

beforeEach(() => {
  storage = memoryStorage()
  vi.stubGlobal('localStorage', storage)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('loadConversations', () => {
  it("reads only the signed-in account's chats", () => {
    storage.setItem(conversationsKey('anna'), JSON.stringify([chat('a1', "Anna's chat")]))
    storage.setItem(conversationsKey('ben'), JSON.stringify([chat('b1', "Ben's chat")]))

    const { list, fromShared } = loadConversations(conversationsKey('anna'))

    expect(list.map((c) => c.title)).toEqual(["Anna's chat"])
    expect(fromShared).toBe(false)
  })

  it('gives the chats saved before accounts to an account that has none yet', () => {
    storage.setItem('maap.conversations', JSON.stringify([chat('old', 'Chat from before')]))

    const { list, fromShared } = loadConversations(conversationsKey('anna'))

    expect(list.map((c) => c.title)).toEqual(['Chat from before'])
    expect(fromShared).toBe(true)
    // Only read here: the hook removes the shared list once it has saved the chats under the account
    expect(storage.getItem('maap.conversations')).not.toBeNull()
  })

  it('leaves the old shared list alone for an account with its own chats, even an empty list', () => {
    storage.setItem('maap.conversations', JSON.stringify([chat('old', 'Chat from before')]))
    storage.setItem(conversationsKey('anna'), JSON.stringify([chat('a1', "Anna's chat")]))
    storage.setItem(conversationsKey('ben'), '[]')

    expect(loadConversations(conversationsKey('anna')).list.map((c) => c.title)).toEqual(["Anna's chat"])
    expect(loadConversations(conversationsKey('ben'))).toEqual({ list: [], fromShared: false })
  })

  it('starts empty with nothing saved, and drops entries that are broken', () => {
    expect(loadConversations(conversationsKey('new'))).toEqual({ list: [], fromShared: false })

    const broken = [chat('ok', 'Fine'), { title: 'no id' }, { ...chat('m', 'Bad message'), messages: [{ role: 'robot' }] }]
    storage.setItem(conversationsKey('anna'), JSON.stringify(broken))

    const { list } = loadConversations(conversationsKey('anna'))
    expect(list.map((c) => c.title)).toEqual(['Fine', 'Bad message'])
    expect(list[1].messages).toEqual([])
  })
})
