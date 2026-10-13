import { beforeEach, describe, expect, it, vi } from 'vitest'
import { rewindConversation, runAgent } from '../api/client'
import type { ConversationsApi } from '../hooks/useConversations'
import type { ChatMessage } from '../types'
import { dispatchMessage, serverTurnsFrom } from './dispatch'

vi.mock('../api/client', () => ({ runAgent: vi.fn(), rewindConversation: vi.fn() }))

const run = vi.mocked(runAgent)
const rewind = vi.mocked(rewindConversation)

function fakeConversations() {
  return {
    appendMessage: vi.fn<ConversationsApi['appendMessage']>(),
    updateMessage: vi.fn<ConversationsApi['updateMessage']>(),
  }
}

/** Only the two methods dispatchMessage uses. */
const asApi = (c: ReturnType<typeof fakeConversations>) => c as unknown as ConversationsApi

beforeEach(() => {
  run.mockReset()
  rewind.mockReset()
})

describe('serverTurnsFrom', () => {
  const msg = (id: string, role: ChatMessage['role'], metadata?: Record<string, unknown>): ChatMessage => ({
    id,
    role,
    content: id,
    createdAt: 0,
    metadata,
  })
  // A real reply, a failed run, a simulated reply, a real reply, then a message still waiting
  const transcript = [
    msg('q1', 'user'),
    msg('a1', 'agent', { model: 'm' }),
    msg('q2', 'user'),
    msg('e2', 'error'),
    msg('q3', 'user'),
    msg('a3', 'agent', { simulated: true }),
    msg('q4', 'user'),
    msg('a4', 'agent', {}),
    msg('q5', 'user'),
  ]

  it('counts only turns a real backend reply answered', () => {
    expect(serverTurnsFrom(transcript, 0)).toBe(2)
    expect(serverTurnsFrom(transcript, 2)).toBe(1)
    expect(serverTurnsFrom(transcript, 6)).toBe(1)
  })

  it('is 0 when the server never saw the edited turn or anything after it', () => {
    expect(serverTurnsFrom(transcript, 8)).toBe(0)
    expect(serverTurnsFrom(transcript.slice(2, 6), 0)).toBe(0)
    expect(serverTurnsFrom([], 0)).toBe(0)
  })
})

describe('dispatchMessage', () => {
  it('adds your message, runs the agent, then adds its reply with the latency', async () => {
    run.mockResolvedValue({ agentId: 'coding', content: 'Here you go.', metadata: { model: 'm' } })
    const c = fakeConversations()
    const files = [{ id: 'doc_1', name: 'notes.pdf' }]

    await dispatchMessage(asApi(c), 'conv-1', 'coding', 'Write a loop', { language: 'java' }, { attachments: files })

    expect(c.appendMessage).toHaveBeenCalledTimes(2)
    const [, mine] = c.appendMessage.mock.calls[0]
    expect(mine).toMatchObject({ role: 'user', content: 'Write a loop', attributes: { language: 'java' }, attachments: files })
    expect(run).toHaveBeenCalledWith('coding', {
      conversationId: 'conv-1',
      message: 'Write a loop',
      attributes: { language: 'java' },
    })
    const [, reply] = c.appendMessage.mock.calls[1]
    expect(reply).toMatchObject({ role: 'agent', agentId: 'coding', content: 'Here you go.', metadata: { model: 'm' } })
    expect(reply.elapsedMs).toBeGreaterThanOrEqual(0)
    expect(reply.id).not.toBe(mine.id)
  })

  it('leaves attachments off a message sent without files', async () => {
    run.mockResolvedValue({ agentId: 'general', content: 'Hi', metadata: {} })
    const c = fakeConversations()

    await dispatchMessage(asApi(c), 'conv-1', 'general', 'Hello', {}, { attachments: [] })

    expect(c.appendMessage.mock.calls[0][1].attachments).toBeUndefined()
  })

  it('edits the message in place when resending an edited one', async () => {
    run.mockResolvedValue({ agentId: 'general', content: 'New answer', metadata: {} })
    const c = fakeConversations()

    await dispatchMessage(asApi(c), 'conv-1', 'general', 'Edited text', {}, { replaceMessageId: 'msg-7' })

    expect(c.updateMessage).toHaveBeenCalledWith('conv-1', 'msg-7', { content: 'Edited text', editedAt: expect.any(Number) })
    expect(c.appendMessage).toHaveBeenCalledTimes(1)
    expect(c.appendMessage.mock.calls[0][1]).toMatchObject({ role: 'agent', content: 'New answer' })
    expect(rewind).not.toHaveBeenCalled()
  })

  it('makes the server forget the replaced turns before the agent answers the edit', async () => {
    run.mockResolvedValue({ agentId: 'general', content: 'New answer', metadata: {} })
    const c = fakeConversations()

    await dispatchMessage(asApi(c), 'conv-1', 'general', 'Edited text', {}, { replaceMessageId: 'msg-7', forgetTurns: 2 })

    expect(rewind).toHaveBeenCalledWith('conv-1', 2)
    expect(rewind.mock.invocationCallOrder[0]).toBeLessThan(run.mock.invocationCallOrder[0])
  })

  it('turns a failed run into an error message instead of throwing', async () => {
    run.mockRejectedValue(new Error('Backend unreachable.'))
    const c = fakeConversations()

    await expect(dispatchMessage(asApi(c), 'conv-1', 'general', 'Hello', {})).resolves.toBeUndefined()

    expect(c.appendMessage.mock.calls[1][1]).toMatchObject({ role: 'error', agentId: 'general', content: 'Backend unreachable.' })
  })

  it('has a fallback message when the failure is not an Error', async () => {
    run.mockRejectedValue('boom')
    const c = fakeConversations()

    await dispatchMessage(asApi(c), 'conv-1', 'general', 'Hello', {})

    expect(c.appendMessage.mock.calls[1][1]).toMatchObject({ role: 'error', content: 'Request failed' })
  })
})
