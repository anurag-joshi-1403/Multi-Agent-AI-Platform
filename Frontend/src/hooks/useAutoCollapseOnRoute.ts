import { useCallback, useEffect, useRef, useState } from 'react'
import type { Route } from './useHashRoute'
import { useSidebar } from './useSidebar'

/**
 * Sidebar collapse, but the Playground gets the full width automatically.
 *
 * Outside Playground this is a pure passthrough to `useSidebar`'s persisted, manually-controlled
 * state — zero behaviour change elsewhere. On Playground the rail is collapsed by default and its
 * state is tracked separately (session-only, not persisted): the toggle button / Ctrl+B can reopen
 * it and it stays open for the rest of that visit, then automatically collapses again the next time
 * Playground is entered from a different page. The manual baseline used by every other page is never
 * read or written while on Playground, so it's exactly as the user left it once they navigate away.
 */
export function useAutoCollapseOnRoute(route: Route): [boolean, () => void] {
  const [manualCollapsed, toggleManual] = useSidebar()
  const [expandedOnPlayground, setExpandedOnPlayground] = useState(false)
  const onPlayground = route.page === 'playground'
  const wasOnPlayground = useRef(onPlayground)

  // Re-collapse every time Playground is (re-)entered from a different page.
  useEffect(() => {
    if (onPlayground && !wasOnPlayground.current) {
      setExpandedOnPlayground(false)
    }
    wasOnPlayground.current = onPlayground
  }, [onPlayground])

  const collapsed = onPlayground ? !expandedOnPlayground : manualCollapsed

  const toggle = useCallback(() => {
    if (onPlayground) {
      setExpandedOnPlayground((v) => !v)
    } else {
      toggleManual()
    }
  }, [onPlayground, toggleManual])

  // Ctrl/⌘+B must call this route-aware toggle. Bound to useSidebar's manual toggle instead, it did
  // nothing visible on Playground and silently flipped the baseline every other page uses.
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.ctrlKey || e.metaKey) && !e.shiftKey && !e.altKey && e.key.toLowerCase() === 'b') {
        e.preventDefault()
        toggle()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [toggle])

  return [collapsed, toggle]
}
