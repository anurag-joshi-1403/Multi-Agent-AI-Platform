import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { saveString, STORAGE_KEYS } from '../lib/storage'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

/**
 * Last line of defence for render errors. Without it React unmounts the whole tree and leaves a
 * blank page; with it the person gets an explanation and a way out — including clearing the saved
 * conversations, the one piece of persisted state that can keep a crash coming back on every load.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: unknown): State {
    return { error: error instanceof Error ? error : new Error(String(error)) }
  }

  componentDidCatch(error: unknown, info: ErrorInfo) {
    console.error('Unhandled error while rendering the console', error, info.componentStack)
  }

  private clearConversations = () => {
    if (!window.confirm('Delete all conversations saved in this browser and reload? This cannot be undone.')) return
    saveString(STORAGE_KEYS.conversations, null)
    window.location.reload()
  }

  render() {
    const { error } = this.state
    if (!error) return this.props.children
    return (
      <main className="app-crash">
        <div className="empty" role="alert">
          <h1>Something went wrong</h1>
          <div className="empty-body">
            The console hit an unexpected error. Reloading usually fixes it. If it keeps happening, the
            conversations saved in this browser may be damaged — clearing them starts fresh.
          </div>
          <div className="row empty-action">
            <button type="button" className="btn btn-primary" onClick={() => window.location.reload()}>
              Reload
            </button>
            <button type="button" className="btn btn-danger" onClick={this.clearConversations}>
              Clear saved conversations
            </button>
          </div>
          <details className="app-crash-details">
            <summary>Technical details</summary>
            <pre className="json">{error.message}</pre>
          </details>
        </div>
      </main>
    )
  }
}
