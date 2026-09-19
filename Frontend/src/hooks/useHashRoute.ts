import { useCallback, useSyncExternalStore } from 'react'

export type Route =
  | { page: 'overview' }
  | { page: 'playground'; conversationId?: string }
  | { page: 'agents' }
  | { page: 'settings' }

function parse(hash: string): Route {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  switch (parts[0]) {
    case 'playground':
      return { page: 'playground', conversationId: parts[1] }
    case 'agents':
      return { page: 'agents' }
    case 'settings':
      return { page: 'settings' }
    default:
      return { page: 'overview' }
  }
}

function subscribe(cb: () => void) {
  window.addEventListener('hashchange', cb)
  return () => window.removeEventListener('hashchange', cb)
}

function getSnapshot() {
  return window.location.hash
}

export function href(route: Route): string {
  switch (route.page) {
    case 'playground':
      return route.conversationId ? `#/playground/${route.conversationId}` : '#/playground'
    case 'agents':
      return '#/agents'
    case 'settings':
      return '#/settings'
    default:
      return '#/'
  }
}

export function useHashRoute(): [Route, (route: Route) => void] {
  const hash = useSyncExternalStore(subscribe, getSnapshot, () => '')
  const navigate = useCallback((route: Route) => {
    window.location.hash = href(route)
  }, [])
  return [parse(hash), navigate]
}
