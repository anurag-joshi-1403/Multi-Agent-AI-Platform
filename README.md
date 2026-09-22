<div align="center">

<img src="Frontend/public/logo.svg" width="96" alt="Multi-Agent AI Platform logo" />

# Multi-Agent AI Platform

**One backend, five specialised AI agents, one console to talk to them.**

A Spring Boot service hosts a set of focused agents (coding, research, summarising, document Q&A,
general chat), each one a plain Spring bean that describes its own capabilities. A React console
turns that self-description into agent cards, chat threads and form controls automatically — chat
with each agent, attach files, tune options, and inspect exactly what was sent to the model and
what came back.

[![Java](https://img.shields.io/badge/Java-25-ed8b00?logo=openjdk&logoColor=white)](Backend/pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f?logo=springboot&logoColor=white)](Backend/pom.xml)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-6db33f)](Backend/pom.xml)
[![React](https://img.shields.io/badge/React-19-61dafb?logo=react&logoColor=white)](Frontend/package.json)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178c6?logo=typescript&logoColor=white)](Frontend/package.json)
[![Vite](https://img.shields.io/badge/Vite-8-646cff?logo=vite&logoColor=white)](Frontend/package.json)

</div>

---

## Contents

- [What this is](#what-this-is)
- [Quick start](#quick-start)
- [The agents](#the-agents)
- [Architecture](#architecture)
- [How a request flows](#how-a-request-flows)
- [Backend reference](#backend-reference) — config, persistence, API, error mapping
- [Frontend reference](#frontend-reference) — pages, structure, modes
- [Add your own agent](#add-your-own-agent)
- [Testing](#testing)
- [Project layout](#project-layout)
- [Troubleshooting](#troubleshooting)
- [Security notes](#security-notes)
- [Project history & roadmap](#project-history--roadmap)

---

## What this is

Most "multi-agent" demos hardcode a router and a fixed UI per agent. This project inverts that:
**agents are self-describing Spring beans**, and the frontend renders itself from what the backend
reports. Add a new agent class on the backend, restart, and it gets a card on the Agents page, a
`POST /api/agents/{id}/run` endpoint, and the right form controls in the chat header — with no
frontend code changes.

Beyond that core idea, the console behaves like a real chat product, not a demo:

| Capability | Detail |
|---|---|
| **Real login** | A session-backed sign-in gates the whole console — nothing renders, not even simulation mode, until you're authenticated. |
| **Per-agent conversations** | Each of the five agents keeps its own conversation list. Switching agents always opens a fresh chat; earlier threads stay filed under the agent they belong to. |
| **Universal file attachments** | Attach a PDF or text file to *any* agent — click the paperclip next to Send, drop files on the chat, or paste them. Files stay in context for the rest of that conversation, not just the message they were attached to. |
| **Edit & regenerate** | Edit a prompt you already sent; the stale reply is discarded and the agent answers the new text, ChatGPT-style. |
| **Response Inspector** | Click any reply to see its latency, model, token usage, the exact request body sent, and the response metadata. |
| **Per-agent theming** | Each agent gets a deterministic accent colour (your bubbles, the composer glow, the send button) so you always know who you're talking to. |
| **Provider-agnostic** | Google Gemini by default; switch to Anthropic Claude or OpenAI with one environment variable — no code changes. |
| **Works without a backend** | If the API is unreachable, the console switches to an in-browser simulation so the whole UI stays explorable. |
| **RFC 9457 errors everywhere** | Every failure — validation, unknown agent, provider auth, rate limit — comes back as `application/problem+json` with an actionable `detail`. |

---

## Quick start

**Prerequisites:** Java 25, Node 20+, and an API key for one model provider (free tier is fine).
No database to install — uploads and conversations persist to a file by default. See
[Persistence](#persistence).

Two terminals.

### 1. Backend — `http://localhost:8080`

```bash
cd Backend

# macOS / Linux
export GEMINI_API_KEY="AIza..."

# Windows PowerShell
$env:GEMINI_API_KEY = "AIza..."

./mvnw spring-boot:run
```

Get a free Gemini key at <https://aistudio.google.com/apikey>. The app boots fine without a key —
`/api/agents` and file uploads work — but every agent run answers `502` until one is set.

On first start the backend creates `Backend/data/platform.mv.db` and its two tables. Uploads and
conversations are still there after a restart, with nothing to install or configure.

Prefer a different provider? Set `AI_PROVIDER` and the matching key — nothing else changes:

| Provider | `AI_PROVIDER` | Key variable | Get a key | Default model |
|---|---|---|---|---|
| Google Gemini | `google-genai` *(default)* | `GEMINI_API_KEY` | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) | `gemini-3.5-flash-lite` |
| Anthropic Claude | `anthropic` | `ANTHROPIC_API_KEY` | [console.anthropic.com](https://console.anthropic.com/settings/keys) | `claude-opus-5` |
| OpenAI | `openai` | `OPENAI_API_KEY` | [platform.openai.com](https://platform.openai.com/api-keys) | `gpt-4o-mini` |

```powershell
$env:AI_PROVIDER = "anthropic"; $env:ANTHROPIC_API_KEY = "sk-ant-..."; .\mvnw spring-boot:run
```

All three provider starters sit on the classpath at once; `AI_PROVIDER` activates exactly one chat
model bean, so nothing else in the app changes when you switch.

> **Keys live in environment variables only — never in `application.properties`.** That file is
> committed to git; GitHub's push protection will reject a push that contains a real key, and
> anyone who clones the repo would get it. See [Security notes](#security-notes).

The console itself sits behind a login. Without `AUTH_USERS` set, it defaults to **`admin` /
`admin`** — fine for trying the project locally, not for anything else:

```bash
export AUTH_USERS="you:apassword,guest:anotherpassword"   # username:password pairs, comma-separated
```

### 2. Frontend — `http://localhost:5173`

```bash
cd Frontend
npm install
npm run dev
```

The dev server proxies `/api` to the backend, so there's no CORS setup to do locally. Open the
printed URL and sign in with one of the accounts from `AUTH_USERS` (or `admin` / `admin` if you
didn't set one) — the console itself is what's behind that login, so this has to happen before
anything else, including simulation mode. Once in, pick an agent from the header and send a
message. If the backend is up but no model key is set, a banner names the exact variable to export.

### Verify everything works

```bash
cd Backend  && ./mvnw test                     # 72 tests, stubbed model, no Docker, network or key
cd Frontend && npm run lint && npm run build    # type-check + bundle
```

---

## The agents

| Id | Name | What it does | Options |
|---|---|---|---|
| `coding` | Coding Agent | Working code first in a fenced block, then a short rationale. A picked language always wins, even if the question mentions another one. | `language` — Auto-detect or one of 21 languages |
| `research` | Research Agent | Breaks a topic into a summary, key findings, open questions and a confidence rating. Knowledge-based; no live web search yet. | — |
| `summarizer` | Summarizer Agent | Condenses long text into bullets, a TL;DR, or an executive brief. | `style` (`bullets` / `tldr` / `executive`), `maxWords` |
| `document` | Document Agent | Answers strictly from the files attached to the conversation, quoting the relevant passage (with page markers for PDFs) before answering. Refuses with a clear `400` if nothing is attached. | — |
| `general` | General Assistant | Fallback for anything the specialists don't cover. | — |

Options render as controls in the **chat header**, next to the New chat button. File attachments
are deliberately *not* modeled as an option — every agent accepts them through the same paperclip
button, because grounding an answer in a file is a universal need, not a document-agent-only one.

---

## Architecture

```
┌─────────────────────────────┐        ┌──────────────────────────────────────────────┐
│           Frontend           │        │                    Backend                     │
│   React 19 · TypeScript      │  HTTP  │            Spring Boot 4 · Spring AI 2         │
│                               │◄──────►│                                                │
│  pages/    Overview,          │  JSON  │  web/        Controllers, DTOs, error handler  │
│            Playground,        │        │  agent/core  Agent contract, registry,         │
│            Agents, Settings   │        │              orchestrator                      │
│  components/ Composer,        │        │  agent/llm   LlmAgent base class                │
│              MessageBubble,   │        │  agent/impl  5 concrete agents                  │
│              Inspector, ...   │        │  document    Store, text extraction,            │
│  hooks/    useConversations,  │        │              attachment resolution              │
│            useAttachments,    │        │  config      Provider selection, CORS, security │
│            useAgents, ...     │        │                                                 │
│  api/      client.ts (real)   │        │                          │                      │
│            mock.ts (offline)  │        │                          ▼                      │
└───────────────┬───────────────┘        │           Gemini · Claude · OpenAI              │
                 │                       └──────────────────────────────────────────────┘
        localStorage (per-browser)
        conversations, theme, sidebar state
```

**Backend, three layers:**

1. **`agent/core`** — the `Agent` contract every agent implements; `AgentRegistry`, which
   discovers every `@Component` implementing `Agent` at startup (no manual registration); and
   `AgentOrchestrator`, the single entry point that looks an agent up by id and calls it.
2. **`agent/llm`** — `LlmAgent`, the abstract base class model-backed agents extend. It owns a
   `ChatClient` pre-loaded with the agent's system prompt, attaches Spring AI's `ChatMemory` per
   call when a `conversationId` is present, and folds any attached files into that call's system
   prompt (never into memory, so files don't bloat the replayed context).
3. **`document`** — `DocumentStore`, an interface with two implementations (`JdbcDocumentStore`,
   the default, and `InMemoryDocumentStore`), capacity-bounded either way with the oldest evicted
   first; `DocumentTextExtractor` (PDFBox for PDFs, UTF-8 for text formats); and
   `AttachmentResolver`, which turns a request's `attachments` id list into the system-prompt block
   every `LlmAgent` call injects.

**Frontend, mirrors the same shape:** `api/` talks to the backend (or simulates it), `hooks/` hold
state (conversations in `localStorage`, agents, attachments, theme, route), `lib/` has the pure
logic (send/edit dispatch, attribute building, per-agent color), and `components/`/`pages/` are
render-only.

---

## How a request flows

```mermaid
sequenceDiagram
    actor U as You
    participant UI as Console
    participant O as AgentOrchestrator
    participant R as AgentRegistry
    participant A as Agent
    participant M as Gemini / Claude / OpenAI
    U->>UI: message · options · attachments
    UI->>O: POST /api/agents/{id}/run
    O->>R: get(id)
    R-->>O: agent bean
    O->>A: handle(AgentRequest)
    A->>M: system prompt + attached files + conversation memory + message
    M-->>A: completion
    A-->>O: AgentResponse {content, metadata}
    O-->>UI: 200 · conversationId · elapsedMs
    UI-->>U: reply bubble · Inspector
```

Conversations exist in two places, kept loosely in sync:

- **Browser** (`localStorage`) — the full transcript, forever, per device. This is what the UI
  renders.
- **Server** (`ChatMemory`, keyed by `conversationId`) — only the last `platform.memory.max-messages`
  turns (default 20), replayed to the model on each call so it has context. Deleting a
  conversation in the console also calls `DELETE /api/conversations/{id}` to forget it server-side.

A failed provider call never leaves a dangling, unanswered user turn in server memory — `LlmAgent`
rolls it back before the exception propagates.

---

## Backend reference

### Configuration

Everything lives in [`Backend/src/main/resources/application.properties`](Backend/src/main/resources/application.properties),
under two prefixes.

**Provider selection** (`spring.ai.*`, `AI_PROVIDER` env var):

| Property | Default | Purpose |
|---|---|---|
| `spring.ai.model.chat` | `${AI_PROVIDER:google-genai}` | Which provider's chat model bean is created |
| `spring.ai.google.genai.api-key` | `${GEMINI_API_KEY}` | Gemini key |
| `spring.ai.google.genai.chat.options.model` | `${GEMINI_MODEL:gemini-3.5-flash-lite}` | Gemini model |
| `spring.ai.anthropic.api-key` | `${ANTHROPIC_API_KEY}` | Claude key |
| `spring.ai.anthropic.chat.options.model` | `${ANTHROPIC_MODEL:claude-opus-5}` | Claude model |
| `spring.ai.openai.api-key` | `${OPENAI_API_KEY}` | OpenAI key |
| `spring.ai.openai.chat.options.model` | `${OPENAI_MODEL:gpt-4o-mini}` | OpenAI model |

Non-chat model types (embedding, image, audio, moderation) are switched off so no starter demands
an unrelated key.

**Platform behaviour** (`platform.*`, bound by [`PlatformProperties`](Backend/src/main/java/com/project/multi_agent_ai_platform/config/PlatformProperties.java)):

| Property | Default | Purpose |
|---|---|---|
| `platform.storage` | `jdbc` | Where documents and conversation memory live: `jdbc` (survives a restart) or `memory` (does not, but needs no database) |
| `platform.cors.allowed-origin-patterns` | `http://localhost:*,http://127.0.0.1:*` | Origins allowed to call the API directly (the Vite proxy needs none of this) |
| `platform.memory.max-messages` | `20` | Recent messages of a conversation replayed to the model |
| `platform.documents.max-context-chars` | `60000` | Attached-file text is truncated to this before reaching the model, split evenly across multiple files |
| `platform.documents.max-stored` | `50` | Oldest uploads are evicted once this many are held, whichever store is active |
| `platform.auth.users` | `${AUTH_USERS:admin:admin}` | Console accounts, as comma-separated `username:password` pairs |

### Persistence

Uploaded documents and server-side conversation memory survive a restart. Two tables, each owned by
whoever generates it:

| Table | Holds | Schema from |
|---|---|---|
| `platform_documents` | Uploaded files: extracted text, media type, page count, upload time | [`schema.sql`](Backend/src/main/resources/schema.sql) |
| `SPRING_AI_CHAT_MEMORY` | The messages replayed to the model per conversation | Spring AI's own per-dialect DDL |

Both are created on boot, idempotently — there is no migration step to run.

**Default: an H2 file** at `Backend/data/platform.mv.db` (gitignored). Nothing to install, nothing
to configure, and data survives restarts. This is the right setting for a developer machine and it
is what the project ships with.

**Postgres**, for anything longer-lived. Either use the profile, which starts the service in
[`compose.yaml`](Backend/compose.yaml) and wires the datasource from the running container —
including the host port, which compose assigns dynamically, so no hardcoded URL would be correct:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres    # needs Docker running
```

…or point at a Postgres you run yourself, which needs no profile:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/agents"
export SPRING_DATASOURCE_USERNAME="agents"
export SPRING_DATASOURCE_PASSWORD="secret"
```

Both databases run the same `schema.sql` and the same JDBC code; only the dialect differs.

**No persistence at all.** `platform.storage=memory` — which the `memory` profile sets, along with
excluding the datasource auto-configuration entirely — restores the pre-persistence behaviour:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=memory
```

Everything works as before, and a restart forgets everything. The switch is one property because
both halves move together: [`StorageConfig`](Backend/src/main/java/com/project/multi_agent_ai_platform/config/StorageConfig.java)
picks `JdbcDocumentStore` or `InMemoryDocumentStore`, and declaring an in-memory
`ChatMemoryRepository` is what makes Spring AI's JDBC auto-configuration back off.

Nothing above the [`DocumentStore`](Backend/src/main/java/com/project/multi_agent_ai_platform/document/DocumentStore.java)
interface knows which implementation is in use — not the controller, not `AttachmentResolver`, not
any agent. `AiConfig` never mentions a repository implementation either; it takes whichever
`ChatMemoryRepository` bean exists and wraps it in the sliding window.

**Accounts are still not persisted.** `AUTH_USERS` is read once on boot into an in-memory user
store, so accounts, roles and password changes do not survive a restart. That is a separate,
larger change — see the [roadmap](PROGRESS.md#-roadmap).

### API

Every `/api/**` endpoint except `POST /api/auth/login` requires a signed-in session — a request
with no session gets a `401` in the same `application/problem+json` shape as every other error.
Remaining Actuator endpoints stay behind HTTP Basic.

**Auth**

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/auth/login` | `{username, password}` → `{username}` + a session cookie. `401` on wrong credentials — the same message either way, so it can't be used to probe for valid usernames |
| `POST` | `/api/auth/logout` | Ends the session — `204` |
| `GET` | `/api/auth/me` | The signed-in user, or `401` if there isn't one |

**Platform & agents**

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/platform` | `{provider, providerName, model, apiKeyConfigured, keyEnvVar, agents, memoryMaxMessages, documents}` — never the key itself |
| `GET` | `/api/agents` | `[{id, name, description, capabilities, parameters}]` for every registered agent |
| `GET` | `/api/agents/{id}` | One agent; `404` if unknown |
| `POST` | `/api/agents/{id}/run` | Run it — see body below |

```jsonc
// request
{
  "conversationId": "c-1",              // optional — server mints one if absent
  "message": "Write a debounce fn",     // required, ≤ 32 000 chars
  "attributes": {
    "language": "TypeScript",           // agent-specific options
    "attachments": ["doc_abc", "doc_def"]  // ids of files to ground the answer in
  }
}

// response
{
  "agentId": "coding",
  "content": "```typescript\n...\n```\n\nThe timer resets on every call...",
  "conversationId": "c-1",
  "elapsedMs": 812,
  "metadata": {
    "model": "gemini-3.5-flash-lite",
    "tokens": { "prompt": 142, "completion": 96, "total": 238 },
    "finishReason": "STOP",
    "language": "typescript"
  }
}
```

**Documents & attachments**

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/documents` | Multipart `file` — PDF or plain text (txt, md, csv, json, yaml, code files, …) → `201` + summary |
| `POST` | `/api/documents/text` | `{ "name": "...", "content": "..." }` → `201` + summary |
| `GET` | `/api/documents` | Summaries, newest first |
| `GET` | `/api/documents/{id}` | One summary: `{id, name, mediaType, chars, pages, uploadedAt, preview}` |
| `GET` | `/api/documents/{id}/content` | Extracted text (`text/plain`); PDF pages are marked `[page N]` |
| `DELETE` | `/api/documents/{id}` | `204` |

Upload a file first, then pass its id in `attributes.attachments` (accepts a single id or a list)
on any agent's `/run` call — not just the document agent's.

**Conversations**

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/conversations/{id}/memory` | Messages currently in the server-side memory window |
| `DELETE` | `/api/conversations/{id}` | Forget the conversation server-side |

### Error mapping

| Situation | Status |
|---|---|
| Validation failure (blank message, message too long) | `400` |
| Document agent called with no attached file | `400` |
| No session, or a session that expired | `401` |
| Wrong username/password on login | `401` |
| Unknown agent id / unknown document id | `404` |
| Unsupported or unreadable upload | `415` |
| Provider rejected credentials (bad or missing key) | `502` |
| Provider rate-limited or degraded | `503` |
| Provider unreachable | `502` |

---

## Frontend reference

Navigation is hash-based, so every page has a shareable, refresh-safe URL.

| Route | Page | What's there |
|---|---|---|
| `#/` | Overview | Getting-started checklist, stat tiles, request pipeline diagram, recent conversations |
| `#/playground` | Playground | Per-agent conversation list · chat thread · composer · response inspector |
| `#/playground/<id>` | Playground | Opens one specific conversation |
| `#/agents` | Agents | Registry cards for every discovered agent, plus an "add a new agent" guide |
| `#/settings` | Settings | API base URL override, simulation toggle, theme, clear local data |

None of these routes are reachable until you sign in — the login screen sits in front of all of
them, checked once via `GET /api/auth/me` so a page refresh doesn't sign you out. A session that
dies mid-visit (backend restarted, timed out) is caught the same way any other call's `401` is, and
sends you back to the login screen rather than silently switching to simulated replies.

### Offline & simulation mode

Once signed in, the console probes `GET /api/agents` and behaves accordingly:

| Mode | When | Behaviour |
|---|---|---|
| `online` | Backend reachable | Real agents, real replies |
| `offline` | Backend unreachable or erroring | Banner shown; simulated agents and replies so the UI stays usable |
| `simulated` | Turned on manually in Settings | Same as offline, chosen on purpose — good for demos with no backend |

Simulated replies carry `"simulated": true` in their metadata and a **simulated** badge on the
bubble, so they're never mistaken for a real model response. Simulation mode mirrors the real
agents' behaviour closely, including file attachments and the coding agent's language selection.

---

## Add your own agent

This is the platform's core idea: one class, no other edits.

```java
package com.project.multi_agent_ai_platform.agent.impl;

@Component
public class TranslatorAgent extends LlmAgent {

    static final String SYSTEM_PROMPT = "You translate text. Reply with only the translation.";

    public TranslatorAgent(ChatClient.Builder builder, ChatMemory memory, AttachmentResolver attachments) {
        super(builder, memory, attachments, SYSTEM_PROMPT);
    }

    @Override public String id()          { return "translator"; }
    @Override public String description() { return "Translates text into a target language."; }

    @Override
    public List<AgentParameter> parameters() {
        return List.of(AgentParameter.select("to", "Translate to", "Target language",
                List.of("French", "German", "Spanish", "Japanese"), "French"));
    }

    @Override
    public AgentResponse handle(AgentRequest request) {
        String to = attribute(request, "to");
        Completion c = complete(request, "Translate to " + to + ":\n\n" + request.message());
        return new AgentResponse(id(), c.content(), c.metadata());
    }
}
```

Restart the backend. `AgentRegistry` picks the bean up automatically, and the agent now has:

- a card on the **Agents** page (from `id()`, `description()`, `capabilities()`)
- a live endpoint at `POST /api/agents/translator/run`
- a "Translate to" dropdown in the chat header (from `parameters()`)
- file attachments, for free, because it extends `LlmAgent`

`AgentParameter` supports `string`, `number` and `select`. An agent with no model call at all can
implement the `Agent` interface directly instead of extending `LlmAgent`.

---

## Testing

```bash
cd Backend && ./mvnw test
```

72 tests, all offline against a stubbed chat model — no API key, no network call, and no Docker.
They assert the *exact prompt* each agent sends: that the coding agent keeps its own system prompt
and still gains an attached file's content, that multiple attachments split the character budget
evenly, that an evicted attachment id is skipped rather than failing the whole request, and that a
failed provider call never leaves a dangling turn in conversation memory. A dedicated
`AuthControllerTest` exercises the real session flow end to end rather than mocking it: login sets
a cookie, that cookie authenticates the next request, wrong credentials are rejected without
revealing which field was wrong, and logout actually ends the session.

Persistence is tested rather than assumed. `DocumentStoreTest` is a **contract test**: one set of
assertions run against *both* `DocumentStore` implementations, so a behaviour that holds for the
in-memory store and not the database one fails the build instead of surfacing after a config
change. The JDBC side runs on in-memory H2 against the real `schema.sql`, which is why the suite
still needs no database server — the shipped DDL is the DDL under test, and Spring AI ships an H2
chat-memory dialect alongside its Postgres one. Two wiring tests then assert what each mode
actually builds, including that the `memory` profile's context contains no `DataSource` at all.

> Writing that contract test immediately paid for itself: it caught `delete(null)` throwing from
> the in-memory store where the database one returned `false`, and a timestamp that could not
> round-trip because `Instant.now()` is nanosecond-resolution while `TIMESTAMP` keeps microseconds.
> Both were real inconsistencies between the two stores, and neither was reachable by testing one
> implementation alone.

```bash
cd Frontend && npm run lint && npm run build
```

Type-checks the whole app (`tsc -b`) and bundles it (`vite build`); `npm run lint` runs ESLint with
the React 19 compiler rules.

---

## Project layout

```
Backend/src/main/java/com/project/multi_agent_ai_platform/
├── agent/
│   ├── core/     Agent (interface) · AgentRegistry · AgentOrchestrator
│   │             AgentParameter · AgentRequest · AgentResponse
│   ├── llm/      LlmAgent — ChatClient + memory + attachment injection
│   └── impl/     CodingAgent · ResearchAgent · SummarizerAgent
│                 DocumentAgent · GeneralAgent
├── document/     DocumentStore (interface) · JdbcDocumentStore · InMemoryDocumentStore
│                 DocumentTextExtractor · AttachmentResolver · DocumentIds
├── config/       AiConfig · StorageConfig · LlmProvider(Info)
│                 PlatformProperties · SecurityConfig
└── web/          AgentController · AuthController · DocumentController
                  ConversationController · PlatformController · ApiExceptionHandler · dto/

Backend/src/main/resources/
├── application.properties            provider, platform, persistence, web, actuator
├── application-postgres.properties   Postgres via compose.yaml, instead of the H2 file
├── application-memory.properties     the no-database profile
└── schema.sql                        platform_documents (Spring AI owns the other table)

Frontend/src/
├── api/          client.ts (real backend) · mock.ts (offline simulation)
├── hooks/        useAuth · useConversations · useAttachments · useAgents
│                 usePlatform · useBackendStatus · useHashRoute · useSidebar
│                 useAutoCollapseOnRoute · useTheme · useCopy
├── lib/          dispatch (send/edit flow) · attributes · agentColor
│                 storage · util
├── components/   Composer · AgentOptions · MessageBubble · Markdown
│                 Inspector · ConversationList · Sidebar · AgentAvatar · Icons
└── pages/        LoginPage · OverviewPage · PlaygroundPage · AgentsPage · SettingsPage
```

---

## Troubleshooting

| Symptom | Cause · fix |
|---|---|
| Stuck on the login screen with "Couldn't reach the backend" | The API is not reachable at `:8080`. Start it, then reload — there's no offline/simulated bypass for login itself. |
| Login says *Invalid credentials* | Check `AUTH_USERS` on the backend (or use the `admin`/`admin` default if you never set it) — the message is the same for a wrong username as a wrong password, on purpose. |
| Signed out unexpectedly mid-session | The session expired or the backend restarted. Sign in again — this is expected, not a bug. |
| Banner: *Backend offline* | The API is not reachable at `:8080`. Start it, then click **Retry**. Replies are simulated until then. |
| Banner: *No API key* | Backend is up but the active provider's key is unset. Export it and restart the backend. |
| `502 — LLM provider rejected the credentials` | The key is wrong, expired or revoked. Issue a new one and export it again. |
| `400 — The document agent needs at least one attached file` | Attach a file with the paperclip before asking the document agent. |
| Agent reply seems to ignore an attached file | The store keeps only the 50 newest uploads; a very old attachment may have been evicted. Re-attach it. |
| Backend won't start: *Unable to start command docker* | Only the `postgres` profile needs Docker. Start Docker Desktop, or drop the profile to use the default H2 file. |
| Uploads and conversations vanish on every restart | You're on the `memory` profile (or `PLATFORM_STORAGE=memory`). That's what it does — drop the profile to keep data. |
| Login fails with *404 Not Found* | The backend isn't running, so Vite has nothing to proxy `/api` to. Start it and check it reached *Started MultiAgentAiPlatformApplication*. |
| Want a clean slate | Stop the backend and delete `Backend/data/` (or `docker compose down -v` on the `postgres` profile). Tables are recreated on the next boot. |
| `git push` rejected: *repository rule violations / push protection* | A real secret is in a commit. Remove it from the file and scrub it from history before pushing — see below. |

---

## Security notes

- **Never put a real key in `application.properties`.** It's committed to git. Use
  `${GEMINI_API_KEY:missing-api-key}`-style placeholders (as the other two providers already do)
  and export the real value as an environment variable in your shell or IDE run configuration.
- If a key ever does land in a commit, GitHub's push protection will block the push before it
  reaches the remote — that's a save, not an inconvenience. Rotate the exposed key immediately
  (treat it as compromised even if the push was blocked), fix the file to use a placeholder, then
  scrub the secret from local git history before pushing again.
- **Change the default login before running this anywhere but your own machine.** `AUTH_USERS`
  defaults to `admin:admin`, the same kind of loud, documented placeholder as the actuator
  password Spring Boot itself generates — fine to explore the project with, not fine to leave as
  a real account. Set `AUTH_USERS` to real `username:password` pairs first.
- Session cookies are `HttpOnly` and same-origin through the Vite dev proxy in local development.
  Serving the frontend and backend from different origins in a real deployment needs `Secure`
  cookies and HTTPS — the session mechanism doesn't provide that on its own.
- Passwords are BCrypt-hashed in memory on boot; the plaintext from `AUTH_USERS` is read once and
  never written anywhere. There is no password-reset flow — change `AUTH_USERS` and restart.

---

## Project history & roadmap

For how the project got from an empty folder to here, phase by phase, and what's planned next
(near-term, mid-term, long-term), see [`PROGRESS.md`](PROGRESS.md).

---

<div align="center">
<sub>Built with Spring Boot 4.1 · Spring AI 2.0 · Java 25 · React 19 · TypeScript 6 · Vite 8</sub>
</div>
