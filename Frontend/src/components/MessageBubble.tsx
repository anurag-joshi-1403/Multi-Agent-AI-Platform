import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { AgentAvatar } from './AgentAvatar'
import { IconCheck, IconCopy, IconEdit, IconFile, IconX } from './Icons'
import { Markdown } from './Markdown'
import { useCopy } from '../hooks/useCopy'
import { formatMs } from '../lib/util'
import type { AgentInfo, ChatMessage } from '../types'

interface Props {
  message: ChatMessage
  agent?: AgentInfo
  selected?: boolean
  onSelect?: (id: string) => void
  /** Editing a prompt truncates the reply that followed it and resends — see `PlaygroundPage.editMessage`. */
  onEdit?: (id: string, text: string) => void
}

export function MessageBubble({ message, agent, selected, onSelect, onEdit }: Props) {
  const [copied, copy] = useCopy()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(message.content)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const isUser = message.role === 'user'
  const time = new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  const selectable = message.role === 'agent' && !!onSelect
  const editable = isUser && !!onEdit
  const simulated = message.metadata?.simulated === true
  const hasAttrs = !!message.attributes && Object.keys(message.attributes).length > 0

  useEffect(() => {
    if (!editing) return
    const el = textareaRef.current
    if (!el) return
    el.focus()
    el.setSelectionRange(el.value.length, el.value.length)
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }, [editing])

  function startEdit() {
    setDraft(message.content)
    setEditing(true)
  }

  function cancelEdit() {
    setEditing(false)
  }

  function saveEdit() {
    const text = draft.trim()
    if (text && text !== message.content) onEdit?.(message.id, text)
    setEditing(false)
  }

  function onEditKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      saveEdit()
    } else if (e.key === 'Escape') {
      e.preventDefault()
      cancelEdit()
    }
  }

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
        {!!message.attachments?.length && (
          <div className="msg-attachments">
            {message.attachments.map((a) => (
              <span key={a.id} className="attach-chip static">
                <IconFile width={12} height={12} />
                <span className="attach-chip-name" title={a.name}>
                  {a.name}
                </span>
              </span>
            ))}
          </div>
        )}
        {editing ? (
          <div className="msg-edit-box">
            <textarea
              ref={textareaRef}
              className="mono"
              value={draft}
              onChange={(e) => {
                setDraft(e.target.value)
                const el = e.target
                el.style.height = 'auto'
                el.style.height = `${el.scrollHeight}px`
              }}
              onKeyDown={onEditKeyDown}
              aria-label="Edit message"
            />
            <div className="msg-edit-actions">
              <span className="small faint">
                <kbd>Enter</kbd> to save · <kbd>Esc</kbd> to cancel
              </span>
              <div className="row" style={{ gap: 6 }}>
                <button type="button" className="btn btn-ghost btn-sm" onClick={cancelEdit}>
                  <IconX width={14} height={14} /> Cancel
                </button>
                <button type="button" className="btn btn-primary btn-sm" onClick={saveEdit} disabled={!draft.trim()}>
                  <IconCheck width={14} height={14} /> Save &amp; resend
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div
            className={`msg-bubble ${selectable ? 'selectable' : ''} ${selected ? 'selected' : ''}`}
            onClick={selectable ? () => onSelect?.(message.id) : undefined}
            title={selectable ? 'Click to inspect this response' : undefined}
          >
            {message.role === 'error' ? <p>{message.content}</p> : <Markdown source={message.content} />}
            <div className="msg-bubble-actions">
              {editable && (
                <button
                  type="button"
                  className="msg-action msg-edit"
                  onClick={(e) => {
                    e.stopPropagation()
                    startEdit()
                  }}
                  aria-label="Edit message"
                  title="Edit"
                >
                  <IconEdit width={13} height={13} />
                </button>
              )}
              {message.role !== 'error' && (
                <button
                  type="button"
                  className={`msg-action msg-copy ${copied ? 'copied' : ''}`}
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
          </div>
        )}
        <div className="msg-meta">
          {!isUser && <span>{agent?.name ?? message.agentId ?? 'Agent'}</span>}
          {!isUser && message.elapsedMs !== undefined && (
            <span className="msg-latency">· {formatMs(message.elapsedMs)}</span>
          )}
          {message.editedAt && <span className="msg-edited-tag">(edited)</span>}
          {simulated && <span className="badge badge-info small">simulated</span>}
          {hasAttrs && <span className="badge small">+attributes</span>}
          {selected && <span className="badge badge-accent small">inspecting</span>}
          <span>{time}</span>
        </div>
      </div>
    </div>
  )
}
