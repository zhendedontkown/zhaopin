<script setup lang="ts">
import { onMounted, ref } from 'vue'
import client from '../api/client'
import type { JobRecord, PageResponse } from '../types'

interface CompanyRecord {
  userId: number
  companyName: string
  contactPerson: string
  phone: string
  email: string
  unifiedSocialCreditCode: string
  auditStatus: string
  createdAt: string
}

const dashboard = ref<Record<string, number>>({})
const companies = ref<CompanyRecord[]>([])
const jobs = ref<JobRecord[]>([])

async function fetchPageData() {
  const [dashboardResponse, companyResponse, jobResponse] = await Promise.all([
    client.get('/admin/dashboard'),
    client.get('/admin/companies', { params: { pageNum: 1, pageSize: 8 } }),
    client.get('/admin/jobs', { params: { pageNum: 1, pageSize: 8 } }),
  ])
  dashboard.value = dashboardResponse.data as Record<string, number>
  companies.value = (companyResponse.data as PageResponse<CompanyRecord>).records
  jobs.value = (jobResponse.data as PageResponse<JobRecord>).records
}

async function auditCompany(userId: number, auditStatus: string) {
  await client.patch(`/admin/companies/${userId}/audit`, { auditStatus })
  await fetchPageData()
}

async function forceOffline(jobId: number) {
  await client.patch(`/admin/jobs/${jobId}/status`, { status: 'OFFLINE' })
  await fetchPageData()
}

function auditLabel(status: string) {
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return '待审核'
}

onMounted(fetchPageData)
</script>

<template>
  <div class="section-grid">
    <section class="page-heading">
      <span class="eyebrow">System Admin</span>
      <h2 class="page-title">管理员后台</h2>
      <p class="page-subtitle">把企业审核、岗位监管和系统数据看板放进同一套管理界面，方便日常巡检和答辩演示。</p>
    </section>

    <section class="kpi-grid">
      <div v-for="(value, key) in dashboard" :key="key" class="kpi-card">
        <span class="kpi-label">{{ key }}</span>
        <strong class="kpi-value">{{ value }}</strong>
      </div>
    </section>

    <section class="split-panel">
      <div class="surface-card admin-panel">
        <div class="panel-head">
          <div>
            <h3>企业审核</h3>
            <p>优先处理新注册企业，确保企业资料和审核状态保持清晰。</p>
          </div>
        </div>

        <div v-for="item in companies" :key="item.userId" class="admin-row">
          <div>
            <strong>{{ item.companyName }}</strong>
            <p>{{ item.contactPerson }} · {{ item.phone }} · {{ auditLabel(item.auditStatus) }}</p>
          </div>
          <div class="action-group">
            <el-button size="small" type="success" @click="auditCompany(item.userId, 'APPROVED')">通过</el-button>
            <el-button size="small" type="danger" @click="auditCompany(item.userId, 'REJECTED')">驳回</el-button>
          </div>
        </div>
      </div>

      <div class="surface-card admin-panel">
        <div class="panel-head">
          <div>
            <h3>岗位监管</h3>
            <p>关注状态异常或需要处理的岗位，必要时可以直接强制下线。</p>
          </div>
        </div>

        <div v-for="item in jobs" :key="item.id" class="admin-row">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.companyName }} · {{ item.location }} · {{ item.status }}</p>
          </div>
          <el-button size="small" type="warning" @click="forceOffline(item.id)">强制下线</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-panel {
  padding: 24px;
}

.admin-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--line-soft);
}

.admin-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.admin-row strong {
  display: block;
}

.admin-row p {
  margin: 8px 0 0;
  color: var(--text-muted);
}

.action-group {
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  .admin-row,
  .action-group {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
