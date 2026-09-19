import { useCallback, useEffect, useState } from 'react'
import { loadString, saveString, STORAGE_KEYS } from '../lib/storage'

export type Theme = 'dark' | 'light'

function initial(): Theme {
  const stored = loadString(STORAGE_KEYS.theme)
  if (stored === 'light' || stored === 'dark') return stored
  return window.matchMedia?.('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

export function useTheme(): [Theme, () => void] {
  const [theme, setTheme] = useState<Theme>(initial)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    saveString(STORAGE_KEYS.theme, theme)
  }, [theme])

  const toggle = useCallback(() => setTheme((t) => (t === 'dark' ? 'light' : 'dark')), [])
  return [theme, toggle]
}
