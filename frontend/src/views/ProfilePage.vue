<script setup lang="ts">
import { ArrowRight, ChatDotRound, Files, Lock, OfficeBuilding, Promotion, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import ResumeExportDialog from '../components/ResumeExportDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'
import type {
  ApplicationRecord,
  JobRecord,
  PageResponse,
  PasswordChangePayload,
  ResumeDetail,
  SavedResumeDetail,
  SavedResumeSummary,
  UserProfileResponse,
} from '../types'

type QuickAction = { key: string; title: string; description: string; path: string; icon: Component }

const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const loading = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const favoriteLoading = ref(false)
const savedResumeLoading = ref(false)
const savedResumeDeletingId = ref<number | null>(null)
const recentJobs = ref<JobRecord[]>([])
const recentApplications = ref<ApplicationRecord[]>([])
const favoriteJobs = ref<JobRecord[]>([])
const savedResumes = ref<SavedResumeSummary[]>([])
const companyJobTotal = ref(0)
const companyApplicationTotal = ref(0)
const jobseekerApplicationTotal = ref(0)
const resumeCompleteness = ref(0)
const resumeMissingItems = ref<string[]>([])
const favoritePagination = reactive({ pageNum: 1, pageSize: 5, total: 0 })
const savedResumePagination = reactive({ pageNum: 1, pageSize: 5, total: 0 })
const savedResumePreviewVisible = ref(false)
const savedResumePreviewLoading = ref(false)
const savedResumePreviewError = ref('')
const savedResumePreview = ref<ResumeDetail | null>(null)

const jobseekerForm = reactive({
  fullName: '',
  phone: '',
  email: '',
})

const companyForm = reactive({
  companyName: '',
  unifiedSocialCreditCode: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  description: '',
})

const passwordForm = reactive<PasswordChangePayload>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const isJobseeker = computed(() => authStore.role === 'JOBSEEKER')
const isCompany = computed(() => authStore.role === 'COMPANY')
const currentProfile = computed(() => authStore.profile)
const currentJobseekerProfile = computed(() => authStore.profile?.jobseekerProfile ?? null)
const currentCompanyProfile = computed(() => authStore.profile?.companyProfile ?? null)

const quickActions = computed<QuickAction[]>(() => (
  isCompany.value
    ? [
        { key: 'dashboard', title: '返回工作台', description: '查看企业招聘概览和近期动态。', path: '/dashboard', icon: OfficeBuilding },
        { key: 'jobs', title: '岗位管理', description: '继续发布、编辑和维护岗位。', path: '/jobs', icon: Search },
        { key: 'applications', title: '投递处理', description: '集中处理候选人投递与状态。', path: '/applications', icon: Files },
        { key: 'chat', title: '在线沟通', description: '继续与求职者沟通安排。', path: '/chat', icon: ChatDotRound },
      ]
    : [
        { key: 'jobs', title: '岗位大厅', description: '继续浏览岗位并按条件筛选。', path: '/jobs', icon: Search },
        { key: 'resume', title: '简历创作', description: '维护你的在线简历内容。', path: '/resume', icon: Promotion },
        { key: 'applications', title: '投递记录', description: '查看最近投递进展。', path: '/applications', icon: Files },
        { key: 'chat', title: '在线沟通', description: '继续和企业沟通岗位细节。', path: '/chat', icon: ChatDotRound },
      ]
))

const identityTitle = computed(() => (
  isCompany.value
    ? currentCompanyProfile.value?.companyName || currentProfile.value?.displayName || '企业账号'
    : currentJobseekerProfile.value?.fullName || currentProfile.value?.displayName || '求职者账号'
))

const identitySubtitle = computed(() => (
  isCompany.value
    ? `${companyAuditStatusLabel(currentCompanyProfile.value?.auditStatus)} | ${currentCompanyProfile.value?.contactPerson || '待完善联系人'}`
    : '个人资料'
))

const headlineStats = computed(() => (
  isCompany.value
    ? [
        { label: '岗位总数', value: String(companyJobTotal.value) },
        { label: '投递总数', value: String(companyApplicationTotal.value) },
        { label: '审核状态', value: companyAuditStatusLabel(currentCompanyProfile.value?.auditStatus) },
      ]
    : [
        { label: '简历完整度', value: `${resumeCompleteness.value}%` },
        { label: '投递总数', value: String(jobseekerApplicationTotal.value) },
        { label: '待完善项', value: String(resumeMissingItems.value.length) },
      ]
))

function favoriteJobStatusLabel(status?: string) {
  if (status === 'PUBLISHED') return '招聘中'
  if (status === 'EXPIRED') return '已过期'
  if (status === 'OFFLINE') return '已下线'
  if (status === 'DRAFT') return '草稿'
  return status || '未知状态'
}

function favoriteJobStatusType(status?: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'EXPIRED') return 'info'
  return 'warning'
}

function savedResumeStatusLabel(item: SavedResumeSummary) {
  return item.completeFlag ? '可投递' : '未完善'
}

function formatDateTime(value?: string) {
  if (!value) return '未更新'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

async function fetchSavedResumes(pageNum = savedResumePagination.pageNum) {
  if (!isJobseeker.value) return
  savedResumeLoading.value = true
  savedResumePagination.pageNum = pageNum
  try {
    const response = await client.get('/jobseeker/saved-resumes', {
      params: {
        pageNum: savedResumePagination.pageNum,
        pageSize: savedResumePagination.pageSize,
      },
    })
    const page = response.data as PageResponse<SavedResumeSummary>
    savedResumes.value = page.records
    savedResumePagination.total = page.total
  } catch (error) {
    savedResumes.value = []
    savedResumePagination.total = 0
    ElMessage.error(
      String(
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
          || (error as { message?: string })?.message
          || '我的简历加载失败',
      ),
    )
  } finally {
    savedResumeLoading.value = false
  }
}

async function fetchFavoriteJobs(pageNum = favoritePagination.pageNum) {
  if (!isJobseeker.value) return
  favoriteLoading.value = true
  favoritePagination.pageNum = pageNum
  try {
    const response = await client.get('/jobseeker/favorites', {
      params: {
        pageNum: favoritePagination.pageNum,
        pageSize: favoritePagination.pageSize,
      },
    })
    const page = response.data as PageResponse<JobRecord>
    favoriteJobs.value = page.records
    favoritePagination.total = page.total
  } finally {
    favoriteLoading.value = false
  }
}

async function loadPageData() {
  loading.value = true
  try {
    const profile = await authStore.fetchProfile(true)
    hydrateProfileForms(profile as UserProfileResponse | null)

    if (isCompany.value) {
      const [jobResponse, applicationResponse] = await Promise.all([
        client.get('/company/jobs', { params: { pageNum: 1, pageSize: 5 } }),
        client.get('/company/applications', { params: { pageNum: 1, pageSize: 5 } }),
      ])
      recentJobs.value = jobResponse.data.records as JobRecord[]
      companyJobTotal.value = Number(jobResponse.data.total || 0)
      recentApplications.value = applicationResponse.data.records as ApplicationRecord[]
      companyApplicationTotal.value = Number(applicationResponse.data.total || 0)
      return
    }

    const [resumeResponse, applicationResponse, favoriteResponse] = await Promise.all([
      client.get('/jobseeker/resume'),
      client.get('/jobseeker/applications', { params: { pageNum: 1, pageSize: 5 } }),
      client.get('/jobseeker/favorites', {
        params: {
          pageNum: favoritePagination.pageNum,
          pageSize: favoritePagination.pageSize,
        },
      }),
    ])

    const savedResumeResponse = await client.get('/jobseeker/saved-resumes', {
      params: {
        pageNum: savedResumePagination.pageNum,
        pageSize: savedResumePagination.pageSize,
      },
    }).catch(() => null)

    const resumeDetail = resumeResponse.data as ResumeDetail
    const favoritePage = favoriteResponse.data as PageResponse<JobRecord>
    resumeCompleteness.value = Number(resumeDetail.completenessScore || 0)
    resumeMissingItems.value = resumeDetail.missingItems || []
    recentApplications.value = applicationResponse.data.records as ApplicationRecord[]
    jobseekerApplicationTotal.value = Number(applicationResponse.data.total || 0)
    favoriteJobs.value = favoritePage.records
    favoritePagination.total = favoritePage.total

    if (savedResumeResponse) {
      const savedResumePage = savedResumeResponse.data as PageResponse<SavedResumeSummary>
      savedResumes.value = savedResumePage.records
      savedResumePagination.total = savedResumePage.total
    } else {
      savedResumes.value = []
      savedResumePagination.total = 0
    }
  } finally {
    loading.value = false
  }
}

function hydrateProfileForms(profile = authStore.profile) {
  if (!profile) return

  if (profile.jobseekerProfile) {
    Object.assign(jobseekerForm, {
      fullName: profile.jobseekerProfile.fullName || '',
      phone: profile.jobseekerProfile.phone || profile.phone || '',
      email: profile.jobseekerProfile.email || profile.email || '',
    })
  }

  if (profile.companyProfile) {
    Object.assign(companyForm, {
      companyName: profile.companyProfile.companyName || '',
      unifiedSocialCreditCode: profile.companyProfile.unifiedSocialCreditCode || '',
      contactPerson: profile.companyProfile.contactPerson || '',
      phone: profile.companyProfile.phone || profile.phone || '',
      email: profile.companyProfile.email || profile.email || '',
      address: profile.companyProfile.address || '',
      description: profile.companyProfile.description || '',
    })
  }
}

async function saveProfile() {
  if (isCompany.value) {
    const previous = currentCompanyProfile.value
    const coreChanged = previous
      ? previous.companyName !== companyForm.companyName || previous.unifiedSocialCreditCode !== companyForm.unifiedSocialCreditCode
      : false

    savingProfile.value = true
    try {
      const response = await client.put('/company/profile', companyForm)
      const updatedProfile = response.data as UserProfileResponse
      authStore.applyProfile(updatedProfile)
      hydrateProfileForms(updatedProfile)
      await loadPageData()
      ElMessage.success(coreChanged ? '企业资料已更新，审核状态已重置为待审核' : '企业资料已更新')
    } finally {
      savingProfile.value = false
    }
    return
  }

  savingProfile.value = true
  try {
    const response = await client.put('/jobseeker/profile', {
      fullName: jobseekerForm.fullName,
      phone: jobseekerForm.phone,
      email: jobseekerForm.email,
    })
    authStore.applyProfile(response.data as UserProfileResponse)
    hydrateProfileForms(response.data as UserProfileResponse)
    ElMessage.success('求职者资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  savingPassword.value = true
  try {
    await client.put('/auth/password', passwordForm)
    ElMessage.success('密码修改成功，请重新登录')
    notificationStore.reset()
    authStore.logout()
    await router.push('/login')
  } finally {
    savingPassword.value = false
  }
}

async function openFavoriteJob(job: JobRecord) {
  await router.push({
    path: '/jobs',
    query: { detailJobId: String(job.id) },
  })
}

async function removeFavorite(job: JobRecord) {
  await client.delete(`/jobseeker/favorites/${job.id}`)
  ElMessage.success('已取消收藏')
  const nextPage = favoriteJobs.value.length === 1 && favoritePagination.pageNum > 1
    ? favoritePagination.pageNum - 1
    : favoritePagination.pageNum
  await fetchFavoriteJobs(nextPage)
}

async function openSavedResumePreview(item: SavedResumeSummary) {
  savedResumePreviewVisible.value = true
  savedResumePreviewLoading.value = true
  savedResumePreviewError.value = ''
  savedResumePreview.value = null
  try {
    const response = await client.get(`/jobseeker/saved-resumes/${item.id}`)
    const detail = response.data as SavedResumeDetail
    savedResumePreview.value = detail.resumeDetail
  } catch (error) {
    savedResumePreviewError.value = String(
      (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '简历加载失败',
    )
  } finally {
    savedResumePreviewLoading.value = false
  }
}

function resetSavedResumePreview() {
  savedResumePreviewLoading.value = false
  savedResumePreviewError.value = ''
  savedResumePreview.value = null
}

async function editSavedResume(item: SavedResumeSummary) {
  await router.push({
    path: '/resume',
    query: {
      mode: 'edit',
      savedResumeId: String(item.id),
    },
  })
}

async function deleteSavedResume(item: SavedResumeSummary) {
  try {
    await ElMessageBox.confirm(`确定要删除简历“${item.name}”吗？`, '删除简历', {
      type: 'warning',
      confirmButtonText: '是',
      cancelButtonText: '否',
      closeOnClickModal: false,
    })
  } catch {
    return
  }

  savedResumeDeletingId.value = item.id
  try {
    await client.delete(`/jobseeker/saved-resumes/${item.id}`)
    ElMessage.success('简历已删除')
    const nextPage = savedResumes.value.length === 1 && savedResumePagination.pageNum > 1
      ? savedResumePagination.pageNum - 1
      : savedResumePagination.pageNum
    await fetchSavedResumes(nextPage)
  } finally {
    savedResumeDeletingId.value = null
  }
}

function goTo(path: string) {
  router.push(path)
}

function companyAuditStatusLabel(status?: string) {
  if (status === 'APPROVED') return '已认证'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'PENDING') return '待审核'
  return status || '待完善'
}

function profileInitial(title: string) {
  return title.slice(0, 1) || '我'
}

onMounted(loadPageData)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div class="profile-layout">
      <aside class="profile-sidebar">
        <article class="surface-card profile-card hero-card">
          <span class="eyebrow">{{ isCompany ? '企业资料' : '个人资料' }}</span>
          <div class="profile-avatar">{{ profileInitial(identityTitle) }}</div>
          <h2 class="identity-title">{{ identityTitle }}</h2>
          <p class="identity-subtitle">{{ identitySubtitle }}</p>

          <div class="sidebar-stats">
            <div v-for="item in headlineStats" :key="item.label" class="sidebar-stat">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </article>

        <article class="surface-card profile-card">
          <div class="panel-head">
            <div>
              <h3>快捷操作</h3>
            </div>
          </div>

          <div class="shortcut-list">
            <button
              v-for="action in quickActions"
              :key="action.key"
              type="button"
              class="shortcut-item"
              @click="goTo(action.path)"
            >
              <span class="shortcut-item__icon">
                <el-icon><component :is="action.icon" /></el-icon>
              </span>
              <span class="shortcut-item__copy">
                <strong>{{ action.title }}</strong>
                <small>{{ action.description }}</small>
              </span>
              <el-icon class="shortcut-item__arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </article>
      </aside>

      <main class="profile-main">
        <section v-if="isJobseeker" class="top-grid">
          <article class="surface-card profile-card">
            <div class="panel-head">
              <div>
                <h3>我的简历</h3>
              </div>
            </div>

            <div v-loading="savedResumeLoading" class="list-panel">
              <el-empty v-if="!savedResumes.length" description="还没有保存的简历">
                <el-button type="primary" plain @click="goTo('/resume')">去保存一份简历</el-button>
              </el-empty>

              <template v-else>
                <div class="saved-resume-list">
                  <article
                    v-for="item in savedResumes"
                    :key="item.id"
                    class="saved-resume-item"
                  >
                    <button
                      type="button"
                      class="saved-resume-item__main"
                      @click="openSavedResumePreview(item)"
                    >
                      <div class="saved-resume-item__copy">
                        <strong>{{ item.name }}</strong>
                        <p>完整度 {{ item.completenessScore }}% · {{ savedResumeStatusLabel(item) }}</p>
                        <span>最近更新 {{ formatDateTime(item.updatedAt) }}</span>
                      </div>
                      <el-icon class="saved-resume-item__arrow"><ArrowRight /></el-icon>
                    </button>

                    <div class="saved-resume-item__actions">
                      <el-button text type="primary" @click="editSavedResume(item)">修改</el-button>
                      <el-button
                        text
                        type="danger"
                        :loading="savedResumeDeletingId === item.id"
                        @click="deleteSavedResume(item)"
                      >
                        删除
                      </el-button>
                    </div>
                  </article>
                </div>

                <el-pagination
                  v-if="savedResumePagination.total > savedResumePagination.pageSize"
                  background
                  layout="prev, pager, next"
                  :current-page="savedResumePagination.pageNum"
                  :page-size="savedResumePagination.pageSize"
                  :total="savedResumePagination.total"
                  @current-change="(page: number) => fetchSavedResumes(page)"
                />
              </template>
            </div>
          </article>

          <article class="surface-card profile-card">
            <div class="panel-head">
              <div>
                <h3>我的收藏</h3>
              </div>
            </div>

            <div v-loading="favoriteLoading" class="list-panel">
              <el-empty v-if="!favoriteJobs.length" description="还没有收藏岗位">
                <el-button type="primary" plain @click="goTo('/jobs')">去岗位大厅看看</el-button>
              </el-empty>

              <template v-else>
                <div class="favorite-list">
                  <article v-for="job in favoriteJobs" :key="job.id" class="favorite-item">
                    <div class="favorite-item__copy">
                      <strong>{{ job.title }}</strong>
                      <p>{{ job.companyName }} · {{ job.location }}</p>
                      <span>{{ job.salaryMin }} - {{ job.salaryMax }} 元/月</span>
                    </div>

                    <div class="favorite-item__meta">
                      <el-tag :type="favoriteJobStatusType(job.status)">{{ favoriteJobStatusLabel(job.status) }}</el-tag>
                      <div class="favorite-item__actions">
                        <el-button text type="primary" @click="openFavoriteJob(job)">查看岗位</el-button>
                        <el-button text type="danger" @click="removeFavorite(job)">取消收藏</el-button>
                      </div>
                    </div>
                  </article>
                </div>

                <el-pagination
                  v-if="favoritePagination.total > favoritePagination.pageSize"
                  background
                  layout="prev, pager, next"
                  :current-page="favoritePagination.pageNum"
                  :page-size="favoritePagination.pageSize"
                  :total="favoritePagination.total"
                  @current-change="(page: number) => fetchFavoriteJobs(page)"
                />
              </template>
            </div>
          </article>
        </section>

        <section v-else class="top-grid">
          <article class="surface-card profile-card">
            <div class="panel-head">
              <div>
                <h3>最近岗位</h3>
              </div>
            </div>

            <div class="simple-list">
              <el-empty v-if="!recentJobs.length" description="还没有岗位数据" />
              <article v-for="job in recentJobs" :key="job.id" class="simple-item">
                <strong>{{ job.title }}</strong>
                <p>{{ job.location }} · {{ job.salaryMin }} - {{ job.salaryMax }} 元/月</p>
              </article>
            </div>
          </article>

          <article class="surface-card profile-card">
            <div class="panel-head">
              <div>
                <h3>最近投递</h3>
              </div>
            </div>

            <div class="simple-list">
              <el-empty v-if="!recentApplications.length" description="还没有投递数据" />
              <article v-for="item in recentApplications" :key="item.id" class="simple-item">
                <strong>{{ item.jobTitle }}</strong>
                <p>{{ item.companyName }} · {{ item.statusText }}</p>
              </article>
            </div>
          </article>
        </section>

        <section class="settings-grid">
          <article class="surface-card profile-card">
            <div class="panel-head">
              <div>
                <h3>资料设置</h3>
              </div>
            </div>

            <el-form v-if="isJobseeker" label-position="top" class="settings-form" :model="jobseekerForm">
              <div class="form-grid">
                <el-form-item label="姓名">
                  <el-input v-model.trim="jobseekerForm.fullName" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model.trim="jobseekerForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model.trim="jobseekerForm.email" />
                </el-form-item>
              </div>
            </el-form>

            <el-form v-else label-position="top" class="settings-form" :model="companyForm">
              <div class="form-grid">
                <el-form-item label="企业名称">
                  <el-input v-model.trim="companyForm.companyName" />
                </el-form-item>
                <el-form-item label="统一社会信用代码">
                  <el-input v-model.trim="companyForm.unifiedSocialCreditCode" maxlength="18" show-word-limit />
                </el-form-item>
                <el-form-item label="联系人">
                  <el-input v-model.trim="companyForm.contactPerson" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model.trim="companyForm.phone" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model.trim="companyForm.email" />
                </el-form-item>
                <el-form-item label="联系地址">
                  <el-input v-model.trim="companyForm.address" />
                </el-form-item>
                <el-form-item class="full-span" label="企业简介">
                  <el-input v-model.trim="companyForm.description" type="textarea" :rows="5" maxlength="500" show-word-limit />
                </el-form-item>
              </div>
            </el-form>

            <div class="card-actions">
              <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
            </div>
          </article>

          <article class="surface-card profile-card">
            <div class="panel-head">
              <div>
                <h3>账号安全</h3>
              </div>
            </div>

            <el-form label-position="top" class="settings-form" :model="passwordForm">
              <div class="form-grid form-grid--single">
                <el-form-item label="当前密码">
                  <el-input v-model="passwordForm.currentPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认新密码">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                </el-form-item>
              </div>
            </el-form>

            <div class="password-rule">
              <el-icon><Lock /></el-icon>
              <div>
                <strong>密码规则</strong>
                <p>新密码长度需在 8 到 32 位之间，且不能与当前密码相同。</p>
              </div>
            </div>

            <div class="card-actions">
              <el-button type="primary" :loading="savingPassword" @click="savePassword">修改密码</el-button>
            </div>
          </article>
        </section>
      </main>
    </div>

    <ResumeExportDialog
      v-model="savedResumePreviewVisible"
      mode="preview"
      :loading="savedResumePreviewLoading"
      :error="savedResumePreviewError"
      :detail="savedResumePreview"
      title="查看简历"
      subtitle="这里展示你已保存的在线简历。"
      @closed="resetSavedResumePreview"
    />
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100%;
}

.profile-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.profile-sidebar,
.profile-main {
  display: grid;
  gap: 20px;
}

.surface-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(217, 226, 241, 0.9);
  border-radius: 24px;
  box-shadow: 0 16px 40px rgba(90, 126, 183, 0.08);
}

.profile-card {
  padding: 20px;
}

.hero-card {
  background: linear-gradient(180deg, rgba(240, 249, 255, 0.98), rgba(232, 244, 255, 0.94));
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(29, 120, 189, 0.08);
  color: #2a6d9b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.profile-avatar {
  width: 64px;
  height: 64px;
  margin-top: 18px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1790d8, #1570b9);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}

.identity-title {
  margin: 14px 0 8px;
  font-size: 22px;
  color: #162033;
}

.identity-subtitle {
  margin: 0;
  color: #6b7a90;
  line-height: 1.6;
}

.sidebar-stats {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.sidebar-stat {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(223, 232, 244, 0.9);
}

.sidebar-stat span {
  color: #6f7f94;
  font-size: 13px;
}

.sidebar-stat strong {
  color: #182235;
  font-size: 22px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  color: #162033;
}

.panel-head p {
  margin: 8px 0 0;
  color: #6f7f94;
  line-height: 1.7;
}

.shortcut-list,
.saved-resume-list,
.favorite-list,
.simple-list {
  display: grid;
  gap: 12px;
}

.shortcut-item,
.saved-resume-item {
  width: 100%;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  border-radius: 18px;
}

.shortcut-item {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  cursor: pointer;
}

.saved-resume-item {
  overflow: hidden;
}

.saved-resume-item__main {
  width: 100%;
  padding: 16px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  cursor: pointer;
}

.saved-resume-item__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 16px 14px;
  border-top: 1px solid rgba(223, 232, 244, 0.75);
}

.shortcut-item__icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(25, 129, 197, 0.08);
  color: #1570b9;
  font-size: 18px;
}

.shortcut-item__copy,
.saved-resume-item__copy {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 6px;
}

.shortcut-item__copy strong,
.saved-resume-item__copy strong,
.favorite-item__copy strong,
.simple-item strong {
  color: #162033;
  font-size: 16px;
}

.shortcut-item__copy small,
.saved-resume-item__copy span,
.favorite-item__copy p,
.simple-item p {
  color: #6f7f94;
  line-height: 1.6;
}

.saved-resume-item__copy p {
  margin: 0;
  color: #6f7f94;
  line-height: 1.6;
}

.shortcut-item__arrow,
.saved-resume-item__arrow {
  color: #8ea1bb;
}

.top-grid,
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.list-panel {
  display: grid;
  gap: 16px;
}

.favorite-item,
.simple-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(223, 232, 244, 0.95);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.favorite-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.favorite-item__copy,
.favorite-item__meta {
  display: grid;
  gap: 8px;
}

.favorite-item__copy span {
  color: #50627a;
}

.favorite-item__actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.form-grid--single {
  grid-template-columns: 1fr;
}

.full-span {
  grid-column: 1 / -1;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.password-rule {
  margin-top: 8px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 240, 240, 0.92);
  border: 1px solid rgba(245, 193, 193, 0.9);
  display: flex;
  gap: 12px;
  color: #c94a4a;
}

.password-rule strong {
  display: block;
  margin-bottom: 4px;
}

.password-rule p {
  margin: 0;
  line-height: 1.7;
}

@media (max-width: 1200px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .top-grid,
  .settings-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .favorite-item {
    grid-template-columns: 1fr;
    display: grid;
  }

  .favorite-item__actions,
  .card-actions {
    justify-content: flex-start;
  }
}
</style>
