export function formatDateTime(value?: string | null) {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 19)
}

export function formatSalaryRange(min?: number | null, max?: number | null) {
  if (min == null || max == null) return '--'
  return `${min} - ${max}`
}

export function userStatusLabel(status?: string) {
  if (status === 'ACTIVE') return '正常'
  if (status === 'DISABLED') return '已禁用'
  if (status === 'DELETED') return '已删除'
  return status || '未知'
}

export function userStatusType(status?: string) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED') return 'warning'
  if (status === 'DELETED') return 'danger'
  return 'info'
}

export function auditStatusLabel(status?: string) {
  if (status === 'APPROVED') return '审核通过'
  if (status === 'REJECTED') return '审核驳回'
  if (status === 'PENDING') return '待审核'
  return status || '未知'
}

export function auditStatusType(status?: string) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

export function jobStatusLabel(status?: string) {
  if (status === 'PUBLISHED') return '招聘中'
  if (status === 'OFFLINE') return '已下线'
  if (status === 'EXPIRED') return '已过期'
  if (status === 'DRAFT') return '草稿'
  return status || '未知'
}

export function jobStatusType(status?: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'OFFLINE') return 'warning'
  if (status === 'EXPIRED') return 'info'
  if (status === 'DRAFT') return ''
  return 'info'
}

export function applicationStatusType(status?: string) {
  if (status === 'INTERVIEWING') return 'warning'
  if (status === 'OFFERED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'INTERVIEW_PENDING') return 'primary'
  if (status === 'VIEWED') return 'info'
  return ''
}

export function applicationStatusLabel(status?: string) {
  if (status === 'SUBMITTED') return '已投递'
  if (status === 'VIEWED') return '已查看'
  if (status === 'INTERVIEW_PENDING') return '待确认面试'
  if (status === 'INTERVIEWING') return '面试中'
  if (status === 'OFFERED') return '已录用'
  if (status === 'REJECTED') return '未通过'
  return status || '未知'
}

export function toDateTimeBoundary(value: Date, boundary: 'start' | 'end') {
  const year = value.getFullYear()
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  const time = boundary === 'start' ? '00:00:00' : '23:59:59'
  return `${year}-${month}-${day}T${time}`
}
