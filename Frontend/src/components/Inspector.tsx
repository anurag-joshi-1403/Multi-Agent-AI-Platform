import type { ReactNode, Ref } from 'react'
import { IconInfo, IconPanelRight } from './Icons'
import { formatMs } from '../lib/util'
import type { AgentInfo, ChatMessage, Conversation } from '../types'

/**
 * Right-hand panel of the playground. Shows what went over the wire for the
 * selected agent reply: the AgentResponse fields and the AgentRequest that
 * produced it. Defaults to the most recent reply.
 */

interface Props {
  conversation?: Conversation
  agent?: AgentInfo
  message?: ChatMessage
  /** The user message that triggered `message`, if known. */
  request?: ChatMessage
  onClose?: () => void
  /** Lets the playground move focus to "Hide" when the panel opens as an overlay on narrow screens. */
  closeButtonRef?: Ref<HTMLButtonElement>
  /** Narrow screens: the panel floats over the chat instead of taking a grid column. */
  overlay?: boolean
}

function pretty(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function Section({ title, hint, children }: { title: string; hint?: string; children: ReactNode }) {
  return (
    <section className="inspector-section">
      <h2>{title}</h2>
      {hint && <p className="small faint inspector-hint">{hint}</p>}
      {children}
    </section>
  )
}

export function Inspector({ conversation, agent, message, request, onClose, closeButtonRef, overlay }: Props) {
  const simulated = message?.metadata?.simulated === true

  return (
    <aside className={`inspector ${overlay ? 'inspector-overlay' : ''}`} aria-label="Response inspector">
      <div className="inspector-head">
        <span className="row">
          <IconPanelRight width={15} height={15} />
          <strong>Inspector</strong>
        </span>
        {onClose && (
          <button
            ref={closeButtonRef}
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={onClose}
            aria-label="Hide inspector"
          >
            Hide
          </button>
        )}
      </div>

      <Section title="Response" hint="The AgentResponse the backend returned for the selected reply.">
        {message ? (
          <>
            {simulated && (
              <p className="callout callout-info small">
                This reply was simulated in the browser — the backend was not called.
              </p>
            )}
            <dl className="kv">
              <dt>agent</dt>
              <dd>{agent?.name ?? message.agentId}</dd>
              <dt>agentId</dt>
              <dd>{message.agentId}</dd>
              {message.elapsedMs !== undefined && (
                <>
                  <dt>latency</dt>
                  <dd>{formatMs(message.elapsedMs)}</dd>
                </>
              )}
              {typeof message.metadata?.provider === 'string' && (
                <>
                  <dt>provider</dt>
                  <dd>{message.metadata.provider}</dd>
                </>
              )}
              {typeof message.metadata?.model === 'string' && (
                <>
                  <dt>model</dt>
                  <dd>{message.metadata.model}</dd>
                </>
              )}
              <dt>length</dt>
              <dd>{message.content.length} chars</dd>
            </dl>
            <div className="stack" style={{ gap: 6 }}>
              <span className="small muted">metadata</span>
              <pre className="json">{pretty(message.metadata ?? {})}</pre>
            </div>
          </>
        ) : (
          <div className="row small muted">
            <IconInfo width={14} height={14} />
            <span>
              {conversation ? 'Click an agent reply in the thread to inspect it.' : 'Send a message to see a response here.'}
            </span>
          </div>
        )}
      </Section>

      {message && request && (
        <Section title="Request" hint="The AgentRequest that was sent to produce it.">
          <pre className="json">
            {pretty({
              conversationId: conversation?.id ?? null,
              message: request.content,
              attributes: request.attributes ?? {},
            })}
          </pre>
        </Section>
      )}

      <Section title="Conversation">
        {conversation ? (
          <dl className="kv">
            <dt>id</dt>
            <dd>{conversation.id}</dd>
            <dt>agent</dt>
            <dd>{conversation.agentId}</dd>
            <dt>messages</dt>
            <dd>{conversation.messages.length}</dd>
            <dt>started</dt>
            <dd>{new Date(conversation.createdAt).toLocaleString()}</dd>
          </dl>
        ) : (
          <p className="small muted">Not started yet — it is created with your first message.</p>
        )}
      </Section>
    </aside>
  )
}
