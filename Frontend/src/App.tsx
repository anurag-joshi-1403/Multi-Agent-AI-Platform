import { useCallback, useState } from 'react'
import { Sidebar } from './components/Sidebar'
import { useAgents } from './hooks/useAgents'
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
import type { Theme } from './hooks/useTheme'
import './app.css'

/**
 * Shows the login front page until it is submitted, then the console. There is no authentication
 * behind it yet — `enter` is the placeholder where a real sign-in call belongs.
 */
function App() {
  // Called once here rather than inside Console, so the login screen and the console share the
  // same theme state (and the same localStorage key) instead of two independent hook instances
  // that would only happen to agree at mount.
  const [theme, toggleTheme] = useTheme()
  const [entered, setEntered] = useState(false)

  const enter = useCallback(async () => {
    // TODO: authenticate here (LoginPage passes username and password).
    setEntered(true)
  }, [])

  if (!entered) {
    return <LoginPage onLogin={enter} theme={theme} onToggleTheme={toggleTheme} />
  }

  return <Console theme={theme} toggleTheme={toggleTheme} />
}

function Console({ theme, toggleTheme }: { theme: Theme; toggleTheme: () => void }) {
  const [route, navigate] = useHashRoute()
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
              Backend is online but no model provider has a key, so agent runs will fail. Set one (e.g.{' '}
              <code>GROQ_API_KEY</code>) in the backend's environment or <code>Backend/.env</code> and restart it.
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
