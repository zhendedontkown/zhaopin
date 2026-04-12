<script setup lang="ts">
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '../api/client'
import ResumeExportDialog from '../components/ResumeExportDialog.vue'
import ResumePreviewDocument from '../components/ResumePreviewDocument.vue'
import {
  applicationStatusFilterOptions,
  canCompanyInviteInterview,
  canCompanyOfferApplication,
  canCompanyRejectApplication,
  canJobseekerRespondToInterview,
  getApplicationStatusMeta,
  getApplicationStatusSummary,
  normalizeApplicationStatus,
  type ApplicationStatusAudience,
} from '../constants/applicationStatus'
import { useAuthStore } from '../stores/auth'
import type { ApplicationRecord, ApplicationResumeViewResponse, ApplicationStatusCode, PageResponse, ResumeDetail } from '../types'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const role = computed(() => authStore.role)
const isCompany = computed(() => role.value === 'COMPANY')
const statusAudience = computed<ApplicationStatusAudience>(() => (isCompany.value ? 'company' : 'jobseeker'))

const loading = ref(false)
const applications = ref<ApplicationRecord[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 8, total: 0 })
const activeStatusFilter = ref<ApplicationStatusCode | ''>('')
const statusActionLoading = reactive<Record<number, boolean>>({})
const resumePayloadCache = reactive<Record<string, ApplicationResumeViewResponse>>({})
const resumeDialogVisible = ref(false)
const resumeDialogLoading = ref(false)
const resumeDialogError = ref('')
const resumeDetail = ref<ResumeDetail | null>(null)
const resumeSource = ref<'SNAPSHOT' | 'CURRENT'>('SNAPSHOT')
const resumeJobTitle = ref('')
const resumeCompanyName = ref('')
const exportDialogVisible = ref(false)
const exportDialogLoading = ref(false)
const exportDialogError = ref('')
const exportResumeDetail = ref<ResumeDetail | null>(null)
const exportResumeSource = ref<'SNAPSHOT' | 'CURRENT'>('SNAPSHOT')
const exportResumeJobTitle = ref('')
const exportResumeCompanyName = ref('')
const highlightedApplicationId = ref<number | null>(null)
const focusHighlightTimer = ref<number | null>(null)

const showCurrentResumeNotice = computed(() => resumeDialogVisible.value && resumeSource.value === 'CURRENT')
const exportCurrentResumeNotice = computed(() =>
  exportDialogVisible.value && exportResumeSource.value === 'CURRENT' ? '当前展示的是最新简历' : '',
)

function normalizeApplicationRecord(record: ApplicationRecord): ApplicationRecord {
  const normalizedStatus = normalizeApplicationStatus(record.status)
  const meta = getApplicationStatusMeta(normalizedStatus, statusAudience.value)
  return {
    ...record,
    status: normalizedStatus,
    statusText: meta.label,
    statusDescription: meta.description,
  }
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ') : '--'
}

function statusBadgeClass(status: string) {
  return getApplicationStatusMeta(status, statusAudience.value).badgeClass
}

function statusSummary(record: ApplicationRecord) {
  return getApplicationStatusSummary(record.status, statusAudience.value, record.statusText, record.statusDescription)
}

function isStatusActionLoading(recordId: number) {
  return Boolean(statusActionLoading[recordId])
}

function extractRequestErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === 'string' && message.trim()) {
      return message
    }
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

function getFocusedApplicationId() {
  const raw = Array.isArray(route.query.focusApplicationId)
    ? route.query.focusApplicationId[0]
    : route.query.focusApplicationId
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

async function focusApplicationIfNeeded() {
  const focusApplicationId = getFocusedApplicationId()
  if (!focusApplicationId) return

  const target = document.getElementById(`application-card-${focusApplicationId}`)
  if (!target) return

  highlightedApplicationId.value = focusApplicationId
  target.scrollIntoView({ behavior: 'smooth', block: 'center' })

  if (focusHighlightTimer.value !== null) {
    window.clearTimeout(focusHighlightTimer.value)
  }
  focusHighlightTimer.value = window.setTimeout(() => {
    highlightedApplicationId.value = null
    focusHighlightTimer.value = null
  }, 2400)

  const nextQuery = { ...route.query }
  delete nextQuery.focusApplicationId
  await router.replace({ path: route.path, query: nextQuery })
}

async function fetchApplications() {
  if (role.value === 'ADMIN') {
    applications.value = []
    pagination.total = 0
    return
  }

  loading.value = true
  try {
    const response = await client.get(isCompany.value ? '/company/applications' : '/jobseeker/applications', {
      params: {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        status: activeStatusFilter.value || undefined,
      },
    })
    const page = response.data as PageResponse<ApplicationRecord>
    applications.value = page.records.map(normalizeApplicationRecord)
    pagination.total = page.total
    await nextTick()
    await focusApplicationIfNeeded()
  } finally {
    loading.value = false
  }
}

async function submitCompanyStatusUpdate(record: ApplicationRecord, targetStatus: ApplicationStatusCode, successMessage: string) {
  statusActionLoading[record.id] = true
  try {
    await client.patch(`/company/applications/${record.id}/status`, {
      status: targetStatus,
    })
    ElMessage.success(successMessage)
    await fetchApplications()
  } catch (error) {
    ElMessage.error(extractRequestErrorMessage(error, '状态更新失败，请稍后重试'))
  } finally {
    statusActionLoading[record.id] = false
  }
}

async function submitInterviewResponse(record: ApplicationRecord, decision: 'ACCEPT' | 'REJECT', successMessage: string) {
  statusActionLoading[record.id] = true
  try {
    await client.patch(`/jobseeker/applications/${record.id}/interview-response`, {
      decision,
    })
    ElMessage.success(successMessage)
    await fetchApplications()
  } catch (error) {
    ElMessage.error(extractRequestErrorMessage(error, '面试确认失败，请稍后重试'))
  } finally {
    statusActionLoading[record.id] = false
  }
}

function handleFilterChange(value: ApplicationStatusCode | '') {
  activeStatusFilter.value = value
  pagination.pageNum = 1
  void fetchApplications()
}

function enterChat(record: ApplicationRecord) {
  if (role.value === 'ADMIN') return
  const peerUserId = isCompany.value ? record.jobseekerUserId : record.companyUserId
  router.push({ path: '/chat', query: { peerUserId } })
}

async function loadApplicationResume(record: ApplicationRecord, markViewed: boolean) {
  const cacheKey = `${record.id}:${markViewed ? 'view' : 'export'}`
  const cachedPayload = resumePayloadCache[cacheKey]
  if (cachedPayload) {
    return cachedPayload
  }

  const response = await client.get(`/company/applications/${record.id}/resume`, {
    params: { markViewed },
  })
  const payload = response.data as ApplicationResumeViewResponse
  resumePayloadCache[cacheKey] = payload
  return payload
}

function applyResumeDialogPayload(payload: ApplicationResumeViewResponse, record: ApplicationRecord) {
  resumeDetail.value = payload.resumeDetail
  resumeSource.value = payload.resumeSource
  resumeJobTitle.value = payload.jobTitle || record.jobTitle
  resumeCompanyName.value = payload.companyName || record.companyName
}

function applyExportDialogPayload(payload: ApplicationResumeViewResponse, record: ApplicationRecord) {
  exportResumeDetail.value = payload.resumeDetail
  exportResumeSource.value = payload.resumeSource
  exportResumeJobTitle.value = payload.jobTitle || record.jobTitle
  exportResumeCompanyName.value = payload.companyName || record.companyName
}

function resetResumeDialogState() {
  resumeDialogLoading.value = false
  resumeDialogError.value = ''
  resumeDetail.value = null
  resumeSource.value = 'SNAPSHOT'
  resumeJobTitle.value = ''
  resumeCompanyName.value = ''
}

function resetExportDialogState() {
  exportDialogLoading.value = false
  exportDialogError.value = ''
  exportResumeDetail.value = null
  exportResumeSource.value = 'SNAPSHOT'
  exportResumeJobTitle.value = ''
  exportResumeCompanyName.value = ''
}

async function openApplicationResumeView(record: ApplicationRecord) {
  resumeDialogVisible.value = true
  resumeDialogLoading.value = true
  resumeDialogError.value = ''
  resumeDetail.value = null
  resumeJobTitle.value = record.jobTitle
  resumeCompanyName.value = record.companyName

  try {
    const payload = await loadApplicationResume(record, true)
    applyResumeDialogPayload(payload, record)
    try {
      await fetchApplications()
    } catch {
      ElMessage.warning('简历已打开，但状态刷新失败，请稍后手动刷新列表')
    }
  } catch {
    resumeDialogError.value = '简历加载失败，请稍后重试。'
  } finally {
    resumeDialogLoading.value = false
  }
}

async function openApplicationResumeExport(record: ApplicationRecord) {
  exportDialogVisible.value = true
  exportDialogLoading.value = true
  exportDialogError.value = ''
  exportResumeDetail.value = null
  exportResumeJobTitle.value = record.jobTitle
  exportResumeCompanyName.value = record.companyName

  try {
    const payload = await loadApplicationResume(record, false)
    applyExportDialogPayload(payload, record)
  } catch {
    exportDialogError.value = '简历加载失败，请稍后重试。'
  } finally {
    exportDialogLoading.value = false
  }
}

function isConfirmCancelled(error: unknown) {
  return error === 'cancel' || error === 'close'
}

async function confirmAction(
  title: string,
  message: string,
  type: 'warning' | 'info' | 'success',
  action: () => Promise<void>,
) {
  try {
    await ElMessageBox.confirm(message, title, {
      type,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
  } catch (error) {
    if (isConfirmCancelled(error)) {
      return
    }
    throw error
  }

  await action()
}

async function rejectApplication(record: ApplicationRecord) {
  await confirmAction(
    '拒绝候选人',
    '确认将该候选人的状态更新为“未通过”吗？',
    'warning',
    () => submitCompanyStatusUpdate(record, 'REJECTED', '候选人状态已更新为未通过'),
  )
}

async function inviteInterview(record: ApplicationRecord) {
  await confirmAction(
    '邀请面试',
    '确认将该候选人的状态更新为“待面试确认”吗？',
    'info',
    () => submitCompanyStatusUpdate(record, 'INTERVIEW_PENDING', '已向候选人发出面试邀请'),
  )
}

async function offerApplication(record: ApplicationRecord) {
  await confirmAction(
    '录用候选人',
    '确认将该候选人的状态更新为“已录用”吗？',
    'success',
    () => submitCompanyStatusUpdate(record, 'OFFERED', '候选人状态已更新为已录用'),
  )
}

async function acceptInterview(record: ApplicationRecord) {
  await confirmAction(
    '接受面试邀请',
    '确认接受该岗位的面试邀请吗？',
    'success',
    () => submitInterviewResponse(record, 'ACCEPT', '已接受面试邀请'),
  )
}

async function declineInterview(record: ApplicationRecord) {
  await confirmAction(
    '拒绝面试邀请',
    '确认暂不接受该岗位的面试邀请吗？',
    'warning',
    () => submitInterviewResponse(record, 'REJECT', '已拒绝面试邀请'),
  )
}

function footerText(record: ApplicationRecord) {
  if (isCompany.value) {
    return record.status === 'INTERVIEW_PENDING' ? '等待候选人确认面试邀请' : '继续推进候选人流程'
  }
  return canJobseekerRespondToInterview(record.status)
    ? '请尽快确认是否参加本次面试'
    : '如有需要，可直接进入沟通'
}

watch(
  () => route.query.focusApplicationId,
  (nextFocus, previousFocus) => {
    if (!nextFocus || nextFocus === previousFocus) return
    activeStatusFilter.value = ''
    pagination.pageNum = 1
    void fetchApplications()
  },
)

onMounted(() => {
  if (getFocusedApplicationId()) {
    activeStatusFilter.value = ''
    pagination.pageNum = 1
  }
  void fetchApplications()
})
</script>

<template>
  <div class="section-grid" v-loading="loading">
    <section class="page-hero surface-card">
      <div class="page-hero__content">
        <span class="eyebrow">{{ isCompany ? '投递流程' : '我的投递' }}</span>
        <h2 class="page-title">{{ isCompany ? '投递处理中心' : '我的投递记录' }}</h2>
      </div>

      <div class="page-hero__aside">
        <div class="hero-stat">
          <span>{{ isCompany ? '处理总数' : '投递总数' }}</span>
          <strong>{{ pagination.total }}</strong>
        </div>
      </div>
    </section>

    <section class="surface-card filter-panel">
      <div class="filter-panel__header">
        <div>
          <span class="eyebrow">状态筛选</span>
          <h3>{{ isCompany ? '按招聘流程筛选投递' : '按投递状态筛选记录' }}</h3>
        </div>
        <span class="result-note">共 {{ pagination.total }} 条投递记录</span>
      </div>

      <div class="filter-chip-group">
        <button
          v-for="item in applicationStatusFilterOptions"
          :key="item.value || 'ALL'"
          type="button"
          class="filter-chip"
          :class="{ 'is-active': activeStatusFilter === item.value }"
          @click="handleFilterChange(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="application-grid">
      <article
        v-for="record in applications"
        :id="`application-card-${record.id}`"
        :key="record.id"
        class="surface-card application-card"
        :class="{ 'application-card--focused': highlightedApplicationId === record.id }"
      >
        <div class="application-head">
          <div class="application-title">
            <h3>{{ record.jobTitle }}</h3>
            <p>{{ record.companyName }}</p>
          </div>
          <span class="status-badge" :class="statusBadgeClass(record.status)">{{ record.statusText }}</span>
        </div>

        <div class="application-meta">
          <div class="application-meta__item">
            <span>投递时间</span>
            <strong>{{ formatDateTime(record.appliedAt) }}</strong>
          </div>
          <div class="application-meta__item">
            <span>状态更新时间</span>
            <strong>{{ formatDateTime(record.statusUpdatedAt || record.viewedAt || record.appliedAt) }}</strong>
          </div>
          <div class="application-meta__item">
            <span>简历编号</span>
            <strong>{{ record.resumeId }}</strong>
          </div>
          <div class="application-meta__item">
            <span>{{ isCompany ? '候选人账号 ID' : '企业账号 ID' }}</span>
            <strong>{{ isCompany ? record.jobseekerUserId : record.companyUserId }}</strong>
          </div>
        </div>

        <div class="status-summary">
          <span class="status-summary__label">当前状态说明</span>
          <strong>{{ statusSummary(record) }}</strong>
          <p v-if="record.statusRemark">{{ record.statusRemark }}</p>
        </div>

        <div v-if="isCompany" class="resume-actions">
          <el-button plain @click="openApplicationResumeView(record)">查看简历</el-button>
          <el-button
            v-if="canCompanyRejectApplication(record.status)"
            plain
            type="danger"
            :loading="isStatusActionLoading(record.id)"
            @click="rejectApplication(record)"
          >
            拒绝
          </el-button>
          <el-button
            v-if="canCompanyInviteInterview(record.status)"
            plain
            type="warning"
            :loading="isStatusActionLoading(record.id)"
            @click="inviteInterview(record)"
          >
            邀请面试
          </el-button>
          <el-button
            v-if="canCompanyOfferApplication(record.status)"
            plain
            type="success"
            :loading="isStatusActionLoading(record.id)"
            @click="offerApplication(record)"
          >
            录用
          </el-button>
          <el-button plain @click="openApplicationResumeExport(record)">导出 PDF</el-button>
        </div>

        <div v-else-if="canJobseekerRespondToInterview(record.status)" class="jobseeker-actions">
          <el-button
            type="success"
            plain
            :loading="isStatusActionLoading(record.id)"
            @click="acceptInterview(record)"
          >
            接受面试
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="isStatusActionLoading(record.id)"
            @click="declineInterview(record)"
          >
            拒绝面试
          </el-button>
        </div>

        <div class="footer-row">
          <span>{{ footerText(record) }}</span>
          <el-button text type="primary" @click="enterChat(record)">进入沟通</el-button>
        </div>
      </article>

      <el-empty v-if="!applications.length" description="当前还没有符合筛选条件的投递记录" />
    </section>

    <el-pagination
      background
      layout="prev, pager, next"
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @current-change="(page: number) => { pagination.pageNum = page; fetchApplications() }"
    />

    <el-dialog
      v-model="resumeDialogVisible"
      class="resume-view-dialog"
      title="查看简历"
      width="920px"
      align-center
      destroy-on-close
      @closed="resetResumeDialogState"
    >
      <div v-loading="resumeDialogLoading" class="resume-view-body">
        <template v-if="resumeDetail">
          <div class="resume-view-head">
            <div>
              <h3>{{ resumeJobTitle || '投递简历' }}</h3>
              <p v-if="resumeCompanyName">{{ resumeCompanyName }}</p>
            </div>
          </div>

          <el-alert
            v-if="showCurrentResumeNotice"
            class="resume-view-alert"
            type="warning"
            :closable="false"
            show-icon
            title="当前展示的是最新简历"
          />

          <div class="resume-view-stage">
            <ResumePreviewDocument :detail="resumeDetail" mode="export" />
          </div>
        </template>

        <el-result
          v-else-if="resumeDialogError"
          icon="error"
          title="无法加载简历"
          :sub-title="resumeDialogError"
        />
      </div>
    </el-dialog>

    <ResumeExportDialog
      v-model="exportDialogVisible"
      :loading="exportDialogLoading"
      :error="exportDialogError"
      :detail="exportResumeDetail"
      :current-notice="exportCurrentResumeNotice"
      title="导出简历 PDF"
      :subtitle="exportResumeCompanyName ? `${exportResumeJobTitle || '投递简历'} / ${exportResumeCompanyName}` : exportResumeJobTitle"
      filename="简历.pdf"
      @closed="resetExportDialogState"
    />
  </div>
</template>

<style scoped>
.filter-panel,
.application-card {
  padding: 22px;
}

.filter-panel {
  display: grid;
  gap: 16px;
}

.filter-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.filter-panel__header h3 {
  margin: 8px 0 0;
  font-size: 18px;
}

.filter-chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-chip {
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(248, 250, 252, 0.92);
  color: var(--text-main);
  border-radius: 999px;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 180ms ease;
}

.filter-chip:hover {
  border-color: rgba(3, 105, 161, 0.28);
  color: var(--primary-deep);
}

.filter-chip.is-active {
  background: linear-gradient(135deg, rgba(3, 105, 161, 0.14), rgba(14, 165, 233, 0.16));
  border-color: rgba(3, 105, 161, 0.28);
  color: var(--primary-deep);
  box-shadow: 0 10px 24px rgba(3, 105, 161, 0.08);
}

.result-note {
  color: var(--text-muted);
  font-size: 13px;
}

.application-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.application-card {
  display: grid;
  gap: 18px;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.application-card--focused {
  border-color: rgba(14, 165, 233, 0.42);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.14), 0 18px 36px rgba(3, 105, 161, 0.12);
}

.resume-actions,
.jobseeker-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.application-card:hover {
  transform: translateY(-2px);
  border-color: rgba(3, 105, 161, 0.16);
  box-shadow: 0 16px 32px rgba(3, 105, 161, 0.08);
}

.application-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.application-title h3 {
  margin: 0;
}

.application-title p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.status-badge--submitted {
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.status-badge--viewed {
  background: rgba(8, 145, 178, 0.14);
  color: #0f766e;
}

.status-badge--interview-pending {
  background: rgba(245, 158, 11, 0.15);
  color: #b45309;
}

.status-badge--rejected {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.status-badge--interviewing {
  background: rgba(249, 115, 22, 0.14);
  color: #c2410c;
}

.status-badge--offered {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.application-meta {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.application-meta__item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.application-meta__item span,
.status-summary__label,
.footer-row {
  color: var(--text-muted);
}

.application-meta__item strong {
  color: var(--text-strong);
  font-size: 14px;
}

.status-summary {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(239, 246, 255, 0.86));
  border: 1px solid rgba(14, 165, 233, 0.12);
}

.status-summary strong {
  color: var(--text-strong);
  line-height: 1.6;
}

.status-summary p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.footer-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  margin-top: 4px;
}

.resume-view-body {
  min-height: 280px;
}

.resume-view-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.resume-view-head h3 {
  margin: 0;
  font-size: 20px;
  color: var(--text-strong);
}

.resume-view-head p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.resume-view-alert {
  margin-bottom: 16px;
}

.resume-view-stage {
  display: grid;
  justify-items: center;
  padding: 4px 0 8px;
}

:deep(.resume-view-dialog .el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

:deep(.resume-view-dialog .el-dialog__body) {
  padding: 20px 24px 28px;
  background: #f8fafc;
}

@media (max-width: 900px) {
  .filter-panel__header,
  .resume-actions,
  .jobseeker-actions,
  .footer-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .application-meta {
    grid-template-columns: 1fr;
  }
}
</style>
