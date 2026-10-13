import { describe, expect, it } from 'vitest'
import type { AgentParameter } from '../types'
import { buildAttributes } from './attributes'

const param = (name: string, type: AgentParameter['type']): AgentParameter => ({
  name,
  label: name,
  description: '',
  type,
  required: false,
  options: [],
  defaultValue: null,
})

const PARAMS = [param('language', 'SELECT'), param('maxWords', 'NUMBER'), param('style', 'STRING')]

describe('buildAttributes', () => {
  it('keeps typed values, trimmed, and turns NUMBER fields into numbers', () => {
    expect(buildAttributes(PARAMS, { language: ' java ', maxWords: ' 150 ', style: 'bullets' })).toEqual({
      language: 'java',
      maxWords: 150,
      style: 'bullets',
    })
  })

  it('leaves out empty and blank fields', () => {
    expect(buildAttributes(PARAMS, { language: '', maxWords: '   ' })).toEqual({})
  })

  it('keeps a zero and decimals, drops a NUMBER that is not a number', () => {
    expect(buildAttributes(PARAMS, { maxWords: '0' })).toEqual({ maxWords: 0 })
    expect(buildAttributes(PARAMS, { maxWords: '2.5' })).toEqual({ maxWords: 2.5 })
    expect(buildAttributes(PARAMS, { maxWords: 'lots' })).toEqual({})
  })

  it('ignores values for options the agent does not have', () => {
    expect(buildAttributes(PARAMS, { secret: 'x', style: 'tldr' })).toEqual({ style: 'tldr' })
  })
})
