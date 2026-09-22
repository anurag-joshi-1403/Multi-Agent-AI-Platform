import { useCallback, useState } from 'react'
import { Sidebar } from './components/Sidebar'
import { useAgents } from './hooks/useAgents'
import { useAuth } from './hooks/useAuth'
import { useAutoCollapseOnRoute } from './hooks/useAutoCollapseOnRoute'
import { useBackendStatus } from './hooks/useBackendStatus'
import { useConversations } from './hooks/useConversations'
import { useHashRoute } from './hooks/useHashRoute'
import { usePlatform } from './hooks/usePlatform'
import { useTheme } from './hooks/useTheme'
import { AgentsPage } from './pages/AgentsPage'
import { LoginPage } from './pages/LoginPage'
import { OverviewPage } from './pages/OverviewPage'
import { PlaygroundPage } from './pages/PlaygroundPage'
import { SettingsPage } from './pages/SettingsPage'
import type { AuthApi } from './hooks/useAuth'
import './app.css'

/**
 * Gates the console behind a real login. Nothing below this — not even the initial `/api/agents`
 * probe — runs until `auth.status === 'authenticated'`, so `Console` mounts (and its hooks start
 * firing requests) only once there is a session to send with them.
 */
function App() {
  const auth = useAuth()

  if (auth.status === 'checking') {
    return (
      <div className="auth-splash" aria-busy="true">
        <span className="spinner" aria-hidden />
      </div>
    )
  }

  if (auth.status === 'anonymous') {
    return <LoginPage onLogin={auth.login} checkError={auth.checkError} />
  }

  return <Console auth={auth} />
}

function Console({ auth }: { auth: AuthApi }) {
  const [route, navigate] = useHashRoute()
  const [theme, toggleTheme] = useTheme()
  const [sidebarCollapsed, toggleSidebar] = useAutoCollapseOnRoute(route)
  const { agents, loading, reload } = useAgents()
  const conversations = useConversations()
  const status = useBackendStatus()
  const platform = usePlatform()
  // Agent picked for a conversation that hasn't been created yet (survives page switches).
  const [draftAgentId, setDraftAgentId] = useState<string | undefined>()

  const openConversation = useCallback(
    (id?: string) => navigate({ page: 'playground', conversationId: id }),
    [navigate],
  )

  const tryAgent = useCallback(
    (agentId: string) => {
      setDraftAgentId(agentId)
      navigate({ page: 'playground' })
    },
    [navigate],
  )

  let page
  switch (route.page) {
    case 'playground':
      page = (
        <PlaygroundPage
          agents={agents}
          conversations={conversations}
          conversationId={route.conversationId}
          draftAgentId={draftAgentId}
          onDraftAgentChange={setDraftAgentId}
          onOpenConversation={openConversation}
        />
      )
      break
    case 'agents':
      page = <AgentsPage agents={agents} loading={loading} onReload={reload} onTryAgent={tryAgent} />
      break
    case 'settings':
      page = (
        <SettingsPage
          theme={theme}
          onToggleTheme={toggleTheme}
          onReloadAgents={reload}
          onClearConversations={conversations.clearAll}
          conversationCount={conversations.conversations.length}
          platform={platform}
        />
      )
      break
    default:
      page = (
        <OverviewPage
          agents={agents}
          agentsLoading={loading}
          conversations={conversations.conversations}
          backend={status}
          platform={platform}
        />
      )
  }

  return (
    <div className={`app ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      <Sidebar
        route={route}
        theme={theme}
        collapsed={sidebarCollapsed}
        onToggleCollapsed={toggleSidebar}
        onToggleTheme={toggleTheme}
        user={auth.user}
        onSignOut={auth.logout}
      />
      <main className="main">
        {status === 'offline' && (
          <div className="banner" role="status">
            <strong>Backend offline.</strong>
            <span>
              Could not reach the API — replies are simulated. Start the Spring Boot app on port 8080, then reconnect
              from Settings.
            </span>
            <span className="spacer" />
            <button type="button" className="btn btn-sm" onClick={reload} disabled={loading}>
              {loading ? 'Checking…' : 'Retry'}
            </button>
          </div>
        )}
        {status === 'online' && platform && !platform.apiKeyConfigured && (
          <div className="banner banner-info" role="status">
            <strong>No API key.</strong>
            <span>
              Backend is online ({platform.providerName} · <code>{platform.model}</code>) but{' '}
              <code>{platform.keyEnvVar}</code> is not set, so agent runs will fail. Set it and restart the backend.
            </span>
            <span className="spacer" />
            <a className="btn btn-sm" href="#/settings">
              Details
            </a>
          </div>
        )}
        {page}
      </main>
    </div>
  )
}

export default App
