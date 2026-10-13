import { beforeEach, describe, expect, it, vi } from 'vitest'
import { runAgent } from '../api/client'
import type { ConversationsApi } from '../hooks/useConversations'
import { dispatchMessage } from './dispatch'

vi.mock('../api/client', () => ({ runAgent: vi.fn() }))

const run = vi.mocked(runAgent)

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
