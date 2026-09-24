import { useBackendStatus } from '../hooks/useBackendStatus'

const LABELS = {
  checking: { text: 'Checking backend…', cls: 'badge', pulse: true },
  online: { text: 'Backend online', cls: 'badge badge-ok', pulse: false },
  offline: { text: 'Backend offline · simulated', cls: 'badge badge-warn', pulse: false },
  simulated: { text: 'Simulation mode', cls: 'badge badge-info', pulse: false },
} as const

/**
 * `compact` shows only the coloured dot (with the text as a tooltip) — used in the collapsed sidebar.
 * The text sits in its own span so the mobile top bar can drop it with CSS as well.
 */
export function BackendStatusBadge({ compact = false }: { compact?: boolean }) {
  const status = useBackendStatus()
  const { text, cls, pulse } = LABELS[status]
  return (
    <span className={`${cls} ${compact ? 'badge-compact' : ''}`} role="status" title={text} aria-label={text}>
      <span className={`dot ${pulse ? 'dot-pulse' : ''}`} />
      {!compact && <span className="badge-label">{text}</span>}
    </span>
  )
}
