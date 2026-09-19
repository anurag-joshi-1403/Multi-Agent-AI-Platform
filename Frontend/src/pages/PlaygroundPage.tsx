import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import { AgentAvatar } from '../components/AgentAvatar'
import { Composer } from '../components/Composer'
import { ConversationList } from '../components/ConversationList'
import { IconArrowDown, IconPanelRight, IconSpark } from '../components/Icons'
import { Inspector } from '../components/Inspector'
import { MessageBubble } from '../components/MessageBubble'
import { forgetConversation } from '../api/client'
import type { ConversationsApi } from '../hooks/useConversations'
import { useDocuments } from '../hooks/useDocuments'
import { buildAttributes } from '../lib/attributes'
import type { ParameterValues } from '../lib/attributes'
import { dispatchMessage } from '../lib/dispatch'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'
import type { AgentInfo, ChatMessage } from '../types'

/**
 * The chat workspace. Three columns:
 *   conversations (left) · thread + composer (middle) · inspector (right, optional)
 * A conversation is created lazily on the first message so the list never fills with empties.
 */

interface Props {
  agents: AgentInfo[]
  conversations: ConversationsApi
  conversationId?: string
  /** Agent chosen for a conversation that has not been created yet. */
  draftAgentId?: string
  onDraftAgentChange: (agentId: string) => void
  onOpenConversation: (id?: string) => void
}

/** The reply shown in the inspector (explicit selection or the latest agent reply) and the user turn before it. */
function pickInspected(
  msgs: ChatMessage[],
  selectedId: string | undefined,
): { selectedMessage?: ChatMessage; requestMessage?: ChatMessage } {
  const sel = selectedId ? msgs.find((m) => m.id === selectedId) : [...msgs].reverse().find((m) => m.role === 'agent')
  if (!sel) return {}
  const idx = msgs.indexOf(sel)
  for (let i = idx - 1; i >= 0; i--) {
    if (msgs[i].role === 'user') return { selectedMessage: sel, requestMessage: msgs[i] }
  }
  return { selectedMessage: sel }
}

const HOW_TO = [
  { title: 'Choose an agent', text: 'from the dropdown above — each one is a specialist.' },
  { title: 'Send a message.', text: 'Try one of the prompts below, or write your own.' },
  { title: 'Click a reply', text: 'to inspect its latency, metadata and the request that produced it.' },
]

const SUGGESTIONS: Record<string, string[]> = {
  coding: ['Write a debounce function in TypeScript', 'Explain the difference between a Map and an object in JS'],
  research: ['What are the trade-offs of vector databases vs. keyword search?', 'Summarise recent work on agent routing'],
  summarizer: ['Summarise: The orchestrator dispatches requests to agents discovered by the registry…'],
  document: ['What does the termination clause say?', 'List every date mentioned in the document'],
}

export function PlaygroundPage({
  agents,
  conversations,
  conversationId,
  draftAgentId,
  onDraftAgentChange,
  onOpenConversation,
}: Props) {
  const active = conversations.conversations.find((c) => c.id === conversationId)
  const [pending, setPending] = useState<Set<string>>(() => new Set())
  // Selection is scoped to a conversation so switching threads naturally clears it.
  const [selected, setSelected] = useState<{ conversationId: string; messageId: string } | undefined>()
  const [showInspector, setShowInspector] = useState(() => loadString(STORAGE_KEYS.inspector) !== 'hidden')
  // Option values typed into the composer, remembered per agent while the page is open.
  const [paramValues, setParamValues] = useState<Record<string, ParameterValues>>({})
  const threadRef = useRef<HTMLDivElement>(null)

  function toggleInspector() {
    setShowInspector((v) => {
      saveString(STORAGE_KEYS.inspector, v ? 'hidden' : 'shown')
      return !v
    })
  }

  // Active conversation's agent wins; otherwise the draft pick; otherwise the first registered agent.
  const agentId = active?.agentId ?? draftAgentId ?? agents[0]?.id ?? ''
  const agent = agents.find((a) => a.id === agentId)
  const isPending = !!active && pending.has(active.id)
  const parameters = agent?.parameters ?? []
  const needsDocuments = parameters.some((p) => p.type === 'DOCUMENT')
  const documents = useDocuments(needsDocuments)
  const values = paramValues[agentId] ?? {}

  // Auto-scroll to the newest message (instant on conversation switch, smooth for new messages).
  const messageCount = active?.messages.length ?? 0
  const lastConvRef = useRef(conversationId)
  useEffect(() => {
    const el = threadRef.current
    if (!el) return
    const switched = lastConvRef.current !== conversationId
    lastConvRef.current = conversationId
    el.scrollTo({ top: el.scrollHeight, behavior: switched ? 'auto' : 'smooth' })
  }, [messageCount, isPending, conversationId])

  // Show a "jump to latest" button once the user has scrolled up a bit.
  const [awayFromBottom, setAwayFromBottom] = useState(false)
  useEffect(() => {
    const el = threadRef.current
    if (!el) return
    const onScroll = () => {
      const gap = el.scrollHeight - el.scrollTop - el.clientHeight
      setAwayFromBottom(gap > 160)
    }
    el.addEventListener('scroll', onScroll, { passive: true })
    return () => el.removeEventListener('scroll', onScroll)
  }, [])

  function jumpToLatest() {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: 'smooth' })
  }

  const selectedMsgId = selected && selected.conversationId === conversationId ? selected.messageId : undefined

  const { selectedMessage, requestMessage } = pickInspected(active?.messages ?? [], selectedMsgId)

  function startConversation() {
    const created = conversations.create(agentId)
    onOpenConversation(created.id)
    return created
  }

  async function send(text: string, attributes: Record<string, unknown>) {
    if (!agentId) return
    const conv = active ?? startConversation()
    const convId = conv.id

    setPending((p) => new Set(p).add(convId))
    try {
      await dispatchMessage(conversations, convId, conv.agentId, text, attributes)
    } finally {
      setPending((p) => {
        const next = new Set(p)
        next.delete(convId)
        return next
      })
    }
  }

  function changeAgent(nextId: string) {
    if (active) conversations.setAgent(active.id, nextId)
    else onDraftAgentChange(nextId)
  }

  function selectMessage(messageId: string) {
    if (conversationId) setSelected({ conversationId, messageId })
  }

  function deleteConversation(id: string) {
    conversations.remove(id)
    void forgetConversation(id)
    if (id === conversationId) onOpenConversation(undefined)
  }

  const suggestions = SUGGESTIONS[agentId] ?? ['Give me three ideas for a weekend project', 'Explain what this platform does']

  return (
    <div className={`playground ${showInspector ? '' : 'no-inspector'}`}>
      <ConversationList
        conversations={conversations.conversations}
        agents={agents}
        activeId={conversationId}
        onSelect={(id) => onOpenConversation(id)}
        onNew={() => onOpenConversation(undefined)}
        onDelete={deleteConversation}
      />

      <section className="chat" aria-label="Chat">
        <header className="chat-head">
          {agent && (
            <div key={agent.id} className="pop-in">
              <AgentAvatar id={agent.id} name={agent.name} />
            </div>
          )}
          <div className="chat-head-agent grow">
            <label className="row chat-head-select">
              <span className="small muted">Talking to</span>
              <select
                className="select"
                value={agentId}
                onChange={(e) => changeAgent(e.target.value)}
                disabled={agents.length === 0}
                aria-label="Select agent"
              >
                {agents.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </label>
            <span className="small muted chat-head-desc">{agent?.description}</span>
          </div>
          <button
            type="button"
            className={`btn btn-ghost btn-sm inspector-toggle ${showInspector ? 'active' : ''}`}
            onClick={toggleInspector}
            aria-pressed={showInspector}
            title={showInspector ? 'Hide the response inspector' : 'Show the response inspector'}
          >
            <IconPanelRight /> <span>Inspector</span>
          </button>
        </header>

        <div className="chat-thread" ref={threadRef}>
          <div className="chat-thread-inner">
            {!active || active.messages.length === 0 ? (
              <div className="empty chat-empty" key={agentId}>
                <div className="spark-halo" aria-hidden>
                  <IconSpark />
                </div>
                <h3 className="fade-up" style={{ '--i': 1 } as CSSProperties}>
                  {agent ? `Ask the ${agent.name} something` : 'Loading agents…'}
                </h3>
                {agent && (
                  <p className="fade-up" style={{ '--i': 2 } as CSSProperties}>
                    {agent.description}
                  </p>
                )}
                <ol className="how-to">
                  {HOW_TO.map((step, i) => (
                    <li key={step.title} className="fade-up" style={{ '--i': 3 + i } as CSSProperties}>
                      <span>
                        <b>{step.title}</b> {step.text}
                      </span>
                    </li>
                  ))}
                </ol>
                <div className="suggestions fade-up" style={{ '--i': 6 } as CSSProperties}>
                  <span className="small faint">Try asking</span>
                  <div className="chips" style={{ justifyContent: 'center' }}>
                    {suggestions.map((s, i) => (
                      <button
                        key={s}
                        type="button"
                        className="btn btn-sm suggestion fade-up"
                        style={{ '--i': 7 + i } as CSSProperties}
                        onClick={() => send(s, buildAttributes(parameters, values))}
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              active.messages.map((m) => (
                <MessageBubble
                  key={m.id}
                  message={m}
                  agent={agents.find((a) => a.id === m.agentId)}
                  selected={selectedMessage?.id === m.id}
                  onSelect={selectMessage}
                />
              ))
            )}
            {isPending && (
              <div className="msg agent">
                <AgentAvatar id={agentId} name={agent?.name} size="sm" />
                <div className="msg-body">
                  <div className="msg-bubble msg-thinking" aria-live="polite" aria-label="Agent is responding">
                    <span className="typing">
                      <span />
                      <span />
                      <span />
                    </span>
                    <span className="small muted">{agent?.name ?? 'Agent'} is thinking…</span>
                  </div>
                </div>
              </div>
            )}
          </div>
          <button
            type="button"
            className={`jump-latest ${awayFromBottom ? 'visible' : ''}`}
            onClick={jumpToLatest}
            aria-label="Jump to latest message"
            tabIndex={awayFromBottom ? 0 : -1}
          >
            <IconArrowDown width={16} height={16} /> Latest
          </button>
        </div>

        <Composer
          disabled={!agentId}
          busy={isPending}
          placeholder={agent ? `Message the ${agent.name}…` : 'Waiting for agents…'}
          parameters={parameters}
          values={values}
          onValuesChange={(next) => setParamValues((prev) => ({ ...prev, [agentId]: next }))}
          documents={documents}
          onSend={send}
        />
      </section>

      {showInspector && (
        <Inspector
          key={selectedMessage?.id ?? 'none'}
          conversation={active}
          agent={agent}
          message={selectedMessage}
          request={requestMessage}
          onClose={toggleInspector}
        />
      )}
    </div>
  )
}
