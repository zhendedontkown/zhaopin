<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import client from '../../api/client'
import type { AdminCompanyAuditRecord, AdminDashboardStats, AdminManagedJobRecord, PageResponse } from '../../types'
import {
  auditStatusLabel,
  auditStatusType,
  formatDateTime,
  formatSalaryRange,
  jobStatusLabel,
  jobStatusType,
} from './adminShared'

const router = useRouter()
const loading = ref(false)
const dashboard = ref<AdminDashboardStats>({
  userCount: 0,
  companyUserCount: 0,
  jobseekerUserCount: 0,
  pendingCompanyAuditCount: 0,
  jobCount: 0,
  applicationCount: 0,
  interviewingCount: 0,
  offeredCount: 0,
  rejectedCount: 0,
})
const pendingAudits = ref<AdminCompanyAuditRecord[]>([])
const recentJobs = ref<AdminManagedJobRecord[]>([])

const statCards = computed(() => [
  { key: 'userCount', label: '用户总数', value: dashboard.value.userCount },
  { key: 'companyUserCount', label: '企业用户数', value: dashboard.value.companyUserCount },
  { key: 'jobseekerUserCount', label: '求职者用户数', value: dashboard.value.jobseekerUserCount },
  { key: 'pendingCompanyAuditCount', label: '待审核企业数', value: dashboard.value.pendingCompanyAuditCount },
  { key: 'jobCount', label: '岗位总数', value: dashboard.value.jobCount },
  { key: 'applicationCount', label: '投递总数', value: dashboard.value.applicationCount },
])

const applicationDistribution = computed(() => [
  { key: 'interviewingCount', label: '面试中', value: dashboard.value.interviewingCount },
  { key: 'offeredCount', label: '已录用', value: dashboard.value.offeredCount },
  { key: 'rejectedCount', label: '未通过', value: dashboard.value.rejectedCount },
])

async function fetchPageData() {
  loading.value = true
  try {
    const [dashboardResponse, auditResponse, jobsResponse] = await Promise.all([
      client.get('/admin/dashboard'),
      client.get('/admin/company-audits', { params: { auditStatus: 'PENDING', pageNum: 1, pageSize: 5 } }),
      client.get('/admin/jobs', { params: { pageNum: 1, pageSize: 5 } }),
    ])
    dashboard.value = dashboardResponse.data as AdminDashboardStats
    pendingAudits.value = (auditResponse.data as PageResponse<AdminCompanyAuditRecord>).records
    recentJobs.value = (jobsResponse.data as PageResponse<AdminManagedJobRecord>).records
  } finally {
    loading.value = false
  }
}

onMounted(fetchPageData)
</script>

<template>
  <div class="section-grid admin-page" v-loading="loading">
    <section class="page-heading">
      <span class="eyebrow">数据统计</span>
      <h2 class="page-title">平台运行概览</h2>
      <p class="page-subtitle">快速查看用户、岗位、投递与审核状态，帮助管理员判断当前平台运行情况。</p>
    </section>

    <section class="kpi-grid">
      <div v-for="item in statCards" :key="item.key" class="kpi-card">
        <span class="kpi-label">{{ item.label }}</span>
        <strong class="kpi-value">{{ item.value }}</strong>
      </div>
    </section>

    <section class="split-panel">
      <article class="surface-card table-card">
        <div class="panel-head">
          <div>
            <h3>待审核企业</h3>
            <p>优先处理新提交的企业认证资料，减少岗位发布前的等待时间。</p>
          </div>
          <el-button type="primary" plain @click="router.push('/admin/company-audits')">进入审核</el-button>
        </div>

        <div v-if="pendingAudits.length" class="summary-list">
          <div v-for="item in pendingAudits" :key="item.userId" class="summary-list__item">
            <div>
              <strong>{{ item.companyName }}</strong>
              <p>{{ item.contactPerson }} · {{ item.phone }} · {{ formatDateTime(item.createdAt) }}</p>
            </div>
            <el-tag :type="auditStatusType(item.auditStatus)">{{ auditStatusLabel(item.auditStatus) }}</el-tag>
          </div>
        </div>
        <el-empty v-else description="当前没有待审核企业" />
      </article>

      <article class="surface-card table-card">
        <div class="panel-head">
          <div>
            <h3>投递状态分布</h3>
            <p>关注投递流程的关键状态，便于管理员快速识别异常波动。</p>
          </div>
        </div>

        <div class="summary-grid">
          <div v-for="item in applicationDistribution" :key="item.key" class="summary-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <p class="table-note">总投递数 {{ dashboard.applicationCount }} 条，可在投递记录管理中进一步筛查企业、岗位与时间范围。</p>
      </article>
    </section>

    <article class="surface-card table-card">
      <div class="panel-head">
        <div>
          <h3>最新岗位动态</h3>
          <p>查看最近发布或维护的岗位，方便管理员快速定位需要监管的内容。</p>
        </div>
        <el-button type="primary" plain @click="router.push('/admin/jobs')">进入岗位管理</el-button>
      </div>

      <el-table :data="recentJobs" empty-text="当前没有可展示的岗位数据">
        <el-table-column label="岗位名称" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.title }}</strong>
            <div class="table-note">{{ row.companyName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="岗位类别" min-width="140" />
        <el-table-column label="薪资范围" min-width="150">
          <template #default="{ row }">{{ formatSalaryRange(row.salaryMin, row.salaryMax) }}</template>
        </el-table-column>
        <el-table-column label="岗位状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="jobStatusType(row.status)">{{ jobStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.publishedAt || row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </article>
  </div>
</template>

<style scoped>
.admin-page {
  align-content: start;
}
</style>
