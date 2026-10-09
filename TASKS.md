<div align="center">

<img src="Frontend/public/logo.svg" width="88" alt="Multi-Agent AI Platform logo" />

# ✅ Tasks

**Everything left to do on the Multi-Agent AI Platform, in order.**
How to build each backend phase: [`BACKEND.md`](BACKEND.md) · How to run it: [`README.md`](README.md)

[![Frontend](https://img.shields.io/badge/Frontend-ready-34d399?logo=react&logoColor=white)](#-where-things-stand)
[![Backend](https://img.shields.io/badge/Backend-Phase%204%20of%207-fbbf24?logo=springboot&logoColor=white)](#-where-things-stand)
[![Security](https://img.shields.io/badge/Leaked%20key-rotate%20first-f87171)](#-do-these-next)
[![Updated](https://img.shields.io/badge/Updated-Oct%2010%2C%202026-8b7cff)](#-where-things-stand)

</div>

---

## 🧭 Where things stand

| | |
|---|---|
| 🖥️ **Frontend** | 🟢 Finished — lint ✅ build ✅, runs on its own with **simulated** replies |
| ⚙️ **Backend** | 🟡 4 agents live on Gemini, with conversation memory · 4 endpoints · Phases 4–7 to go |
| 🔐 **Login** | 🔴 Placeholder — any username and password gets in ([`App.tsx:30`](Frontend/src/App.tsx#L30)) |
| 🧪 **Tests** | 🟡 Backend: 58 passing, no key or network needed · Frontend: none |
| 📚 **Docs** | 🟡 Behind the code — `README.md` still says there's no backend, and links to removed or missing files |
| 🔑 **Secrets** | 🟡 New key is in `Backend/.env` · make sure the leaked `…Egng` (`eebf0a8`, on GitHub) is revoked |
| 💾 **Git** | 🟢 Phases 1–3 pushed to `origin/main` · working tree clean |

🔴 not started &nbsp;·&nbsp; 🟡 in progress &nbsp;·&nbsp; 🟢 done

---

## 🗺️ Roadmap

```mermaid
flowchart LR
    classDef done fill:#34d399,stroke:#0f9f6e,color:#04351f,font-weight:bold
    classDef doing fill:#fde68a,stroke:#b7791f,color:#3a2a05,font-weight:bold
    classDef todo fill:#e5e7eb,stroke:#6b7280,color:#1f2937
    P0["🧹 0<br/>Get ready"] --> P1["🧱 1<br/>Skeleton"] --> P2["🤖 2<br/>First agent"] --> P3["🧠 3<br/>More agents<br/>+ memory"]
    P3 --> P4["📎 4<br/>Files"] --> P5["🔐 5<br/>Login"] --> P6["🔁 6<br/>Failover"] --> P7["🚀 7<br/>Launch"]
    class P1,P2,P3 done
    class P0 doing
    class P4,P5,P6,P7 todo
```

| Phase | Status | Left to do |
|---|---|---|
| 🧹 **0 · Get ready** | 🟡 | Confirm the leaked keys are revoked |
| 🧱 **1 · Skeleton** | 🟢 | — |
| 🤖 **2 · First agent** | 🟢 | — |
| 🧠 **3 · More agents + memory** | 🟢 | — |
| 📎 **4 · Files** | 🔴 | Uploads, H2 storage, Document Agent |
| 🔐 **5 · Login** | 🔴 | Spring Security + frontend wiring |
| 🔁 **6 · Failover** | 🔴 | Provider chain |
| 🚀 **7 · Launch & extras** | 🔴 | Streaming, Docker, CI |

---

## 🔥 Do these next

1. 🔑 **Make sure the leaked keys are revoked** in Google AI Studio (`…Egng` and `…5i5w`).
2. 📎 **Start Phase 4**: file uploads, H2 storage and the Document Agent.
3. 📚 **Bring `README.md` up to date**: the backend runs now, and the old links are broken.

---

## ⚙️ Backend, phase by phase

> File-by-file details for every phase are in [`BACKEND.md`](BACKEND.md). Keep JSON names identical
> to [`Frontend/src/types.ts`](Frontend/src/types.ts).

### 🧹 Phase 0 — Get ready

- [x] ☕ Java 25 installed (Temurin 25.0.3) · 🟩 Node 24 · 🧰 VS Code Java + Spring extensions
- [ ] 🔑 Revoke **both** keys found in the git history, in [AI Studio](https://aistudio.google.com/apikey)

  | Key ending | Found in | Public? |
  |---|---|---|
  | `…Egng` | `eebf0a8` on `main` | 🔴 Yes — on GitHub |
  | `…5i5w` | `6619ece` on local branch `backup-before-scrub` | 🟡 Local only |

- [x] 🆕 Create a new key
- [x] 📝 `Backend/.env` template ready (git ignores it)
- [x] 📋 New key pasted in `Backend/.env` (checked: not one of the leaked keys)

### 🧱 Phase 1 — Skeleton

- [x] 📦 Project generated: Web MVC, Validation, Actuator, Lombok (`cf110e9`)
- [x] 📁 Package folders created: `agent/core` · `agent/llm` · `agent/impl` · `document` · `config` · `web/dto`
  (all filled in Phase 2 except `document/`, which Phase 4 fills)
- [x] ⚙️ `application.properties` filled in (port, `127.0.0.1`, `.env` import, problem+json, health)
- [x] 📄 Create `Backend/.env.example` with empty values
- [x] 🙈 Add `!.env.example` to the root `.gitignore` (its `.env.*` rule hides the example)
- [x] ▶️ `./mvnw spring-boot:run`, then check health through the frontend proxy
- [x] 💾 Committed and pushed: `419fea2`

✅ **Done when** <http://localhost:5173/actuator/health> shows `{"status":"UP"}`. **Checked Oct 10:** 🟢 `UP`

| Check | Result |
|---|---|
| 🩺 Health on `:8080` and through the proxy on `:5173` | 🟢 `{"status":"UP"}` |
| 🔑 `.env` loaded | 🟢 `GEMINI_API_KEY` read from `file [.env]` (value hidden by Spring) |
| ❗ Unknown route, e.g. `/api/agents` | 🟢 `404` as `application/problem+json` |
| 🏠 Listening on | 🟢 `127.0.0.1:8080` only |

> 💡 Start the backend from the `Backend/` folder. The `.env` path is relative to where you run it.

### 🤖 Phase 2 — First agent

- [x] ➕ `pom.xml`: Spring AI BOM `2.0.1` + `spring-ai-starter-model-google-genai`
- [x] ⚙️ Add the Gemini settings to `application.properties` (key from `${GEMINI_API_KEY}`)
- [x] 🧩 `agent/core` — `AgentRequest` · `AgentResponse` · `AgentParameter` · `Agent` · `AgentRegistry` · `AgentOrchestrator` · `UnknownAgentException`
- [x] 🧠 `agent/llm` — `LlmAgent` (also sends `provider`, `model`, `tokens`, `finishReason` for the Inspector)
- [x] 🤖 `agent/impl` — `GeneralAgent`
- [x] ⚙️ `config` — `AiConfig` · `LlmProvider` (warns at startup when no key is set)
- [x] 📦 `web/dto` — `AgentSummary` · `RunAgentRequest` · `RunAgentResponse` · `PlatformStatus`
- [x] 🌐 `web` — `AgentController` · `PlatformController` · `ApiExceptionHandler`
- [x] 🧪 35 tests with a fake AI model (no key, no network) — `./mvnw test` 🟢
- [x] 💾 Committed and pushed: `89279fa` agent core · `57d59ef` Gemini + General Assistant · `f238c88` REST API

✅ **Done when** the sidebar says **Backend online** and the General Assistant answers for real. **Checked Oct 10:** 🟢

| Check (through the frontend proxy) | Result |
|---|---|
| 📋 `GET /api/agents` | 🟢 `200` — the General Assistant, in the shape `types.ts` expects |
| ⚙️ `GET /api/platform` | 🟢 `200` — Google Gemini · `gemini-3.5-flash-lite` · key configured |
| 💬 `POST /api/agents/general/run` | 🟢 Real answer in 1.6 s: *"The capital of France is Paris."* |
| 🔑 Wrong key | 🟢 `502` problem+json: *"Set GEMINI_API_KEY in Backend/.env and restart"* |
| ❓ Unknown agent | 🟢 `404` problem+json with `agentId` |

```mermaid
flowchart LR
    UI["🖥️ Console"] -->|"/api (proxy)"| C["🌐 AgentController"]
    C --> O["🧭 AgentOrchestrator"] --> R["📋 AgentRegistry"]
    R -.->|finds| G["🤖 GeneralAgent"]
    G -->|extends| L["🧠 LlmAgent"] --> M["☁️ Gemini"]
    C -.->|errors| E["❗ ApiExceptionHandler<br/>problem+json"]
```

### 🧠 Phase 3 — More agents + memory

- [x] 🤖 `CodingAgent` (`language` option) · `ResearchAgent` · `SummarizerAgent` (`style`, `maxWords`)
- [x] ⚙️ `PlatformProperties` for `platform.*` settings (`platform.memory.max-messages=20`)
- [x] 🧠 Chat memory bean in `AiConfig` — last 20 messages per conversation, in RAM until Phase 4
- [x] 🔧 `LlmAgent`: memory per `conversationId`, a failed call leaves no unanswered question behind, `attribute()` helpers
- [x] 📊 `/api/platform`: reports the real `memoryMaxMessages` (20)
- [x] 🌐 `ConversationController` — `DELETE /api/conversations/{id}` → `204`
- [x] 🧪 58 tests: every agent's options, memory, forgetting — `./mvnw test` 🟢
- [x] 💾 Committed and pushed: `ce9b0f9` memory · `6ea75a3` forget endpoint · `a7a1660` three agents

✅ **Done when** the Agents page shows four agents and a follow-up question remembers the last one. **Checked Oct 10:** 🟢

| Check (real Gemini, through the frontend proxy) | Result |
|---|---|
| 📋 `GET /api/agents` | 🟢 coding · general · research · summarizer, with their options |
| 🧠 Follow-up question | 🟢 *"My favourite colour is teal"* → *"What is my favourite colour?"* → **Teal** |
| 🗑️ `DELETE /api/conversations/{id}` | 🟢 `204`, then the same question → **unknown** |
| 💻 Coding, `language=Python` / auto-detect | 🟢 Python code · `metadata.language` = `Python` / `rust` |
| 📝 Summarizer, `tldr`, 25 words | 🟢 One sentence · `compressionRatio` 0.35 |
| 🔬 Research | 🟢 Summary / findings format · `confidence` = `high` |

```mermaid
sequenceDiagram
    participant UI as 🖥️ Console
    participant A as 🤖 Agent
    participant M as 🧠 Chat memory
    participant G as ☁️ Gemini
    UI->>A: message + conversationId
    A->>M: load the last 20 messages
    A->>G: system prompt + history + message
    G-->>A: answer
    A->>M: save question + answer
    A-->>UI: reply + metadata
```

> 🐛 Fixed on the way: the old Research Agent read *"Confidence: medium, not high"* as **high**. It now takes the level
> right after the heading.

- [ ] ✏️ **Known gap:** editing an earlier message in the console drops later turns in the browser only. The server
  still remembers them, so the re-asked question is answered with the old turns as context. Fix later: the console
  could forget and replay the conversation, or send its history with each message.

### 📎 Phase 4 — Files

- [ ] ➕ `pom.xml`: `spring-boot-starter-jdbc` · `h2` · `spring-ai-pdf-document-reader` · `spring-ai-starter-model-chat-memory-repository-jdbc`
- [ ] 🗄️ `schema.sql` — the documents table
- [ ] 📎 `document/` — `StoredDocument` · `DocumentStore` · `JdbcDocumentStore` · `DocumentTextExtractor` · `AttachmentResolver` · 2 exceptions
- [ ] ⚙️ `StorageConfig` · 📦 `DocumentSummary` · 🌐 `DocumentController`
- [ ] 📄 `DocumentAgent` — answers only from attached files
- [ ] 🙈 Add `data/` to `Backend/.gitignore`
- [ ] 📊 `/api/platform`: report real document counts (all `0` today)
- [ ] 🔁 Test a real restart, not just `./mvnw test`

✅ **Done when** an answer quotes an attached PDF, and the file is still there after a restart.

### 🔐 Phase 5 — Login

**Backend**
- [ ] ➕ `spring-boot-starter-security`
- [ ] 🛡️ `SecurityConfig` — every route needs a session, except login
- [ ] 🌐 `AuthController` — `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/auth/me`
- [ ] 📦 `LoginRequest` · `AuthResponse`
- [ ] ❗ The `401` uses problem+json too

**Frontend**
- [ ] 🔌 Replace the `TODO` in [`App.tsx:30`](Frontend/src/App.tsx#L30) with the real login call
- [ ] ♻️ Bring back `useAuth.ts`: `git show 434d0ed^:Frontend/src/hooks/useAuth.ts`
- [ ] 🔄 Stay signed in after a page refresh (`GET /api/auth/me` on load)
- [ ] ⏱️ A `401` from any call sends you back to the login page
- [ ] 🚪 Account row + **Sign out** button in the Sidebar

✅ **Done when** `/api/agents` returns `401` before signing in, and everything works after.

### 🔁 Phase 6 — Provider failover

- [ ] ➕ OpenAI + Anthropic starters (Groq and OpenRouter reuse the OpenAI one)
- [ ] 🔗 `ProviderChain` — providers that have a key, in order; on failure, try the next one
- [ ] 📦 `ProviderStatus` · add `providers[]` to `PlatformStatus` · `metadata.provider` = the provider that actually answered
  (Phase 2 already sends `metadata.provider`; it's always Gemini today)
- [ ] ⚙️ One setting: `AI_PROVIDERS=groq,openrouter,google-genai,openai,anthropic`

🖥️ The frontend already shows the chain in Settings, so no work is needed there.

✅ **Done when** a broken first key fails over to the next provider and the Inspector names it.

### 🚀 Phase 7 — Launch & extras

- [ ] 🌊 **Streaming** — `POST /api/agents/{id}/stream` **and** word-by-word rendering in the Playground
- [ ] 🧭 **Auto-pick** the right agent from the message
- [ ] 🌐 **Web search** for the Research Agent
- [ ] 👥 **Accounts in the database**: sign-up, password reset, Google sign-in (the login page shows notices for these today)
- [ ] 🐳 **Docker**: `Dockerfile` + `compose.yaml` with Postgres
- [ ] 🔒 **HTTPS**, `Secure` cookies, rate limits
- [ ] 🤖 **CI**: `.github/workflows/ci.yml` runs backend tests + frontend lint/build on every push

---

## 🖥️ Frontend

Small fixes you can do now, without the backend:

- [ ] ✏️ Login error says *"email or username"*; there are no emails ([`LoginPage.tsx:157`](Frontend/src/pages/LoginPage.tsx#L157))
- [ ] 💬 [`client.ts:7`](Frontend/src/api/client.ts#L7) points to a `Backend/README.md` that doesn't exist, and lists
  `GET /api/documents`, which nothing calls
- [ ] 🔑 The "no API key" hints name `GROQ_API_KEY`, but the backend uses `GEMINI_API_KEY`. Show `platform.keyEnvVar` instead
  ([`App.tsx:133`](Frontend/src/App.tsx#L133), [`OverviewPage.tsx:89`](Frontend/src/pages/OverviewPage.tsx#L89))
- [ ] 🧪 Add tests (there are none) — e.g. Vitest for `lib/` and `api/client.ts`
- [ ] 💰 Token and cost totals per chat in the Inspector *(later)*

---

## 📚 Docs & housekeeping

- [x] 📈 `PROGRESS.md` removed (`25f5d71`)
- [ ] 📖 **`README.md` still describes a repo with no backend.** Update it for Phase 2:
  - add **how to run the backend**: copy `.env.example` to `.env`, add the key, `./mvnw spring-boot:run` from `Backend/`
  - the status note (line 19), the *rebuilding* badge (13), the diagram (72), line 127 and the footer (194)
  - Troubleshooting (line 162) still says *"there is no backend yet"*
- [ ] 🔗 Remove the links to `PROGRESS.md` from `README.md` (lines 20, 123, 154, 187) and `BACKEND.md` (footer)
- [ ] 🩺 `README.md` lists `FRONTEND_BUG_AUDIT.md` (lines 124, 189), but that file was never committed
- [ ] 🏷️ `BACKEND.md`: the status badge says *Not started* and every phase is 🔴; mark Phases 0–2
- [ ] 📝 [`Frontend/README.md:175`](Frontend/README.md#L175) says the backend includes Spring Security; it doesn't yet
- [ ] 🧾 `pom.xml`: fill in or delete the empty `<name/>`, `<description/>`, `<licenses>`, `<developers>`, `<scm>`
- [ ] 🌶️ Lombok is in `pom.xml` but no code uses it. On Java 25 it prints `sun.misc.Unsafe` warnings and breaks VS Code's annotation processing, so consider removing it
- [ ] 🗑️ Delete the stray `target/` folder at the repo root (old build output)
- [ ] 🌿 Delete branch `phase-2/foundation` (local + GitHub). It's from the old backend and fully merged into `main`
- [ ] ⚖️ Add a `LICENSE` *(optional)*

---

## 🔒 Before anyone else can reach it

- [ ] 🔑 The leaked key is revoked (see [Do these next](#-do-these-next))
- [ ] 🧹 Delete the local branch `backup-before-scrub`, the only place both leaked keys still sit together
  (`53ec5b2`, `6619ece`). It was never pushed, and deleting it can't be undone
- [ ] 🏠 Keep `server.address=127.0.0.1` until Phase 5 login works
- [ ] 🔐 HTTPS + `Secure` cookies (Phase 7)
- [ ] 🙈 No keys in `VITE_*` variables; every visitor can see them

---

<div align="center">
<sub>✅ Task list · last checked against the code on 10 October 2026 · tick boxes as you go</sub>
</div>
