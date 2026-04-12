<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import client from '../../api/client'
import type { AdminApplicationRecord, PageResponse } from '../../types'
import {
  applicationStatusLabel,
  applicationStatusType,
  formatDateTime,
  toDateTimeBoundary,
} from './adminShared'

const loading = ref(false)
const records = ref<AdminApplicationRecord[]>([])
const dateRange = ref<Date[] | null>(null)

const filters = reactive({
  keyword: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

const startAt = computed(() => {
  if (!dateRange.value?.[0]) return undefined
  return toDateTimeBoundary(dateRange.value[0], 'start')
})

const endAt = computed(() => {
  if (!dateRange.value?.[1]) return undefined
  return toDateTimeBoundary(dateRange.value[1], 'end')
})

async function fetchRecords() {
  loading.value = true
  try {
    const response = await client.get('/admin/applications', {
      params: {
        keyword: filters.keyword || undefined,
        status: filters.status || undefined,
        startAt: startAt.value,
        endAt: endAt.value,
        pageNum: filters.pageNum,
        pageSize: filters.pageSize,
      },
    })
    const page = response.data as PageResponse<AdminApplicationRecord>
    records.value = page.records
    filters.total = page.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  dateRange.value = null
  filters.pageNum = 1
  void fetchRecords()
}

onMounted(fetchRecords)
</script>

<template>
  <div class="section-grid admin-page">
    <section class="page-heading">
      <span class="eyebrow">投递记录</span>
      <h2 class="page-title">投递记录监管</h2>
      <p class="page-subtitle">
        从平台视角查看求职者投递链路，支持按状态、岗位、企业、求职者和时间范围筛选，便于排查异常数据。
      </p>
    </section>

    <article class="surface-card table-card" v-loading="loading">
      <section class="filter-panel">
        <div class="filter-grid">
          <el-form-item label="关键词">
            <el-input v-model.trim="filters.keyword" placeholder="求职者 / 岗位 / 企业名称" clearable />
          </el-form-item>
          <el-form-item label="投递状态">
            <el-select v-model="filters.status" placeholder="全部投递状态" clearable>
              <el-option label="已投递" value="SUBMITTED" />
              <el-option label="已查看" value="VIEWED" />
              <el-option label="待确认面试" value="INTERVIEW_PENDING" />
              <el-option label="面试中" value="INTERVIEWING" />
              <el-option label="已录用" value="OFFERED" />
              <el-option label="未通过" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="投递时间">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              clearable
            />
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="filters.pageNum = 1; fetchRecords()">查询</el-button>
        </div>
      </section>

      <el-table :data="records" empty-text="当前没有符合条件的投递记录">
        <el-table-column label="求职者" min-width="160">
          <template #default="{ row }">
            <strong>{{ row.jobseekerName }}</strong>
            <div class="table-note">用户 ID：{{ row.jobseekerUserId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="应聘岗位" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.jobTitle }}</strong>
            <div class="table-note">岗位 ID：{{ row.jobId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="所属企业" min-width="200">
          <template #default="{ row }">
            <strong>{{ row.companyName }}</strong>
            <div class="table-note">企业用户 ID：{{ row.companyUserId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="投递状态" min-width="140">
          <template #default="{ row }">
            <el-tag :type="applicationStatusType(row.status)">
              {{ row.statusText || applicationStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.appliedAt) }}</template>
        </el-table-column>
        <el-table-column label="最近更新" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.statusUpdatedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态说明" min-width="260">
          <template #default="{ row }">
            <div>{{ row.statusDescription || '--' }}</div>
            <div v-if="row.statusRemark" class="table-note">备注：{{ row.statusRemark }}</div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-card__footer">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="filters.pageNum"
          :page-size="filters.pageSize"
          :total="filters.total"
          @current-change="(page: number) => { filters.pageNum = page; fetchRecords() }"
        />
      </div>
    </article>
  </div>
</template>

<style scoped>
.admin-page {
  align-content: start;
}
</style>
