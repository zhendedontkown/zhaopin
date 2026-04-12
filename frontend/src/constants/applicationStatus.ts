import type { ApplicationStatusCode } from '../types'

export type ApplicationStatusAudience = 'company' | 'jobseeker'

type ApplicationStatusMetaRecord = {
  value: ApplicationStatusCode
  label: string
  descriptions: Record<ApplicationStatusAudience, string>
  badgeClass: string
}

type ApplicationStatusOption = {
  value: ApplicationStatusCode
  label: string
}

type ApplicationStatusFilterOption = {
  value: ApplicationStatusCode | ''
  label: string
}

type ApplicationStatusMeta = {
  value: ApplicationStatusCode
  label: string
  description: string
  badgeClass: string
}

const STATUS_META_LIST: ApplicationStatusMetaRecord[] = [
  {
    value: 'SUBMITTED',
    label: '已投递',
    descriptions: {
      company: '等待企业查看',
      jobseeker: '等待企业查看',
    },
    badgeClass: 'status-badge--submitted',
  },
  {
    value: 'VIEWED',
    label: '企业已查看',
    descriptions: {
      company: '企业已查看该简历',
      jobseeker: '企业已查看你的简历，请留意后续通知',
    },
    badgeClass: 'status-badge--viewed',
  },
  {
    value: 'INTERVIEW_PENDING',
    label: '待面试确认',
    descriptions: {
      company: '已发出面试邀请，等待候选人确认',
      jobseeker: '企业已向你发出面试邀请，请尽快确认',
    },
    badgeClass: 'status-badge--interview-pending',
  },
  {
    value: 'INTERVIEWING',
    label: '面试中',
    descriptions: {
      company: '候选人已进入面试流程',
      jobseeker: '你已进入面试流程，请留意企业通知',
    },
    badgeClass: 'status-badge--interviewing',
  },
  {
    value: 'OFFERED',
    label: '已录用',
    descriptions: {
      company: '候选人已被录用',
      jobseeker: '恭喜，你已被录用',
    },
    badgeClass: 'status-badge--offered',
  },
  {
    value: 'REJECTED',
    label: '未通过',
    descriptions: {
      company: '该候选人未通过当前筛选',
      jobseeker: '很遗憾，本次申请未通过',
    },
    badgeClass: 'status-badge--rejected',
  },
]

const STATUS_META_MAP = Object.fromEntries(
  STATUS_META_LIST.map((item) => [item.value, item]),
) as Record<ApplicationStatusCode, ApplicationStatusMetaRecord>

const STATUS_TRANSITIONS: Record<ApplicationStatusCode, ApplicationStatusCode[]> = {
  SUBMITTED: ['VIEWED'],
  VIEWED: ['INTERVIEW_PENDING', 'REJECTED'],
  INTERVIEW_PENDING: ['VIEWED', 'INTERVIEWING'],
  INTERVIEWING: ['REJECTED', 'OFFERED'],
  OFFERED: [],
  REJECTED: [],
}

export const applicationStatusOptions: ApplicationStatusOption[] = STATUS_META_LIST.map(({ value, label }) => ({ value, label }))

export const applicationStatusFilterOptions: ApplicationStatusFilterOption[] = [
  { value: '', label: '全部' },
  ...applicationStatusOptions,
]

export function normalizeApplicationStatus(status?: string): ApplicationStatusCode {
  if (status === 'ACCEPTED') return 'OFFERED'
  if (status && status in STATUS_META_MAP) {
    return status as ApplicationStatusCode
  }
  return 'SUBMITTED'
}

export function getApplicationStatusMeta(
  status?: string,
  audience: ApplicationStatusAudience = 'jobseeker',
): ApplicationStatusMeta {
  const meta = STATUS_META_MAP[normalizeApplicationStatus(status)]
  return {
    value: meta.value,
    label: meta.label,
    description: meta.descriptions[audience],
    badgeClass: meta.badgeClass,
  }
}

export function getApplicationStatusLabel(status?: string) {
  return getApplicationStatusMeta(status).label
}

export function getApplicationStatusSummary(
  status?: string,
  audience: ApplicationStatusAudience = 'jobseeker',
  statusText?: string,
  statusDescription?: string,
) {
  const meta = getApplicationStatusMeta(status, audience)
  const label = statusText || meta.label
  const description = statusDescription || meta.description
  return `当前状态：${label}，${description}`
}

export function getAvailableApplicationStatusTransitions(status?: string) {
  return [...STATUS_TRANSITIONS[normalizeApplicationStatus(status)]]
}

export function canCompanyRejectApplication(status?: string) {
  const normalizedStatus = normalizeApplicationStatus(status)
  return normalizedStatus === 'VIEWED' || normalizedStatus === 'INTERVIEWING'
}

export function canCompanyInviteInterview(status?: string) {
  return normalizeApplicationStatus(status) === 'VIEWED'
}

export function canCompanyOfferApplication(status?: string) {
  return normalizeApplicationStatus(status) === 'INTERVIEWING'
}

export function canJobseekerRespondToInterview(status?: string) {
  return normalizeApplicationStatus(status) === 'INTERVIEW_PENDING'
}
