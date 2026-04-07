<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { ApplicationRecord, JobRecord, PageResponse } from '../types'

const authStore = useAuthStore()
const role = computed(() => authStore.role)
const loading = ref(false)
const dialogVisible = ref(false)
const editingJobId = ref<number | null>(null)
const jobseekerTab = ref('search')

const searchPagination = reactive({ pageNum: 1, pageSize: 6, total: 0 })
const companyPagination = reactive({ pageNum: 1, pageSize: 8, total: 0 })

const searchForm = reactive({
  keyword: '',
  category: '',
  location: '',
  salaryMin: undefined as number | undefined,
  salaryMax: undefined as number | undefined,
  sortBy: 'publishedAt',
  sortDirection: 'desc',
})

const companyFilter = reactive({
  status: '',
})

const companyJobs = ref<JobRecord[]>([])
const browseJobs = ref<JobRecord[]>([])
const recommendJobs = ref<JobRecord[]>([])
const recentAppliedJobIds = ref<number[]>([])
const duplicateApplyMessage = '同一岗位 7 天内仅允许投递一次'

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
  expireAt: '',
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' },
  { label: '已过期', value: 'EXPIRED' },
]

function statusLabel(status: string) {
  return statusOptions.find((item) => item.value === status)?.label ?? status
}

function statusTagType(status: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'OFFLINE') return 'warning'
  if (status === 'EXPIRED') return 'info'
  return ''
}

async function fetchBrowseJobs() {
  loading.value = true
  try {
    const response = await client.get('/jobs/search', {
      params: {
        ...searchForm,
        pageNum: searchPagination.pageNum,
        pageSize: searchPagination.pageSize,
      },
    })
    const page = response.data as PageResponse<JobRecord>
    browseJobs.value = page.records
    searchPagination.total = page.total
  } finally {
    loading.value = false
  }
}

async function fetchRecommendJobs() {
  if (role.value !== 'JOBSEEKER') return
  const response = await client.get('/jobs/recommend')
  recommendJobs.value = response.data as JobRecord[]
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
  await Promise.all([fetchRecentApplications(), fetchBrowseJobs(), fetchRecommendJobs()])
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
    expireAt: job.expireAt ?? '',
  })
  dialogVisible.value = true
}

async function saveCompanyJob() {
  if (editingJobId.value) {
    await client.put(`/company/jobs/${editingJobId.value}`, companyForm)
    ElMessage.success('岗位已更新')
  } else {
    await client.post('/company/jobs', companyForm)
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
  for (const collection of [browseJobs.value, recommendJobs.value]) {
    const target = collection.find((job) => job.id === jobId)
    if (target) {
      target.recentlyApplied = true
    }
  }
}

async function showDuplicateApplyDialog() {
  await ElMessageBox.alert(duplicateApplyMessage, '提示', {
    confirmButtonText: '知道了',
    type: 'warning',
  })
}

async function applyJob(jobId: number) {
  try {
    await client.post('/jobseeker/applications', { jobId })
    markJobAsRecentlyApplied(jobId)
    ElMessage.success('投递成功')
  } catch (error) {
    const message = String((error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
      || (error as { message?: string })?.message
      || '')
    if (message.includes('7') || message.includes('同一岗位')) {
      markJobAsRecentlyApplied(jobId)
      await showDuplicateApplyDialog()
    }
  }
}

async function handleApplyClick(job: JobRecord) {
  if (isRecentlyApplied(job)) {
    await showDuplicateApplyDialog()
    return
  }
  await applyJob(job.id)
}

function isWithinSevenDays(appliedAt?: string) {
  if (!appliedAt) return false
  const appliedTime = new Date(appliedAt).getTime()
  if (Number.isNaN(appliedTime)) return false
  return Date.now() - appliedTime < 7 * 24 * 60 * 60 * 1000
}

onMounted(fetchAll)
</script>

<template>
  <div class="section-grid">
    <section class="page-hero surface-card">
      <div class="page-hero__content">
        <span class="eyebrow">{{ role === 'COMPANY' ? 'Company Workspace' : 'Job Hall' }}</span>
        <h2 class="page-title">{{ role === 'COMPANY' ? '岗位管理' : '岗位大厅与智能推荐' }}</h2>
        <p class="page-subtitle">
          {{ role === 'COMPANY'
            ? '把岗位创建、编辑、发布和状态管理收进同一个操作视图。'
            : '结合关键词搜索、多条件筛选和匹配推荐，更快找到适合自己的岗位。' }}
        </p>
      </div>

      <div class="page-hero__aside">
        <div class="hero-stat">
          <span>{{ role === 'COMPANY' ? '岗位数量' : '当前模式' }}</span>
          <strong>{{ role === 'COMPANY' ? companyPagination.total : jobseekerTab === 'recommend' ? 'Recommend' : 'Search' }}</strong>
        </div>
      </div>
    </section>

    <template v-if="role === 'COMPANY'">
      <section class="surface-card toolbar-panel">
        <div class="toolbar-head">
          <div>
            <h3>岗位列表</h3>
            <p>清晰区分草稿、已发布、已下线和已过期岗位，并在这里集中处理。</p>
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
            <el-form-item label="经验要求"><el-input v-model="companyForm.experienceRequirement" placeholder="例如 3 年以上" /></el-form-item>
            <el-form-item label="学历要求"><el-input v-model="companyForm.educationRequirement" placeholder="例如 本科及以上" /></el-form-item>
            <el-form-item label="招聘人数"><el-input-number v-model="companyForm.headcount" :min="1" /></el-form-item>
            <el-form-item label="过期时间">
              <el-date-picker
                v-model="companyForm.expireAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择过期时间"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <el-form-item label="岗位描述（发布前不少于 200 字）">
            <el-input
              v-model="companyForm.description"
              type="textarea"
              :rows="8"
              placeholder="请写清楚业务背景、职责范围、任职要求和岗位亮点。"
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
      <el-tabs v-model="jobseekerTab">
        <el-tab-pane label="岗位搜索" name="search">
          <section class="surface-card search-panel">
            <div class="search-grid">
              <el-input v-model="searchForm.keyword" clearable placeholder="搜索岗位名称、描述或关键词" />
              <el-input v-model="searchForm.category" clearable placeholder="岗位类别" />
              <el-input v-model="searchForm.location" clearable placeholder="工作地点" />
              <el-input-number v-model="searchForm.salaryMin" :min="0" placeholder="最低薪资" />
              <el-input-number v-model="searchForm.salaryMax" :min="0" placeholder="最高薪资" />
              <el-select v-model="searchForm.sortBy">
                <el-option label="按发布时间" value="publishedAt" />
                <el-option label="按薪资" value="salary" />
                <el-option label="按匹配度" value="matchScore" />
              </el-select>
            </div>
            <div class="toolbar-row">
              <span class="result-note">共 {{ searchPagination.total }} 个搜索结果</span>
              <el-button type="primary" :loading="loading" @click="fetchBrowseJobs">搜索岗位</el-button>
            </div>
          </section>

          <section class="job-grid">
            <article v-for="job in browseJobs" :key="job.id" class="surface-card job-card">
              <div class="job-head">
                <div>
                  <h3>{{ job.title }}</h3>
                  <p>{{ job.companyName }} · {{ job.location }}</p>
                </div>
                <el-tag v-if="job.matchScore !== undefined" type="success">{{ job.matchScore }} 分</el-tag>
              </div>

              <div class="salary-line">{{ job.salaryMin }} - {{ job.salaryMax }} 元/月</div>

              <div class="job-meta">
                <span>{{ job.category }}</span>
                <span>{{ job.experienceRequirement }}</span>
                <span>{{ job.educationRequirement }}</span>
              </div>

              <p class="job-desc">{{ job.description }}</p>

              <div class="job-footer">
                <span>岗位编号：{{ job.jobCode }}</span>
                <el-button
                  :type="isRecentlyApplied(job) ? 'info' : 'primary'"
                  :class="['apply-button', isRecentlyApplied(job) ? 'is-applied' : '']"
                  @click="handleApplyClick(job)"
                >
                  {{ isRecentlyApplied(job) ? '已投递' : '立即投递' }}
                </el-button>
              </div>
            </article>

            <el-empty v-if="!browseJobs.length" description="没有找到符合条件的岗位" />
          </section>

          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="searchPagination.pageNum"
            :page-size="searchPagination.pageSize"
            :total="searchPagination.total"
            @current-change="(page: number) => { searchPagination.pageNum = page; fetchBrowseJobs() }"
          />
        </el-tab-pane>

        <el-tab-pane label="推荐岗位" name="recommend">
          <section class="job-grid">
            <article v-for="job in recommendJobs" :key="job.id" class="surface-card job-card">
              <div class="job-head">
                <div>
                  <h3>{{ job.title }}</h3>
                  <p>{{ job.companyName }} · {{ job.location }}</p>
                </div>
                <el-tag type="success">{{ job.matchScore }} 分</el-tag>
              </div>

              <div class="salary-line">{{ job.salaryMin }} - {{ job.salaryMax }} 元/月</div>

              <div class="reason-list">
                <el-tag v-for="reason in job.matchReasons" :key="reason" size="small" type="success">{{ reason }}</el-tag>
              </div>

              <p class="job-desc">{{ job.description }}</p>

              <div class="job-footer">
                <span>岗位编号：{{ job.jobCode }}</span>
                <el-button
                  :type="isRecentlyApplied(job) ? 'info' : 'primary'"
                  :class="['apply-button', isRecentlyApplied(job) ? 'is-applied' : '']"
                  @click="handleApplyClick(job)"
                >
                  {{ isRecentlyApplied(job) ? '已投递' : '立即投递' }}
                </el-button>
              </div>
            </article>

            <el-empty v-if="!recommendJobs.length" description="请先完善简历，再查看推荐岗位" />
          </section>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.toolbar-panel,
.table-panel,
.search-panel,
.job-card {
  padding: 22px;
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

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-grid,
.search-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.job-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.job-card:hover {
  transform: translateY(-2px);
  border-color: rgba(3, 105, 161, 0.16);
  box-shadow: 0 16px 32px rgba(3, 105, 161, 0.08);
}

.job-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.job-head h3 {
  margin: 0;
}

.job-head p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.salary-line {
  margin-top: 18px;
  font-size: 24px;
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

.job-desc {
  margin: 16px 0 0;
  color: var(--text-main);
  line-height: 1.75;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.job-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  color: var(--text-muted);
  font-size: 13px;
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

@media (max-width: 1280px) {
  .job-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .toolbar-head,
  .toolbar-row,
  .job-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .dialog-grid,
  .search-grid,
  .job-grid {
    grid-template-columns: 1fr;
  }
}
</style>
