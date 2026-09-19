import { agentColor } from '../lib/agentColor'

interface Props {
  id: string
  name?: string
  size?: 'sm' | 'md'
}

export function AgentAvatar({ id, name, size = 'md' }: Props) {
  const label = (name ?? id).trim()
  const initials = label
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('')
  return (
    <div
      className={`agent-avatar ${size === 'sm' ? 'sm' : ''}`}
      style={{ background: agentColor(id) }}
      title={label}
      aria-hidden
    >
      {initials || '?'}
    </div>
  )
}
