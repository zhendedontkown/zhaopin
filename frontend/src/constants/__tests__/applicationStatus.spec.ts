import { describe, expect, it } from 'vitest'
import {
  getApplicationStatusMeta,
  getApplicationStatusSummary,
  getAvailableApplicationStatusTransitions,
  normalizeApplicationStatus,
} from '../applicationStatus'

describe('applicationStatus metadata', () => {
  it('maps legacy accepted status to offered', () => {
    expect(normalizeApplicationStatus('ACCEPTED')).toBe('OFFERED')
  })

  it('returns role-specific descriptions', () => {
    expect(getApplicationStatusMeta('VIEWED', 'company').description).toBe('企业已查看该简历')
    expect(getApplicationStatusMeta('VIEWED', 'jobseeker').description).toBe('企业已查看你的简历，请留意后续通知')
    expect(getApplicationStatusMeta('INTERVIEW_PENDING', 'jobseeker').description).toBe('企业已向你发出面试邀请，请尽快确认')
  })

  it('returns allowed next transitions for the company workflow', () => {
    expect(getAvailableApplicationStatusTransitions('SUBMITTED')).toEqual(['VIEWED'])
    expect(getAvailableApplicationStatusTransitions('VIEWED')).toEqual(['INTERVIEW_PENDING', 'REJECTED'])
    expect(getAvailableApplicationStatusTransitions('INTERVIEW_PENDING')).toEqual(['VIEWED', 'INTERVIEWING'])
    expect(getAvailableApplicationStatusTransitions('INTERVIEWING')).toEqual(['REJECTED', 'OFFERED'])
    expect(getAvailableApplicationStatusTransitions('REJECTED')).toEqual([])
    expect(getAvailableApplicationStatusTransitions('OFFERED')).toEqual([])
  })

  it('builds readable status summaries', () => {
    expect(getApplicationStatusSummary('OFFERED', 'jobseeker')).toBe('当前状态：已录用，恭喜，你已被录用')
  })
})
