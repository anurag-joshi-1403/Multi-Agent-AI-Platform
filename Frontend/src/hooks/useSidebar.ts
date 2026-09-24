import { useCallback, useEffect, useState } from 'react'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'

/**
 * Collapsed/expanded sidebar, remembered per browser. The Ctrl/⌘+B shortcut is bound in
 * `useAutoCollapseOnRoute`, not here, so it drives the same route-aware toggle as the button.
 */
export function useSidebar(): [boolean, () => void] {
  const [collapsed, setCollapsed] = useState(() => loadString(STORAGE_KEYS.sidebar) === 'collapsed')

  useEffect(() => {
    saveString(STORAGE_KEYS.sidebar, collapsed ? 'collapsed' : 'open')
  }, [collapsed])

  const toggle = useCallback(() => setCollapsed((c) => !c), [])

  return [collapsed, toggle]
}
