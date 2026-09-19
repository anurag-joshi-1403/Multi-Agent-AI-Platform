import { useCallback, useEffect, useState } from 'react'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'

/** Collapsed/expanded sidebar, remembered per browser. Ctrl/⌘+B toggles it. */
export function useSidebar(): [boolean, () => void] {
  const [collapsed, setCollapsed] = useState(() => loadString(STORAGE_KEYS.sidebar) === 'collapsed')

  useEffect(() => {
    saveString(STORAGE_KEYS.sidebar, collapsed ? 'collapsed' : 'open')
  }, [collapsed])

  const toggle = useCallback(() => setCollapsed((c) => !c), [])

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
