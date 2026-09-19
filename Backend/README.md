# Multi-Agent AI Platform — Backend

Spring Boot 4 / Spring AI 2 service that routes each request to a specialised agent. The model provider is
switchable: **Google Gemini** (default), Anthropic Claude or OpenAI. The
[frontend](../Frontend) talks to it over the JSON API below.

## Run

```bash
export GEMINI_API_KEY=AIza...          # PowerShell: $env:GEMINI_API_KEY="AIza..."
./mvnw spring-boot:run                 # http://localhost:8080
./mvnw test                            # 50 tests, no network needed
```

Without a key the app still boots (so the UI, agent list and document uploads work) and logs a
warning naming the variable to set; every agent run then answers `502 LLM provider rejected the credentials`.

### Choosing the provider

| `AI_PROVIDER` | Key env var | Get a key | Model env var (default) |
|---|---|---|---|
| `google-genai` *(default)* | `GEMINI_API_KEY` | https://aistudio.google.com/apikey | `GEMINI_MODEL` (`gemini-2.5-flash`) |
| `anthropic` | `ANTHROPIC_API_KEY` | https://console.anthropic.com/settings/keys | `ANTHROPIC_MODEL` (`claude-opus-5`) |
| `openai` | `OPENAI_API_KEY` | https://platform.openai.com/api-keys | `OPENAI_MODEL` (`gpt-4o-mini`) |

```powershell
$env:AI_PROVIDER = "anthropic"; $env:ANTHROPIC_API_KEY = "sk-ant-..."; .\mvnw spring-boot:run
```

All three starters are on the classpath; `spring.ai.model.chat` (bound to `AI_PROVIDER`) activates exactly
one chat model, so nothing else changes when you switch. Keys are read **only** from environment
variables — never paste them into `application.properties`, which is committed to git.

Other knobs live in [`application.properties`](src/main/resources/application.properties) under
`platform.*` (CORS origins, memory window, document limits).

## Agents

| id | What it does | Attributes it understands |
|---|---|---|
| `coding` | Code first, short rationale after | `language` |
| `research` | Summary / key findings / open questions / confidence (knowledge-based, no live web) | — |
| `summarizer` | Bullets, TL;DR or executive brief | `style` = `bullets`\|`tldr`\|`executive`, `maxWords` |
| `document` | Answers grounded in an uploaded PDF/text, quoting page markers | `documentId` (required) |
| `general` | Fallback assistant | — |

Adding one: implement `Agent` as a `@Component` (or extend `LlmAgent` for an LLM-backed one).
The registry discovers it at startup and it appears in `GET /api/agents` automatically. Override
`parameters()` to describe the attributes you read (`AgentParameter.string/number/select/document`)
and the frontend renders matching controls — including a document picker with upload.

## API

All endpoints are unauthenticated (developer platform behind the Vite proxy). Errors are RFC 9457
`application/problem+json`.

### Agents

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/platform` | `{provider, providerName, model, apiKeyConfigured, keyEnvVar, agents, memoryMaxMessages, documents}` — never the key itself |
| `GET` | `/api/agents` | `[{id, name, description, capabilities, parameters}]` |
| `GET` | `/api/agents/{id}` | one agent, `404` if unknown |
| `POST` | `/api/agents/{id}/run` | body below |

```jsonc
// request
{ "conversationId": "c-1",            // optional; server mints one if absent
  "message": "Write a debounce fn",   // required, ≤ 32 000 chars
  "attributes": { "language": "ts" } } // optional, agent-specific

// response
{ "agentId": "coding", "content": "...", "conversationId": "c-1", "elapsedMs": 812,
  "metadata": { "model": "gemini-2.5-flash", "tokens": { "prompt": 142, "completion": 96, "total": 238 },
                "finishReason": "STOP", "language": "typescript" } }
```

Passing the same `conversationId` replays the last `platform.memory.max-messages` turns to the
model. A failed call does not leave its user turn in memory.

### Documents (for the `document` agent)

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/documents` | multipart `file` — PDF or plain text (txt, md, csv, json, …) → `201` + summary |
| `POST` | `/api/documents/text` | `{ "name": "...", "content": "..." }` → `201` + summary |
| `GET` | `/api/documents` | summaries, newest first |
| `GET` | `/api/documents/{id}` | summary (`id, name, mediaType, chars, pages, uploadedAt, preview`) |
| `GET` | `/api/documents/{id}/content` | extracted text (`text/plain`); PDF pages are marked `[page N]` |
| `DELETE` | `/api/documents/{id}` | `204` |

Documents are held in memory (oldest evicted past `platform.documents.max-stored`) and
truncated to `platform.documents.max-context-chars` before reaching the model.

### Conversations

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/conversations/{id}/memory` | messages currently in the memory window |
| `DELETE` | `/api/conversations/{id}` | forget the conversation server-side |

### Error mapping

| Situation | Status |
|---|---|
| Validation failure, missing `documentId` | `400` |
| Unknown agent / document | `404` |
| Unsupported or unreadable upload | `415` |
| Provider rejected credentials (bad/missing key) or request | `502` |
| Provider rate-limited / down | `503` |
| Provider unreachable | `502` |

## Layout

```
agent/core      Agent contract, registry, orchestrator, request/response records
agent/llm       LlmAgent — ChatClient + per-conversation memory + metadata extraction
agent/impl      CodingAgent, ResearchAgent, SummarizerAgent, DocumentAgent, GeneralAgent
document        DocumentStore (in-memory), DocumentTextExtractor (PDFBox / UTF-8)
web             AgentController, DocumentController, ConversationController, PlatformController, ApiExceptionHandler
config          SecurityConfig (open /api, CORS), AiConfig (memory, provider info), LlmProvider(+Info), PlatformProperties
```

Postgres in `compose.yaml` is reserved for a later persistence phase; Docker Compose startup is
disabled until then (`spring.docker.compose.enabled=false`).
