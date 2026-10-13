import { describe, expect, it } from 'vitest'
import { agentColor, agentTheme, agentThemeStyle } from './agentColor'

const IDS = Array.from({ length: 500 }, (_, i) => `agent-${i}`)

function rgb(color: string): [number, number, number] {
  if (color.startsWith('#')) return [1, 3, 5].map((i) => parseInt(color.slice(i, i + 2), 16)) as [number, number, number]
  const [r, g, b] = color.match(/\d+(\.\d+)?/g)!.map(Number)
  return [r, g, b]
}

/** WCAG 2 relative luminance and contrast ratio. */
function luminance(color: string): number {
  const [r, g, b] = rgb(color).map((c) => {
    const s = c / 255
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

const contrastWithWhite = (color: string) => 1.05 / (luminance(color) + 0.05)

describe('agentColor', () => {
  it('gives an agent the same colour every time', () => {
    expect(agentColor('coding')).toBe(agentColor('coding'))
    expect(agentColor('coding')).toMatch(/^#[0-9a-f]{6}$/)
  })

  it('spreads agents over all seven colours', () => {
    expect(new Set(IDS.map(agentColor)).size).toBe(7)
  })
})

describe('agentTheme', () => {
  it('pairs each avatar colour with one chat colour', () => {
    const pairs = new Map<string, Set<string>>()
    for (const id of IDS) {
      const accents = pairs.get(agentColor(id)) ?? new Set()
      accents.add(agentTheme(id).accent)
      pairs.set(agentColor(id), accents)
    }
    expect(pairs.size).toBe(7)
    for (const accents of pairs.values()) expect(accents.size).toBe(1)
  })

  it('keeps white text readable on every chat colour (WCAG 4.5:1)', () => {
    const accents = new Set(IDS.map((id) => agentTheme(id).accent))
    expect(accents.size).toBe(7)
    for (const accent of accents) expect(contrastWithWhite(accent)).toBeGreaterThanOrEqual(4.5)
  })

  it('derives a darker strong tone and see-through soft and ring tones', () => {
    const t = agentTheme('research')
    expect(luminance(t.accentStrong)).toBeLessThan(luminance(t.accent))
    const [r, g, b] = rgb(t.accent)
    expect(t.accentSoft).toBe(`rgba(${r}, ${g}, ${b}, 0.14)`)
    expect(t.accentRing).toBe(`rgba(${r}, ${g}, ${b}, 0.45)`)
  })
})

describe('agentThemeStyle', () => {
  it('names the CSS custom properties the stylesheet reads', () => {
    const t = agentTheme('summarizer')
    expect(agentThemeStyle('summarizer')).toEqual({
      '--accent': t.accent,
      '--accent-strong': t.accentStrong,
      '--accent-soft': t.accentSoft,
      '--accent-ring': t.accentRing,
    })
  })
})
