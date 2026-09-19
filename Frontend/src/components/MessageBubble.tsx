import { AgentAvatar } from './AgentAvatar'
import { IconCheck, IconCopy } from './Icons'
import { Markdown } from './Markdown'
import { useCopy } from '../hooks/useCopy'
import { formatMs } from '../lib/util'
import type { AgentInfo, ChatMessage } from '../types'

interface Props {
  message: ChatMessage
  agent?: AgentInfo
  selected?: boolean
  onSelect?: (id: string) => void
}

export function MessageBubble({ message, agent, selected, onSelect }: Props) {
  const [copied, copy] = useCopy()
  const isUser = message.role === 'user'
  const time = new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  const selectable = message.role === 'agent' && !!onSelect
  const simulated = message.metadata?.simulated === true
  const hasAttrs = !!message.attributes && Object.keys(message.attributes).length > 0

  return (
    <div className={`msg ${message.role}`}>
      {isUser ? (
        <div className="msg-avatar" aria-hidden>
          You
        </div>
      ) : (
        <AgentAvatar id={message.agentId ?? 'agent'} name={agent?.name} size="sm" />
      )}
      <div className="msg-body">
        <div
          className={`msg-bubble ${selectable ? 'selectable' : ''} ${selected ? 'selected' : ''}`}
          onClick={selectable ? () => onSelect?.(message.id) : undefined}
          title={selectable ? 'Click to inspect this response' : undefined}
        >
          {message.role === 'error' ? <p>{message.content}</p> : <Markdown source={message.content} />}
          {message.role !== 'error' && (
            <button
              type="button"
              className={`msg-copy ${copied ? 'copied' : ''}`}
              onClick={(e) => {
                e.stopPropagation()
                copy(message.content)
              }}
              aria-label={copied ? 'Copied' : 'Copy message'}
              title={copied ? 'Copied!' : 'Copy'}
            >
              {copied ? <IconCheck width={13} height={13} /> : <IconCopy width={13} height={13} />}
            </button>
          )}
        </div>
        <div className="msg-meta">
          {!isUser && <span>{agent?.name ?? message.agentId ?? 'Agent'}</span>}
          {!isUser && message.elapsedMs !== undefined && (
            <span className="msg-latency">· {formatMs(message.elapsedMs)}</span>
          )}
          {simulated && <span className="badge badge-info small">simulated</span>}
          {hasAttrs && <span className="badge small">+attributes</span>}
          {selected && <span className="badge badge-accent small">inspecting</span>}
          <span>{time}</span>
        </div>
      </div>
    </div>
  )
}
