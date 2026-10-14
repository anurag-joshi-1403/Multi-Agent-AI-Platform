import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { json, memoryStorage, problem, proxyDown } from '../test/fakes'
import type { AgentRequest } from '../types'

// The client keeps the backend status and its listeners in module state, so every test loads a
// fresh copy (and of the simulation it falls back to).
let client: typeof import('./client')
let storage: Storage
const fetchMock = vi.fn<typeof fetch>()

/** Answers `METHOD url` from `routes`; any other call fails like a server that is not there. */
function serve(routes: Record<string, () => Response>) {
  fetchMock.mockImplementation(async (input, init) => {
    const route = routes[`${init?.method ?? 'GET'} ${String(input)}`]
    if (!route) throw new TypeError('Failed to fetch')
    return route()
  })
}

const AGENTS = [{ id: 'general', name: 'General Assistant', description: 'Answers anything.', capabilities: [], parameters: [] }]
const ONLINE = { 'GET /api/agents': () => json(200, AGENTS) }
const REQUEST: AgentRequest = { conversationId: 'conv-1', message: 'hello', attributes: {} }
const PLATFORM = {
  provider: 'groq',
  providerName: 'Groq',
  model: 'llama-3.3-70b-versatile',
  apiKeyConfigured: true,
  keyEnvVar: 'GROQ_API_KEY',
  agents: 1,
  memoryMaxMessages: 20,
  documents: { stored: 0, maxStored: 50, maxContextChars: 60000 },
}
/** The backend's answer to a call without a session (SecurityConfig's entry point). */
const signInFirst = () => problem(401, 'Authentication required', 'Sign in to use this endpoint.')

/** Awaits a call that ends in a simulated reply, skipping the simulation's 0.5-1.4 s pause. */
async function withoutPause<T>(call: Promise<T>): Promise<T> {
  await vi.advanceTimersByTimeAsync(1500)
  return call
}

beforeEach(async () => {
  vi.resetModules()
  storage = memoryStorage()
  vi.stubGlobal('localStorage', storage)
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  client = await import('./client')
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('API address', () => {
  it('uses /api unless a valid address is saved', () => {
    expect(client.apiBase()).toBe('/api')
    storage.setItem('maap.apiBase', '/backend/api/')
    expect(client.apiBase()).toBe('/backend/api')
    storage.setItem('maap.apiBase', 'https://agents.example.com/api')
    expect(client.apiBase()).toBe('https://agents.example.com/api')
  })

  it('drops a saved address that can never work, such as a pasted API key', () => {
    for (const bad of ['AIzaFAKE-not-a-real-key', 'localhost:8080/api', '//elsewhere.example/api']) {
      storage.setItem('maap.apiBase', bad)
      expect(client.apiBase()).toBe('/api')
      expect(storage.getItem('maap.apiBase')).toBeNull()
    }
  })

  it('accepts a path or an http(s) URL in Settings', () => {
    expect(client.isValidApiBase('')).toBe(true)
    expect(client.isValidApiBase('/api')).toBe(true)
    expect(client.isValidApiBase('http://localhost:8080/api')).toBe(true)
    expect(client.isValidApiBase('//elsewhere.example')).toBe(false)
    expect(client.isValidApiBase('ftp://files.example')).toBe(false)
    expect(client.isValidApiBase('localhost:8080/api')).toBe(false)
  })
})

describe('when the backend is down', () => {
  it('lists the simulated agents and reports offline', async () => {
    serve({})
    const seen: string[] = []
    client.subscribeBackendStatus(() => seen.push(client.getBackendStatus()))

    const agents = await client.listAgents()

    expect(agents.map((a) => a.id)).toContain('general')
    expect(client.getBackendStatus()).toBe('offline')
    expect(seen).toEqual(['offline'])
  })

  it("reads the dev proxy's empty 502 as offline too", async () => {
    serve({ 'GET /api/agents': proxyDown })
    await client.listAgents()
    expect(client.getBackendStatus()).toBe('offline')
  })

  it('answers with a simulated reply that says the backend could not be reached', async () => {
    serve({})
    await client.listAgents()
    vi.useFakeTimers()

    const reply = await withoutPause(client.runAgent('general', REQUEST))

    expect(reply.metadata.simulated).toBe(true)
    expect(reply.content).toContain('the backend could not be reached')
    // It looked for the backend again first, and never sent the message to it
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(['/api/agents', '/api/agents'])
  })

  it('uses the backend again as soon as it is back, without a refresh', async () => {
    serve({})
    await client.listAgents()
    serve({ ...ONLINE, 'POST /api/agents/general/run': () => json(200, { agentId: 'general', content: 'A real reply.', metadata: {} }) })

    const reply = await client.runAgent('general', REQUEST)

    expect(reply.content).toBe('A real reply.')
    expect(client.getBackendStatus()).toBe('online')
  })

  it('says so when the backend drops in the middle of a chat', async () => {
    serve(ONLINE)
    await client.listAgents()
    serve({})

    await expect(client.runAgent('general', REQUEST)).rejects.toThrow(
      'Backend unreachable. Send again to get a simulated reply, or a real one once it is back.',
    )
    expect(client.getBackendStatus()).toBe('offline')
  })

  it("treats the proxy's empty 502 in the middle of a chat the same way", async () => {
    serve({ ...ONLINE, 'POST /api/agents/general/run': proxyDown })
    await client.listAgents()

    await expect(client.runAgent('general', REQUEST)).rejects.toThrow('Backend unreachable.')
    expect(client.getBackendStatus()).toBe('offline')
  })
})

describe('Simulation mode (Settings)', () => {
  beforeEach(() => {
    storage.setItem('maap.simulate', 'true')
  })

  it('never calls the backend, and the reply says why it is simulated', async () => {
    serve(ONLINE)
    await client.listAgents()
    expect(client.getBackendStatus()).toBe('simulated')
    vi.useFakeTimers()

    const reply = await withoutPause(client.runAgent('general', REQUEST))

    expect(reply.content).toContain('is on in Settings')
    expect(reply.content).toContain('Apply & reconnect')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('hands out a copy of the simulated platform details', async () => {
    const first = await client.getPlatform()
    first.documents.stored = 99
    expect((await client.getPlatform()).documents.stored).toBe(0)
  })
})

describe('with the backend up', () => {
  it('lists the agents and fills in a missing name', async () => {
    serve({ 'GET /api/agents': () => json(200, [...AGENTS, { id: 'code-review' }]) })

    const agents = await client.listAgents()

    expect(client.getBackendStatus()).toBe('online')
    expect(agents[1]).toEqual({ id: 'code-review', name: 'Code Review Agent', description: '', capabilities: [], parameters: [] })
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/agents')
    expect(init?.headers).toEqual({ Accept: 'application/json' })
  })

  it('sends a message as JSON and returns the reply', async () => {
    const answer = { agentId: 'general', content: 'Hi!', metadata: { provider: 'Groq' }, conversationId: 'conv-1', elapsedMs: 840 }
    serve({ ...ONLINE, 'POST /api/agents/general/run': () => json(200, answer) })
    await client.listAgents()

    await expect(client.runAgent('general', REQUEST)).resolves.toEqual(answer)

    const [url, init] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/agents/general/run')
    expect(init?.headers).toEqual({ Accept: 'application/json', 'Content-Type': 'application/json' })
    expect(JSON.parse(String(init?.body))).toEqual(REQUEST)
  })

  it('lets the browser set the multipart header for an upload', async () => {
    const summary = { id: 'doc_1', name: 'notes.txt', mediaType: 'text/plain', chars: 5, pages: null, uploadedAt: '2026-10-11T00:00:00Z', preview: 'hello' }
    serve({ ...ONLINE, 'POST /api/documents': () => json(201, summary) })
    await client.listAgents()

    await expect(client.uploadDocument(new File(['hello'], 'notes.txt', { type: 'text/plain' }))).resolves.toEqual(summary)

    const [, init] = fetchMock.mock.calls[1]
    expect(init?.body).toBeInstanceOf(FormData)
    expect(init?.headers).toEqual({ Accept: 'application/json' })
  })

  it('refuses a reply in an unexpected shape instead of showing it', async () => {
    serve({ ...ONLINE, 'POST /api/agents/general/run': () => json(200, { text: 'no content field' }) })
    await client.listAgents()

    await expect(client.runAgent('general', REQUEST)).rejects.toThrow('The backend returned a reply in an unexpected format.')
  })

  it('keeps only well-formed providers from /api/platform', async () => {
    const groq = { provider: 'groq', providerName: 'Groq', model: 'llama-3.3-70b-versatile', active: true, keyEnvVar: 'GROQ_API_KEY' }
    serve({ ...ONLINE, 'GET /api/platform': () => json(200, { ...PLATFORM, providers: [groq, { provider: 'broken' }] }) })
    await client.listAgents()

    expect((await client.getPlatform()).providers).toEqual([groq])
    serve({ ...ONLINE, 'GET /api/platform': () => json(200, { provider: 'groq' }) })
    await expect(client.getPlatform()).rejects.toThrow('The backend returned platform details in an unexpected format.')
  })

  it('forgets a conversation on the server, quietly if that fails', async () => {
    serve({ ...ONLINE, 'DELETE /api/conversations/conv-1': () => problem(500, 'Internal error', 'Something went wrong.') })
    await client.listAgents()

    await expect(client.forgetConversation('conv-1')).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenLastCalledWith('/api/conversations/conv-1', expect.objectContaining({ method: 'DELETE' }))
  })

  it('asks the server to forget the latest turns before an edit is resent', async () => {
    serve({ ...ONLINE, 'POST /api/conversations/conv-1/rewind': () => new Response(null, { status: 204 }) })
    await client.listAgents()

    await expect(client.rewindConversation('conv-1', 2)).resolves.toBeUndefined()

    const [url, init] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/conversations/conv-1/rewind')
    expect(init?.headers).toEqual({ Accept: 'application/json', 'Content-Type': 'application/json' })
    expect(JSON.parse(String(init?.body))).toEqual({ turns: 2 })
  })

  it('lets an edit go ahead when the rewind fails, e.g. on an older backend', async () => {
    serve({ ...ONLINE, 'POST /api/conversations/conv-1/rewind': () => problem(404, 'Not Found', 'No endpoint') })
    await client.listAgents()

    await expect(client.rewindConversation('conv-1', 1)).resolves.toBeUndefined()
  })

  it('does not call the server for nothing to forget, or in Simulation mode', async () => {
    serve(ONLINE)
    await client.listAgents()
    await client.rewindConversation('conv-1', 0)
    storage.setItem('maap.simulate', 'true')
    await client.listAgents()
    await client.rewindConversation('conv-1', 2)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(['/api/agents'])
  })
})

describe('a 401 sends you to the login page', () => {
  it('tells the login gate about a 401 from any call', async () => {
    serve({ ...ONLINE, 'GET /api/platform': signInFirst, 'POST /api/agents/general/run': signInFirst })
    await client.listAgents()
    const expired = vi.fn()
    const stop = client.subscribeAuthExpired(expired)

    await expect(client.getPlatform()).rejects.toMatchObject({ status: 401 })
    await expect(client.runAgent('general', REQUEST)).rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledTimes(2)

    stop()
    await expect(client.getPlatform()).rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledTimes(2)
  })

  it('reads "not signed in" as no user, and an unreachable backend as an error', async () => {
    serve({ 'GET /api/auth/me': signInFirst })
    await expect(client.me()).resolves.toBeNull()

    serve({ 'GET /api/auth/me': () => json(200, { username: 'anna' }) })
    await expect(client.me()).resolves.toEqual({ username: 'anna' })

    serve({})
    await expect(client.me()).rejects.toThrow('Could not reach the backend. Start it, then try again.')
  })
})

describe('sign-up', () => {
  const created = () => json(201, { username: 'anna' })
  const sentBody = () => JSON.parse(String(fetchMock.mock.calls.at(-1)?.[1]?.body))

  it('sends the invite code only when one was typed', async () => {
    serve({ 'POST /api/auth/signup': created })

    await client.signup('anna', 'a-long-password', '  let-me-in ')
    expect(sentBody()).toEqual({ username: 'anna', password: 'a-long-password', inviteCode: 'let-me-in' })

    await client.signup('anna', 'a-long-password', '   ')
    expect(sentBody()).toEqual({ username: 'anna', password: 'a-long-password' })

    await client.signup('anna', 'a-long-password')
    expect(sentBody()).toEqual({ username: 'anna', password: 'a-long-password' })
  })

  it("shows the backend's words when an invite code is required", async () => {
    serve({
      'POST /api/auth/signup': () =>
        problem(403, 'Invite code required', 'Sign-up needs a valid invite code. Ask whoever runs this console for one.'),
    })

    await expect(client.signup('anna', 'a-long-password')).rejects.toMatchObject({
      status: 403,
      message: 'Sign-up needs a valid invite code. Ask whoever runs this console for one.',
    })
  })
})

describe('error messages', () => {
  it("shows the backend's problem detail, with its title", async () => {
    const detail = 'Authentication with Groq failed. Set GROQ_API_KEY in Backend/.env and restart the backend.'
    serve({ ...ONLINE, 'POST /api/agents/general/run': () => problem(502, 'LLM provider rejected the credentials', detail) })
    await client.listAgents()

    const err = await client.runAgent('general', REQUEST).catch((e: unknown) => e)

    expect(err).toBeInstanceOf(client.ApiError)
    expect(err).toMatchObject({ status: 502, message: detail, title: 'LLM provider rejected the credentials', fromBackend: true })
    // The backend itself answered, so it is not offline
    expect(client.getBackendStatus()).toBe('online')
  })

  it('falls back to message, then title, skipping blank fields', async () => {
    serve({ ...ONLINE, 'GET /api/platform': () => json(400, { message: 'Old-style message', detail: ' ' }) })
    await client.listAgents()
    await expect(client.getPlatform()).rejects.toThrow('Old-style message')

    serve({ ...ONLINE, 'GET /api/platform': () => json(400, { title: 'Only a title' }) })
    await expect(client.getPlatform()).rejects.toThrow('Only a title')
  })

  it('says the backend could not be reached for an empty 5xx', async () => {
    serve({ ...ONLINE, 'GET /api/platform': proxyDown })
    await client.listAgents()

    await expect(client.getPlatform()).rejects.toMatchObject({
      message: 'The backend could not be reached (HTTP 502).',
      fromBackend: false,
    })
  })

  it('keeps the status line for an answer that is not JSON', async () => {
    serve({ ...ONLINE, 'GET /api/platform': () => new Response('<h1>Not found</h1>', { status: 404, statusText: 'Not Found' }) })
    await client.listAgents()

    await expect(client.getPlatform()).rejects.toMatchObject({ message: '404 Not Found', fromBackend: false })
  })

  it('passes sign-in and sign-up errors on in the words the backend uses', async () => {
    serve({
      'POST /api/auth/login': () => problem(401, 'Invalid credentials', 'Incorrect username or password.'),
      'POST /api/auth/signup': () => problem(409, 'Username taken', 'That username is already registered. Pick another one.'),
    })
    await expect(client.login('anna', 'wrong-password')).rejects.toThrow('Incorrect username or password.')
    await expect(client.signup('anna', 'a-long-password')).rejects.toThrow('That username is already registered. Pick another one.')

    serve({})
    await expect(client.login('anna', 'a-long-password')).rejects.toThrow('Could not reach the backend. Start it, then try again.')
  })
})
