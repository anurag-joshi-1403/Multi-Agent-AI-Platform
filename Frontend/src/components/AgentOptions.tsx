import type { ParameterValues } from '../lib/attributes'
import type { AgentParameter } from '../types'

interface Props {
  parameters: AgentParameter[]
  values: ParameterValues
  onChange: (values: ParameterValues) => void
}

/**
 * The agent's own settings, rendered in the chat header so they sit above the conversation rather
 * than crowding the message box. Files are not settings — they are attached per message instead.
 */
export function AgentOptions({ parameters, values, onChange }: Props) {
  if (parameters.length === 0) return null

  function setValue(name: string, value: string) {
    onChange({ ...values, [name]: value })
  }

  return (
    <div className="agent-options" role="group" aria-label="Agent options">
      {parameters.map((p) => {
        const id = `param-${p.name}`
        const value = values[p.name] ?? ''
        return (
          <label key={p.name} className="agent-option" htmlFor={id} title={p.description}>
            <span className="small muted">{p.label}</span>
            {p.type === 'SELECT' ? (
              <select
                id={id}
                className="select"
                value={value || String(p.defaultValue ?? '')}
                onChange={(e) => setValue(p.name, e.target.value)}
              >
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
  )
}
