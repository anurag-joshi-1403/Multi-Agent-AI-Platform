import { useCallback, useEffect, useState } from 'react'

async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

/** Copy-to-clipboard with a short "copied" confirmation window. */
export function useCopy(resetAfterMs = 1600): [boolean, (text: string) => void] {
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!copied) return
    const t = setTimeout(() => setCopied(false), resetAfterMs)
    return () => clearTimeout(t)
  }, [copied, resetAfterMs])

  const copy = useCallback((text: string) => {
    void copyText(text).then((ok) => {
      if (ok) setCopied(true)
    })
  }, [])

  return [copied, copy]
}
