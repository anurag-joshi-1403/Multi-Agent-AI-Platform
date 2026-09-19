import type { ReactNode } from 'react'

interface Props {
  /** Small label above the title, e.g. the section this page belongs to. */
  eyebrow?: string
  title: string
  /** One sentence explaining what the page is for. */
  description?: ReactNode
  /** Buttons/links shown on the right. */
  actions?: ReactNode
}

/** Consistent top-of-page block so every screen answers "where am I / what is this for?". */
export function PageHeader({ eyebrow, title, description, actions }: Props) {
  return (
    <header className="page-header">
      <div className="page-header-text">
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </header>
  )
}
