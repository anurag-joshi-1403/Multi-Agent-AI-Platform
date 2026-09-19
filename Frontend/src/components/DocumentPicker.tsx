import { useRef } from 'react'
import type { DocumentsApi } from '../hooks/useDocuments'
import { IconTrash, IconUpload } from './Icons'

interface Props {
  id: string
  value: string
  onChange: (documentId: string) => void
  documents: DocumentsApi
}

const ACCEPT = '.pdf,.txt,.md,.markdown,.csv,.json,.xml,.html,.yaml,.yml,.log,.java,.ts,.js,.py,.kt,.go,.rs,.c,.cpp,.h,.cs,.sql'

/** Pick an uploaded document for the document agent, or upload a new one right here. */
export function DocumentPicker({ id, value, onChange, documents }: Props) {
  const fileInput = useRef<HTMLInputElement>(null)
  const selected = documents.documents.find((d) => d.id === value)

  async function onFile(file: File | undefined) {
    if (!file) return
    const doc = await documents.upload(file)
    if (doc) onChange(doc.id)
    if (fileInput.current) fileInput.current.value = ''
  }

  return (
    <div className="doc-picker">
      <div className="row" style={{ gap: 6 }}>
        <select
          id={id}
          className="select"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={documents.loading}
          aria-label="Document"
        >
          <option value="">{documents.loading ? 'Loading…' : documents.documents.length ? 'Choose a document…' : 'No documents yet'}</option>
          {documents.documents.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
              {d.pages ? ` · ${d.pages} p` : ''} · {Math.round(d.chars / 1000)}k chars
            </option>
          ))}
        </select>
        <button
          type="button"
          className="btn btn-sm"
          onClick={() => fileInput.current?.click()}
          disabled={documents.uploading}
          title="Upload a PDF or text file"
        >
          {documents.uploading ? <span className="spinner spinner-dark" aria-hidden /> : <IconUpload />}
          {documents.uploading ? 'Uploading…' : 'Upload'}
        </button>
        {selected && (
          <button
            type="button"
            className="btn btn-ghost btn-icon"
            onClick={() => {
              void documents.remove(selected.id)
              onChange('')
            }}
            aria-label={`Delete ${selected.name}`}
            title="Delete this document"
          >
            <IconTrash width={15} height={15} />
          </button>
        )}
        <input
          ref={fileInput}
          type="file"
          accept={ACCEPT}
          hidden
          onChange={(e) => void onFile(e.target.files?.[0])}
          aria-hidden
        />
      </div>
      {selected && <p className="small faint doc-preview">“{selected.preview}”</p>}
      {documents.error && (
        <p className="small" style={{ color: 'var(--danger)' }}>
          {documents.error}
        </p>
      )}
    </div>
  )
}
