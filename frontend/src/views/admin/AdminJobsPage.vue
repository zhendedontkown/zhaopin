<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import client from '../../api/client'
import type { AdminManagedJobRecord, PageResponse } from '../../types'
import {
  formatDateTime,
  formatSalaryRange,
  jobStatusLabel,
  jobStatusType,
} from './adminShared'

const loading = ref(false)
const records = ref<AdminManagedJobRecord[]>([])

const filters = reactive({
  keyword: '',
  companyKeyword: '',
  status: '',
  category: '',
  pageNum: 1,
  pageSize: 8,
  total: 0,
})

function isDialogCancel(error: unknown) {
  return error === 'cancel' || error === 'close'
}

async function fetchRecords() {
  loading.value = true
  try {
    const response = await client.get('/admin/jobs', {
      params: {
        keyword: filters.keyword || undefined,
        companyKeyword: filters.companyKeyword || undefined,
        status: filters.status || undefined,
        category: filters.category || undefined,
        pageNum: filters.pageNum,
        pageSize: filters.pageSize,
      },
    })
    const page = response.data as PageResponse<AdminManagedJobRecord>
    records.value = page.records
    filters.total = page.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.companyKeyword = ''
  filters.status = ''
  filters.category = ''
  filters.pageNum = 1
  void fetchRecords()
}

async function handleModeration(jobId: number, action: 'OFFLINE' | 'DELETE') {
  const actionText = action === 'OFFLINE' ? '下线' : '删除'
  try {
    const { value } = await ElMessageBox.prompt(`请填写岗位${actionText}原因，系统会通知对应企业。`, `岗位${actionText}`, {
      inputType: 'textarea',
      confirmButtonText: `确认${actionText}`,
      cancelButtonText: '取消',
      inputValidator: (input) => (input.trim() ? true : `请填写岗位${actionText}原因`),
    })

    await client.patch(`/admin/jobs/${jobId}/moderation`, {
      action,
      reason: value.trim(),
    })
    ElMessage.success(`岗位已${actionText}`)
    await fetchRecords()
  } catch (error) {
    if (!isDialogCancel(error)) {
      throw error
    }
  }
}

onMounted(fetchRecords)
</script>

<template>
  <div class="section-grid admin-page">
    <section class="page-heading">
      <span class="eyebrow">岗位管理</span>
      <h2 class="page-title">平台岗位监管</h2>
      <p class="page-subtitle">
        统一查看企业发布岗位，支持按关键词、企业、状态和类别筛选，并对违规岗位执行下线或逻辑删除处理。
      </p>
    </section>

    <article class="surface-card table-card" v-loading="loading">
      <section class="filter-panel">
        <div class="filter-grid">
          <el-form-item label="岗位关键词">
            <el-input v-model.trim="filters.keyword" placeholder="岗位名称 / 编号 / 地点 / 类别" clearable />
          </el-form-item>
          <el-form-item label="企业关键词">
            <el-input v-model.trim="filters.companyKeyword" placeholder="企业名称" clearable />
          </el-form-item>
          <el-form-item label="岗位状态">
            <el-select v-model="filters.status" placeholder="全部岗位状态" clearable>
              <el-option label="招聘中" value="PUBLISHED" />
              <el-option label="已下线" value="OFFLINE" />
              <el-option label="已过期" value="EXPIRED" />
              <el-option label="草稿" value="DRAFT" />
            </el-select>
          </el-form-item>
          <el-form-item label="岗位类别">
            <el-input v-model.trim="filters.category" placeholder="如：后端开发、产品经理" clearable />
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="filters.pageNum = 1; fetchRecords()">查询</el-button>
        </div>
      </section>

      <el-table :data="records" empty-text="当前没有符合条件的岗位记录">
        <el-table-column label="岗位信息" min-width="260">
          <template #default="{ row }">
            <strong>{{ row.title }}</strong>
            <div class="table-note">{{ row.jobCode }} · {{ row.location }}</div>
            <div class="table-note">{{ row.companyName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="岗位类别" min-width="140" />
        <el-table-column label="薪资范围" min-width="150">
          <template #default="{ row }">{{ formatSalaryRange(row.salaryMin, row.salaryMax) }}</template>
        </el-table-column>
        <el-table-column prop="educationRequirement" label="学历要求" min-width="120" />
        <el-table-column prop="experienceRequirement" label="经验要求" min-width="120" />
        <el-table-column label="岗位状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="jobStatusType(row.status)">{{ jobStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.publishedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button
                size="small"
                type="warning"
                plain
                :disabled="row.status === 'OFFLINE'"
                @click="handleModeration(row.id, 'OFFLINE')"
              >
                下线
              </el-button>
              <el-button size="small" type="danger" plain @click="handleModeration(row.id, 'DELETE')">
                删除
              </el-button>
            </div>
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

.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
