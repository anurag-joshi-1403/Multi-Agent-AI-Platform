import { useCallback, useEffect, useState } from 'react'
import { deleteDocument, listDocuments, uploadDocument } from '../api/client'
import type { DocumentSummary } from '../types'
import { useBackendStatus } from './useBackendStatus'

export interface DocumentsApi {
  documents: DocumentSummary[]
  loading: boolean
  uploading: boolean
  error: string | null
  refresh: () => Promise<void>
  upload: (file: File) => Promise<DocumentSummary | null>
  remove: (id: string) => Promise<void>
}

/** Documents available to the document agent, kept in sync with the backend (or the simulation). */
export function useDocuments(enabled: boolean): DocumentsApi {
  const status = useBackendStatus()
  const [documents, setDocuments] = useState<DocumentSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setDocuments(await listDocuments())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load documents')
    } finally {
      setLoading(false)
    }
  }, [])

  // Initial load / reload when the backend comes online: subscribe-style, no synchronous setState.
  useEffect(() => {
    if (!enabled || status === 'checking') return
    let cancelled = false
    listDocuments()
      .then((docs) => {
        if (cancelled) return
        setDocuments(docs)
        setError(null)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Could not load documents')
      })
    return () => {
      cancelled = true
    }
  }, [enabled, status])

  const upload = useCallback(async (file: File) => {
    setUploading(true)
    try {
      const doc = await uploadDocument(file)
      setDocuments((prev) => [doc, ...prev.filter((d) => d.id !== doc.id)])
      setError(null)
      return doc
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Upload failed')
      return null
    } finally {
      setUploading(false)
    }
  }, [])

  const remove = useCallback(async (id: string) => {
    try {
      await deleteDocument(id)
      setDocuments((prev) => prev.filter((d) => d.id !== id))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Delete failed')
    }
  }, [])

  return { documents, loading, uploading, error, refresh, upload, remove }
}
