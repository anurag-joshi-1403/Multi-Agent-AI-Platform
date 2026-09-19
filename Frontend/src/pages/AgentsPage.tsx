import { AgentAvatar } from '../components/AgentAvatar'
import { EmptyState } from '../components/EmptyState'
import { IconBot, IconChat, IconCode, IconRefresh } from '../components/Icons'
import { PageHeader } from '../components/PageHeader'
import type { AgentInfo } from '../types'

/** Registry view: every agent the backend knows about, plus how to add another one. */

interface Props {
  agents: AgentInfo[]
  loading: boolean
  onReload: () => void
  onTryAgent: (agentId: string) => void
}

const AGENT_SNIPPET = `@Component
public class TranslationAgent implements Agent {

    @Override
    public String id() { return "translation"; }

    @Override
    public String description() {
        return "Translates text between languages, preserving tone.";
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        // call the LLM, then:
        return AgentResponse.of(id(), translated);
    }
}`

export function AgentsPage({ agents, loading, onReload, onTryAgent }: Props) {
  return (
    <div className="page">
      <PageHeader
        eyebrow="Registry"
        title="Agents"
        description="Each agent is a specialist with a stable id. The backend discovers them automatically at startup, so this list always reflects what is deployed."
        actions={
          <button type="button" className="btn" onClick={onReload} disabled={loading}>
            <IconRefresh /> {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        }
      />

      {agents.length === 0 && !loading ? (
        <div className="card">
          <EmptyState icon={<IconBot />} title="No agents registered">
            The registry returned an empty list. Add an agent bean on the backend (see below) and refresh.
          </EmptyState>
        </div>
      ) : (
        <div className="agent-grid">
          {agents.map((a) => (
            <article key={a.id} className="card agent-card">
              <div className="agent-head">
                <AgentAvatar id={a.id} name={a.name} />
                <div className="grow">
                  <div className="agent-name">{a.name}</div>
                  <div className="agent-id" title="Stable identifier used in API calls">
                    id: {a.id}
                  </div>
                </div>
                <span className="badge badge-ok" title="Discovered by the registry and ready to handle requests">
                  <span className="dot" /> ready
                </span>
              </div>
              <p className="agent-desc">{a.description || 'No description provided.'}</p>
              {a.capabilities.length > 0 && (
                <div className="chips" aria-label="Capabilities">
                  {a.capabilities.map((c) => (
                    <span key={c} className="chip">
                      {c}
                    </span>
                  ))}
                </div>
              )}
              {a.parameters.length > 0 && (
                <div className="agent-params" aria-label="Options">
                  {a.parameters.map((p) => (
                    <div key={p.name} title={p.description}>
                      <code>{p.name}</code>
                      {p.required ? ' (required)' : ''} — {p.description}
                    </div>
                  ))}
                </div>
              )}
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <code className="small faint">POST /api/agents/{a.id}/run</code>
                <button type="button" className="btn btn-sm" onClick={() => onTryAgent(a.id)}>
                  <IconChat /> Try it
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      <section className="card howto-card" aria-labelledby="add-agent-title">
        <div className="card-title" id="add-agent-title">
          <span className="row">
            <IconCode width={15} height={15} /> Adding a new agent
          </span>
        </div>
        <div className="howto-grid">
          <div className="stack">
            <p className="muted">
              Implement the <code>Agent</code> interface as a Spring <code>@Component</code>. The registry picks it up
              on the next start — no orchestrator or config changes, and it appears on this page automatically.
            </p>
            <ul className="muted checklist">
              <li>
                <code>id()</code> must be unique and URL-safe; it becomes the path segment in{' '}
                <code>/api/agents/{'{id}'}/run</code>.
              </li>
              <li>
                <code>description()</code> is shown here and will later let an LLM router pick the agent.
              </li>
              <li>
                Put secondary output (sources, token usage, language) in <code>metadata</code>, not in{' '}
                <code>content</code>.
              </li>
              <li>
                Override <code>parameters()</code> to describe the attributes you read; the playground turns them into
                controls automatically.
              </li>
            </ul>
          </div>
          <pre className="json code-sample">
            <code>{AGENT_SNIPPET}</code>
          </pre>
        </div>
      </section>
    </div>
  )
}
