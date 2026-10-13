import { afterEach, describe, expect, it, vi } from 'vitest'
import { blockedStorage, memoryStorage } from '../test/fakes'
import { loadJson, loadString, saveJson, saveString } from './storage'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('storage helpers', () => {
  it('store and read back JSON and strings', () => {
    vi.stubGlobal('localStorage', memoryStorage())
    saveJson('k', { a: [1, 2], b: 'x' })
    expect(loadJson('k')).toEqual({ a: [1, 2], b: 'x' })
    saveString('s', 'value')
    expect(loadString('s')).toBe('value')
  })

  it('remove a string saved as null', () => {
    const storage = memoryStorage()
    vi.stubGlobal('localStorage', storage)
    saveString('s', 'value')
    saveString('s', null)
    expect(loadString('s')).toBeNull()
    expect(storage.length).toBe(0)
  })

  it('read missing or hand-edited JSON as undefined', () => {
    const storage = memoryStorage()
    vi.stubGlobal('localStorage', storage)
    expect(loadJson('missing')).toBeUndefined()
    storage.setItem('broken', '{not json')
    expect(loadJson('broken')).toBeUndefined()
  })

  it('never throw when the browser blocks storage', () => {
    vi.stubGlobal('localStorage', blockedStorage())
    expect(loadString('s')).toBeNull()
    expect(loadJson('k')).toBeUndefined()
    expect(() => saveString('s', 'value')).not.toThrow()
    expect(() => saveString('s', null)).not.toThrow()
    expect(() => saveJson('k', { a: 1 })).not.toThrow()
  })

  it('never throw when storage is full', () => {
    const storage = memoryStorage()
    storage.setItem = () => {
      throw new DOMException('Quota exceeded', 'QuotaExceededError')
    }
    vi.stubGlobal('localStorage', storage)
    expect(() => saveJson('k', { big: 'x'.repeat(1000) })).not.toThrow()
    expect(loadJson('k')).toBeUndefined()
  })
})
