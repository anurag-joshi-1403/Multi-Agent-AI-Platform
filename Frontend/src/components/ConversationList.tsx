import type { CSSProperties } from 'react'
import { AgentAvatar } from './AgentAvatar'
import { IconChat, IconPlus, IconTrash } from './Icons'
import { formatRelative } from '../lib/util'
import type { AgentInfo, Conversation } from '../types'

interface Props {
  /** Every stored conversation; the rail shows only the current agent's, so threads never mix. */
  conversations: Conversation[]
  agent?: AgentInfo
  activeId?: string
  onSelect: (id: string) => void
  onNew: () => void
  onDelete: (id: string) => void
}

export function ConversationList({ conversations, agent, activeId, onSelect, onNew, onDelete }: Props) {
  const mine = agent ? conversations.filter((c) => c.agentId === agent.id) : []
  const newLabel = agent ? `New chat with ${agent.name}` : 'New chat'

  return (
    <aside className="conv-list" aria-label={agent ? `${agent.name} conversations` : 'Conversations'}>
      <div className="conv-list-head">
        <div className="conv-list-title grow">
          <h2 title={agent?.name}>{agent?.name ?? 'Conversations'}</h2>
          <span className="small faint">
            {mine.length === 0 ? 'No chats yet' : `${mine.length} chat${mine.length === 1 ? '' : 's'}`}
          </span>
        </div>
        <button type="button" className="btn btn-icon btn-ghost" onClick={onNew} aria-label={newLabel} title={newLabel}>
          <IconPlus />
        </button>
      </div>
      <div className="conv-items">
        {mine.length === 0 && (
          <div className="empty" style={{ padding: '32px 8px' }}>
            <IconChat />
            <p className="small">No chats with {agent?.name ?? 'this agent'} yet.</p>
          </div>
        )}
        {mine.map((c, i) => {
          const active = c.id === activeId
          return (
            <div
              key={c.id}
              className={`conv-item list-in ${active ? 'active' : ''}`}
              style={{ '--i': Math.min(i, 8) } as CSSProperties}
              role="button"
              tabIndex={0}
              onClick={() => onSelect(c.id)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  onSelect(c.id)
                }
              }}
              aria-current={active ? 'true' : undefined}
            >
              <AgentAvatar id={c.agentId} name={agent?.name} size="sm" />
              <div className="grow">
                <div className="conv-title">{c.title}</div>
                <div className="conv-meta">
                  {c.messages.length} msg · {formatRelative(c.updatedAt)}
                </div>
              </div>
              <button
                type="button"
                className="conv-del"
                aria-label={`Delete conversation ${c.title}`}
                onClick={(e) => {
                  e.stopPropagation()
                  onDelete(c.id)
                }}
              >
                <IconTrash width={15} height={15} />
              </button>
            </div>
          )
        })}
      </div>
    </aside>
  )
}
