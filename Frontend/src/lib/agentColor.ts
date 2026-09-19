const PALETTE = ['#8b7cff', '#34d399', '#fbbf24', '#60a5fa', '#f472b6', '#fb923c', '#2dd4bf']

function hash(id: string): number {
  let h = 0
  for (const ch of id) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return h
}

/** Stable colour per agent id so the same agent always looks the same (small avatar swatch only). */
export function agentColor(id: string): string {
  return PALETTE[hash(id) % PALETTE.length]
}

/**
 * Index-aligned with `PALETTE` — same hash, same agent, so an agent's avatar hue and its chat theme
 * always correspond. These are deliberately different (darker/more saturated) hexes: the vivid
 * `PALETTE` swatches read fine as a small avatar fill but several fail WCAG contrast for white text
 * on a full bubble background. Each of these clears 4.5:1 against white.
 */
const ACCENT_PALETTE = ['#6656e6', '#0c7f59', '#92600f', '#1f5fc4', '#b52c78', '#a34f0a', '#0c7f72']

function hexToRgb(hex: string): [number, number, number] {
  const h = hex.replace('#', '')
  return [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]
}

function hexToRgba(hex: string, alpha: number): string {
  const [r, g, b] = hexToRgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** Darken a hex colour toward black by `amount` (0-1), for a readable `--accent-strong` in any theme. */
function darken(hex: string, amount: number): string {
  const [r, g, b] = hexToRgb(hex)
  const f = (c: number) => Math.round(c * (1 - amount))
  return `rgb(${f(r)}, ${f(g)}, ${f(b)})`
}

export interface AgentTheme {
  accent: string
  accentStrong: string
  accentSoft: string
  accentRing: string
}

/**
 * The four `--accent*` tokens (see `index.css`) re-themed for one agent. Applying these as inline
 * CSS custom properties on a wrapping element re-colours every selector that already consumes those
 * tokens (composer focus ring, send button, your own message bubbles, the selected-reply highlight)
 * without touching a single CSS rule — see `PlaygroundPage.tsx`.
 */
export function agentTheme(id: string): AgentTheme {
  const accent = ACCENT_PALETTE[hash(id) % ACCENT_PALETTE.length]
  return {
    accent,
    accentStrong: darken(accent, 0.18),
    accentSoft: hexToRgba(accent, 0.14),
    accentRing: hexToRgba(accent, 0.45),
  }
}

/**
 * `agentTheme(id)` as an inline-`style`-ready object, keyed by the actual `--accent*` custom
 * property names. Cast the result `as CSSProperties` at the call site (React's types don't know
 * about custom properties) — see `PlaygroundPage.tsx`.
 */
export function agentThemeStyle(id: string): Record<string, string> {
  const t = agentTheme(id)
  return {
    '--accent': t.accent,
    '--accent-strong': t.accentStrong,
    '--accent-soft': t.accentSoft,
    '--accent-ring': t.accentRing,
  }
}
