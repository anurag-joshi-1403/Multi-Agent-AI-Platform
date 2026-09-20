import type { AgentInfo, AgentRequest, AgentResponse, DocumentSummary, PlatformStatus } from '../types'

/**
 * Simulated registry, documents and responses, used when the backend is unreachable or when
 * simulation mode is switched on in Settings. Keeps the UI fully explorable without a server.
 * Agent ids, descriptions and parameters mirror the real backend agents.
 */
export const MOCK_AGENTS: AgentInfo[] = [
  {
    id: 'coding',
    name: 'Coding Agent',
    description: 'Generates, explains and refactors code in mainstream languages with a short rationale.',
    capabilities: ['Generate', 'Explain', 'Refactor', 'Tests'],
    parameters: [
      {
        name: 'language',
        label: 'Language',
        description: 'Target programming language. Leave on Auto-detect to infer it from context.',
        type: 'SELECT',
        required: false,
        options: [
          '',
          'TypeScript',
          'JavaScript',
          'Python',
          'Java',
          'C#',
          'C++',
          'C',
          'Go',
          'Rust',
          'Kotlin',
          'Swift',
          'PHP',
          'Ruby',
          'SQL',
          'Bash',
          'HTML',
          'CSS',
          'R',
          'Scala',
          'Dart',
          'Elixir',
        ],
        defaultValue: '',
      },
    ],
  },
  {
    id: 'document',
    name: 'Document Agent',
    description: 'Answers questions grounded in the PDF or text files you attach to the message.',
    capabilities: ['PDF Q&A', 'Extraction', 'Grounded answers'],
    parameters: [],
  },
  {
    id: 'general',
    name: 'General Assistant',
    description: 'Fallback conversational assistant for anything the specialists do not cover.',
    capabilities: ['Chat', 'Brainstorm', 'Drafting'],
    parameters: [],
  },
  {
    id: 'research',
    name: 'Research Agent',
    description:
      'Breaks a topic into findings, open questions and a confidence rating. Knowledge-based; live web search arrives later.',
    capabilities: ['Analysis', 'Findings', 'Confidence'],
    parameters: [],
  },
  {
    id: 'summarizer',
    name: 'Summarizer Agent',
    description: 'Condenses long text into key points, a TL;DR or an executive brief.',
    capabilities: ['Bullets', 'TL;DR', 'Executive brief'],
    parameters: [
      {
        name: 'style',
        label: 'Style',
        description: 'Shape of the summary.',
        type: 'SELECT',
        required: false,
        options: ['bullets', 'tldr', 'executive'],
        defaultValue: 'bullets',
      },
      {
        name: 'maxWords',
        label: 'Max words',
        description: 'Upper bound on the summary length.',
        type: 'NUMBER',
        required: false,
        options: [],
        defaultValue: 150,
      },
    ],
  },
]

export const MOCK_PLATFORM: PlatformStatus = {
  provider: 'simulated',
  providerName: 'Simulation',
  model: 'simulated',
  apiKeyConfigured: true,
  keyEnvVar: '—',
  agents: MOCK_AGENTS.length,
  memoryMaxMessages: 20,
  documents: { stored: 0, maxStored: 50, maxContextChars: 60000 },
}

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms))

// --- simulated document store ---------------------------------------------
const mockDocuments: DocumentSummary[] = []

function mockId() {
  return 'doc_' + Math.random().toString(36).slice(2, 14)
}

export async function mockUploadDocument(file: File): Promise<DocumentSummary> {
  await delay(400)
  const isPdf = file.type.includes('pdf') || file.name.toLowerCase().endsWith('.pdf')
  const text = isPdf ? `[page 1]\n(simulated extraction of ${file.name})` : await file.text()
  const doc: DocumentSummary = {
    id: mockId(),
    name: file.name || 'upload',
    mediaType: file.type || 'application/octet-stream',
    chars: text.length,
    pages: isPdf ? 1 : null,
    uploadedAt: new Date().toISOString(),
    preview: text.replace(/\s+/g, ' ').slice(0, 200),
  }
  mockDocuments.unshift(doc)
  MOCK_PLATFORM.documents.stored = mockDocuments.length
  return doc
}

export async function mockDeleteDocument(id: string): Promise<void> {
  await delay(80)
  const idx = mockDocuments.findIndex((d) => d.id === id)
  if (idx >= 0) mockDocuments.splice(idx, 1)
  MOCK_PLATFORM.documents.stored = mockDocuments.length
}

// --- simulated replies ------------------------------------------------------
/** Documents named by the request's `attachments` attribute, skipping ids the store no longer has. */
function attachedDocuments(request: AgentRequest): DocumentSummary[] {
  const raw = request.attributes.attachments
  const ids = Array.isArray(raw) ? raw.map(String) : typeof raw === 'string' ? [raw] : []
  return ids.map((id) => mockDocuments.find((d) => d.id === id)).filter((d): d is DocumentSummary => !!d)
}

function reply(agentId: string, request: AgentRequest): { content: string; metadata: Record<string, unknown> } {
  const message = request.message
  const snippet = message.length > 80 ? message.slice(0, 77) + '…' : message
  const attached = attachedDocuments(request)
  switch (agentId) {
    case 'coding': {
      const language = String(request.attributes.language ?? 'typescript').toLowerCase()
      return {
        content: [
          `Here is a first implementation for: "${snippet}"`,
          '',
          '```' + language,
          'export function debounce<T extends (...args: never[]) => void>(fn: T, wait = 200) {',
          '  let timer: ReturnType<typeof setTimeout> | undefined',
          '  return (...args: Parameters<T>) => {',
          '    clearTimeout(timer)',
          '    timer = setTimeout(() => fn(...args), wait)',
          '  }',
          '}',
          '```',
          '',
          'The timer is reset on every call, so `fn` only runs once the calls stop for `wait` ms.',
        ].join('\n'),
        metadata: { language, model: 'simulated', tokens: { prompt: 142, completion: 96, total: 238 } },
      }
    }
    case 'research':
      return {
        content: [
          `**Summary** — ${snippet}: the main claims are broadly supported, with one open question.`,
          '',
          '**Key findings**',
          '- Most treatments of the topic agree on the core mechanism.',
          '- Two commonly cited figures differ by roughly a factor of two.',
          '',
          '**Open questions**',
          '- Whether recent changes (last 12 months) alter the picture.',
          '',
          '**Confidence** — medium: the fundamentals are stable, the specifics may have moved.',
        ].join('\n'),
        metadata: { model: 'simulated', mode: 'knowledge', confidence: 'medium' },
      }
    case 'summarizer': {
      const style = String(request.attributes.style ?? 'bullets')
      const maxWords = Number(request.attributes.maxWords ?? 150)
      const body =
        style === 'tldr'
          ? 'TL;DR — The input describes a request with one main subject, a few constraints, and a recommended next step.'
          : style === 'executive'
            ? '**Bottom line:** the request is feasible.\n\n**Why it matters:** it unblocks the next phase.\n\n**Next steps:** confirm constraints, then proceed.'
            : '- The main subject and why it matters.\n- The constraints that shape the outcome.\n- The recommended next step.'
      return {
        content: body,
        metadata: {
          model: 'simulated',
          style,
          maxWords,
          inputChars: message.length,
          compressionRatio: Math.round((body.length / Math.max(1, message.length)) * 100) / 100,
        },
      }
    }
    case 'document': {
      if (attached.length === 0) {
        return {
          content:
            'No files attached. Use the paperclip in the message box (or drop a file on the chat), then ask again.',
          metadata: { model: 'simulated' },
        }
      }
      return {
        content: [
          `Based on ${attached.map((d) => `**${d.name}**`).join(' and ')}:`,
          '',
          ...attached.map((d) => `> "${d.preview.slice(0, 120)}${d.preview.length > 120 ? '…' : ''}"`),
          '',
          `That is the closest match for "${snippet}". (Simulated — no model was called.)`,
        ].join('\n'),
        metadata: {
          model: 'simulated',
          documentIds: attached.map((d) => d.id),
          documentNames: attached.map((d) => d.name),
        },
      }
    }
    default: {
      const note = attached.length
        ? `\n\nI can see ${attached.length} attached file${attached.length === 1 ? '' : 's'}: ${attached
            .map((d) => d.name)
            .join(', ')}.`
        : ''
      return {
        content: `Sure — here's a quick take on "${snippet}".${note}\n\nThis is a simulated answer: the backend is offline, so no model was called. Start the Spring Boot app on port 8080 and refresh to talk to real agents.`,
        metadata: attached.length
          ? { model: 'simulated', documentNames: attached.map((d) => d.name) }
          : { model: 'simulated' },
      }
    }
  }
}

export async function mockRun(agentId: string, request: AgentRequest): Promise<AgentResponse> {
  await delay(500 + Math.random() * 900)
  const { content, metadata } = reply(agentId, request)
  return {
    agentId,
    content,
    metadata: { ...metadata, simulated: true, conversationId: request.conversationId },
    conversationId: request.conversationId ?? undefined,
  }
}
