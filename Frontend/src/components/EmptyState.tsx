import type { ReactNode } from 'react'

interface Props {
  icon?: ReactNode
  title: string
  children?: ReactNode
  /** Optional call to action rendered under the text. */
  action?: ReactNode
  compact?: boolean
}

/** Friendly placeholder that says what is missing and what to do about it. */
export function EmptyState({ icon, title, children, action, compact }: Props) {
  return (
    <div className={`empty ${compact ? 'empty-compact' : ''}`}>
      {icon}
      <h3>{title}</h3>
      {children && <div className="empty-body">{children}</div>}
      {action && <div className="empty-action">{action}</div>}
    </div>
  )
}
