import { useMemo } from 'react'
import type { CSSProperties } from 'react'
import { AgentAvatar } from '../components/AgentAvatar'
import { EmptyState } from '../components/EmptyState'
import {
  IconActivity,
  IconArrowRight,
  IconBot,
  IconChat,
  IconCheck,
  IconClock,
  IconKey,
  IconLayers,
  IconSettings,
} from '../components/Icons'
import { PageHeader } from '../components/PageHeader'
import type { BackendStatus } from '../api/client'
import { href } from '../hooks/useHashRoute'
import { formatMs, formatRelative } from '../lib/util'
import type { AgentInfo, Conversation, PlatformStatus } from '../types'

/**
 * Landing page. Answers three questions for a newcomer:
 *   1. What is this platform?  (intro + request pipeline)
 *   2. What should I do first? (getting-started checklist)
 *   3. What has happened so far? (stats, recent conversations, registry)
 */

interface Props {
  agents: AgentInfo[]
  agentsLoading: boolean
  conversations: Conversation[]
  backend: BackendStatus
  platform: PlatformStatus | null
}

const PIPELINE = [
  { title: 'You send a message', desc: 'The playground builds an AgentRequest: your text plus optional attributes.' },
  { title: 'Orchestrator receives it', desc: 'One entry point for every call. It times the run and logs it.' },
  { title: 'Registry finds the agent', desc: 'Agents are Spring beans discovered at startup — no config to edit.' },
  { title: 'Agent answers', desc: 'The specialist calls the LLM and returns content plus metadata.' },
]

/** Each stat gets its own semantic colour so the grid reads at a glance instead of as four grey boxes. */
const STAT_COLORS = {
  accent: 'var(--accent)',
  info: 'var(--info)',
  ok: 'var(--ok)',
  warn: 'var(--warn)',
} as const

export function OverviewPage({ agents, agentsLoading, conversations, backend, platform }: Props) {
  const stats = useMemo(() => {
    const agentMsgs = conversations.flatMap((c) => c.messages.filter((m) => m.role === 'agent'))
    const totalMsgs = conversations.reduce((n, c) => n + c.messages.length, 0)
    const latencies = agentMsgs.map((m) => m.elapsedMs ?? 0).filter((v) => v > 0)
    const avg = latencies.length ? latencies.reduce((a, b) => a + b, 0) / latencies.length : 0
    const byAgent = new Map<string, number>()
    for (const m of agentMsgs) byAgent.set(m.agentId ?? '?', (byAgent.get(m.agentId ?? '?') ?? 0) + 1)
    const top = [...byAgent.entries()].sort((a, b) => b[1] - a[1])[0]
    return { totalMsgs, agentRuns: agentMsgs.length, avg, top }
  }, [conversations])

  const recent = conversations.slice(0, 5)
  const hasSent = stats.agentRuns > 0
  const hasInspected = hasSent // inspecting is available as soon as there is a reply

  const online = backend === 'online'
  const keyReady = online && !!platform?.apiKeyConfigured
  const steps = [
    {
      done: online,
      title: online ? 'Backend connected' : 'Connect the backend',
      desc: online
        ? 'Talking to the Spring Boot API.'
        : 'Start the Spring Boot app on port 8080. Until then, replies are simulated so you can still explore.',
      link: href({ page: 'settings' }),
      linkText: 'Settings',
    },
    {
      done: keyReady,
      title: keyReady ? `${platform?.providerName} key configured` : 'Add a model provider key',
      desc: keyReady
        ? `Agents answer with ${platform?.model}.`
        : online && platform
          ? `Set ${platform.keyEnvVar} in the backend's environment and restart it. Until then agent runs return an error.`
          : 'Once the backend is up, export the API key for your chosen provider (Gemini, Claude or OpenAI).',
      link: href({ page: 'settings' }),
      linkText: 'How',
    },
    {
      done: hasSent,
      title: 'Send your first message',
      desc: 'Pick an agent in the playground and ask it something — or click one of the suggested prompts.',
      link: href({ page: 'playground' }),
      linkText: 'Open playground',
    },
    {
      done: hasInspected,
      title: 'Inspect a response',
      desc: 'Click any agent reply to see its latency, model and metadata, and the exact request that produced it.',
      link: href({ page: 'playground' }),
      linkText: 'Playground',
    },
  ]
  const doneCount = steps.filter((s) => s.done).length

  return (
    <div className="page">
      <PageHeader
        eyebrow="Home"
        title="Multi-Agent AI Platform"
        description="A single orchestrator routes each request to a specialised AI agent — coding, research, documents and more. Use this console to try agents, inspect what they return, and see how requests flow."
        actions={
          <a className="btn btn-primary" href={href({ page: 'playground' })}>
            <IconChat /> Open playground
          </a>
        }
      />

      <div className="overview-top">
        <section className="card fade-up" style={{ '--i': 1 } as CSSProperties} aria-labelledby="start-title">
          <div className="card-title" id="start-title">
            <span>Getting started</span>
            <span className="badge">
              {doneCount}/{steps.length} done
            </span>
          </div>
          <div className="progress-track" aria-hidden>
            <div className="progress-fill" style={{ width: `${(doneCount / steps.length) * 100}%` }} />
          </div>
          <ol className="steps">
            {steps.map((s, i) => (
              <li key={s.title} className={`step ${s.done ? 'done' : ''}`}>
                <span className="step-marker" aria-hidden>
                  {s.done ? <IconCheck width={14} height={14} /> : i + 1}
                </span>
                <div className="grow">
                  <div className="step-title">{s.title}</div>
                  <div className="step-desc">{s.desc}</div>
                </div>
                {!s.done && (
                  <a className="btn btn-sm" href={s.link}>
                    {s.linkText} <IconArrowRight width={14} height={14} />
                  </a>
                )}
              </li>
            ))}
          </ol>
        </section>

        <section className="stat-grid stat-grid-compact" aria-label="Key numbers">
          <div
            className="stat fade-up"
            style={{ '--i': 2, '--stat-color': STAT_COLORS.accent } as CSSProperties}
          >
            <div className="stat-label">
              <span className="stat-icon">
                <IconBot />
              </span>
              Agents
            </div>
            <div className="stat-value">{agentsLoading ? '—' : agents.length}</div>
            <div className="stat-sub">registered in the backend</div>
          </div>
          <div className="stat fade-up" style={{ '--i': 3, '--stat-color': STAT_COLORS.info } as CSSProperties}>
            <div className="stat-label">
              <span className="stat-icon">
                <IconChat />
              </span>
              Conversations
            </div>
            <div className="stat-value">{conversations.length}</div>
            <div className="stat-sub">{stats.totalMsgs} messages, saved in this browser</div>
          </div>
          <div className="stat fade-up" style={{ '--i': 4, '--stat-color': STAT_COLORS.ok } as CSSProperties}>
            <div className="stat-label">
              <span className="stat-icon">
                <IconActivity />
              </span>
              Agent runs
            </div>
            <div className="stat-value">{stats.agentRuns}</div>
            <div className="stat-sub">{stats.top ? `most used: ${stats.top[0]}` : 'no runs yet'}</div>
          </div>
          <div className="stat fade-up" style={{ '--i': 5, '--stat-color': STAT_COLORS.warn } as CSSProperties}>
            <div className="stat-label">
              <span className="stat-icon">
                <IconClock />
              </span>
              Avg. latency
            </div>
            <div className="stat-value">{stats.avg ? formatMs(stats.avg) : '—'}</div>
            <div className="stat-sub">round-trip, as seen by the browser</div>
          </div>
        </section>
      </div>

      <section
        className="card fade-up"
        style={{ marginBottom: 16, '--i': 6 } as CSSProperties}
        aria-labelledby="pipeline-title"
      >
        <div className="card-title" id="pipeline-title">
          <span className="row">
            <IconLayers width={15} height={15} /> How a request flows through the platform
          </span>
          <span className="badge" title="You choose the agent. Automatic routing arrives in a later phase.">
            Phase 2 · you pick the agent
          </span>
        </div>
        <div className="pipeline">
          {PIPELINE.map((s, i) => (
            <div className="pipeline-step" key={s.title}>
              <span className="step-n">STEP {i + 1}</span>
              <span className="step-title">{s.title}</span>
              <span className="step-desc">{s.desc}</span>
            </div>
          ))}
        </div>
      </section>

      <div className="overview-grid">
        <section className="card fade-up" style={{ '--i': 7 } as CSSProperties} aria-labelledby="recent-title">
          <div className="card-title" id="recent-title">
            Recent conversations
            <a className="small" href={href({ page: 'playground' })}>
              View all
            </a>
          </div>
          {recent.length === 0 ? (
            <EmptyState compact icon={<IconChat />} title="No conversations yet">
              Start one in the playground and it will show up here.
            </EmptyState>
          ) : (
            <div className="list">
              {recent.map((c, i) => {
                const agent = agents.find((a) => a.id === c.agentId)
                return (
                  <a
                    key={c.id}
                    className="list-item list-in"
                    style={{ '--i': i } as CSSProperties}
                    href={href({ page: 'playground', conversationId: c.id })}
                  >
                    <AgentAvatar id={c.agentId} name={agent?.name} size="sm" />
                    <div className="grow">
                      <div className="title">{c.title}</div>
                      <div className="small faint">
                        {agent?.name ?? c.agentId} · {c.messages.length} messages
                      </div>
                    </div>
                    <span className="small faint">{formatRelative(c.updatedAt)}</span>
                    <IconArrowRight width={14} height={14} className="faint" />
                  </a>
                )
              })}
            </div>
          )}
        </section>

        <section className="card fade-up" style={{ '--i': 8 } as CSSProperties} aria-labelledby="agents-title">
          <div className="card-title" id="agents-title">
            Available agents
            <a className="small" href={href({ page: 'agents' })}>
              See details
            </a>
          </div>
          {agents.length === 0 ? (
            <EmptyState compact icon={<IconBot />} title={agentsLoading ? 'Loading agents…' : 'No agents found'}>
              {!agentsLoading && 'The registry returned nothing. Check the backend or reconnect from Settings.'}
            </EmptyState>
          ) : (
            <div className="list">
              {agents.map((a, i) => (
                <a key={a.id} className="list-item list-in" style={{ '--i': i } as CSSProperties} href={href({ page: 'agents' })}>
                  <AgentAvatar id={a.id} name={a.name} size="sm" />
                  <div className="grow">
                    <div className="title">{a.name}</div>
                    <div className="small muted">{a.description}</div>
                  </div>
                </a>
              ))}
            </div>
          )}
          {backend !== 'online' ? (
            <p className="small muted" style={{ marginTop: 12 }}>
              <IconSettings width={13} height={13} style={{ verticalAlign: '-2px' }} /> These are simulated agents
              because the backend is not connected.
            </p>
          ) : (
            platform && (
              <p className="small muted" style={{ marginTop: 12 }}>
                <IconKey width={13} height={13} style={{ verticalAlign: '-2px' }} /> Powered by {platform.providerName}{' '}
                · <code>{platform.model}</code>
                {platform.apiKeyConfigured ? '' : ` · ${platform.keyEnvVar} not set`}
              </p>
            )
          )}
        </section>
      </div>
    </div>
  )
}
