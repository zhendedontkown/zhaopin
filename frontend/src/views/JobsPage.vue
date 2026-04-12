<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { chinaCityOptions, type ChinaCityOption } from '../data/china-city-options'
import { presetJobBenefitTags } from '../data/job-benefit-tags'
import { presetJobSkillTags } from '../data/job-skill-tags'
import { useAuthStore } from '../stores/auth'
import type { ApplicationRecord, ApplyJobRequest, JobRecord, PageResponse, SavedResumeSummary } from '../types'

type JobDiscoverySortKey = 'default' | 'latest' | 'salary'

interface JobDiscoverySortOption {
  key: JobDiscoverySortKey
  label: string
}

interface DiscoveryEmptyState {
  title: string
  description: string
  actionLabel?: string
}

const DEFAULT_DISCOVERY_SORT_KEY: JobDiscoverySortKey = 'default'
const JOB_BENEFIT_TAG_LIMIT = 3
const JOB_SKILL_TAG_LIMIT = 3
const experienceOptions = ['不限', '1年', '2年', '3年', '5年', '8年']
const educationOptions = ['不限', '大专', '本科', '硕士', '博士']
const jobDiscoverySortOptions: JobDiscoverySortOption[] = [
  {
    key: 'default',
    label: '默认排序',
  },
  {
    key: 'latest',
    label: '最新优先',
  },
  {
    key: 'salary',
    label: '薪资优先',
  },
]
const jobDiscoverySortOptionMap = Object.fromEntries(
  jobDiscoverySortOptions.map((option) => [option.key, option]),
) as Record<JobDiscoverySortKey, JobDiscoverySortOption>

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const role = computed(() => authStore.role)
const loading = ref(false)
const dialogVisible = ref(false)
const editingJobId = ref<number | null>(null)

const jobDetailVisible = ref(false)
const jobDetailLoading = ref(false)
const favoriteSubmitting = ref(false)
const selectedJobId = ref<number | null>(null)
const selectedJobDetail = ref<JobRecord | null>(null)
const applyResumeDialogVisible = ref(false)
const applyResumeLoading = ref(false)
const applyResumeSubmitting = ref(false)
const applyResumeError = ref('')
const applyTargetJob = ref<JobRecord | null>(null)
const applySavedResumes = ref<SavedResumeSummary[]>([])
const selectedSavedResumeId = ref<number | null>(null)
const selectedLocationPath = ref<string[]>([])

const BROWSE_JOBS_DEBOUNCE_MS = 300
const SEARCH_RESULT_PAGE_SIZE = 1000
const searchPagination = reactive({ pageNum: 1, pageSize: SEARCH_RESULT_PAGE_SIZE, total: 0 })
const companyPagination = reactive({ pageNum: 1, pageSize: 8, total: 0 })

let jobsFetchTimer: ReturnType<typeof setTimeout> | undefined
let jobsRequestId = 0
let suspendAutoLoadJobs = false

const defaultSearchForm = {
  keyword: '',
  category: '',
  location: '',
  salaryMin: undefined as number | undefined,
  salaryMax: undefined as number | undefined,
  experienceRequirement: '',
  educationRequirement: '',
  benefitTags: [] as string[],
  sortKey: DEFAULT_DISCOVERY_SORT_KEY as JobDiscoverySortKey,
}

const searchForm = reactive({
  ...defaultSearchForm,
  benefitTags: [] as string[],
})

const companyFilter = reactive({
  status: '',
})

const companyJobs = ref<JobRecord[]>([])
const discoveryJobs = ref<JobRecord[]>([])
const recentAppliedJobIds = ref<number[]>([])
const duplicateApplyMessage = '同一岗位 7 天内仅允许投递一次'
const selectedApplyResume = computed(() => applySavedResumes.value.find((item) => item.id === selectedSavedResumeId.value) ?? null)

const companyForm = reactive({
  title: '',
  category: '',
  location: '',
  salaryMin: 10000,
  salaryMax: 15000,
  experienceRequirement: '',
  educationRequirement: '',
  headcount: 1,
  description: '',
  skillTags: [] as string[],
  benefitTags: [] as string[],
  expireAt: '',
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' },
  { label: '已过期', value: 'EXPIRED' },
]

const selectedJobApplied = computed(() => {
  if (!selectedJobDetail.value) return false
  return isRecentlyApplied(selectedJobDetail.value)
})

const selectedJobFavorited = computed(() => Boolean(selectedJobDetail.value?.favorited))

const favoriteTooltipText = computed(() => {
  if (favoriteSubmitting.value) return '正在更新收藏状态'
  return selectedJobFavorited.value ? '取消收藏' : '收藏岗位'
})

const companyBenefitPreviewTags = computed(() => normalizeBenefitTags(companyForm.benefitTags))
const companySkillPreviewTags = computed(() => normalizeTagList(companyForm.skillTags))

const selectedLocationLabelPath = computed(() => resolveLocationLabels(selectedLocationPath.value))

const hasActiveSearchFilters = computed(() => Boolean(
  searchForm.category.trim()
  || selectedLocationPath.value.length
  || searchForm.salaryMin !== undefined
  || searchForm.salaryMax !== undefined
  || searchForm.experienceRequirement.trim()
  || searchForm.educationRequirement.trim()
  || searchForm.benefitTags.length,
))

const hasKeyword = computed(() => Boolean(searchForm.keyword.trim()))
const isDefaultListMode = computed(() => !hasKeyword.value && hasActiveSearchFilters.value === false)
const discoveryModeLabel = computed(() => {
  if (hasKeyword.value) return '关键词搜索'
  if (hasActiveSearchFilters.value) return '条件筛选'
  return '默认列表'
})

const discoveryPanelTitle = computed(() => '岗位列表')

const searchSummaryItems = computed(() => {
  const items: string[] = []

  if (searchForm.keyword.trim()) {
    items.push(`关键词：${searchForm.keyword.trim()}`)
  }

  if (searchForm.category.trim()) {
    items.push(`类别：${searchForm.category.trim()}`)
  }

  if (selectedLocationLabelPath.value.length) {
    items.push(`地点：${selectedLocationLabelPath.value.join(' / ')}`)
  }

  const salarySummary = formatSearchSalarySummary()
  if (salarySummary) {
    items.push(`薪资：${salarySummary}`)
  }

  if (searchForm.experienceRequirement.trim()) {
    items.push(`经验：${searchForm.experienceRequirement.trim()}`)
  }

  if (searchForm.educationRequirement.trim()) {
    items.push(`学历：${searchForm.educationRequirement.trim()}`)
  }

  if (searchForm.benefitTags.length) {
    items.push(`福利：${searchForm.benefitTags.join('、')}`)
  }

  if (searchForm.sortKey !== defaultSearchForm.sortKey) {
    items.push(`排序：${getSortLabel(searchForm.sortKey)}`)
  }

  return items
})

const emptyStateModel = computed<DiscoveryEmptyState>(() => {
  if (isDefaultListMode.value) {
    return {
      title: '当前暂无岗位',
      description: '当前没有可展示的已发布岗位，请稍后再来看看。',
    }
  }
  return {
    title: '暂无符合条件的岗位',
    description: '可以尝试更换关键词、放宽筛选范围或清空筛选重新查看岗位。',
    actionLabel: '清空筛选',
  }
})

function statusLabel(status: string) {
  return statusOptions.find((item) => item.value === status)?.label ?? status
}

function statusTagType(status: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'OFFLINE') return 'warning'
  if (status === 'EXPIRED') return 'info'
  return ''
}

function formatDateTime(value?: string) {
  if (!value) return '未设置'
  const normalized = value.replace('T', ' ')
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return normalized
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function getSortLabel(sortKey: JobDiscoverySortKey) {
  return jobDiscoverySortOptionMap[sortKey]?.label ?? jobDiscoverySortOptionMap[DEFAULT_DISCOVERY_SORT_KEY].label
}

function formatSearchSalarySummary() {
  if (searchForm.salaryMin !== undefined && searchForm.salaryMax !== undefined) {
    return `${searchForm.salaryMin} - ${searchForm.salaryMax} 元/月`
  }
  if (searchForm.salaryMin !== undefined) {
    return `${searchForm.salaryMin} 元/月以上`
  }
  if (searchForm.salaryMax !== undefined) {
    return `${searchForm.salaryMax} 元/月以下`
  }
  return ''
}

function normalizeTagList(tags?: string[]) {
  if (!Array.isArray(tags)) return []

  const deduped = new Set<string>()
  for (const rawTag of tags) {
    const tag = rawTag?.trim()
    if (!tag) continue
    deduped.add(tag)
  }

  return [...deduped]
}

function normalizeBenefitTags(tags?: string[]) {
  return normalizeTagList(tags)
}

function getJobSkillTags(job: JobRecord) {
  return normalizeTagList(job.skillTags).slice(0, JOB_SKILL_TAG_LIMIT)
}

function getJobBenefitTags(job: JobRecord) {
  return normalizeBenefitTags(job.benefitTags).slice(0, JOB_BENEFIT_TAG_LIMIT)
}

function resolveLocationLabels(pathValues: string[]) {
  const labels: string[] = []
  let currentOptions: ChinaCityOption[] = chinaCityOptions

  for (const value of pathValues) {
    const matched = currentOptions.find((option) => option.value === value)
    if (!matched) {
      labels.push(value)
      break
    }

    labels.push(matched.label)
    currentOptions = matched.children ?? []
  }

  return labels
}

function resolveLocationOption(pathValues: string[]) {
  let currentOptions: ChinaCityOption[] = chinaCityOptions
  let matchedOption: ChinaCityOption | undefined

  for (const value of pathValues) {
    matchedOption = currentOptions.find((option) => option.value === value)
    if (!matchedOption) {
      return undefined
    }

    currentOptions = matchedOption.children ?? []
  }

  return matchedOption
}

function handleLocationChange(value?: string[]) {
  const normalized = Array.isArray(value) ? value : []
  selectedLocationPath.value = normalized
  const targetOption = resolveLocationOption(normalized)
  searchForm.location = targetOption?.searchValue ?? targetOption?.value ?? ''
}

function buildSearchParams() {
  return {
    keyword: searchForm.keyword,
    category: searchForm.category,
    location: searchForm.location,
    salaryMin: searchForm.salaryMin,
    salaryMax: searchForm.salaryMax,
    experienceRequirement: searchForm.experienceRequirement,
    educationRequirement: searchForm.educationRequirement,
    benefitTags: normalizeBenefitTags(searchForm.benefitTags),
    sortKey: searchForm.sortKey,
    pageNum: searchPagination.pageNum,
    pageSize: searchPagination.pageSize,
  }
}

function applyDiscoveryJobs(requestId: number, jobs: JobRecord[], total: number) {
  if (requestId !== jobsRequestId) return
  discoveryJobs.value = jobs
  searchPagination.total = total
}

function cancelScheduledJobsFetch() {
  if (jobsFetchTimer !== undefined) {
    clearTimeout(jobsFetchTimer)
    jobsFetchTimer = undefined
  }
}

function scheduleJobsFetch(options: { immediate?: boolean } = {}) {
  if (role.value !== 'JOBSEEKER') return

  cancelScheduledJobsFetch()

  const runFetch = () => {
    jobsFetchTimer = undefined
    void loadJobs()
  }

  if (options.immediate) {
    runFetch()
    return
  }

  jobsFetchTimer = setTimeout(runFetch, BROWSE_JOBS_DEBOUNCE_MS)
}

function handleSearchSubmit() {
  searchPagination.pageNum = 1
  scheduleJobsFetch({ immediate: true })
}

async function clearSearchFilters() {
  suspendAutoLoadJobs = true
  Object.assign(searchForm, defaultSearchForm, {
    benefitTags: [],
  })
  selectedLocationPath.value = []
  searchPagination.pageNum = 1
  cancelScheduledJobsFetch()
  await nextTick()
  suspendAutoLoadJobs = false
  scheduleJobsFetch({ immediate: true })
}

async function fetchSearchJobs(requestId: number) {
  const response = await client.get('/jobs/search', {
    params: buildSearchParams(),
  })
  const page = response.data as PageResponse<JobRecord>
  applyDiscoveryJobs(requestId, page.records, page.total)
}

async function loadJobs() {
  if (role.value !== 'JOBSEEKER') return

  const requestId = ++jobsRequestId
  loading.value = true

  try {
    await fetchSearchJobs(requestId)
  } finally {
    if (requestId === jobsRequestId) {
      loading.value = false
    }
  }
}

async function fetchCompanyJobs() {
  if (role.value !== 'COMPANY') return
  loading.value = true
  try {
    const response = await client.get('/company/jobs', {
      params: {
        status: companyFilter.status || undefined,
        pageNum: companyPagination.pageNum,
        pageSize: companyPagination.pageSize,
      },
    })
    const page = response.data as PageResponse<JobRecord>
    companyJobs.value = page.records
    companyPagination.total = page.total
  } finally {
    loading.value = false
  }
}

async function fetchRecentApplications() {
  if (role.value !== 'JOBSEEKER') return
  const response = await client.get('/jobseeker/applications', {
    params: {
      pageNum: 1,
      pageSize: 200,
    },
  })
  const page = response.data as PageResponse<ApplicationRecord>
  recentAppliedJobIds.value = page.records
    .filter((record) => isWithinSevenDays(record.appliedAt))
    .map((record) => record.jobId)
}

async function fetchAll() {
  if (role.value === 'COMPANY') {
    await fetchCompanyJobs()
    return
  }
  await Promise.all([fetchRecentApplications(), loadJobs()])
}

function openCreateDialog() {
  editingJobId.value = null
  Object.assign(companyForm, {
    title: '',
    category: '',
    location: '',
    salaryMin: 10000,
    salaryMax: 15000,
    experienceRequirement: '',
    educationRequirement: '',
    headcount: 1,
    description: '',
    skillTags: [],
    benefitTags: [],
    expireAt: '',
  })
  dialogVisible.value = true
}

function openEditDialog(job: JobRecord) {
  editingJobId.value = job.id
  Object.assign(companyForm, {
    title: job.title,
    category: job.category,
    location: job.location,
    salaryMin: job.salaryMin,
    salaryMax: job.salaryMax,
    experienceRequirement: job.experienceRequirement,
    educationRequirement: job.educationRequirement,
    headcount: job.headcount,
    description: job.description,
    skillTags: normalizeTagList(job.skillTags),
    benefitTags: normalizeBenefitTags(job.benefitTags),
    expireAt: job.expireAt ?? '',
  })
  dialogVisible.value = true
}

async function saveCompanyJob() {
  const payload = {
    ...companyForm,
    skillTags: normalizeTagList(companyForm.skillTags),
    benefitTags: normalizeBenefitTags(companyForm.benefitTags),
  }
  companyForm.skillTags = payload.skillTags
  companyForm.benefitTags = payload.benefitTags

  if (editingJobId.value) {
    await client.put(`/company/jobs/${editingJobId.value}`, payload)
    ElMessage.success('岗位已更新')
  } else {
    await client.post('/company/jobs', payload)
    ElMessage.success('岗位已创建')
  }
  dialogVisible.value = false
  await fetchCompanyJobs()
}

async function updateStatus(jobId: number, status: string) {
  await client.patch(`/company/jobs/${jobId}/status`, { status })
  ElMessage.success('岗位状态已更新')
  await fetchCompanyJobs()
}

async function removeJob(jobId: number) {
  await ElMessageBox.confirm('删除后无法恢复，确认继续吗？', '删除岗位', { type: 'warning' })
  await client.delete(`/company/jobs/${jobId}`)
  ElMessage.success('岗位已删除')
  await fetchCompanyJobs()
}

function isRecentlyApplied(job: JobRecord) {
  return Boolean(job.recentlyApplied || recentAppliedJobIds.value.includes(job.id))
}

function markJobAsRecentlyApplied(jobId: number) {
  if (!recentAppliedJobIds.value.includes(jobId)) {
    recentAppliedJobIds.value = [...recentAppliedJobIds.value, jobId]
  }

  const target = discoveryJobs.value.find((job) => job.id === jobId)
  if (target) {
    target.recentlyApplied = true
  }

  if (selectedJobDetail.value?.id === jobId) {
    selectedJobDetail.value = {
      ...selectedJobDetail.value,
      recentlyApplied: true,
    }
  }
}

function findJobAcrossCollections(jobId: number) {
  return discoveryJobs.value.find((job) => job.id === jobId) ?? null
}

function syncJobFavoriteState(jobId: number, favorited: boolean) {
  const target = discoveryJobs.value.find((job) => job.id === jobId)
  if (target) {
    target.favorited = favorited
  }

  if (selectedJobDetail.value?.id === jobId) {
    selectedJobDetail.value = {
      ...selectedJobDetail.value,
      favorited,
    }
  }
}

async function showDuplicateApplyDialog() {
  await ElMessageBox.alert(duplicateApplyMessage, '提示', {
    confirmButtonText: '知道了',
    type: 'warning',
  })
}

function resetApplyResumeDialog() {
  applyResumeLoading.value = false
  applyResumeSubmitting.value = false
  applyResumeError.value = ''
  applyTargetJob.value = null
  applySavedResumes.value = []
  selectedSavedResumeId.value = null
}

async function openApplyResumeDialog(job: JobRecord) {
  applyTargetJob.value = job
  applyResumeDialogVisible.value = true
  applyResumeLoading.value = true
  applyResumeError.value = ''
  applySavedResumes.value = []
  selectedSavedResumeId.value = null
  try {
    const response = await client.get('/jobseeker/saved-resumes', {
      params: {
        completeOnly: true,
        pageNum: 1,
        pageSize: 20,
      },
    })
    const page = response.data as PageResponse<SavedResumeSummary>
    applySavedResumes.value = page.records
    selectedSavedResumeId.value = page.records[0]?.id ?? null
  } catch (error) {
    applyResumeError.value = String(
      (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '简历列表加载失败',
    )
  } finally {
    applyResumeLoading.value = false
  }
}

async function applyJob(jobId: number, savedResumeId: number) {
  try {
    const payload: ApplyJobRequest = {
      jobId,
      savedResumeId,
    }
    await client.post('/jobseeker/applications', payload)
    markJobAsRecentlyApplied(jobId)
    ElMessage.success('投递成功')
    return true
  } catch (error) {
    const message = String(
      (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '',
    )

    if (message.includes('7') || message.includes('同一岗位')) {
      markJobAsRecentlyApplied(jobId)
      await showDuplicateApplyDialog()
      return true
    }
    return false
  }
}

async function confirmApplyWithSelectedResume() {
  if (!applyTargetJob.value) return
  if (!selectedSavedResumeId.value) {
    ElMessage.warning('请选择要投递的简历')
    return
  }
  applyResumeSubmitting.value = true
  try {
    const applied = await applyJob(applyTargetJob.value.id, selectedSavedResumeId.value)
    if (applied) {
      applyResumeDialogVisible.value = false
    }
  } finally {
    applyResumeSubmitting.value = false
  }
}

function goToResumeStudioForApply() {
  applyResumeDialogVisible.value = false
  void router.push('/resume')
}

async function handleApplyClick(job: JobRecord) {
  if (isRecentlyApplied(job)) {
    await showDuplicateApplyDialog()
    return
  }
  await openApplyResumeDialog(job)
}

async function openJobDetail(job: JobRecord) {
  await openJobDetailById(job.id, job)
}

async function openJobDetailById(jobId: number, fallbackJob?: JobRecord | null) {
  selectedJobId.value = jobId
  selectedJobDetail.value = fallbackJob ? { ...fallbackJob } : null
  jobDetailVisible.value = true
  jobDetailLoading.value = true

  try {
    const response = await client.get(`/jobs/${jobId}`)
    if (selectedJobId.value !== jobId) return
    const detail = response.data as JobRecord
    selectedJobDetail.value = {
      ...detail,
      recentlyApplied: isRecentlyApplied(fallbackJob ?? detail),
      favorited: fallbackJob?.favorited ?? detail.favorited,
    }
  } catch {
    if (selectedJobId.value === jobId && fallbackJob) {
      selectedJobDetail.value = {
        ...fallbackJob,
        recentlyApplied: isRecentlyApplied(fallbackJob),
      }
    }
  } finally {
    if (selectedJobId.value === jobId) {
      jobDetailLoading.value = false
    }
  }
}

async function toggleFavoriteJob() {
  if (!selectedJobDetail.value || favoriteSubmitting.value) return

  const jobId = selectedJobDetail.value.id
  favoriteSubmitting.value = true
  try {
    if (selectedJobFavorited.value) {
      const response = await client.delete(`/jobseeker/favorites/${jobId}`)
      const updatedJob = response.data as Partial<JobRecord>
      syncJobFavoriteState(jobId, false)
      if (selectedJobDetail.value?.id === jobId) {
        selectedJobDetail.value = {
          ...selectedJobDetail.value,
          ...updatedJob,
          favorited: false,
        }
      }
      ElMessage.success('已取消收藏')
      return
    }

    const response = await client.post(`/jobseeker/favorites/${jobId}`)
    const updatedJob = response.data as Partial<JobRecord>
    syncJobFavoriteState(jobId, true)
    if (selectedJobDetail.value?.id === jobId) {
      selectedJobDetail.value = {
        ...selectedJobDetail.value,
        ...updatedJob,
        favorited: true,
      }
    }
    ElMessage.success('已加入收藏')
  } finally {
    favoriteSubmitting.value = false
  }
}

function handleJobCardKeydown(event: KeyboardEvent, job: JobRecord) {
  if (event.key !== 'Enter' && event.key !== ' ') return
  event.preventDefault()
  void openJobDetail(job)
}

async function handleDetailApplyClick() {
  if (!selectedJobDetail.value) return
  await handleApplyClick(selectedJobDetail.value)
}

async function startConversationWithCompany(job?: JobRecord | null) {
  if (!job?.companyUserId) return
  jobDetailVisible.value = false
  await router.push({
    path: '/chat',
    query: { peerUserId: String(job.companyUserId) },
  })
}

function clearDetailJobQuery() {
  if (!route.query.detailJobId) return
  const nextQuery = { ...route.query }
  delete nextQuery.detailJobId
  void router.replace({ path: route.path, query: nextQuery })
}

function resetJobDetailState() {
  jobDetailLoading.value = false
  favoriteSubmitting.value = false
  selectedJobId.value = null
  selectedJobDetail.value = null
  clearDetailJobQuery()
}

async function openDetailFromRouteQuery() {
  if (role.value !== 'JOBSEEKER') return
  const rawJobId = Array.isArray(route.query.detailJobId) ? route.query.detailJobId[0] : route.query.detailJobId
  const parsedJobId = Number(rawJobId)
  if (!Number.isInteger(parsedJobId) || parsedJobId <= 0) return
  if (selectedJobId.value === parsedJobId && jobDetailVisible.value) return
  await openJobDetailById(parsedJobId, findJobAcrossCollections(parsedJobId))
}

function isWithinSevenDays(appliedAt?: string) {
  if (!appliedAt) return false
  const appliedTime = new Date(appliedAt).getTime()
  if (Number.isNaN(appliedTime)) return false
  return Date.now() - appliedTime < 7 * 24 * 60 * 60 * 1000
}

watch(
  () => [
    searchForm.keyword,
    searchForm.category,
    searchForm.location,
    searchForm.salaryMin,
    searchForm.salaryMax,
    searchForm.experienceRequirement,
    searchForm.educationRequirement,
    searchForm.benefitTags.join('|'),
    searchForm.sortKey,
  ],
  () => {
    if (role.value !== 'JOBSEEKER' || suspendAutoLoadJobs) return
    searchPagination.pageNum = 1
    scheduleJobsFetch()
  },
)

watch(
  () => route.query.detailJobId,
  () => {
    void openDetailFromRouteQuery()
  },
)

onBeforeUnmount(() => {
  cancelScheduledJobsFetch()
  jobsRequestId += 1
})

onMounted(async () => {
  await fetchAll()
  await openDetailFromRouteQuery()
})
</script>

<template>
  <div class="section-grid">
    <template v-if="role === 'COMPANY'">
      <section class="surface-card toolbar-panel">
        <div class="toolbar-head">
          <div>
            <h3>岗位列表</h3>
          </div>
          <el-button type="primary" @click="openCreateDialog">新建岗位</el-button>
        </div>

        <div class="toolbar-row">
          <el-select v-model="companyFilter.status" clearable placeholder="按状态筛选" style="width: 180px" @change="fetchCompanyJobs">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <span class="result-note">共 {{ companyPagination.total }} 个岗位</span>
        </div>
      </section>

      <section class="surface-card table-panel">
        <el-table :data="companyJobs" v-loading="loading">
          <el-table-column prop="title" label="岗位名称" min-width="180" />
          <el-table-column prop="category" label="岗位类别" min-width="120" />
          <el-table-column prop="location" label="工作地点" min-width="120" />
          <el-table-column label="薪资范围" min-width="150">
            <template #default="{ row }">{{ row.salaryMin }} - {{ row.salaryMax }} 元/月</template>
          </el-table-column>
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="320">
            <template #default="{ row }">
              <div class="action-row">
                <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button text @click="updateStatus(row.id, 'PUBLISHED')">发布</el-button>
                <el-button text @click="updateStatus(row.id, 'OFFLINE')">下线</el-button>
                <el-button text type="danger" @click="removeJob(row.id)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="companyPagination.pageNum"
          :page-size="companyPagination.pageSize"
          :total="companyPagination.total"
          @current-change="(page: number) => { companyPagination.pageNum = page; fetchCompanyJobs() }"
        />
      </section>

      <el-dialog v-model="dialogVisible" :title="editingJobId ? '编辑岗位' : '新建岗位'" width="760px">
        <el-form label-position="top">
          <div class="dialog-grid">
            <el-form-item label="岗位名称"><el-input v-model="companyForm.title" /></el-form-item>
            <el-form-item label="岗位类别"><el-input v-model="companyForm.category" /></el-form-item>
            <el-form-item label="工作地点"><el-input v-model="companyForm.location" /></el-form-item>
            <el-form-item label="薪资下限"><el-input-number v-model="companyForm.salaryMin" :min="0" /></el-form-item>
            <el-form-item label="薪资上限"><el-input-number v-model="companyForm.salaryMax" :min="0" /></el-form-item>
            <el-form-item label="经验要求"><el-input v-model="companyForm.experienceRequirement" placeholder="例如：3 年及以上" /></el-form-item>
            <el-form-item label="学历要求"><el-input v-model="companyForm.educationRequirement" placeholder="例如：本科及以上" /></el-form-item>
            <el-form-item label="招聘人数"><el-input-number v-model="companyForm.headcount" :min="1" /></el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker
                v-model="companyForm.expireAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择截止时间"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="福利待遇" class="dialog-grid__full">
              <div class="benefit-editor">
                <el-select
                  v-model="companyForm.benefitTags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  collapse-tags
                  collapse-tags-tooltip
                  :multiple-limit="12"
                  class="benefit-select"
                  placeholder="选择或输入福利标签"
                >
                  <el-option v-for="item in presetJobBenefitTags" :key="item" :label="item" :value="item" />
                </el-select>
                <p class="benefit-editor__hint">最多选择 12 个标签，也可以输入自定义福利标签。</p>
                <div class="benefit-preview-strip">
                  <span v-for="tag in companyBenefitPreviewTags" :key="tag" class="benefit-tag">{{ tag }}</span>
                  <span v-if="!companyBenefitPreviewTags.length" class="empty-inline">暂未选择福利标签</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="技能标签" class="dialog-grid__full">
              <div class="benefit-editor">
                <el-select
                  v-model="companyForm.skillTags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  collapse-tags
                  collapse-tags-tooltip
                  :multiple-limit="12"
                  class="benefit-select"
                  placeholder="选择或输入技能标签"
                >
                  <el-option v-for="item in presetJobSkillTags" :key="item" :label="item" :value="item" />
                </el-select>
                <p class="benefit-editor__hint">最多选择 12 个技能标签，也可以输入自定义技能标签。</p>
                <div class="benefit-preview-strip">
                  <span v-for="tag in companySkillPreviewTags" :key="tag" class="benefit-tag">{{ tag }}</span>
                  <span v-if="!companySkillPreviewTags.length" class="empty-inline">暂未选择技能标签</span>
                </div>
              </div>
            </el-form-item>
          </div>
          <el-form-item label="岗位描述">
            <el-input
              v-model="companyForm.description"
              type="textarea"
              :rows="8"
              placeholder="请填写业务背景、岗位职责、任职要求和岗位亮点。"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCompanyJob">保存岗位</el-button>
        </template>
      </el-dialog>
    </template>

    <template v-else>
      <section class="surface-card search-panel">
        <form class="search-workbench" @submit.prevent="handleSearchSubmit">
          <div class="search-primary-row">
            <div class="search-field search-field--keyword">
              <label class="search-field__label" for="job-search-keyword">关键词搜索</label>
              <el-input
                id="job-search-keyword"
                v-model="searchForm.keyword"
                clearable
                placeholder="搜索岗位名称、公司名称或关键词"
              />
            </div>
            <div class="search-primary-row__actions">
              <el-button native-type="submit" type="primary" class="search-submit-button" :loading="loading">
                立即搜索
              </el-button>
            </div>
          </div>

          <div class="search-filter-grid">
            <div class="search-field">
              <span class="search-field__label">岗位类别</span>
              <el-input v-model="searchForm.category" clearable placeholder="例如：后端开发、测试、运营" />
            </div>

            <div class="search-field">
              <span class="search-field__label">工作地点</span>
              <el-cascader
                v-model="selectedLocationPath"
                class="search-location-cascader"
                :options="chinaCityOptions"
                clearable
                filterable
                placeholder="选择省 / 市"
                separator=" / "
                @change="handleLocationChange"
              />
            </div>

            <div class="search-field search-field--salary">
              <span class="search-field__label">薪资范围</span>
              <div class="salary-range-group">
                <el-input-number v-model="searchForm.salaryMin" :min="0" placeholder="最低薪资" />
                <span class="salary-range-group__divider">至</span>
                <el-input-number v-model="searchForm.salaryMax" :min="0" placeholder="最高薪资" />
              </div>
            </div>

            <div class="search-field">
              <span class="search-field__label">工作经验</span>
              <el-select v-model="searchForm.experienceRequirement" clearable placeholder="选择经验要求">
                <el-option v-for="item in experienceOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </div>

            <div class="search-field">
              <span class="search-field__label">学历要求</span>
              <el-select v-model="searchForm.educationRequirement" clearable placeholder="选择学历要求">
                <el-option v-for="item in educationOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </div>

            <div class="search-field">
              <span class="search-field__label">福利待遇</span>
              <el-select
                v-model="searchForm.benefitTags"
                multiple
                filterable
                allow-create
                default-first-option
                collapse-tags
                collapse-tags-tooltip
                :multiple-limit="12"
                placeholder="选择或输入福利标签"
              >
                <el-option v-for="item in presetJobBenefitTags" :key="item" :label="item" :value="item" />
              </el-select>
            </div>

            <div class="search-field">
              <span class="search-field__label">排序方式</span>
              <el-select v-model="searchForm.sortKey" placeholder="选择排序方式">
                <el-option
                  v-for="option in jobDiscoverySortOptions"
                  :key="option.key"
                  :label="option.label"
                  :value="option.key"
                />
              </el-select>
            </div>
          </div>

          <div class="search-panel__footer">
            <div class="search-summary">
              <span class="search-summary__label">当前筛选</span>
              <div class="search-summary__content">
                <template v-if="searchSummaryItems.length">
                  <span v-for="item in searchSummaryItems" :key="item" class="search-chip">{{ item }}</span>
                </template>
                <p v-else class="search-summary__empty">当前未设置筛选条件。</p>
              </div>
            </div>

            <div class="search-panel__footer-actions">
              <el-button plain native-type="button" class="search-reset-button" @click="clearSearchFilters">
                清空筛选
              </el-button>
            </div>
          </div>
        </form>
      </section>

      <section class="surface-card discovery-results">
        <div class="discovery-results__header">
          <div class="discovery-results__heading">
            <h3>{{ discoveryPanelTitle }}</h3>
          </div>
          <div class="discovery-results__summary">
            <span class="discovery-results__status-tag">{{ discoveryModeLabel }}</span>
            <div class="discovery-results__count">
              <span>岗位数量</span>
              <strong>{{ searchPagination.total }}</strong>
            </div>
          </div>
        </div>

        <div v-loading="loading" class="discovery-results__content" :class="{ 'is-loading': loading }">
          <div v-if="discoveryJobs.length" class="job-grid">
            <article
              v-for="job in discoveryJobs"
              :key="job.id"
              class="surface-card job-card job-card--interactive"
              role="button"
              tabindex="0"
              :aria-label="`查看 ${job.title} 岗位详情`"
              @click="openJobDetail(job)"
              @keydown="handleJobCardKeydown($event, job)"
            >
              <div class="job-head">
                <div>
                  <h3>{{ job.title }}</h3>
                  <p>{{ job.companyName }} · {{ job.location }}</p>
                </div>
              </div>

              <div class="salary-line">{{ job.salaryMin }} - {{ job.salaryMax }} 元/月</div>

              <div class="job-meta">
                <span>{{ job.category }}</span>
                <span>{{ job.experienceRequirement }}</span>
                <span>{{ job.educationRequirement }}</span>
              </div>

              <div v-if="getJobSkillTags(job).length" class="job-benefit-list">
                <span v-for="tag in getJobSkillTags(job)" :key="tag" class="job-benefit-chip">{{ tag }}</span>
              </div>

              <div v-if="getJobBenefitTags(job).length" class="job-benefit-list">
                <span v-for="tag in getJobBenefitTags(job)" :key="tag" class="job-benefit-chip">{{ tag }}</span>
              </div>

              <p class="job-desc">{{ job.description }}</p>

              <div class="job-footer">
                <span>岗位编号：{{ job.jobCode }}</span>
                <span class="job-footer__hint">查看详情</span>
              </div>
            </article>
          </div>

          <div v-else class="discovery-empty-state">
            <el-empty :description="emptyStateModel.description">
              <template #image>
                <div class="discovery-empty-state__icon">
                  <span>岗</span>
                </div>
              </template>
              <template #description>
                <div class="discovery-empty-state__copy">
                  <strong>{{ emptyStateModel.title }}</strong>
                  <p>{{ emptyStateModel.description }}</p>
                </div>
              </template>
            </el-empty>
            <el-button
              v-if="emptyStateModel.actionLabel"
              plain
              class="discovery-empty-state__action"
              @click="clearSearchFilters"
            >
              {{ emptyStateModel.actionLabel }}
            </el-button>
          </div>
        </div>
      </section>

        <el-dialog
          v-model="applyResumeDialogVisible"
          title="选择投递简历"
          width="520px"
          destroy-on-close
          @closed="resetApplyResumeDialog"
        >
          <div v-loading="applyResumeLoading" class="apply-resume-dialog__body">
            <template v-if="applyResumeError">
              <el-result icon="error" title="简历列表加载失败" :sub-title="applyResumeError" />
            </template>
            <template v-else-if="!applySavedResumes.length">
              <el-empty description="还没有可投递的简历">
                <el-button type="primary" plain @click="goToResumeStudioForApply">去保存一份简历</el-button>
              </el-empty>
            </template>
            <template v-else>
              <p class="apply-resume-dialog__hint">
                {{ applyTargetJob ? `请选择要投递到“${applyTargetJob.title}”的简历。` : '请选择一份可投递的简历。' }}
              </p>
              <div class="apply-resume-list">
                <button
                  v-for="item in applySavedResumes"
                  :key="item.id"
                  type="button"
                  :class="['apply-resume-card', selectedSavedResumeId === item.id ? 'is-selected' : '']"
                  @click="selectedSavedResumeId = item.id"
                >
                  <div class="apply-resume-card__copy">
                    <strong>{{ item.name }}</strong>
                    <p>完整度 {{ item.completenessScore }}%</p>
                  </div>
                  <span v-if="selectedApplyResume?.id === item.id" class="apply-resume-card__badge">已选择</span>
                </button>
              </div>
            </template>
          </div>
          <template #footer>
            <div class="apply-resume-dialog__footer">
              <el-button @click="applyResumeDialogVisible = false">取消</el-button>
              <el-button
                type="primary"
                :loading="applyResumeSubmitting"
                :disabled="!selectedSavedResumeId || !applySavedResumes.length"
                @click="confirmApplyWithSelectedResume"
              >
                确认投递
              </el-button>
            </div>
          </template>
        </el-dialog>

        <el-dialog
          v-model="jobDetailVisible"
          class="job-detail-dialog"
          transition="job-detail-motion"
          modal-class="job-detail-overlay"
          title="岗位详情"
          width="820px"
        align-center
        destroy-on-close
        @closed="resetJobDetailState"
      >
        <div v-loading="jobDetailLoading" class="job-detail-body">
          <template v-if="selectedJobDetail">
            <section class="job-detail-hero">
              <div class="job-detail-hero__head">
                <div>
                  <h3>{{ selectedJobDetail.title }}</h3>
                  <p>{{ selectedJobDetail.companyName }} · {{ selectedJobDetail.location }}</p>
                </div>
                <div class="job-detail-hero__actions">
                  <el-tooltip :content="favoriteTooltipText" placement="top">
                    <button
                      type="button"
                      class="favorite-toggle"
                      :class="{
                        'is-active': selectedJobFavorited,
                        'is-loading': favoriteSubmitting,
                      }"
                      :aria-label="favoriteTooltipText"
                      :disabled="favoriteSubmitting"
                      @click="toggleFavoriteJob"
                    >
                      <span class="favorite-toggle__halo" aria-hidden="true"></span>
                      <span class="favorite-toggle__surface" aria-hidden="true">
                        <svg viewBox="0 0 24 24" class="favorite-toggle__icon" focusable="false" aria-hidden="true">
                          <path
                            d="M12 2.2l2.77 6.14 6.72.57-5.08 4.42 1.52 6.58L12 16.47 6.09 19.9l1.52-6.58-5.08-4.42 6.72-.57L12 2.2z"
                          />
                        </svg>
                      </span>
                    </button>
                  </el-tooltip>
                </div>
              </div>

              <div class="job-detail-salary">{{ selectedJobDetail.salaryMin }} - {{ selectedJobDetail.salaryMax }} 元/月</div>

              <div class="job-meta job-detail-tags">
                <span>{{ selectedJobDetail.category }}</span>
                <span>{{ selectedJobDetail.experienceRequirement }}</span>
                <span>{{ selectedJobDetail.educationRequirement }}</span>
                <span>{{ selectedJobDetail.headcount }} 人招聘</span>
              </div>
            </section>

            <section v-if="selectedJobDetail.benefitTags?.length" class="job-detail-panel">
              <h4>福利待遇</h4>
              <div class="benefit-preview-strip">
                <span v-for="tag in selectedJobDetail.benefitTags" :key="tag" class="benefit-tag">{{ tag }}</span>
              </div>
            </section>

            <section v-if="selectedJobDetail.skillTags?.length" class="job-detail-panel">
              <h4>技能标签</h4>
              <div class="benefit-preview-strip">
                <span v-for="tag in selectedJobDetail.skillTags" :key="tag" class="benefit-tag">{{ tag }}</span>
              </div>
            </section>

            <section class="job-detail-panel">
              <h4>岗位信息</h4>
              <div class="job-detail-facts">
                <div class="job-detail-fact">
                  <span>岗位编号</span>
                  <strong>{{ selectedJobDetail.jobCode }}</strong>
                </div>
                <div class="job-detail-fact">
                  <span>发布时间</span>
                  <strong>{{ formatDateTime(selectedJobDetail.publishedAt) }}</strong>
                </div>
                <div class="job-detail-fact">
                  <span>截止时间</span>
                  <strong>{{ formatDateTime(selectedJobDetail.expireAt) }}</strong>
                </div>
                <div class="job-detail-fact">
                  <span>投递状态</span>
                  <strong>{{ selectedJobApplied ? '已投递' : '可投递' }}</strong>
                </div>
              </div>
            </section>

            <section class="job-detail-panel">
              <h4>岗位描述</h4>
              <p class="job-detail-description">{{ selectedJobDetail.description }}</p>
            </section>
          </template>

          <el-empty v-else description="未获取到岗位详情" />
        </div>

        <template #footer>
          <div class="job-detail-footer">
            <p class="job-detail-footer__tip">
              {{ selectedJobApplied
                ? '你已完成投递，也可以继续和企业在线沟通。'
                : '你可以先投递简历，也可以直接发起在线沟通。' }}
            </p>
            <div v-if="selectedJobDetail" class="job-detail-actions">
              <el-button
                size="large"
                :type="selectedJobApplied ? 'info' : 'primary'"
                :class="['apply-button', 'job-detail-apply', selectedJobApplied ? 'is-applied' : '']"
                @click="handleDetailApplyClick"
              >
                {{ selectedJobApplied ? '已投递' : '投递简历' }}
              </el-button>
              <el-button
                size="large"
                plain
                class="job-detail-chat"
                @click="startConversationWithCompany(selectedJobDetail)"
              >
                在线沟通
              </el-button>
            </div>
          </div>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
.toolbar-panel,
.table-panel {
  padding: 22px;
}

.search-panel {
  padding: 26px;
  border-color: rgba(14, 165, 233, 0.12);
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.09), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(240, 249, 255, 0.96));
  box-shadow:
    0 24px 52px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.toolbar-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.toolbar-head h3 {
  margin: 0;
}

.toolbar-head p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.toolbar-row {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.result-note {
  color: var(--text-muted);
  font-size: 13px;
}

.search-workbench {
  display: grid;
  gap: 22px;
}

.search-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.search-panel__heading {
  display: grid;
  gap: 8px;
}

.search-panel__heading h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.search-panel__heading p {
  margin: 0;
  max-width: 640px;
  color: var(--text-muted);
  line-height: 1.7;
}

.search-panel__status {
  min-width: 188px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(3, 105, 161, 0.12);
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  gap: 4px;
  text-align: right;
}

.search-panel__status.is-loading {
  border-color: rgba(3, 105, 161, 0.24);
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.08);
}

.search-panel__status-label {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.search-panel__status strong {
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.search-panel__status-meta {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.55;
}

.search-primary-row {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
}

.search-primary-row__actions {
  display: flex;
  align-items: stretch;
}

.search-submit-button {
  min-width: 152px;
  min-height: 52px;
  border-radius: 16px;
  font-weight: 700;
  box-shadow: 0 16px 28px rgba(3, 105, 161, 0.16);
}

.search-filter-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.search-field {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.search-field--keyword,
.search-field--salary {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.search-field--salary {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(239, 246, 255, 0.82));
}

.search-field__label {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 700;
}

.search-field__hint {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.search-location-cascader {
  width: 100%;
}

.salary-range-group {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
}

.salary-range-group__divider {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.search-panel__footer {
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.search-summary {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.search-summary__label {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.search-summary__content {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.search-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(3, 105, 161, 0.09);
  color: var(--primary-deep);
  font-size: 13px;
  font-weight: 600;
}

.search-summary__empty {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.search-panel__footer-actions {
  display: grid;
  justify-items: end;
  gap: 12px;
  max-width: 320px;
}

.search-panel__hint {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.65;
  text-align: right;
}

.search-reset-button {
  border-radius: 999px;
}

.discovery-results {
  padding: 26px;
  display: grid;
  gap: 22px;
  border-color: rgba(14, 165, 233, 0.1);
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.96));
}

.discovery-results__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.discovery-results__heading {
  display: grid;
  gap: 8px;
}

.discovery-results__heading h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.12;
}

.discovery-results__heading p {
  margin: 0;
  max-width: 680px;
  color: var(--text-muted);
  line-height: 1.7;
}

.discovery-results__summary {
  display: grid;
  justify-items: end;
  gap: 12px;
  min-width: 168px;
}

.discovery-results__status-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(3, 105, 161, 0.1);
  color: var(--primary-deep);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.discovery-results__count {
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(3, 105, 161, 0.12);
  background: rgba(255, 255, 255, 0.84);
  display: grid;
  gap: 4px;
  text-align: right;
}

.discovery-results__count span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.discovery-results__count strong {
  font-size: 30px;
  line-height: 1;
  letter-spacing: -0.04em;
  color: var(--text-strong);
}

.discovery-results__notice {
  margin: -4px 0 0;
  padding: 13px 16px;
  border-radius: 16px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--primary-deep);
  font-size: 13px;
  line-height: 1.7;
}

.discovery-results__content {
  transition: opacity 180ms ease;
}

.discovery-results__content.is-loading {
  opacity: 0.92;
}

.discovery-empty-state {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 16px 0 4px;
}

.discovery-empty-state__icon {
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.14), rgba(59, 130, 246, 0.08));
  color: var(--primary-deep);
  display: grid;
  place-items: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.discovery-empty-state__icon span {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.discovery-empty-state__copy {
  display: grid;
  gap: 8px;
  justify-items: center;
  text-align: center;
}

.discovery-empty-state__copy strong {
  font-size: 20px;
  color: var(--text-strong);
}

.discovery-empty-state__copy p {
  margin: 0;
  max-width: 420px;
  color: var(--text-muted);
  line-height: 1.75;
}

.discovery-empty-state__action {
  border-radius: 999px;
}

:deep(.search-panel .el-input__wrapper),
:deep(.search-panel .el-select__wrapper),
:deep(.search-panel .el-cascader .el-input__wrapper) {
  min-height: 48px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.18) inset;
  transition: box-shadow 180ms ease, background-color 180ms ease;
}

:deep(.search-panel .el-input__wrapper:hover),
:deep(.search-panel .el-select__wrapper:hover),
:deep(.search-panel .el-cascader .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(3, 105, 161, 0.24) inset;
}

:deep(.search-panel .el-input__wrapper.is-focus),
:deep(.search-panel .el-select__wrapper.is-focused),
:deep(.search-panel .el-cascader .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(3, 105, 161, 0.34) inset,
    0 0 0 4px rgba(14, 165, 233, 0.08);
}

:deep(.search-panel .el-input-number) {
  width: 100%;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dialog-grid__full {
  grid-column: 1 / -1;
}

.benefit-editor {
  display: grid;
  gap: 12px;
}

.benefit-select {
  width: 100%;
}

.benefit-editor__hint {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.benefit-preview-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.benefit-tag {
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  font-size: 13px;
  font-weight: 700;
}

.empty-inline {
  color: var(--text-muted);
  font-size: 13px;
}

.job-grid {
  display: grid;
  gap: 26px;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
}

.job-card {
  padding: 30px;
  display: flex;
  flex-direction: column;
  min-height: 392px;
  transition:
    transform 260ms cubic-bezier(0.22, 1, 0.36, 1),
    border-color 260ms ease,
    box-shadow 260ms ease,
    background-color 260ms ease;
}

.job-card--interactive {
  cursor: pointer;
}

.job-card:hover {
  transform: translateY(-6px);
  border-color: rgba(3, 105, 161, 0.22);
  box-shadow: 0 24px 44px rgba(3, 105, 161, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(240, 249, 255, 0.94));
}

.job-card--interactive:active {
  transform: translateY(-2px);
}

.job-card--interactive:focus-visible {
  outline: none;
  border-color: rgba(3, 105, 161, 0.26);
  box-shadow:
    0 0 0 3px rgba(3, 105, 161, 0.12),
    0 16px 32px rgba(3, 105, 161, 0.12);
}

.job-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.job-head h3 {
  margin: 0;
  font-size: 28px;
  line-height: 1.18;
}

.job-head p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.job-score-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(22, 163, 74, 0.1);
  color: #15803d;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
}

.job-score-badge.is-default {
  background: linear-gradient(180deg, rgba(14, 165, 233, 0.14), rgba(34, 197, 94, 0.12));
  color: var(--primary-deep);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.salary-line {
  margin-top: 18px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.job-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.job-meta span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.9);
  color: var(--text-main);
  font-size: 13px;
}

.reason-list {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.job-benefit-list {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.job-benefit-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.92);
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 600;
}

.job-desc {
  margin: 18px 0 0;
  color: var(--text-main);
  line-height: 1.8;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 6;
  -webkit-box-orient: vertical;
}

.job-footer {
  margin-top: auto;
  padding-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  color: var(--text-muted);
  font-size: 13px;
}

.job-footer__hint {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--primary);
  font-weight: 600;
  transform: translateX(0);
  transition: transform 240ms cubic-bezier(0.22, 1, 0.36, 1), color 240ms ease;
}

.job-footer__hint::after {
  content: '->';
  font-size: 14px;
}

.job-card--interactive:hover .job-footer__hint,
.job-card--interactive:focus-visible .job-footer__hint {
  color: var(--primary-deep);
  transform: translateX(4px);
}

.job-detail-body {
  display: grid;
  gap: 18px;
  min-height: 260px;
}

.job-detail-hero {
  display: grid;
  gap: 18px;
}

.job-detail-hero__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.job-detail-hero__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.job-detail-hero__head h3 {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
}

.job-detail-hero__head p {
  margin: 10px 0 0;
  color: var(--text-muted);
}

.favorite-toggle {
  position: relative;
  width: 58px;
  min-width: 58px;
  height: 58px;
  padding: 0;
  border: none;
  background: transparent;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition:
    transform 220ms ease,
    filter 220ms ease,
    opacity 180ms ease;
  isolation: isolate;
}

.favorite-toggle:disabled {
  cursor: wait;
}

.favorite-toggle__halo,
.favorite-toggle__surface {
  position: absolute;
  top: 0;
  left: 50%;
  border-radius: 999px;
  transform: translateX(-50%);
  transition:
    transform 220ms ease,
    opacity 220ms ease,
    border-color 220ms ease,
    box-shadow 220ms ease,
    background 220ms ease;
}

.favorite-toggle__halo {
  width: 58px;
  height: 58px;
  top: 0;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.68), transparent 48%),
    radial-gradient(circle at 65% 70%, rgba(96, 165, 250, 0.1), transparent 58%);
  opacity: 0.62;
  filter: blur(2px);
  z-index: 0;
}

.favorite-toggle__surface {
  width: 54px;
  height: 54px;
  z-index: 1;
  border: 1px solid rgba(191, 219, 254, 0.82);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 255, 0.9));
  box-shadow:
    0 14px 26px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.favorite-toggle__surface::before {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: inherit;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.56), transparent 42%);
  opacity: 0.82;
}

.favorite-toggle__icon {
  position: relative;
  z-index: 1;
  width: 24px;
  height: 24px;
  fill: rgba(203, 213, 225, 0.16);
  stroke: #a8b4c5;
  stroke-width: 1.7;
  stroke-linejoin: round;
  transition:
    transform 220ms ease,
    fill 220ms ease,
    stroke 220ms ease,
    filter 220ms ease;
}

.favorite-toggle:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: saturate(1.02);
}

.favorite-toggle:hover:not(:disabled) .favorite-toggle__surface {
  transform: translateX(-50%) scale(1.03);
  border-color: rgba(147, 197, 253, 0.9);
  box-shadow:
    0 16px 28px rgba(15, 23, 42, 0.11),
    inset 0 1px 0 rgba(255, 255, 255, 0.96);
}

.favorite-toggle:hover:not(:disabled) .favorite-toggle__icon {
  transform: scale(1.06);
}

.favorite-toggle:active:not(:disabled) {
  transform: translateY(0);
}

.favorite-toggle:active:not(:disabled) .favorite-toggle__surface {
  transform: translateX(-50%) scale(0.98);
}

.favorite-toggle:focus-visible {
  outline: none;
}

.favorite-toggle:focus-visible .favorite-toggle__surface {
  border-color: rgba(96, 165, 250, 0.72);
  box-shadow:
    0 0 0 4px rgba(191, 219, 254, 0.5),
    0 16px 28px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.96);
}

.favorite-toggle.is-active .favorite-toggle__halo {
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.68), transparent 48%),
    radial-gradient(circle at 65% 70%, rgba(96, 165, 250, 0.1), transparent 58%);
  opacity: 0.62;
}

.favorite-toggle.is-active .favorite-toggle__surface {
  border-color: rgba(191, 219, 254, 0.82);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 255, 0.9));
  box-shadow:
    0 14px 26px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.favorite-toggle.is-active .favorite-toggle__icon {
  fill: #ffbf1f;
  stroke: #ffbf1f;
  filter: drop-shadow(0 4px 8px rgba(255, 191, 31, 0.22));
}

.favorite-toggle.is-loading {
  opacity: 0.88;
}

.favorite-toggle.is-loading .favorite-toggle__surface {
  border-color: rgba(147, 197, 253, 0.82);
}

.favorite-toggle.is-loading .favorite-toggle__icon {
  animation: favorite-star-pulse 900ms ease-in-out infinite;
}

@keyframes favorite-star-pulse {
  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.12);
  }
}

.job-detail-salary {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -0.04em;
}

.job-detail-tags {
  margin-top: 0;
}

.job-detail-panel {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(3, 105, 161, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(239, 246, 255, 0.86));
}

.job-detail-panel h4 {
  margin: 0 0 14px;
  font-size: 16px;
}

.job-detail-reasons {
  margin-top: 0;
}

.job-detail-facts {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.job-detail-fact {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.12);
  display: grid;
  gap: 6px;
}

.job-detail-fact span {
  color: var(--text-muted);
  font-size: 12px;
}

.job-detail-fact strong {
  color: var(--text-strong);
  font-size: 15px;
}

.job-detail-description {
  margin: 0;
  color: var(--text-main);
  line-height: 1.85;
  white-space: pre-line;
}

.apply-resume-dialog__body {
  min-height: 220px;
  display: grid;
  gap: 16px;
}

.apply-resume-dialog__hint {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.apply-resume-list {
  display: grid;
  gap: 12px;
}

.apply-resume-card {
  width: 100%;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.apply-resume-card:hover,
.apply-resume-card.is-selected {
  transform: translateY(-1px);
  border-color: rgba(3, 105, 161, 0.28);
  box-shadow: 0 16px 24px rgba(15, 23, 42, 0.06);
}

.apply-resume-card__copy {
  display: grid;
  gap: 6px;
}

.apply-resume-card__copy strong {
  color: var(--text-strong);
}

.apply-resume-card__copy p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.apply-resume-card__badge {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(3, 105, 161, 0.1);
  color: var(--primary-deep);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.apply-resume-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.job-detail-footer {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.job-detail-footer__tip {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.7;
}

.job-detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.job-detail-apply,
.job-detail-chat {
  min-width: 160px;
}

.apply-button.is-applied {
  --el-button-bg-color: #e5e7eb;
  --el-button-border-color: #d1d5db;
  --el-button-text-color: #6b7280;
  --el-button-hover-bg-color: #e5e7eb;
  --el-button-hover-border-color: #d1d5db;
  --el-button-hover-text-color: #6b7280;
  --el-button-active-bg-color: #dfe4ea;
  --el-button-active-border-color: #cbd5e1;
  --el-button-active-text-color: #6b7280;
}

:deep(.job-detail-dialog) {
  width: min(820px, calc(100vw - 24px)) !important;
}

:deep(.job-detail-dialog .el-dialog) {
  overflow: hidden;
  border-radius: 28px;
  box-shadow:
    0 36px 90px rgba(15, 23, 42, 0.24),
    0 10px 26px rgba(3, 105, 161, 0.1);
  transform: translateZ(0);
  will-change: transform, opacity;
}

:deep(.job-detail-dialog .el-dialog__body) {
  padding-top: 10px;
}

:deep(.job-detail-overlay) {
  background: rgba(15, 23, 42, 0.42) !important;
  backdrop-filter: blur(10px) saturate(120%);
}

:deep(.job-detail-motion-enter-active),
:deep(.job-detail-motion-leave-active) {
  transition: opacity 220ms ease;
}

:deep(.job-detail-motion-enter-from),
:deep(.job-detail-motion-leave-to) {
  opacity: 0;
}

:deep(.job-detail-motion-enter-to),
:deep(.job-detail-motion-leave-from) {
  opacity: 1;
}

:deep(.job-detail-motion-enter-active .el-dialog),
:deep(.job-detail-motion-leave-active .el-dialog) {
  transform-origin: center center;
  transition:
    transform 260ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 220ms ease;
}

:deep(.job-detail-motion-enter-from .el-dialog),
:deep(.job-detail-motion-leave-to .el-dialog) {
  opacity: 0;
  transform: translate3d(0, 18px, 0) scale(0.975);
}

:deep(.job-detail-motion-enter-to .el-dialog),
:deep(.job-detail-motion-leave-from .el-dialog) {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
}

@media (max-width: 900px) {
  .search-panel__header,
  .discovery-results__header,
  .search-primary-row,
  .search-panel__footer,
  .search-panel__footer-actions,
  .toolbar-head,
  .toolbar-row,
  .job-footer,
  .job-detail-hero__head,
  .job-detail-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .dialog-grid,
  .search-filter-grid,
  .job-grid,
  .job-detail-facts {
    grid-template-columns: 1fr;
  }

  .search-panel__status,
  .search-panel__footer-actions,
  .discovery-results__summary {
    width: 100%;
    max-width: none;
    text-align: left;
    justify-items: start;
  }

  .search-panel__hint {
    text-align: left;
  }

  .apply-resume-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-primary-row__actions,
  .search-submit-button,
  .search-reset-button,
  .apply-resume-dialog__footer,
  .job-detail-actions,
  .job-detail-apply,
  .job-detail-chat {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .search-panel {
    padding: 20px;
  }

  .salary-range-group {
    grid-template-columns: 1fr;
  }

  .salary-range-group__divider {
    justify-self: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .job-card,
  .job-footer__hint,
  .favorite-toggle,
  .favorite-toggle__halo,
  .favorite-toggle__surface,
  .favorite-toggle__icon,
  .search-submit-button,
  :deep(.search-panel .el-input__wrapper),
  :deep(.search-panel .el-select__wrapper),
  :deep(.search-panel .el-cascader .el-input__wrapper) {
    transition: none;
  }

  .favorite-toggle.is-loading .favorite-toggle__icon {
    animation: none;
  }

  .job-card:hover,
  .job-card--interactive:active,
  .job-card--interactive:focus-visible,
  .job-card--interactive:hover .job-footer__hint,
  .job-card--interactive:focus-visible .job-footer__hint {
    transform: none;
  }

  :deep(.job-detail-motion-enter-active),
  :deep(.job-detail-motion-enter-active .el-dialog),
  :deep(.job-detail-motion-leave-active),
  :deep(.job-detail-motion-leave-active .el-dialog) {
    transition: none !important;
  }
}
</style>



