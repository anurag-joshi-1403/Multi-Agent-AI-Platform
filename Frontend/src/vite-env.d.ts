// Typed build-time env vars (Vite exposes only VITE_-prefixed ones to the client bundle).
// Never put secrets here: every VITE_ value is inlined into the JavaScript served to browsers.
interface ImportMetaEnv {
  /** Backend API base URL, e.g. `https://api.example.com/api`. Defaults to `/api` — see `api/client.ts`. */
  readonly VITE_API_BASE?: string
}
