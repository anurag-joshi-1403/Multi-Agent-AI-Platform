import { afterEach, describe, expect, it, vi } from 'vitest'
import { formatMs, formatRelative, isRecord, titleCase, truncate, uid } from './util'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('formatRelative', () => {
  const now = Date.UTC(2026, 9, 11, 12, 0, 0)
  const ago = (ms: number) => formatRelative(now - ms, now)
  const SECOND = 1000
  const MINUTE = 60 * SECOND
  const HOUR = 60 * MINUTE
  const DAY = 24 * HOUR

  it('says "just now" for the first minute', () => {
    expect(ago(0)).toBe('just now')
    expect(ago(30 * SECOND)).toBe('just now')
    expect(ago(59 * SECOND)).toBe('just now')
  })

  it('counts minutes, hours and days', () => {
    expect(ago(MINUTE)).toBe('1m ago')
    expect(ago(59 * MINUTE)).toBe('59m ago')
    expect(ago(HOUR)).toBe('1h ago')
    expect(ago(23 * HOUR)).toBe('23h ago')
    expect(ago(DAY)).toBe('1d ago')
    expect(ago(6 * DAY)).toBe('6d ago')
  })

  it('shows the date from a week on', () => {
    expect(ago(7 * DAY)).toBe(new Date(now - 7 * DAY).toLocaleDateString())
  })

  it('treats a time in the future (clock skew) as just now', () => {
    expect(ago(-5 * MINUTE)).toBe('just now')
  })
})

describe('formatMs', () => {
  it('shows whole milliseconds under a second', () => {
    expect(formatMs(0)).toBe('0 ms')
    expect(formatMs(12.4)).toBe('12 ms')
    expect(formatMs(999.4)).toBe('999 ms')
  })

  it('switches to seconds when it rounds to a full second', () => {
    expect(formatMs(999.6)).toBe('1.00 s')
    expect(formatMs(1000)).toBe('1.00 s')
    expect(formatMs(1234)).toBe('1.23 s')
    expect(formatMs(65_000)).toBe('65.00 s')
  })
})

describe('truncate', () => {
  it('collapses whitespace and leaves short text alone', () => {
    expect(truncate('hello')).toBe('hello')
    expect(truncate('  a \n\t b  ')).toBe('a b')
  })

  it('cuts long text to the limit, ellipsis included', () => {
    const exact = 'x'.repeat(48)
    expect(truncate(exact)).toBe(exact)
    const long = truncate('y'.repeat(49))
    expect(long).toHaveLength(48)
    expect(long.endsWith('…')).toBe(true)
    expect(truncate('abcdefgh', 5)).toBe('abcd…')
  })
})

describe('isRecord', () => {
  it('accepts plain objects only', () => {
    expect(isRecord({})).toBe(true)
    expect(isRecord({ a: 1 })).toBe(true)
    expect(isRecord([])).toBe(false)
    expect(isRecord(null)).toBe(false)
    expect(isRecord(undefined)).toBe(false)
    expect(isRecord('text')).toBe(false)
    expect(isRecord(42)).toBe(false)
  })
})

describe('titleCase', () => {
  it('turns ids into words', () => {
    expect(titleCase('general')).toBe('General')
    expect(titleCase('code-review')).toBe('Code Review')
    expect(titleCase('snake_case id')).toBe('Snake Case Id')
    expect(titleCase('--a--b')).toBe('A B')
    expect(titleCase('')).toBe('')
  })
})

describe('uid', () => {
  it('returns a new id every call', () => {
    const ids = new Set(Array.from({ length: 100 }, () => uid()))
    expect(ids.size).toBe(100)
  })

  it('still works without crypto.randomUUID (older browsers, plain http)', () => {
    vi.stubGlobal('crypto', {})
    const a = uid()
    const b = uid()
    expect(a).toMatch(/^[a-z0-9]+$/)
    expect(a).not.toBe(b)
  })
})
