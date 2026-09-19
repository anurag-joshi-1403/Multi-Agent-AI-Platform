import { useEffect, useState } from 'react'
import { getPlatform } from '../api/client'
import type { PlatformStatus } from '../types'
import { useBackendStatus } from './useBackendStatus'

/** Provider/model/key state from `GET /api/platform`, refreshed whenever the backend status changes. */
export function usePlatform(): PlatformStatus | null {
  const status = useBackendStatus()
  const [platform, setPlatform] = useState<PlatformStatus | null>(null)

  useEffect(() => {
    if (status === 'checking') return
    let cancelled = false
    getPlatform()
      .then((p) => {
        if (!cancelled) setPlatform(p)
      })
      .catch(() => {
        if (!cancelled) setPlatform(null)
      })
    return () => {
      cancelled = true
    }
  }, [status])

  return platform
}
