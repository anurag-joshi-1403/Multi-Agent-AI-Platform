import { useState } from 'react'
import type { AuthUser } from '../api/client'
import { href } from '../hooks/useHashRoute'
import type { Route } from '../hooks/useHashRoute'
import type { Theme } from '../hooks/useTheme'
import { BackendStatusBadge } from './BackendStatusBadge'
import { IconBot, IconChat, IconChevronLeft, IconGrid, IconLogout, IconMoon, IconPanelLeft, IconSettings, IconSun } from './Icons'

interface Props {
  route: Route
  theme: Theme
  collapsed: boolean
  onToggleCollapsed: () => void
  onToggleTheme: () => void
  user: AuthUser | null
  onSignOut: () => Promise<void>
}

const NAV: Array<{ route: Route; label: string; Icon: typeof IconGrid }> = [
  { route: { page: 'overview' }, label: 'Overview', Icon: IconGrid },
  { route: { page: 'playground' }, label: 'Playground', Icon: IconChat },
  { route: { page: 'agents' }, label: 'Agents', Icon: IconBot },
  { route: { page: 'settings' }, label: 'Settings', Icon: IconSettings },
]

export function Sidebar({ route, theme, collapsed, onToggleCollapsed, onToggleTheme, user, onSignOut }: Props) {
  const [signingOut, setSigningOut] = useState(false)
  const themeLabel = theme === 'dark' ? 'Light mode' : 'Dark mode'
  const collapseLabel = collapsed ? 'Expand sidebar' : 'Collapse sidebar'

  async function signOut() {
    setSigningOut(true)
    await onSignOut()
    // No need to reset signingOut on success — App unmounts this Sidebar for the login screen.
  }

  return (
    <aside className={`sidebar ${collapsed ? 'collapsed' : ''}`} aria-label="Sidebar">
      <div className="sidebar-top">
        <a className="brand" href="#/" aria-label="Multi-Agent AI Platform home">
          <div className="brand-mark" aria-hidden>
            <IconBot width={18} height={18} />
          </div>
          <div className="brand-text">
            <div className="brand-title">Multi-Agent AI</div>
            <div className="brand-sub">Platform console</div>
          </div>
        </a>
        <button
          type="button"
          className="btn btn-ghost btn-icon sidebar-toggle"
          onClick={onToggleCollapsed}
          aria-label={collapseLabel}
          aria-expanded={!collapsed}
          title={`${collapseLabel} (Ctrl+B)`}
        >
          {collapsed ? <IconPanelLeft /> : <IconChevronLeft />}
        </button>
      </div>

      <nav className="nav" aria-label="Primary">
        {NAV.map(({ route: r, label, Icon }) => (
          <a
            key={r.page}
            className="nav-link"
            href={href(r)}
            aria-current={route.page === r.page ? 'page' : undefined}
            title={collapsed ? label : undefined}
            aria-label={label}
          >
            <Icon />
            <span>{label}</span>
          </a>
        ))}
      </nav>

      <div className="sidebar-foot">
        {user && (
          <div className="account-row" title={collapsed ? user.username : undefined}>
            <span className="account-avatar" aria-hidden>
              {user.username.slice(0, 1).toUpperCase()}
            </span>
            <span className="account-name grow">{user.username}</span>
            <button
              type="button"
              className="btn btn-ghost btn-icon sign-out-btn"
              onClick={() => void signOut()}
              disabled={signingOut}
              aria-label="Sign out"
              title="Sign out"
            >
              <IconLogout width={15} height={15} />
            </button>
          </div>
        )}
        <BackendStatusBadge compact={collapsed} />
        <button
          type="button"
          className="btn btn-ghost btn-sm theme-toggle"
          onClick={onToggleTheme}
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
          title={collapsed ? themeLabel : undefined}
        >
          {theme === 'dark' ? <IconSun /> : <IconMoon />}
          <span>{themeLabel}</span>
        </button>
      </div>
    </aside>
  )
}
