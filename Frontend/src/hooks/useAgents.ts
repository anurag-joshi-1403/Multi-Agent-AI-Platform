import { useCallback, useEffect, useState } from 'react'
import { listAgents, refreshBackend } from '../api/client'
import type { AgentInfo } from '../types'

export interface AgentsState {
  agents: AgentInfo[]
  loading: boolean
  reload: () => Promise<void>
}

export function useAgents(): AgentsState {
  const [agents, setAgents] = useState<AgentInfo[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    listAgents().then((list) => {
      if (cancelled) return
      setAgents(list)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [])

  const reload = useCallback(async () => {
    setLoading(true)
    const list = await refreshBackend()
    setAgents(list)
    setLoading(false)
  }, [])

  return { agents, loading, reload }
}
