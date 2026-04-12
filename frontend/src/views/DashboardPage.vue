<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import client from '../api/client'
import { getApplicationStatusLabel, getApplicationStatusMeta } from '../constants/applicationStatus'
import { useAuthStore } from '../stores/auth'
import type { ApplicationRecord, JobRecord } from '../types'

const authStore = useAuthStore()
const role = computed(() => authStore.role)
const dashboard = ref<Record<string, number>>({})
const jobs = ref<JobRecord[]>([])
const applications = ref<ApplicationRecord[]>([])

const heroTitle = computed(() => {
  if (role.value === 'ADMIN') return '系统总览'
  if (role.value === 'COMPANY') return '招聘工作台'
  return '求职工作台'
})

const heroDescription = computed(() => {
  if (role.value === 'ADMIN') return '集中查看系统运行情况，并优先处理企业审核和岗位监管。'
  if (role.value === 'COMPANY') return ''
  return '查看最近发布的岗位列表，并跟进最近投递进展。'
})

const metrics = computed(() => {
  if (role.value === 'ADMIN') {
    return Object.entries(dashboard.value).map(([label, value]) => ({ label, value }))
  }

  if (role.value === 'COMPANY') {
    return [
      { label: '当前岗位数', value: jobs.value.length },
      { label: '近期投递数', value: applications.value.length },
    ]
  }

  return [
    { label: '展示岗位数', value: jobs.value.length },
    { label: '我的投递数', value: applications.value.length },
    { label: '当前身份', value: '求职者端' },
  ]
})

async function fetchData() {
  if (role.value === 'ADMIN') {
    const response = await client.get('/admin/dashboard')
    dashboard.value = response.data as Record<string, number>
    return
  }

  if (role.value === 'COMPANY') {
    const [jobResponse, applicationResponse] = await Promise.all([
      client.get('/company/jobs', { params: { pageNum: 1, pageSize: 5 } }),
      client.get('/company/applications', { params: { pageNum: 1, pageSize: 5 } }),
    ])
    jobs.value = jobResponse.data.records as JobRecord[]
    applications.value = applicationResponse.data.records as ApplicationRecord[]
    return
  }

  const [jobResponse, applicationResponse] = await Promise.all([
    client.get('/jobs/search', { params: { pageNum: 1, pageSize: 5, sortKey: 'default' } }),
    client.get('/jobseeker/applications', { params: { pageNum: 1, pageSize: 5 } }),
  ])
  jobs.value = (jobResponse.data as { records: JobRecord[] }).records
  applications.value = applicationResponse.data.records as ApplicationRecord[]
}

function statusLabel(status: string) {
  return getApplicationStatusLabel(status)
}

function statusDescription(status: string) {
  return getApplicationStatusMeta(status, role.value === 'COMPANY' ? 'company' : 'jobseeker').description
}

onMounted(fetchData)
</script>

<template>
  <div class="section-grid">
    <section class="workspace-hero surface-card">
      <div class="workspace-copy">
        <span class="eyebrow">{{ authStore.workspaceLabel || '工作区' }}</span>
        <h2 class="page-title">{{ heroTitle }}</h2>
        <p v-if="heroDescription" class="page-subtitle">{{ heroDescription }}</p>
      </div>
    </section>

    <section class="kpi-grid">
      <div v-for="item in metrics" :key="item.label" class="kpi-card">
        <span class="kpi-label">{{ item.label }}</span>
        <strong class="kpi-value">{{ item.value }}</strong>
      </div>
    </section>

    <section class="split-panel">
      <div class="surface-card board-panel">
        <div class="panel-head">
          <div>
            <h3>{{ role === 'COMPANY' ? '岗位动态' : role === 'ADMIN' ? '系统指标明细' : '最新岗位' }}</h3>
            <p v-if="role !== 'COMPANY'">
              {{ role === 'ADMIN'
                ? '管理员可以从这里快速浏览核心统计项，判断系统当前状态。'
                : '默认展示已发布且未过期岗位，帮助你快速开始浏览。' }}
            </p>
          </div>
        </div>

        <div v-if="role === 'ADMIN'" class="meta-list">
          <div v-for="(value, key) in dashboard" :key="key" class="list-row">
            <div>
              <strong>{{ key }}</strong>
              <p>当前统计值</p>
            </div>
            <span class="value-chip">{{ value }}</span>
          </div>
        </div>

        <div v-else class="meta-list">
          <el-empty v-if="!jobs.length" description="当前还没有可展示的数据" />
          <div v-for="job in jobs" :key="job.id" class="list-row">
            <div>
              <strong>{{ job.title }}</strong>
              <p>{{ job.companyName || '我的岗位' }} · {{ job.location }} · {{ job.salaryMin }} - {{ job.salaryMax }}</p>
            </div>
            <span class="value-chip">{{ role === 'COMPANY' ? statusLabel(job.status) : '已发布' }}</span>
          </div>
        </div>
      </div>

      <div class="surface-card board-panel">
        <div class="panel-head">
          <div>
            <h3>{{ role === 'ADMIN' ? '处理建议' : '最近投递' }}</h3>
            <p v-if="role !== 'COMPANY'">
              {{ role === 'ADMIN'
                ? '当前系统管理页面更适合优先处理企业审核、岗位下线和日常巡检。'
                : '这里保留最近的投递状态，帮助你快速理解当前招聘进度。' }}
            </p>
          </div>
        </div>

        <template v-if="role === 'ADMIN'">
          <div class="guide-card">
            <strong>管理员下一步建议</strong>
            <ul>
              <li>优先检查待审核企业和状态异常的岗位。</li>
              <li>关注岗位总量、投递总量和消息增长是否异常。</li>
              <li>用系统管理页完成审核和强制下线操作。</li>
            </ul>
          </div>
        </template>

        <template v-else>
          <el-empty v-if="!applications.length" description="当前还没有新的投递记录" />
          <div v-for="item in applications" :key="item.id" class="list-row">
            <div>
              <strong>{{ item.jobTitle }}</strong>
              <p>{{ item.companyName }} · {{ item.appliedAt?.replace('T', ' ') }} · {{ statusDescription(item.status) }}</p>
            </div>
            <span class="value-chip">{{ statusLabel(item.status) }}</span>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<style scoped>
.workspace-hero,
.board-panel {
  padding: 24px;
}

.list-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--line-soft);
}

.list-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.list-row strong {
  display: block;
}

.list-row p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.value-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(3, 105, 161, 0.08);
  color: var(--primary-deep);
  font-size: 13px;
  font-weight: 700;
}

.guide-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(239, 246, 255, 0.86);
  line-height: 1.8;
  border: 1px solid rgba(3, 105, 161, 0.1);
}

.guide-card strong {
  display: block;
  margin-bottom: 8px;
}

.guide-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--text-main);
}
</style>
