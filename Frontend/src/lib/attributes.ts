import type { AgentParameter } from '../types'

/** Values typed into the composer's option controls, keyed by parameter name. */
export type ParameterValues = Record<string, string>

/** Turn the typed control values into the attributes map the backend expects. */
export function buildAttributes(parameters: AgentParameter[], values: ParameterValues): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const p of parameters) {
    const raw = (values[p.name] ?? '').trim()
    if (!raw) continue
    if (p.type === 'NUMBER') {
      const n = Number(raw)
      if (!Number.isNaN(n)) out[p.name] = n
    } else {
      out[p.name] = raw
    }
  }
  return out
}
