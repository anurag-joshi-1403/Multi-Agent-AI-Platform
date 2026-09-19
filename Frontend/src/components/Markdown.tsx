import { Fragment } from 'react'
import type { ReactNode } from 'react'
import { IconCheck, IconCopy } from './Icons'
import { useCopy } from '../hooks/useCopy'

/**
 * Deliberately tiny markdown renderer: fenced code, inline code, bold, lists,
 * blockquotes, paragraphs. Output is plain React elements — no innerHTML.
 */

/** A fenced code block with a language chip and its own copy button (independent of the message-level copy). */
function CodeBlock({ lang, body }: { lang: string; body: string }) {
  const [copied, copy] = useCopy()
  return (
    <div className="md-code">
      <div className="md-code-head">
        <span className="md-code-lang">{lang || 'code'}</span>
        <button
          type="button"
          className={`md-code-copy ${copied ? 'copied' : ''}`}
          onClick={() => copy(body)}
          aria-label={copied ? 'Copied' : 'Copy code'}
          title={copied ? 'Copied!' : 'Copy code'}
        >
          {copied ? <IconCheck width={12} height={12} /> : <IconCopy width={12} height={12} />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      <pre data-lang={lang || undefined}>
        <code>{body}</code>
      </pre>
    </div>
  )
}

function inline(text: string): ReactNode[] {
  const out: ReactNode[] = []
  const re = /(`[^`]+`|\*\*[^*]+\*\*)/g
  let last = 0
  let m: RegExpExecArray | null
  let k = 0
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) out.push(text.slice(last, m.index))
    const tok = m[0]
    if (tok.startsWith('`')) out.push(<code key={k++}>{tok.slice(1, -1)}</code>)
    else out.push(<strong key={k++}>{tok.slice(2, -2)}</strong>)
    last = m.index + tok.length
  }
  if (last < text.length) out.push(text.slice(last))
  return out
}

type Block =
  | { type: 'code'; lang: string; body: string }
  | { type: 'ul'; items: string[] }
  | { type: 'ol'; items: string[] }
  | { type: 'quote'; lines: string[] }
  | { type: 'p'; lines: string[] }

function parse(src: string): Block[] {
  const lines = src.replace(/\r\n/g, '\n').split('\n')
  const blocks: Block[] = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    if (line.startsWith('```')) {
      const lang = line.slice(3).trim()
      const body: string[] = []
      i++
      while (i < lines.length && !lines[i].startsWith('```')) body.push(lines[i++])
      i++ // closing fence
      blocks.push({ type: 'code', lang, body: body.join('\n') })
      continue
    }
    if (/^\s*[-*]\s+/.test(line)) {
      const items: string[] = []
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) items.push(lines[i++].replace(/^\s*[-*]\s+/, ''))
      blocks.push({ type: 'ul', items })
      continue
    }
    if (/^\s*\d+[.)]\s+/.test(line)) {
      const items: string[] = []
      while (i < lines.length && /^\s*\d+[.)]\s+/.test(lines[i])) items.push(lines[i++].replace(/^\s*\d+[.)]\s+/, ''))
      blocks.push({ type: 'ol', items })
      continue
    }
    if (line.startsWith('>')) {
      const q: string[] = []
      while (i < lines.length && lines[i].startsWith('>')) q.push(lines[i++].replace(/^>\s?/, ''))
      blocks.push({ type: 'quote', lines: q })
      continue
    }
    if (line.trim() === '') {
      i++
      continue
    }
    const p: string[] = []
    while (
      i < lines.length &&
      lines[i].trim() !== '' &&
      !lines[i].startsWith('```') &&
      !lines[i].startsWith('>') &&
      !/^\s*[-*]\s+/.test(lines[i]) &&
      !/^\s*\d+[.)]\s+/.test(lines[i])
    ) {
      p.push(lines[i++])
    }
    blocks.push({ type: 'p', lines: p })
  }
  return blocks
}

export function Markdown({ source }: { source: string }) {
  const blocks = parse(source)
  return (
    <div className="md">
      {blocks.map((b, idx) => {
        switch (b.type) {
          case 'code':
            return <CodeBlock key={idx} lang={b.lang} body={b.body} />

          case 'ul':
            return (
              <ul key={idx}>
                {b.items.map((it, j) => (
                  <li key={j}>{inline(it)}</li>
                ))}
              </ul>
            )
          case 'ol':
            return (
              <ol key={idx}>
                {b.items.map((it, j) => (
                  <li key={j}>{inline(it)}</li>
                ))}
              </ol>
            )
          case 'quote':
            return (
              <blockquote key={idx}>
                {b.lines.map((l, j) => (
                  <Fragment key={j}>
                    {inline(l)}
                    {j < b.lines.length - 1 && <br />}
                  </Fragment>
                ))}
              </blockquote>
            )
          default:
            return (
              <p key={idx}>
                {b.lines.map((l, j) => (
                  <Fragment key={j}>
                    {inline(l)}
                    {j < b.lines.length - 1 && <br />}
                  </Fragment>
                ))}
              </p>
            )
        }
      })}
    </div>
  )
}
