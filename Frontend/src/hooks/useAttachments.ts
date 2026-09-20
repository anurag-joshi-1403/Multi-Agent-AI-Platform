import { useCallback, useState } from 'react'
import { deleteDocument, uploadDocument } from '../api/client'
import type { Attachment } from '../types'

export interface AttachmentsApi {
  /** Files staged for the next message, in the order they were attached. */
  pending: Attachment[]
  /** How many uploads are still in flight. */
  uploading: number
  error: string | null
  add: (files: Iterable<File>) => Promise<void>
  remove: (id: string) => void
  clear: () => void
  dismissError: () => void
}

/**
 * Files staged for the next message. Each is uploaded as soon as it is attached, so only its id
 * travels with the send and the same file never gets posted twice.
 */
export function useAttachments(): AttachmentsApi {
  const [pending, setPending] = useState<Attachment[]>([])
  const [uploading, setUploading] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const add = useCallback(async (files: Iterable<File>) => {
    const list = [...files]
    if (list.length === 0) return
    setUploading((n) => n + list.length)
    setError(null)
    await Promise.all(
      list.map(async (file) => {
        try {
          const doc = await uploadDocument(file)
          setPending((prev) => [...prev, { id: doc.id, name: doc.name }])
        } catch (e) {
          setError(e instanceof Error ? `${file.name}: ${e.message}` : `Could not upload ${file.name}`)
        } finally {
          setUploading((n) => n - 1)
        }
      }),
    )
  }, [])

  const remove = useCallback((id: string) => {
    setPending((prev) => prev.filter((a) => a.id !== id))
    // Best effort: an orphaned upload is evicted by the store's capacity limit anyway.
    void deleteDocument(id).catch(() => undefined)
  }, [])

  const clear = useCallback(() => setPending([]), [])
  const dismissError = useCallback(() => setError(null), [])

  return { pending, uploading, error, add, remove, clear, dismissError }
}
