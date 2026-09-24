import { useEffect, useRef, useState } from 'react'
import type { ClipboardEvent, KeyboardEvent } from 'react'
import type { AttachmentsApi } from '../hooks/useAttachments'
import { isRecord } from '../lib/util'
import type { Attachment } from '../types'
import { IconFile, IconPaperclip, IconSend, IconX } from './Icons'

interface Props {
  disabled?: boolean
  busy?: boolean
  placeholder?: string
  attachments: AttachmentsApi
  onSend: (message: string, extraAttributes: Record<string, unknown>, attachments: Attachment[]) => void
}

/** Everything the upload endpoint can turn into text; also what the file dialog offers. */
export const ACCEPT =
  '.pdf,.txt,.md,.markdown,.csv,.json,.xml,.html,.yaml,.yml,.log,.java,.ts,.tsx,.js,.jsx,.py,.kt,.go,.rs,.c,.cpp,.h,.cs,.sql'

export function Composer({ disabled, busy, placeholder, attachments, onSend }: Props) {
  const [text, setText] = useState('')
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [advancedJson, setAdvancedJson] = useState('')
  const [error, setError] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  // Grow the textarea with its content, up to the CSS max-height.
  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }, [text])

  function collectAttributes(): Record<string, unknown> | null {
    if (!showAdvanced || !advancedJson.trim()) {
      setError(null)
      return {}
    }
    try {
      const parsed: unknown = JSON.parse(advancedJson)
      if (!isRecord(parsed)) {
        setError('Advanced attributes must be a JSON object, e.g. {"tone": "formal"}')
        return null
      }
      setError(null)
      return parsed
    } catch (e) {
      setError(e instanceof Error ? `Advanced attributes: ${e.message}` : 'Invalid JSON')
      return null
    }
  }

  // A file still uploading has no id yet, so a message sent now would silently go without it — and
  // the finished upload would then attach itself to the *next* message instead.
  const uploading = attachments.uploading > 0

  function submit() {
    const message = text.trim()
    if (!message || disabled || busy || uploading) return
    const extra = collectAttributes()
    if (extra === null) return
    onSend(message, extra, attachments.pending)
    setText('')
    textareaRef.current?.focus()
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  function onPaste(e: ClipboardEvent<HTMLTextAreaElement>) {
    const files = [...e.clipboardData.files]
    if (files.length === 0) return
    e.preventDefault()
    void attachments.add(files)
  }

  function onPick(list: FileList | null) {
    if (list && list.length) void attachments.add(list)
    if (fileInput.current) fileInput.current.value = ''
  }

  const ready = !!text.trim() && !disabled && !busy && !uploading
  const advancedActive = showAdvanced && advancedJson.trim() !== ''
  const sendLabel = busy ? 'Waiting for reply' : uploading ? 'Wait for the upload to finish' : 'Send message'

  return (
    <div className="composer">
      <div className="composer-inner">
        {(attachments.pending.length > 0 || attachments.uploading > 0) && (
          <div className="attach-chips" aria-label="Attached files">
            {attachments.pending.map((a) => (
              <span key={a.id} className="attach-chip">
                <IconFile width={13} height={13} />
                <span className="attach-chip-name" title={a.name}>
                  {a.name}
                </span>
                <button
                  type="button"
                  onClick={() => attachments.remove(a.id)}
                  aria-label={`Remove ${a.name}`}
                  title="Remove"
                >
                  <IconX width={12} height={12} />
                </button>
              </span>
            ))}
            {attachments.uploading > 0 && (
              <span className="attach-chip pending">
                <span className="spinner spinner-dark" aria-hidden />
                Uploading {attachments.uploading}…
              </span>
            )}
          </div>
        )}

        <div className={`composer-box ${busy ? 'busy' : ''}`}>
          <textarea
            ref={textareaRef}
            rows={1}
            value={text}
            placeholder={placeholder ?? 'Send a message…'}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={onKeyDown}
            onPaste={onPaste}
            disabled={disabled}
            aria-label="Message"
          />
          <button
            type="button"
            className="btn btn-ghost btn-icon attach-btn"
            onClick={() => fileInput.current?.click()}
            disabled={disabled}
            aria-label="Attach files"
            title="Attach files — or drop them on the chat, or paste them here"
          >
            <IconPaperclip />
          </button>
          <button
            type="button"
            className={`btn btn-primary btn-icon send-btn ${ready ? 'ready' : ''} ${busy ? 'busy' : ''}`}
            onClick={submit}
            disabled={!ready}
            aria-label={sendLabel}
            title={uploading ? sendLabel : undefined}
          >
            {busy ? <span className="spinner" aria-hidden /> : <IconSend />}
          </button>
          <input
            ref={fileInput}
            type="file"
            accept={ACCEPT}
            multiple
            hidden
            onChange={(e) => onPick(e.target.files)}
            aria-hidden
          />
        </div>

        <div className={`composer-attrs ${showAdvanced ? 'open' : ''}`} aria-hidden={!showAdvanced}>
          <div className="composer-attrs-inner">
            <textarea
              className="textarea mono"
              rows={3}
              value={advancedJson}
              onChange={(e) => {
                setAdvancedJson(e.target.value)
                if (error) setError(null)
              }}
              placeholder='Extra attributes as JSON, merged over the options above — e.g. {"tone": "formal"}'
              aria-label="Advanced attributes (JSON)"
              spellCheck={false}
              tabIndex={showAdvanced ? 0 : -1}
            />
          </div>
        </div>

        {(error || attachments.error) && (
          <p className="small composer-error" role="alert" onClick={() => attachments.dismissError()}>
            {error ?? attachments.error}
          </p>
        )}

        <div className="composer-foot">
          <span>
            <kbd>Enter</kbd> to send · <kbd>Shift</kbd>+<kbd>Enter</kbd> for a new line
          </span>
          <button
            type="button"
            className={`btn btn-ghost btn-sm ${advancedActive ? 'attrs-active' : ''}`}
            onClick={() => setShowAdvanced((v) => !v)}
            aria-expanded={showAdvanced}
          >
            {advancedActive && <span className="dot" />}
            {showAdvanced ? 'Hide advanced' : 'Advanced (JSON)'}
          </button>
        </div>
      </div>
    </div>
  )
}
