const PALETTE = ['#8b7cff', '#34d399', '#fbbf24', '#60a5fa', '#f472b6', '#fb923c', '#2dd4bf']

/** Stable colour per agent id so the same agent always looks the same. */
export function agentColor(id: string): string {
  let h = 0
  for (const ch of id) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return PALETTE[h % PALETTE.length]
}
