<div align="center">

<img src="Frontend/public/logo.svg" width="88" alt="Multi-Agent AI Platform logo" />

# ✅ Tasks

**Everything left to do on the Multi-Agent AI Platform, in order.**
How to build each backend phase: [`BACKEND.md`](BACKEND.md) · How to run it: [`README.md`](README.md) ·
Done and left, side by side: [`FRONTEND_STATUS.md`](FRONTEND_STATUS.md) · [`BACKEND_STATUS.md`](BACKEND_STATUS.md)

[![Frontend](https://img.shields.io/badge/Frontend-ready-34d399?logo=react&logoColor=white)](#-where-things-stand)
[![Backend](https://img.shields.io/badge/Backend-Phase%207%20of%207-fbbf24?logo=springboot&logoColor=white)](#-where-things-stand)
[![Security](https://img.shields.io/badge/Leaked%20key-rotate%20first-f87171)](#-a--wrap-up-whats-built)
[![Updated](https://img.shields.io/badge/Updated-Oct%2010%2C%202026-8b7cff)](#-where-things-stand)

</div>

---

## 🧭 Where things stand

| | |
|---|---|
| 🖥️ **Frontend** | 🟢 Finished — lint ✅ build ✅, runs on its own with **simulated** replies |
| ⚙️ **Backend** | 🟡 5 agents · **5 AI providers with failover** · uploads · H2 storage · login · Phase 7 to go |
| 🔐 **Login** | 🟢 Real session login · **sign-up + login against MongoDB** (`AUTH_USERS` is gone) |
| 🍃 **Accounts DB** | 🟢 MongoDB set up and checked — [checklist](#-mongodb-accounts--manual-setup), code explained in [`DATABASE.md`](DATABASE.md) |
| 🧪 **Tests** | 🟡 Backend: 136 passing (no AI key or network; the account tests need MongoDB running) · Frontend: none |
| 📚 **Docs** | 🟡 Behind the code — `README.md` still says there's no backend, and links to removed or missing files · 🟢 new: [`FRONTEND_STATUS.md`](FRONTEND_STATUS.md), [`BACKEND_STATUS.md`](BACKEND_STATUS.md) |
| 🔑 **Secrets** | 🟡 New key is in `Backend/.env` · make sure the leaked `…Egng` (`eebf0a8`, on GitHub) is revoked |
| 💾 **Git** | 🟢 Everything committed and pushed to `origin/main` — MongoDB accounts `bbc501c` · sign-up `44b3856` · sign-up form `087a3e6` |

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
    class P1,P2,P3,P4,P5,P6 done
    class P0 doing
    class P7 todo
```

| Phase | Status | Left to do |
|---|---|---|
| 🧹 **0 · Get ready** | 🟡 | Confirm the leaked keys are revoked |
| 🧱 **1 · Skeleton** | 🟢 | — |
| 🤖 **2 · First agent** | 🟢 | — |
| 🧠 **3 · More agents + memory** | 🟢 | — |
| 📎 **4 · Files** | 🟢 | — |
| 🔐 **5 · Login** | 🟢 | — |
| 🔁 **6 · Failover** | 🟢 | — |
| 🚀 **7 · Launch & extras** | 🔴 | Streaming, Docker, CI |

---

## 📝 To-do list

> **All the remaining work, cut into small pieces** — one piece is one sitting. Work top to bottom; a milestone is
> done when all its boxes are ticked. The phase sections further down are the record of what was built.
>
> **Size:** 🟢 under 30 min · 🟡 1–2 hours · 🔴 half a day &nbsp;·&nbsp; **Who:** 👤 you · 🤖 me (Claude) · 👥 together

```mermaid
flowchart LR
    classDef now fill:#fde68a,stroke:#b7791f,color:#3a2a05,font-weight:bold
    classDef later fill:#e5e7eb,stroke:#6b7280,color:#1f2937
    A["🏁 A<br/>Wrap up"] --> B["📚 B<br/>Docs"] --> C["🖥️ C<br/>Frontend fixes"]
    B --> D["🧹 D<br/>Housekeeping"]
    C --> E["🔒 E<br/>Safe to share"]
    D --> E
    E --> F["🌊 F<br/>New features"] --> G["🐳 G<br/>Ship it"]
    class A now
    class B,C,D,E,F,G later
```

| Milestone | Pieces | Why it matters |
|---|---|---|
| 🏁 **A · Wrap up what's built** | 5 | Save the MongoDB work and close the leaked-key story |
| 📚 **B · Docs catch up** | 6 | `README.md` still says there is no backend |
| 🖥️ **C · Frontend fixes** | 7 | Small bugs and the first frontend tests |
| 🧹 **D · Backend housekeeping** | 6 | Warnings, leftovers, optional checks |
| 🔒 **E · Safe to share** | 5 | Needed before anyone but you can reach it |
| 🌊 **F · New features** | 9 | Phase 7: streaming, auto-pick, web search, more sign-in |
| 🐳 **G · Ship it** | 4 | Phase 7: Docker and automatic checks |

### 🏁 A · Wrap up what's built

- [ ] **A1** 🔑 Revoke the leaked Gemini keys `…Egng` and `…5i5w` in [AI Studio](https://aistudio.google.com/apikey) — 👤 🟢
- [ ] **A2** 📝 Sign up once in the browser (MongoDB → backend → frontend → *Sign Up*); write any error in the
  [errors log](#-setup-errors-log) — 👤 🟢
- [x] **A3** 💾 Committed and pushed in small commits: `bbc501c` accounts in MongoDB · `44b3856` sign-up endpoint ·
  `087a3e6` sign-up form · `fbb041d` DATABASE.md · `25c2fdc` status pages — 🤖 🟢
- [ ] **A4** 🧹 Delete the local branch `backup-before-scrub`, the only place both leaked keys still sit together
  (`53ec5b2`, `6619ece`; never pushed, can't be undone) — after **A1** — 🤖 🟢
- [ ] **A5** 🌿 Delete the branch `phase-2/foundation`, local and on GitHub (old backend, fully merged into `main`) — 🤖 🟢

### 📚 B · Bring the docs up to date

- [ ] **B1** 📖 `README.md` quick start for the backend: MongoDB running, `Backend/.env` (`GEMINI_API_KEY`, `MONGODB_URI`),
  `.\mvnw spring-boot:run` from `Backend/` — 🤖 🟢
- [ ] **B2** 🔐 `README.md` login part: sign up / log in; drop *"any username and password"* (line 54); rewrite the
  Security notes — 🤖 🟢
- [ ] **B3** 🏷️ `README.md` status: the note (line 19), the *rebuilding* badge (13), the diagram (72), line 127, the
  footer (194), and Troubleshooting's *"there is no backend yet"* (162) — 🤖 🟢
- [ ] **B4** 🔗 Remove dead links: `PROGRESS.md` (README lines 20, 123, 154, 187 and the `BACKEND.md` footer) and
  `FRONTEND_BUG_AUDIT.md` (README lines 124, 189) — 🤖 🟢
- [ ] **B5** 🗺️ `BACKEND.md`: the status badge still says *Not started* and every phase is 🔴 — mark Phases 0–6 done — 🤖 🟢
- [ ] **B6** 📝 [`Frontend/README.md:175`](Frontend/README.md#L175): the Spring Security line — describe the real
  login and sign-up — 🤖 🟢

### 🖥️ C · Frontend fixes

- [ ] **C1** 🚪 A broken API address saved in the browser must not lock you out (the login-page 404): `apiBase()`
  ignores values that fail `isValidApiBase` — `api/client.ts` — 🤖 🟢
- [ ] **C2** 🔑 The "no API key" hints show `platform.keyEnvVar` instead of a fixed `GROQ_API_KEY` —
  [`App.tsx:133`](Frontend/src/App.tsx#L133), [`OverviewPage.tsx:89`](Frontend/src/pages/OverviewPage.tsx#L89) — 🤖 🟢
- [ ] **C3** ✏️ Editing an earlier message also rewinds the server's memory (today the server keeps the turns the
  browser dropped) — backend + `PlaygroundPage.tsx` — 🤖 🟡
- [ ] **C4** 💬 Chats per account, not per browser (two people on one browser see each other's chats) —
  `useConversations.ts`, `lib/storage.ts` — 🤖 🟡
- [ ] **C5** 🧪 Set up Vitest and write the first tests, for `lib/` — 🤖 🟡
- [ ] **C6** 🧪 Tests for `api/client.ts`: offline fallback, a `401` sends you to login, problem+json messages — 🤖 🟡
- [ ] **C7** 🎭 A simulated reply says *why*: "Simulation mode is on (Settings)" when it was chosen, and "the backend is offline" only when it is — today it always says offline — `api/mock.ts` — 🤖 🟢

### 🧹 D · Backend housekeeping

- [ ] **D1** 📏 The `413` names the limit (*"The file is larger than 20 MB"*), not Spring's *"Maximum upload size
  exceeded"* — `ApiExceptionHandler` — 🤖 🟢
- [ ] **D2** 🌶️ Remove Lombok: no code uses it, and on Java 25 it causes `sun.misc.Unsafe` warnings and VS Code
  errors — `pom.xml` — 🤖 🟢
- [ ] **D3** 🧾 `pom.xml`: fill in or remove the empty `<name/>`, `<description/>`, `<licenses>`, `<developers>`, `<scm>` — 🤖 🟢
- [ ] **D4** 🗑️ Delete the stray `target/` folder at the repo root (old build output) — 🤖 🟢
- [ ] **D5** 🐘 *(optional)* Run once on a real PostgreSQL (`SPRING_DATASOURCE_URL`); `schema.sql` is written for it
  but only H2 is tested — 👥 🟡
- [ ] **D6** ⚖️ *(optional)* Add a `LICENSE` — your choice of license — 👤 🟢

### 🔒 E · Safe to share — before anyone but you can reach it

- [ ] **E1** 🎟️ Sign-up needs an invite code (`SIGNUP_CODE` in `.env`), so strangers can't spend your AI credits — 🤖 🟡
- [ ] **E2** 🧱 Rate limits on login and sign-up (e.g. 5 tries a minute per IP) — 🤖 🟡
- [ ] **E3** 🔐 HTTPS and the `Secure` session cookie — depends on where it runs (see **G3**) — 👥 🟡
- [ ] **E4** 🌍 Only then open it to other machines: change `server.address=127.0.0.1` — 👥 🟢
- [ ] **E5** 🙈 Check that no key sits in a `VITE_*` variable (every visitor can read those) — 🤖 🟢

### 🌊 F · New features (Phase 7)

- [ ] **F1** 🌊 Streaming, backend: `POST /api/agents/{id}/stream` sends the reply as it is written (one provider) — 🤖 🟡
- [ ] **F2** 🔁 Streaming with failover: switch provider if one fails before the first word — 🤖 🟡
- [ ] **F3** 🖥️ Streaming in the Playground: replies appear word by word — 🤖 🟡
- [ ] **F4** 🧭 Auto-pick, backend: choose the right agent from the message — 🤖 🟡
- [ ] **F5** 🧭 Auto-pick, frontend: an *Auto* choice in the agent picker — 🤖 🟢
- [ ] **F6** 🌐 Web search for the Research Agent (needs a search API key, e.g. Tavily or Brave) — 👥 🔴
- [ ] **F7** 🔑 Password reset — an admin sets a new password, or a reset link — 🤖 🟡
- [ ] **F8** 🇬 Google sign-in (OAuth2; needs a Google Cloud OAuth client) — 👥 🔴
- [ ] **F9** 💰 Token totals per chat in the Inspector — 🤖 🟡

### 🐳 G · Ship it (Phase 7)

- [ ] **G1** 🐳 Backend `Dockerfile` — 🤖 🟢
- [ ] **G2** 🐳 Frontend `Dockerfile`: build, plus a small web server that forwards `/api` to the backend — 🤖 🟡
- [ ] **G3** 🧩 `compose.yaml`: backend + frontend + MongoDB, with volumes for the H2 file and MongoDB — 🤖 🟡
- [ ] **G4** 🤖 CI: `.github/workflows/ci.yml` runs the backend tests (with a MongoDB service) and the frontend
  lint + build on every push — 🤖 🟡

---

## ⚙️ Backend, phase by phase

> File-by-file details for every phase are in [`BACKEND.md`](BACKEND.md). Keep JSON names identical
> to [`Frontend/src/types.ts`](Frontend/src/types.ts).

### 🧹 Phase 0 — Get ready

- [x] ☕ Java 25 installed (Temurin 25.0.3) · 🟩 Node 24 · 🧰 VS Code Java + Spring extensions
- ➡️ 🔑 Revoke **both** keys found in the git history, in [AI Studio](https://aistudio.google.com/apikey) — to-do **A1**

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

- ➡️ ✏️ **Known gap** (to-do **C3**): editing an earlier message in the console drops later turns in the browser only. The server
  still remembers them, so the re-asked question is answered with the old turns as context. Fix later: the console
  could forget and replay the conversation, or send its history with each message.

### 📎 Phase 4 — Files

- [x] ➕ `pom.xml`: `spring-boot-starter-jdbc` · `h2` · `spring-ai-pdf-document-reader` · `spring-ai-starter-model-chat-memory-repository-jdbc`
- [x] 🗄️ `schema.sql` — the documents table (works on H2 and Postgres)
- [x] 📎 `document/` — `StoredDocument` · `DocumentStore` · `JdbcDocumentStore` · `DocumentTextExtractor` · `AttachmentResolver` · `UnsupportedDocumentException` (→ `415`)
- [x] 📦 `DocumentSummary` · 🌐 `DocumentController` — `POST /api/documents` → `201`, `DELETE /api/documents/{id}` → `204`
- [x] 📄 `DocumentAgent` — answers only from attached files; `400` without one (`InvalidAgentRequestException`)
- [x] 📎 **Every** agent reads attached files (in the system prompt, never in memory) and reports `documentNames`
- [x] 📝 Summarizer summarises the attached file, not the short message, when a file is attached
- [x] 🙈 `Backend/data/` in `Backend/.gitignore`
- [x] 📊 `/api/platform` reports real document counts and limits (50 files, 60 000 chars)
- [x] 🧪 89 tests, including the real `schema.sql` on H2 — `./mvnw test` 🟢
- [x] 🔁 Tested a real restart, not just `./mvnw test`
- [x] 💾 Committed and pushed: `d41a0b6` H2 storage · `7b44671` uploads · `633fcd0` files for every agent + Document Agent

> ✂️ **Left out on purpose:** `StorageConfig` and the old in-memory store. There is only one store now, so nothing
> needs choosing; Postgres is a change of `SPRING_DATASOURCE_URL`, not of code. `DocumentNotFoundException` too:
> nothing throws it without a "get one document" endpoint.

✅ **Done when** an answer quotes an attached PDF, and the file is still there after a restart. **Checked Oct 10:** 🟢

| Check (real Gemini, through the frontend proxy) | Result |
|---|---|
| 📤 Upload a 2-page PDF | 🟢 `201` · `pages: 2` · preview with `[page 1]` / `[page 2]` |
| 📄 Document Agent: *"When does the lease renew?"* | 🟢 Quotes page 2: *"renews automatically every 1 May unless cancelled 90 days before"* |
| 🚫 Document Agent with no file | 🟢 `400` *"needs at least one attached file"* |
| 💬 General Assistant with the same file | 🟢 *"Northwind Traders"* · `documentNames: [lease.pdf]` |
| 🖼️ PNG upload · 21 MB upload | 🟢 `415` · `413`, both problem+json |
| 🔁 **Restart the backend** | 🟢 File still stored · rent question answered from it · memory: *"code word?"* → **Falcon** |
| 🗑️ `DELETE /api/documents/{id}` | 🟢 `204`, stored count back to 0 |

```mermaid
flowchart LR
    F["📎 File"] -->|"POST /api/documents"| X["🔍 DocumentTextExtractor<br/>PDF pages / text"]
    X --> S[("🗄️ H2 file<br/>Backend/data")]
    Q["💬 Message +<br/>attachments ids"] --> A["🤖 Any agent"]
    S -->|"AttachmentResolver"| A
    A -->|"system prompt + files"| G["☁️ Gemini"]
    M[("🧠 Chat memory")] <--> A
    M -.->|same database| S
```

- ➡️ 🐘 (to-do **D5**) Run once on a real Postgres (set `SPRING_DATASOURCE_URL`); the schema is written for it but only H2 is tested
- ➡️ 📏 (to-do **D1**) The `413` message is Spring's *"Maximum upload size exceeded"*; it could name the 20 MB limit

### 🔐 Phase 5 — Login

**Backend**
- [x] ➕ `spring-boot-starter-security` (+ `-test`)
- [x] 🛡️ `SecurityConfig` — every route needs a session, except login, logout and `/actuator/health`
- [x] 🌐 `AuthController` — `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/auth/me`
- [x] 📦 `LoginRequest` (never prints the password) · `AuthResponse`
- [x] ❗ The `401` uses problem+json too; a wrong username and a wrong password get the same answer
- [x] 👤 Accounts from `AUTH_USERS` (`name:password,…`), BCrypt-hashed on boot; a malformed or repeated entry stops startup
- [x] 🔒 Safer than the old version: **no `admin`/`admin` default** (random one-time password instead), **new session id on
  login** (session fixation), cookie `HttpOnly; SameSite=Strict`, no session opened by rejected calls, 8 h timeout

**Frontend**
- [x] 🔌 Replaced the `TODO` in `App.tsx` with the real login call
- [x] ♻️ Brought back `useAuth.ts`
- [x] 🔄 Stays signed in after a page refresh (`GET /api/auth/me` on load, with a short splash)
- [x] ⏱️ A `401` from any call sends you back to the login page
- [x] 🚪 Account row + **Sign out** button in the Sidebar (name hidden on phones)
- [x] 💬 Login page explains where accounts come from, and says plainly when the backend can't be reached
- [x] 🧪 `./mvnw test` → 110 🟢 · `npm run lint` + `npm run build` 🟢
- [x] 💾 Committed and pushed: `12880ad` backend login · `8e9f564` frontend login

✅ **Done when** `/api/agents` returns `401` before signing in, and everything works after. **Checked Oct 10:** 🟢

| Check (through the frontend proxy, with your real `.env`) | Result |
|---|---|
| 🚫 `/api/agents`, `/api/auth/me` without a session | 🟢 `401` problem+json · no cookie handed out |
| 🔑 Wrong password | 🟢 `401` *"Incorrect username or password."* |
| ✅ Sign in as `admin` with the one-time password from the log | 🟢 `200` · cookie `HttpOnly; SameSite=Strict` |
| 🤖 With the session: agents · a real Gemini run · upload + delete | 🟢 5 agents · *"Ready."* · `201` / `204` |
| 🚪 Sign out, then reuse the old cookie | 🟢 `204`, then `401` |

```mermaid
sequenceDiagram
    actor U as 👤 You
    participant L as 🔐 Login page
    participant A as ⚙️ AuthController
    participant C as 🖥️ Console
    U->>L: username + password
    L->>A: POST /api/auth/login
    A-->>L: 200 + session cookie (new id)
    L->>C: open the console
    C->>A: every /api call carries the cookie
    A-->>C: 401 if the session is gone → back to Login
```

> ⚠️ **Not checked:** clicking through the login page in a real browser. The API flow, lint and build all pass; give it
> one manual try. Also by design: login needs the backend (no offline bypass), and restarting the backend signs
> everyone out (sessions live in memory).

- ➡️ 💬 (to-do **C4**) Chats are stored per **browser**, not per account: two people sharing a browser see each other's chats

### 🔁 Phase 6 — Provider failover

- [x] ➕ OpenAI + Anthropic starters (Groq and OpenRouter use the OpenAI client at their own address)
- [x] 🔗 `ProviderChain` — providers that have a key, in order; on any failure, the next one answers
- [x] 🧯 `ProviderFailure` reads every SDK's errors (Gemini, OpenAI, Anthropic): bad key · busy · unreachable · rejected
- [x] 📦 `ProviderStatus` · `providers[]` in `/api/platform` · `metadata.provider` = who answered · `metadata.failedOver` = who failed first
- [x] ⚙️ One setting: `AI_PROVIDERS` (default `groq,openrouter,google-genai,openai,anthropic`; empty = default)
- [x] 🔑 All keys in `Backend/.env` (`GEMINI_API_KEY`, `GROQ_API_KEY`, `OPENROUTER_API_KEY`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`);
  models via `*_MODEL`
- [x] ⚡ Fails over fast: one retry per provider (not Spring AI's default of 10 with growing waits), 60 s timeout
- [x] ❗ All failed → one message listing each provider's problem; none has a key → `503` *"No AI provider configured"*
- [x] 🧪 132 tests, including startup with every key empty and with all five clients built — `./mvnw test` 🟢
- [x] 💾 Committed and pushed: `6440093`

> 🐛 **Found on the way:** Spring AI's per-provider setup refuses to start on an empty key, so a copied `.env.example`
> would have crashed the backend. It is switched off now; `AiConfig` builds a client only for providers that have a key.

🖥️ The frontend already shows the chain in Settings, so no work was needed there.

✅ **Done when** a broken first key fails over to the next provider and the Inspector names it. **Checked Oct 10:** 🟢

| Check (real APIs, with your Gemini key + a fake Groq key) | Result |
|---|---|
| 📋 Startup log and `/api/platform` | 🟢 Groq ✅ → OpenRouter ⏭️ → Gemini ✅ → OpenAI ⏭️ → Anthropic ⏭️ |
| 🔁 Groq rejects the fake key | 🟢 Gemini answers *"Ready."* in 2.8 s · `provider: Google Gemini` · `failedOver: [Groq rejected the key]` |
| 💥 Every key fake | 🟢 `502` *"All AI providers failed"*, listing Groq and Gemini |

```mermaid
flowchart LR
    Q["❓ Agent call"] --> G{"Groq<br/>has key?"}
    G -->|no| R{"OpenRouter?"}
    G -->|"yes: try"| GA["❌ rejected"] --> R
    R -->|no| M{"Gemini?"}
    M -->|"yes: try"| A["✅ Answer<br/>provider + failedOver"]
    M -.->|"fails"| N["➡️ OpenAI → Anthropic"] -.-> E["❗ All failed: list"]
```

> ⚠️ **Not checked for real:** OpenRouter, OpenAI and Anthropic answering, and the default model names for Groq,
> OpenRouter, OpenAI and Anthropic. Only a Gemini key was available; the others were checked to build and to fail
> cleanly. Override a model with `GROQ_MODEL`, `OPENROUTER_MODEL`, `OPENAI_MODEL` or `ANTHROPIC_MODEL`.

- ➡️ 🌊 (to-do **F2**) Streaming is not part of the chain yet: Phase 7 needs to add failover to streamed replies too

### 🚀 Phase 7 — Launch & extras

➡️ Split into small pieces in the [to-do list](#-to-do-list): streaming, auto-pick, web search, password reset,
Google sign-in and token totals are milestone **F**; Docker and CI are milestone **G**; HTTPS, `Secure` cookies and
rate limits are milestone **E**.

- [x] 👥 **Accounts in the database**: sign-up + login in MongoDB ([done](#-mongodb-accounts--manual-setup))

---

## 🍃 MongoDB accounts — manual setup

> Sign-ups saved in MongoDB, and every login checked against it. Uploads and chat memory stay in H2.
> The code for each step is in [`DATABASE.md`](DATABASE.md) (**Option B** + the **shared steps**); this is the checklist.

```mermaid
flowchart LR
    I["💿 1–6<br/>Install + run"] --> C["🔌 7–9<br/>Connect backend"] --> K["🧩 10–14<br/>Code"] --> T["🧪 15–20<br/>Check it"]
```

### 💿 Part 1 — Install and run MongoDB (no code yet)

- [x] **1.** Download **MongoDB Community Server** for Windows (`.msi`): <https://www.mongodb.com/try/download/community>
- [x] **2.** Run the installer → **Complete** → ✅ *Install MongoDB as a Service* → ✅ *Install MongoDB Compass*
- [x] **3.** Check it is running — PowerShell: `Get-Service MongoDB` → **Status: Running**
  (or <kbd>Win</kbd>+<kbd>R</kbd> → `services.msc` → *MongoDB Server*)
- [x] **4.** *(optional)* Install **MongoDB Shell**: <https://www.mongodb.com/try/download/shell> → run `mongosh` →
  it should print `Connecting to: mongodb://127.0.0.1:27017` and show a `test>` prompt
- ⏭️ **5.** *(not needed — the backend creates `agents` and `users` on the first sign-up)* Open **Compass** → *Add new connection* → `mongodb://localhost:27017` → *Connect* →
  create database **`agents`** with collection **`users`** (the backend would also create them on the first sign-up)
- ⏭️ **6.** *(optional, skipped — local MongoDB runs without a password)* An app user with its own password — in `mongosh`:

  ```js
  use agents
  db.createUser({ user: "agents_app", pwd: "choose-a-long-password", roles: [{ role: "readWrite", db: "agents" }] })
  ```

  A local install accepts connections without a password unless you switch on authorization in `mongod.cfg`, so
  this only matters once you do.

> ☁️ **MongoDB Atlas instead of a local install?** Create a free cluster → *Database Access*: add a user →
> *Network Access*: add your IP → *Connect* → *Drivers* → copy the `mongodb+srv://…` string, put your password in it,
> and add `/agents` before the `?`. Then skip to step 7.

### 🔌 Part 2 — Connect the backend

- [x] **7.** `Backend/.env` — one line, pick the one that fits:

  | Setup | Line |
  |---|---|
  | Local, no password | `MONGODB_URI=mongodb://localhost:27017/agents` |
  | Local, with the step 6 user | `MONGODB_URI=mongodb://agents_app:YOUR-PASSWORD@localhost:27017/agents?authSource=agents` |
  | Atlas | `MONGODB_URI=mongodb+srv://USER:YOUR-PASSWORD@CLUSTER.mongodb.net/agents?retryWrites=true&w=majority` |

- [x] **8.** `Backend/pom.xml` → add `spring-boot-starter-data-mongodb` **below the H2 block** ([DATABASE.md](DATABASE.md) step **B2**).
  ⚠️ Keep the H2 dependency, and give the MongoDB one **no** `<scope>`
- [x] **9.** **main** `src/main/resources/application.properties`, at the end → `spring.mongodb.uri=${MONGODB_URI}` and
  `spring.data.mongodb.auto-index-creation=true` · **test** `src/test/resources/application.properties`, at the end →
  only `spring.mongodb.uri=mongodb://localhost:27017/agents_test` (step **B4**)

### 🧩 Part 3 — The code (copy it from DATABASE.md)

> Every step in [DATABASE.md](DATABASE.md) says **where** to paste: 🆕 new file · 📍 find this text · ✂️ paste below ·
> ✏️ replace · 🗑️ delete. `AuthController` comes as a complete file to paste over the old one.

- [x] **10.** New folder `Backend/src/main/java/com/project/multi_agent_ai_platform/user/` with `UserAccount`,
  `UserStore`, `UsernameTakenException` (step **3**)
- [x] **11.** `UserDocument`, `UserRepository`, `MongoUserStore` (steps **B5–B7**)
- [x] **12.** `DatabaseUserDetailsService` — login reads MongoDB (step **5**)
- [x] **13.** `SecurityConfig` — delete the `AUTH_USERS` bean, open `/api/auth/signup` (step **6**)
- [x] **14.** `SignupRequest`, the `POST /api/auth/signup` endpoint and the `409` (steps **7–9**)

### 🧪 Part 4 — Check it

- [x] **15.** Start the backend from `Backend/` → no errors, and a line from `org.mongodb.driver` mentioning
  `localhost:27017` (or your Atlas host)
- [x] **16.** Sign up with PowerShell ([DATABASE.md](DATABASE.md) step **10**) → `201`
- [x] **17.** Compass → `agents` → `users`: your account is there, `passwordHash` starts with `$2a$`;
  *Indexes* tab shows `username_1` as **UNIQUE**
- [x] **18.** Same name again → `409` · log in → works · restart the backend → still works
- [x] **19.** Tests (step **11**): `AuthIntegrationTest` now needs MongoDB running — or give the tests a fake in-memory
  `UserStore` so `./mvnw test` works without it
- [x] **20.** The sign-up form in the frontend (step **12**)

✅ **Done when** you sign up in the browser, see the account in Compass, and can still log in after a restart.
**Checked Oct 10:** 🟢 — everything except the browser click (done through the same proxy the browser uses)

| Check (MongoDB 8.3 local, through the frontend proxy) | Result |
|---|---|
| 🍃 Startup | 🟢 `Monitor thread successfully connected … localhost:27017` · 0 warnings |
| 📝 Sign up `checkuser` | 🟢 `201` · signed straight in · `/api/auth/me` `200` · agents + a real Gemini run work |
| 🔁 Same name again | 🟢 `409` *"Username taken"* |
| ✋ Password `short` | 🟢 `400` *"password: at least 8 characters"* |
| 🔐 Log out → log in · wrong password | 🟢 `200` · `401` |
| 🗄️ In MongoDB | 🟢 `passwordHash` = 60-char BCrypt (`$2a$…`) · index `username` **UNIQUE** |
| 🔄 Restart the backend → log in | 🟢 `200` — the account is still there |

> 🧹 The check used a throwaway database (`agents_check`, deleted afterwards), so your real `agents` database is
> created by your first sign-up. `./mvnw test` uses `agents_test`.

**Fixed on the way** (from the earlier hand edits): the H2 dependency was back in `pom.xml` and the MongoDB one had
`<scope>runtime</scope>` (wouldn't compile) · `"api/auth/signup"` was missing its leading `/` · the test properties
had `spring.mongodb.uri=${}` (breaks every test) · `AUTH_USERS` and its leftovers removed everywhere.

---

## 🐞 Setup errors log

> Paste every error here as it happens: the step number, what you did, and the **exact** message (the first
> `Caused by:` line of a Java error is the most useful). Then tell me **"fix error #1"**.

| # | Step | What you did | Error message (paste it exactly) | Status | Fix |
|---|---|---|---|---|---|
| 1 | 15 | `mvn spring-boot:run` in `Backend/` | `ApiExceptionHandler.java:[132,37] cannot find symbol` · `class UsernameTakenException` | 🟢 fixed | The file was saved from an older editor copy, which dropped the `import …user.UsernameTakenException` line (and the `400` field-message override). Both restored; 136 tests pass. Tip: if VS Code says the file changed on disk, use *Revert File* before editing |
| 2 | A2 | Signed in, sent *"hello"* | The reply is tagged **simulated** and says *"the backend is offline"* · console: two `401` on `/api/auth/me` | 🟡 your turn | The backend was up (checked). **Simulation mode** is switched on in this browser (`maap.simulate = true`), so the console never calls it: Settings → turn *Simulation mode* off → *Apply & reconnect*. The two `401`s are normal: on load the page asks "am I signed in?" and the answer is no (twice, because React's dev mode runs that check twice). The wrong wording is to-do **C7** |
| 3 | | | | 🔴 open | |

🔴 open &nbsp;·&nbsp; 🟡 trying a fix &nbsp;·&nbsp; 🟢 fixed

<details>
<summary>🔎 Errors you are likely to meet, and what they mean</summary>

| The error contains | What it means | What to do |
|---|---|---|
| `Timed out after 30000 ms while waiting for a server` · `Connection refused` | MongoDB isn't running, or the host/port is wrong | Step 3: start the service · check `MONGODB_URI` |
| `MongoSecurityException` · `Exception authenticating` · `Authentication failed` | Wrong user or password, or `authSource` is missing | Steps 6–7 |
| `Failed looking up SRV record` | A typo in the Atlas `mongodb+srv://` host | Copy the string from Atlas again |
| Atlas: a timeout, though the string is right | Your IP is not allowed | Atlas → *Network Access* → add your IP |
| `E11000 duplicate key error` | The same username twice — expected | It should come back as `409`; if not, check the `catch` in `MongoUserStore` |
| `expected single matching bean but found 2` … `UserDetailsService` | The old `AUTH_USERS` bean is still there | Step 13 |
| `expected single matching bean but found 2` … `UserStore` | Both `JdbcUserStore` and `MongoUserStore` exist | Keep only `MongoUserStore` |
| The backend tries `localhost:27017` although `MONGODB_URI` points elsewhere | The setting name | Swap `spring.mongodb.uri` ↔ `spring.data.mongodb.uri` |
| Duplicate usernames are accepted | The unique index was never created | Step 9: `auto-index-creation=true`, then drop the collection and restart |
| Sign-up answers `401` | `/api/auth/signup` is not open | Step 13 |

</details>

---

<div align="center">
<sub>✅ Task list · last checked against the code on 10 October 2026 · the <a href="#-to-do-list">to-do list</a> is where the open work lives</sub>
</div>
