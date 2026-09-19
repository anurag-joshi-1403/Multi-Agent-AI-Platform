import { useState } from 'react'
import { apiBase, simulationForced } from '../api/client'
import { BackendStatusBadge } from '../components/BackendStatusBadge'
import { IconKey, IconRefresh, IconTrash } from '../components/Icons'
import { PageHeader } from '../components/PageHeader'
import type { Theme } from '../hooks/useTheme'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'
import type { PlatformStatus } from '../types'

interface Props {
  theme: Theme
  onToggleTheme: () => void
  onReloadAgents: () => Promise<void>
  onClearConversations: () => void
  conversationCount: number
  platform: PlatformStatus | null
}

export function SettingsPage({
  theme,
  onToggleTheme,
  onReloadAgents,
  onClearConversations,
  conversationCount,
  platform,
}: Props) {
  const [base, setBase] = useState(() => loadString(STORAGE_KEYS.apiBase) ?? '')
  const [simulate, setSimulate] = useState(simulationForced)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  async function applyConnection() {
    setSaving(true)
    saveString(STORAGE_KEYS.apiBase, base.trim() || null)
    saveString(STORAGE_KEYS.simulate, simulate ? 'true' : null)
    await onReloadAgents()
    setSaving(false)
    setSaved(true)
    setTimeout(() => setSaved(false), 1800)
  }

  function clearData() {
    if (conversationCount === 0) return
    if (window.confirm(`Delete all ${conversationCount} local conversations? This cannot be undone.`)) {
      onClearConversations()
    }
  }

  return (
    <div className="page">
      <PageHeader
        eyebrow="Preferences"
        title="Settings"
        description="Connection, appearance and local data. Everything here is stored in this browser only — nothing is sent to the backend."
      />

      <div className="settings">
        <section className="card" aria-labelledby="conn-title">
          <div className="card-title" id="conn-title">
            Backend connection
            <BackendStatusBadge />
          </div>

          <div className="field">
            <label htmlFor="api-base">API base URL</label>
            <input
              id="api-base"
              className="input mono"
              placeholder={apiBase()}
              value={base}
              onChange={(e) => setBase(e.target.value)}
              spellCheck={false}
            />
            <span className="hint">
              Leave empty to use <code>{import.meta.env.VITE_API_BASE ?? '/api'}</code> (proxied to{' '}
              <code>localhost:8080</code> by the Vite dev server).
            </span>
          </div>

          <div className="settings-row">
            <div className="field">
              <label htmlFor="simulate">Simulation mode</label>
              <span className="hint">
                Never call the backend; answer with canned responses. Handy for demoing the UI or working offline.
              </span>
            </div>
            <label className="switch">
              <input id="simulate" type="checkbox" checked={simulate} onChange={(e) => setSimulate(e.target.checked)} />
            </label>
          </div>

          <div className="row" style={{ justifyContent: 'flex-end' }}>
            {saved && <span className="small muted">Applied</span>}
            <button type="button" className="btn btn-primary" onClick={applyConnection} disabled={saving}>
              <IconRefresh /> {saving ? 'Reconnecting…' : 'Apply & reconnect'}
            </button>
          </div>
        </section>

        <section className="card" aria-labelledby="provider-title">
          <div className="card-title" id="provider-title">
            <span className="row">
              <IconKey width={15} height={15} /> Model provider
            </span>
            {platform &&
              (platform.apiKeyConfigured ? (
                <span className="badge badge-ok">
                  <span className="dot" /> key configured
                </span>
              ) : (
                <span className="badge badge-warn">
                  <span className="dot" /> key missing
                </span>
              ))}
          </div>
          {platform ? (
            <>
              <dl className="kv">
                <dt>provider</dt>
                <dd>{platform.providerName}</dd>
                <dt>model</dt>
                <dd>{platform.model}</dd>
                <dt>agents</dt>
                <dd>{platform.agents}</dd>
                <dt>memory</dt>
                <dd>{platform.memoryMaxMessages} messages / conversation</dd>
                <dt>documents</dt>
                <dd>
                  {platform.documents.stored} stored · max {platform.documents.maxStored} ·{' '}
                  {Math.round(platform.documents.maxContextChars / 1000)}k chars each
                </dd>
              </dl>
              <p className="small muted">
                The provider is chosen on the backend with <code>AI_PROVIDER</code> (<code>google-genai</code>,{' '}
                <code>anthropic</code> or <code>openai</code>) and its key with{' '}
                <code>{platform.keyEnvVar === '—' ? 'GEMINI_API_KEY' : platform.keyEnvVar}</code>. Keys are never
                entered here or stored in the browser — set them in the backend's environment and restart it.
              </p>
            </>
          ) : (
            <p className="small muted">Provider details appear once the backend is reachable.</p>
          )}
        </section>

        <section className="card" aria-labelledby="appearance-title">
          <div className="card-title" id="appearance-title">
            Appearance
          </div>
          <div className="settings-row">
            <div className="field">
              <label htmlFor="theme-toggle">Dark theme</label>
              <span className="hint">Follows your system preference until you change it here.</span>
            </div>
            <label className="switch">
              <input id="theme-toggle" type="checkbox" checked={theme === 'dark'} onChange={onToggleTheme} />
            </label>
          </div>
        </section>

        <section className="card" aria-labelledby="data-title">
          <div className="card-title" id="data-title">
            Local data
          </div>
          <div className="settings-row">
            <div className="field">
              <label>Conversations</label>
              <span className="hint">
                {conversationCount} conversation{conversationCount === 1 ? '' : 's'} saved in this browser's storage.
              </span>
            </div>
            <button type="button" className="btn btn-danger" onClick={clearData} disabled={conversationCount === 0}>
              <IconTrash /> Delete all
            </button>
          </div>
        </section>
      </div>
    </div>
  )
}
