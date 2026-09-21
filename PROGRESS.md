# Project Progress

How the Multi-Agent AI Platform got from an empty folder to what it is today, and what's planned
next. For how to run and use the project, see [`README.md`](README.md).

**Timeline:** 18–21 September 2026 · **Commits:** 43 on `main` · **Tests:** 54, all passing

---

## At a glance

```
Day 1 (Sep 18)          Day 2 (Sep 19)          Day 3 (Sep 20)                    Today
│                        │                        │                                │
├─ Backend scaffold      ├─ Frontend hooks,       ├─ Provider switch                ├─ Universal
├─ Agent core            │  libraries,            │  (OpenAI → Claude →             │  attachments
│  (contract, registry,  │  components            │  Gemini)                        ├─ Security
│  orchestrator)         ├─ Pages + routing        ├─ CORS fix                       │  hardening
└─ Frontend scaffold     └─ First working UI       ├─ Full Playground overhaul      └─ Docs rewrite
                                                    │  (per-agent chats, edit/copy,
                                                    │  per-agent color, language
                                                    │  picker)
                                                    └─ Document Q&A → universal
                                                       file attachments
```

---

## Phase 0 — Scaffolding (Sep 18)

The two halves of the project started as independent scaffolds.

| Commit | What it added |
|---|---|
| `810600e` | Vite + React 19 + TypeScript frontend scaffold |
| `04b3960` | Spring Boot 4 backend scaffold |
| `5fc9723` | Trimmed backend dependencies so the app context actually boots |

**Outcome:** two apps that start, neither one doing anything yet.

## Phase 1 — Backend core: the agent abstraction (Sep 18)

The foundational design decision of the whole project, made early and never revisited: agents are
plain Spring beans that describe themselves.

| Commit | What it added |
|---|---|
| `f1e6bd1` | `Agent` interface, `AgentRegistry` (auto-discovers `@Component` beans), `AgentOrchestrator` (dispatch by id) |

**Outcome:** a backend that can run an agent by name, with zero agents implemented yet.

## Phase 2 — Frontend foundation (Sep 19)

| Commit | What it added |
|---|---|
| `4d76b31` | First components |
| `c2e315d` | First hooks |
| `61be389` | First libraries (pure logic, separated from components) |
| `2fc0664` | The four pages: Overview, Playground, Agents, Settings |
| `1917714` | Sidebar + hash-based routing wired into `App.tsx` |
| `9b0f1ae` | End-to-end functionality — first version that actually worked |

**Outcome:** a console with real navigation and a chat page, talking to nothing real yet.

## Phase 3 — Wiring frontend to backend (Sep 19–20)

| Commit | What it added |
|---|---|
| `500cd8c` | Shared TypeScript types mirroring the backend's Java records |
| `f711de4` | Design tokens and base styles (dark/light theme system) |
| `8c33a18` | Real API client + an in-browser **simulation mode** for when the backend is offline |
| `ca5acbf` | Vite dev-server proxy so the browser only ever talks to one origin |
| `386a94a` | Page metadata, fonts |

Two bugs fixed in this phase, both from real testing rather than code review:

- **CORS rejecting the dev server.** Vite falls back to port 5174+ when 5173 is busy; the backend's
  CORS allowlist was pinned to `5173` exactly. Fixed by switching to `http://localhost:*` origin
  patterns.
- **Provider churn.** The backend went through three model providers before settling —
  **OpenAI → Anthropic Claude → Google Gemini** — while building out `AI_PROVIDER` as a proper
  switch (all three provider starters stay on the classpath; one activates). Gemini is the default
  today because it has a generous free tier for development.

**Outcome:** a console that talks to a real backend, with an offline fallback so the UI is always
explorable.

## Phase 4 — The five agents, the document pipeline, and first docs (Sep 20)

The backend grew from "can run an agent" to five real ones, plus everything needed to ground an
answer in an uploaded file: `DocumentStore`, PDF/text extraction, and the `document` agent. The
first `README.md` landed here too — commit `e707cb9`, with a `Backend/README.md` and
`Frontend/README.md` alongside it.

**Outcome:** feature-complete v1 — five working agents, document Q&A, a documented API.

## Phase 5 — The Playground overhaul (Sep 20)

The single biggest chunk of work in the project: turning a working chat page into a real chat
product. Delivered as one coherent set of changes, confirmed with two design decisions along the
way (edit-and-regenerate over edit-in-place; accent-only per-agent color over full-theme).

| Area | What changed |
|---|---|
| **Layout** | Sidebar auto-collapses when you open the Playground, so the conversation takes the full screen; reopens on request for that visit only |
| **Editing** | Edit a sent prompt — the stale reply is dropped and the agent re-answers the new text |
| **Per-agent identity** | Each agent gets a deterministic, contrast-checked accent color (your bubbles, composer glow, send button) |
| **Output** | Wider reply bubbles for long output; code blocks get a language chip and their own copy button |
| **Coding agent** | Free-text language field replaced with a real 21-language dropdown |
| **Per-agent chats** | *(commits `ec72603`, `2e8bab0`, `3dd68db`)* Each agent keeps its own conversation list; switching agents always opens a fresh chat instead of relabeling the current one |

**Outcome:** the Playground stopped looking like a wired-up prototype and started looking like a
product.

## Phase 6 — Universal file attachments (Sep 20)

The `document` agent originally owned the only file picker, tucked into an options bar above the
message box. This phase generalised it: any agent can ground an answer in an attached file.

| Commit | What it added |
|---|---|
| `d04cfb9`, `27db4ca` | `AttachmentResolver` + `LlmAgent` folding attached files into every agent's system prompt |
| `a60b30e`, `dea1f82`, `4aa2ab6`, `d83ecb3` | Every concrete agent updated to take the resolver |
| `e1989be` | `document` agent simplified to "answer from whatever is attached," refusing with a clear `400` if nothing is |
| `8511783` | The now-redundant `DOCUMENT` parameter type removed from `AgentParameter` |
| `3cd57ad` | `Attachment[]` added to `ChatMessage` so a sent file shows as a chip on the message |
| `e351eae` | Paperclip button beside Send, plus drag-and-drop and paste-to-attach on the whole chat |
| `d5e826c`, `c4b99aa` | Frontend client and simulation mode brought in line |
| `2f07e9d`, `24ae5fa` | Backend tests rewritten against the new attachment path (4 new cases: shared context budget across files, evicted ids skipped rather than erroring) |

**Outcome:** attaching a file is a universal action, not a document-agent feature. A file stays in
context for the whole conversation, not just the message it was attached to.

## Phase 7 — Security hardening (Sep 20–21)

A real Gemini API key was accidentally hardcoded into `application.properties` — twice, in two
separate commits, the second time after the first was already caught. Both times the fix was the
same:

1. Replace the hardcoded value with the same safe `${GEMINI_API_KEY:missing-api-key}` placeholder
   pattern the other two providers already used.
2. Scrub the literal key from local git history (`git filter-branch` across all 43 affected
   commits), with a backup branch taken first.
3. **Rotate the exposed key** — history-scrubbing local commits does not undo a key that already
   reached the GitHub remote; only revoking the credential does.

GitHub's push protection caught the second occurrence before it left the machine — a working
example of the safety net doing its job, not just a problem to work around.

**Outcome:** no live secret in the codebase or its history; the pattern documented in `README.md`
under Security notes so it doesn't recur a third time.

## Phase 8 — Documentation (Sep 21)

The root `README.md` was rebuilt twice: first into a focused quick-start-plus-feature-tour, then
into the comprehensive reference it is today — full API request/response shapes, every
configuration property, an "add your own agent" walkthrough with a complete working example, and
a troubleshooting table built from errors actually seen while building the project. This file is
the third piece: the project's own history, kept separate so the usage guide doesn't get diluted
with narrative.

---

## Where things stand today

**Backend**
- 5 agents: coding, research, summarizer, document, general — each a self-describing Spring bean
- 3 interchangeable model providers (Gemini default, Claude, OpenAI) via one environment variable
- File attachments available to every agent, not just document; shared context budget, stale ids
  skipped gracefully
- Per-conversation memory (last 20 messages replayed), forgotten on request
- RFC 9457 `problem+json` errors, with provider-specific exception translation
- 54 tests, all offline against a stubbed model — no API key needed to verify correctness

**Frontend**
- Full chat console: per-agent conversation lists, edit-and-regenerate, response inspector,
  per-agent accent theming, drag/drop/paste file attachments
- Works with no backend running (simulation mode mirrors real behaviour, including attachments)
- Zero runtime dependencies beyond React itself
- Passes `tsc -b`, `vite build`, and ESLint with the React 19 compiler rules clean

**What's deliberately not built yet:** authentication (the API is open by design, for local use),
persistent storage (documents and memory are in-memory), and automatic agent routing (the caller
names the agent; the orchestrator is shaped so a router can slot in later).

---

## Roadmap

### Near-term
- [ ] **Automatic agent routing** — let an LLM (or a cheap classifier) pick the agent from the
  message instead of the caller naming it. `AgentOrchestrator` already isolates dispatch behind
  one method specifically for this.
- [ ] **Live web search** for the Research Agent, which is knowledge-only today and says so.
- [ ] **Streaming responses** — replies currently arrive whole; token-by-token streaming would cut
  perceived latency on longer answers.

### Mid-term
- [ ] **Persistent storage** — `DocumentStore` and conversation memory are in-memory by design,
  sitting behind interfaces so a real database is a swap, not a rewrite.
- [ ] **Multi-file reasoning tools** for the Document Agent — cross-file comparison and citation
  across more than the two-or-three-file case it handles today.
- [ ] **Usage accounting** — token/cost totals per conversation, surfaced in the Inspector.

### Long-term
- [ ] **Authentication & multi-user support** — the API is intentionally open for a single
  developer today; a real auth layer is needed before it could run beyond localhost.
- [ ] **Agent-to-agent handoff** — today each request goes to exactly one agent; a pipeline mode
  (e.g. Research → Summarizer) is a natural extension of the existing orchestrator.
- [ ] **Deployment story** — Dockerfile / Compose setup for a one-command production-style run
  (Postgres is already a dependency in `pom.xml`, reserved for this).

---

<div align="center">
<sub>See <a href="README.md">README.md</a> for how to run and use the project today.</sub>
</div>
