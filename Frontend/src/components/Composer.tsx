import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import type { DocumentsApi } from '../hooks/useDocuments'
import { buildAttributes } from '../lib/attributes'
import type { ParameterValues } from '../lib/attributes'
import type { AgentParameter } from '../types'
import { DocumentPicker } from './DocumentPicker'
import { IconSend, IconSliders } from './Icons'

interface Props {
  disabled?: boolean
  busy?: boolean
  placeholder?: string
  /** Controls the agent asks for; rendered above the message box. */
  parameters: AgentParameter[]
  values: ParameterValues
  onValuesChange: (values: ParameterValues) => void
  documents: DocumentsApi
  onSend: (message: string, attributes: Record<string, unknown>) => void
}

export function Composer({ disabled, busy, placeholder, parameters, values, onValuesChange, documents, onSend }: Props) {
  const [text, setText] = useState('')
  const [showAdvanced, setShowAdvanced] = useState(false)
  const [advancedJson, setAdvancedJson] = useState('')
  const [error, setError] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  // Grow the textarea with its content, up to the CSS max-height.
  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }, [text])

  function setValue(name: string, value: string) {
    onValuesChange({ ...values, [name]: value })
    if (error) setError(null)
  }

  function collectAttributes(): Record<string, unknown> | null {
    const missing = parameters.find((p) => p.required && !(values[p.name] ?? '').trim())
    if (missing) {
      setError(`${missing.label} is required for this agent.`)
      return null
    }
    let attributes = buildAttributes(parameters, values)
    if (showAdvanced && advancedJson.trim()) {
      try {
        const parsed: unknown = JSON.parse(advancedJson)
        if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
          setError('Advanced attributes must be a JSON object, e.g. {"documentId": "doc_1"}')
          return null
        }
        attributes = { ...attributes, ...(parsed as Record<string, unknown>) }
      } catch (e) {
        setError(e instanceof Error ? `Advanced attributes: ${e.message}` : 'Invalid JSON')
        return null
      }
    }
    setError(null)
    return attributes
  }

  function submit() {
    const message = text.trim()
    if (!message || disabled || busy) return
    const attributes = collectAttributes()
    if (attributes === null) return
    onSend(message, attributes)
    setText('')
    textareaRef.current?.focus()
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  const ready = !!text.trim() && !disabled && !busy
  const advancedActive = showAdvanced && advancedJson.trim() !== ''

  return (
    <div className="composer">
      <div className="composer-inner">
        {parameters.length > 0 && (
          <div className="param-bar" role="group" aria-label="Agent options">
            <span className="param-bar-label">
              <IconSliders width={14} height={14} /> Options
            </span>
            {parameters.map((p) => {
              const id = `param-${p.name}`
              const value = values[p.name] ?? ''
              return (
                <label key={p.name} className={`param ${p.type === 'DOCUMENT' ? 'param-wide' : ''}`} htmlFor={id} title={p.description}>
                  <span className="param-label">
                    {p.label}
                    {p.required && <span className="param-required"> *</span>}
                  </span>
                  {p.type === 'SELECT' ? (
                    <select id={id} className="select" value={value || String(p.defaultValue ?? '')} onChange={(e) => setValue(p.name, e.target.value)}>
                      {p.options.map((o) => (
                        <option key={o} value={o}>
                          {o === '' ? 'Auto-detect' : o}
                        </option>
                      ))}
                    </select>
                  ) : p.type === 'NUMBER' ? (
                    <input
                      id={id}
                      className="input"
                      type="number"
                      inputMode="numeric"
                      value={value}
                      placeholder={p.defaultValue == null ? '' : String(p.defaultValue)}
                      onChange={(e) => setValue(p.name, e.target.value)}
                    />
                  ) : p.type === 'DOCUMENT' ? (
                    <DocumentPicker id={id} value={value} onChange={(v) => setValue(p.name, v)} documents={documents} />
                  ) : (
                    <input
                      id={id}
                      className="input"
                      type="text"
                      value={value}
                      placeholder={p.description}
                      onChange={(e) => setValue(p.name, e.target.value)}
                    />
                  )}
                </label>
              )
            })}
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
            disabled={disabled}
            aria-label="Message"
          />
          <button
            type="button"
            className={`btn btn-primary btn-icon send-btn ${ready ? 'ready' : ''} ${busy ? 'busy' : ''}`}
            onClick={submit}
            disabled={!ready}
            aria-label={busy ? 'Waiting for reply' : 'Send message'}
          >
            {busy ? <span className="spinner" aria-hidden /> : <IconSend />}
          </button>
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

        {error && (
          <p className="small composer-error" role="alert">
            {error}
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
