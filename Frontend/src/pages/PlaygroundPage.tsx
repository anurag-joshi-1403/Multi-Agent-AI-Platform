import { useEffect, useRef, useState } from 'react'
import type { CSSProperties, DragEvent } from 'react'
import { AgentAvatar } from '../components/AgentAvatar'
import { AgentOptions } from '../components/AgentOptions'
import { Composer } from '../components/Composer'
import { ConversationList } from '../components/ConversationList'
import { IconArrowDown, IconPanelRight, IconPaperclip, IconPlus, IconSpark } from '../components/Icons'
import { Inspector } from '../components/Inspector'
import { MessageBubble } from '../components/MessageBubble'
import { forgetConversation } from '../api/client'
import { useAttachments } from '../hooks/useAttachments'
import type { ConversationsApi } from '../hooks/useConversations'
import { useMediaQuery } from '../hooks/useMediaQuery'
import { agentThemeStyle } from '../lib/agentColor'
import { buildAttributes } from '../lib/attributes'
import type { ParameterValues } from '../lib/attributes'
import { dispatchMessage } from '../lib/dispatch'
import type { DispatchOptions } from '../lib/dispatch'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'
import type { AgentInfo, Attachment, ChatMessage } from '../types'

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

/**
 * The `attachments` attribute for a turn: every file attached earlier in the thread plus the ones
 * just added, so a follow-up question still sees a file attached three messages ago. Omitted
 * entirely when there is nothing attached.
 */
function withAttachments(history: ChatMessage[], added: Attachment[]): { attachments?: string[] } {
  const ids = [...new Set([...history.flatMap((m) => m.attachments ?? []), ...added].map((a) => a.id))]
  return ids.length ? { attachments: ids } : {}
}

/**
 * Must match the `max-width: 1180px` block in app.css: below it there is no room for a third grid
 * column, so the inspector is shown as an overlay instead of being hidden outright.
 */
const INSPECTOR_OVERLAY_QUERY = '(max-width: 1180px)'

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
  // On narrow screens the panel is an overlay over the chat, so it starts closed each visit rather
  // than following the remembered wide-screen preference (which would cover the thread on arrival).
  const compactInspector = useMediaQuery(INSPECTOR_OVERLAY_QUERY)
  const [overlayOpen, setOverlayOpen] = useState(false)
  const inspectorOpen = compactInspector ? overlayOpen : showInspector
  const inspectorToggleRef = useRef<HTMLButtonElement>(null)
  const inspectorCloseRef = useRef<HTMLButtonElement>(null)
  // Option values typed into the header controls, remembered per agent while the page is open.
  const [paramValues, setParamValues] = useState<Record<string, ParameterValues>>({})
  const attachments = useAttachments()
  const [dragging, setDragging] = useState(false)
  const threadRef = useRef<HTMLDivElement>(null)

  function setInspectorOpen(open: boolean) {
    if (compactInspector) {
      setOverlayOpen(open)
      return
    }
    saveString(STORAGE_KEYS.inspector, open ? 'shown' : 'hidden')
    setShowInspector(open)
  }

  function toggleInspector() {
    setInspectorOpen(!inspectorOpen)
  }

  /** Closing the overlay from inside it hands focus back to the toggle instead of dropping it on <body>. */
  function closeInspector() {
    const focusWasInside = document.activeElement?.closest('.inspector') != null
    setInspectorOpen(false)
    if (compactInspector && focusWasInside) inspectorToggleRef.current?.focus()
  }

  // Opening the overlay moves focus into it; Escape closes it (unless something inside, like the
  // message editor, already handled the key).
  useEffect(() => {
    if (!compactInspector || !overlayOpen) return
    inspectorCloseRef.current?.focus()
    function onKey(e: KeyboardEvent) {
      if (e.key !== 'Escape' || e.defaultPrevented) return
      const focusWasInside = document.activeElement?.closest('.inspector') != null
      setOverlayOpen(false)
      if (focusWasInside) inspectorToggleRef.current?.focus()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [compactInspector, overlayOpen])

  // Active conversation's agent wins; otherwise the draft pick; otherwise the first registered agent.
  const agentId = active?.agentId ?? draftAgentId ?? agents[0]?.id ?? ''
  const agent = agents.find((a) => a.id === agentId)
  const isPending = !!active && pending.has(active.id)
  const parameters = agent?.parameters ?? []
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

  async function runDispatch(convId: string, targetAgentId: string, text: string, attributes: Record<string, unknown>, opts?: DispatchOptions) {
    setPending((p) => new Set(p).add(convId))
    try {
      await dispatchMessage(conversations, convId, targetAgentId, text, attributes, opts)
    } finally {
      setPending((p) => {
        const next = new Set(p)
        next.delete(convId)
        return next
      })
    }
  }

  async function send(text: string, extra: Record<string, unknown>, added: Attachment[]) {
    // Files still uploading have no id yet; sending now would leave them out of this turn.
    if (!agentId || attachments.uploading > 0) return
    const conv = active ?? startConversation()
    const attributes = {
      ...buildAttributes(parameters, values),
      ...extra,
      ...withAttachments(active?.messages ?? [], added),
    }
    attachments.clear()
    await runDispatch(conv.id, conv.agentId, text, attributes, { attachments: added })
  }

  /** Edit a previously-sent prompt: drop the stale reply after it, then resend with the new text. */
  async function editMessage(messageId: string, text: string) {
    // A reply still in flight would land after the edited turn (and clear the pending flag early).
    if (!active || pending.has(active.id)) return
    const idx = active.messages.findIndex((m) => m.id === messageId)
    if (idx < 0) return
    const original = active.messages[idx]
    // Only files attached up to and including this turn — the truncated ones are gone.
    const attributes = { ...(original.attributes ?? {}), ...withAttachments(active.messages.slice(0, idx + 1), []) }
    conversations.truncateAfter(active.id, messageId)
    await runDispatch(active.id, active.agentId, text, attributes, { replaceMessageId: messageId })
  }

  function onDragOver(e: DragEvent<HTMLElement>) {
    if (!e.dataTransfer.types.includes('Files')) return
    e.preventDefault()
    setDragging(true)
  }

  function onDragLeave(e: DragEvent<HTMLElement>) {
    if (e.relatedTarget instanceof Node && e.currentTarget.contains(e.relatedTarget)) return
    setDragging(false)
  }

  function onDrop(e: DragEvent<HTMLElement>) {
    if (!e.dataTransfer.types.includes('Files')) return
    e.preventDefault()
    setDragging(false)
    void attachments.add(e.dataTransfer.files)
  }

  /** Each agent keeps its own threads, so picking a different one always opens a fresh chat. */
  function changeAgent(nextId: string) {
    onDraftAgentChange(nextId)
    onOpenConversation(undefined)
  }

  /** Blank chat with the agent you are on — without the draft the id would fall back to `agents[0]`. */
  function newChat() {
    // Guard the empty id: `??` would not fall through it, pinning the page to no agent.
    if (agentId) onDraftAgentChange(agentId)
    onOpenConversation(undefined)
  }

  function selectMessage(messageId: string) {
    if (conversationId) setSelected({ conversationId, messageId })
  }

  /** The explicit "inspect" action: select the reply and make sure the panel is showing it. */
  function inspectMessage(messageId: string) {
    selectMessage(messageId)
    setInspectorOpen(true)
  }

  function deleteConversation(id: string) {
    const doomed = conversations.conversations.find((c) => c.id === id)
    if (!doomed) return
    // Same safeguard as Settings > Delete all: the transcript only lives in this browser.
    if (!window.confirm(`Delete "${doomed.title}"? This cannot be undone.`)) return
    conversations.remove(id)
    void forgetConversation(id)
    if (id === conversationId) {
      onDraftAgentChange(doomed.agentId)
      onOpenConversation(undefined)
    }
  }

  const suggestions = SUGGESTIONS[agentId] ?? ['Give me three ideas for a weekend project', 'Explain what this platform does']

  return (
    <div className={`playground ${showInspector ? '' : 'no-inspector'}`}>
      <h1 className="sr-only">Playground</h1>
      <ConversationList
        conversations={conversations.conversations}
        agent={agent}
        activeId={conversationId}
        onSelect={(id) => onOpenConversation(id)}
        onNew={newChat}
        onDelete={deleteConversation}
      />

      <section
        className={`chat ${dragging ? 'dropping' : ''}`}
        aria-label="Chat"
        style={agent ? (agentThemeStyle(agent.id) as CSSProperties) : undefined}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
      >
        {dragging && (
          <div className="dropzone" aria-hidden>
            <IconPaperclip width={26} height={26} />
            <strong>Drop files to attach them</strong>
            <span className="small muted">PDFs and text files · they stay in context for this chat</span>
          </div>
        )}
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
          <AgentOptions
            parameters={parameters}
            values={values}
            onChange={(next) => setParamValues((prev) => ({ ...prev, [agentId]: next }))}
          />
          <button
            type="button"
            className="btn btn-sm new-chat-btn"
            onClick={newChat}
            disabled={!active}
            title={agent ? `Start a new chat with ${agent.name}` : 'Start a new chat'}
          >
            <IconPlus width={15} height={15} /> <span>New chat</span>
          </button>
          <button
            ref={inspectorToggleRef}
            type="button"
            className={`btn btn-ghost btn-sm inspector-toggle ${inspectorOpen ? 'active' : ''}`}
            onClick={toggleInspector}
            aria-pressed={inspectorOpen}
            title={inspectorOpen ? 'Hide the response inspector' : 'Show the response inspector'}
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
                        onClick={() => void send(s, {}, attachments.pending)}
                        disabled={attachments.uploading > 0}
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
                  onInspect={inspectMessage}
                  onEdit={isPending ? undefined : editMessage}
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
          attachments={attachments}
          onSend={send}
        />
      </section>

      {inspectorOpen && (
        <Inspector
          key={selectedMessage?.id ?? 'none'}
          conversation={active}
          agent={agent}
          message={selectedMessage}
          request={requestMessage}
          onClose={closeInspector}
          closeButtonRef={inspectorCloseRef}
          overlay={compactInspector}
        />
      )}
    </div>
  )
}
