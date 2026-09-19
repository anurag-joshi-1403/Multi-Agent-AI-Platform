import type { CSSProperties } from 'react'
import { AgentAvatar } from './AgentAvatar'
import { IconChat, IconPlus, IconTrash } from './Icons'
import { formatRelative } from '../lib/util'
import type { AgentInfo, Conversation } from '../types'

interface Props {
  conversations: Conversation[]
  agents: AgentInfo[]
  activeId?: string
  onSelect: (id: string) => void
  onNew: () => void
  onDelete: (id: string) => void
}

export function ConversationList({ conversations, agents, activeId, onSelect, onNew, onDelete }: Props) {
  return (
    <aside className="conv-list" aria-label="Conversations">
      <div className="conv-list-head">
        <h2>Conversations</h2>
        <button type="button" className="btn btn-icon btn-ghost" onClick={onNew} aria-label="New conversation">
          <IconPlus />
        </button>
      </div>
      <div className="conv-items">
        {conversations.length === 0 && (
          <div className="empty" style={{ padding: '32px 8px' }}>
            <IconChat />
            <p className="small">No conversations yet.</p>
          </div>
        )}
        {conversations.map((c, i) => {
          const agent = agents.find((a) => a.id === c.agentId)
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
                  {agent?.name ?? c.agentId} · {c.messages.length} msg · {formatRelative(c.updatedAt)}
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
