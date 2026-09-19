import { useSyncExternalStore } from 'react'
import { getBackendStatus, subscribeBackendStatus } from '../api/client'
import type { BackendStatus } from '../api/client'

export function useBackendStatus(): BackendStatus {
  return useSyncExternalStore(subscribeBackendStatus, getBackendStatus, getBackendStatus)
}
