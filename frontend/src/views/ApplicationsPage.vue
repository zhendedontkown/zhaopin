<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import type { ApplicationRecord, PageResponse } from '../types'

const authStore = useAuthStore()
const router = useRouter()
const role = computed(() => authStore.role)

const loading = ref(false)
const applications = ref<ApplicationRecord[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 8, total: 0 })
const companyStatusFilter = ref('')
const statusDraft = reactive<Record<number, string>>({})

const statusOptions = [
  { label: '已投递', value: 'SUBMITTED' },
  { label: '企业已查看', value: 'VIEWED' },
  { label: '面试中', value: 'INTERVIEWING' },
  { label: '未通过', value: 'REJECTED' },
  { label: '已录用', value: 'ACCEPTED' },
]

function statusLabel(status: string) {
  return statusOptions.find((item) => item.value === status)?.label ?? status
}

function statusTagType(status: string) {
  if (status === 'REJECTED') return 'danger'
  if (status === 'ACCEPTED') return 'success'
  if (status === 'INTERVIEWING') return 'warning'
  if (status === 'VIEWED') return 'info'
  return undefined
}

async function fetchApplications() {
  if (role.value === 'ADMIN') {
    applications.value = []
    pagination.total = 0
    return
  }

  loading.value = true
  try {
    const url = role.value === 'COMPANY'
      ? '/company/applications'
      : '/jobseeker/applications'
    const response = await client.get(url, {
      params: {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        status: role.value === 'COMPANY' ? companyStatusFilter.value || undefined : undefined,
      },
    })
    const page = response.data as PageResponse<ApplicationRecord>
    applications.value = page.records
    pagination.total = page.total
    page.records.forEach((item) => {
      statusDraft[item.id] = item.status
    })
  } finally {
    loading.value = false
  }
}

async function updateStatus(record: ApplicationRecord) {
  await client.patch(`/company/applications/${record.id}/status`, { status: statusDraft[record.id] })
  await fetchApplications()
}

function enterChat(record: ApplicationRecord) {
  if (role.value === 'ADMIN') return
  const peerUserId = role.value === 'COMPANY' ? record.jobseekerUserId : record.companyUserId
  router.push({ path: '/chat', query: { peerUserId } })
}

onMounted(fetchApplications)
</script>

<template>
  <div class="section-grid">
    <section class="page-hero surface-card">
      <div class="page-hero__content">
        <span class="eyebrow">{{ role === 'COMPANY' ? 'Application Workflow' : 'My Applications' }}</span>
      <h2 class="page-title">{{ role === 'COMPANY' ? '投递处理中心' : '我的投递记录' }}</h2>
      <p class="page-subtitle">
        {{ role === 'COMPANY'
          ? '把查看、筛选、状态流转和候选人沟通放进一条连续的招聘流程里。'
          : '每一次投递都保留清晰状态，让你知道当前进度和下一步动作。' }}
      </p>
      </div>

      <div class="page-hero__aside">
        <div class="hero-stat">
          <span>{{ role === 'COMPANY' ? '处理总数' : '投递总数' }}</span>
          <strong>{{ pagination.total }}</strong>
        </div>
      </div>
    </section>

    <section v-if="role === 'COMPANY'" class="surface-card filter-panel">
      <div class="toolbar-row">
        <el-select v-model="companyStatusFilter" clearable placeholder="按状态筛选" style="width: 180px" @change="fetchApplications">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <span class="result-note">共 {{ pagination.total }} 条投递记录</span>
      </div>
    </section>

    <section class="application-grid">
      <article v-for="record in applications" :key="record.id" class="surface-card application-card">
        <div class="application-head">
          <div>
            <h3>{{ record.jobTitle }}</h3>
            <p>{{ record.companyName }}</p>
          </div>
          <el-tag :type="statusTagType(record.status)">{{ statusLabel(record.status) }}</el-tag>
        </div>

        <div class="application-meta">
          <span>投递时间：{{ record.appliedAt?.replace('T', ' ') }}</span>
          <span v-if="record.viewedAt">查看时间：{{ record.viewedAt?.replace('T', ' ') }}</span>
          <span>简历编号：{{ record.resumeId }}</span>
          <span>{{ role === 'COMPANY' ? `候选人账号 ID：${record.jobseekerUserId}` : `企业账号 ID：${record.companyUserId}` }}</span>
        </div>

        <div v-if="role === 'COMPANY'" class="company-actions">
          <el-select v-model="statusDraft[record.id]" style="width: 180px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" @click="updateStatus(record)">更新状态</el-button>
        </div>

        <div class="footer-row">
          <span>{{ role === 'COMPANY' ? '继续推进候选人流程' : '如有需要，可直接进入沟通' }}</span>
          <el-button text type="primary" @click="enterChat(record)">进入沟通</el-button>
        </div>
      </article>

      <el-empty v-if="!applications.length" description="当前还没有投递记录" />
    </section>

    <el-pagination
      background
      layout="prev, pager, next"
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @current-change="(page: number) => { pagination.pageNum = page; fetchApplications() }"
    />
  </div>
</template>

<style scoped>
.filter-panel,
.application-card {
  padding: 22px;
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
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

.application-head h3 {
  margin: 0;
}

.application-head p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.application-meta {
  margin: 18px 0;
  display: grid;
  gap: 10px;
  color: var(--text-main);
  line-height: 1.6;
}

.company-actions,
.footer-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.footer-row {
  margin-top: 18px;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .toolbar-row,
  .company-actions,
  .footer-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
