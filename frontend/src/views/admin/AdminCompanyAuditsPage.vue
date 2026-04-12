<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import client from '../../api/client'
import type { AdminCompanyAuditRecord, PageResponse } from '../../types'
import {
  auditStatusLabel,
  auditStatusType,
  formatDateTime,
  userStatusLabel,
  userStatusType,
} from './adminShared'

const loading = ref(false)
const records = ref<AdminCompanyAuditRecord[]>([])

const filters = reactive({
  keyword: '',
  auditStatus: '',
  userStatus: '',
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
    const response = await client.get('/admin/company-audits', {
      params: {
        keyword: filters.keyword || undefined,
        auditStatus: filters.auditStatus || undefined,
        userStatus: filters.userStatus || undefined,
        pageNum: filters.pageNum,
        pageSize: filters.pageSize,
      },
    })
    const page = response.data as PageResponse<AdminCompanyAuditRecord>
    records.value = page.records
    filters.total = page.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.auditStatus = ''
  filters.userStatus = ''
  filters.pageNum = 1
  void fetchRecords()
}

async function handleAudit(userId: number, auditStatus: 'APPROVED' | 'REJECTED') {
  try {
    let reason: string | undefined

    if (auditStatus === 'APPROVED') {
      await ElMessageBox.confirm('确认将该企业审核为通过状态吗？', '审核通过', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
    } else {
      const promptResult = await ElMessageBox.prompt('请填写驳回原因，企业会收到对应通知。', '审核驳回', {
        inputType: 'textarea',
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputValidator: (value) => (value.trim() ? true : '请填写驳回原因'),
      })
      reason = promptResult.value.trim()
    }

    await client.patch(`/admin/companies/${userId}/audit`, {
      auditStatus,
      reason,
    })
    ElMessage.success(auditStatus === 'APPROVED' ? '企业审核已通过' : '企业审核已驳回')
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
      <span class="eyebrow">企业审核</span>
      <h2 class="page-title">企业入驻资料审核</h2>
      <p class="page-subtitle">
        查看企业提交的认证信息，支持按审核状态和账号状态快速筛选，并对待处理企业执行通过或驳回操作。
      </p>
    </section>

    <article class="surface-card table-card" v-loading="loading">
      <section class="filter-panel">
        <div class="filter-grid">
          <el-form-item label="关键词">
            <el-input
              v-model.trim="filters.keyword"
              placeholder="企业名称 / 联系人 / 电话 / 邮箱 / 统一社会信用代码"
              clearable
            />
          </el-form-item>
          <el-form-item label="审核状态">
            <el-select v-model="filters.auditStatus" placeholder="全部审核状态" clearable>
              <el-option label="待审核" value="PENDING" />
              <el-option label="审核通过" value="APPROVED" />
              <el-option label="审核驳回" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="账号状态">
            <el-select v-model="filters.userStatus" placeholder="默认排除已删除" clearable>
              <el-option label="正常" value="ACTIVE" />
              <el-option label="已禁用" value="DISABLED" />
              <el-option label="已删除" value="DELETED" />
            </el-select>
          </el-form-item>
        </div>
        <div class="filter-actions">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="filters.pageNum = 1; fetchRecords()">查询</el-button>
        </div>
      </section>

      <el-table :data="records" empty-text="当前没有符合条件的企业审核记录">
        <el-table-column label="企业信息" min-width="280">
          <template #default="{ row }">
            <strong>{{ row.companyName }}</strong>
            <div class="table-note">{{ row.contactPerson }} · {{ row.phone }}</div>
            <div class="table-note">{{ row.email }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="unifiedSocialCreditCode" label="统一社会信用代码" min-width="220" />
        <el-table-column label="审核状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="auditStatusType(row.auditStatus)">{{ auditStatusLabel(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="userStatusType(row.userStatus)">{{ userStatusLabel(row.userStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button
                size="small"
                type="success"
                plain
                :disabled="row.userStatus === 'DELETED' || row.auditStatus === 'APPROVED'"
                @click="handleAudit(row.userId, 'APPROVED')"
              >
                通过
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="row.userStatus === 'DELETED'"
                @click="handleAudit(row.userId, 'REJECTED')"
              >
                驳回
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
