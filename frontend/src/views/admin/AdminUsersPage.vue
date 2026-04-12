<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import client from '../../api/client'
import type {
  AdminCompanyUserRecord,
  AdminJobseekerUserRecord,
  PageResponse,
  UserAccountStatus,
} from '../../types'
import {
  auditStatusLabel,
  auditStatusType,
  formatDateTime,
  userStatusLabel,
  userStatusType,
} from './adminShared'

type UserTabKey = 'companies' | 'jobseekers'

const loading = ref(false)
const activeTab = ref<UserTabKey>('companies')
const companyUsers = ref<AdminCompanyUserRecord[]>([])
const jobseekerUsers = ref<AdminJobseekerUserRecord[]>([])

const companyFilters = reactive({
  keyword: '',
  auditStatus: '',
  userStatus: '',
  pageNum: 1,
  pageSize: 8,
  total: 0,
})

const jobseekerFilters = reactive({
  keyword: '',
  userStatus: '',
  pageNum: 1,
  pageSize: 8,
  total: 0,
})

async function fetchCompanyUsers() {
  loading.value = true
  try {
    const response = await client.get('/admin/users/companies', {
      params: {
        keyword: companyFilters.keyword || undefined,
        auditStatus: companyFilters.auditStatus || undefined,
        userStatus: companyFilters.userStatus || undefined,
        pageNum: companyFilters.pageNum,
        pageSize: companyFilters.pageSize,
      },
    })
    const page = response.data as PageResponse<AdminCompanyUserRecord>
    companyUsers.value = page.records
    companyFilters.total = page.total
  } finally {
    loading.value = false
  }
}

async function fetchJobseekerUsers() {
  loading.value = true
  try {
    const response = await client.get('/admin/users/jobseekers', {
      params: {
        keyword: jobseekerFilters.keyword || undefined,
        userStatus: jobseekerFilters.userStatus || undefined,
        pageNum: jobseekerFilters.pageNum,
        pageSize: jobseekerFilters.pageSize,
      },
    })
    const page = response.data as PageResponse<AdminJobseekerUserRecord>
    jobseekerUsers.value = page.records
    jobseekerFilters.total = page.total
  } finally {
    loading.value = false
  }
}

async function fetchCurrentTab() {
  if (activeTab.value === 'companies') {
    await fetchCompanyUsers()
    return
  }
  await fetchJobseekerUsers()
}

function resetCompanyFilters() {
  companyFilters.keyword = ''
  companyFilters.auditStatus = ''
  companyFilters.userStatus = ''
  companyFilters.pageNum = 1
  void fetchCompanyUsers()
}

function resetJobseekerFilters() {
  jobseekerFilters.keyword = ''
  jobseekerFilters.userStatus = ''
  jobseekerFilters.pageNum = 1
  void fetchJobseekerUsers()
}

function handleTabChange(tab: string | number) {
  activeTab.value = tab as UserTabKey
  void fetchCurrentTab()
}

function isDialogCancel(error: unknown) {
  return error === 'cancel' || error === 'close'
}

async function collectReason(title: string, placeholder: string) {
  const { value } = await ElMessageBox.prompt(placeholder, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValidator: (inputValue) => (inputValue.trim() ? true : '请填写原因'),
  })
  return value.trim()
}

async function updateUserStatus(userId: number, status: UserAccountStatus, successMessage: string) {
  try {
    let reason: string | undefined
    if (status !== 'ACTIVE') {
      reason = await collectReason('请填写处理原因', '请说明本次操作原因')
    } else {
      await ElMessageBox.confirm('确认将该账号恢复为正常状态吗？', '启用账号', {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
    }

    await client.patch(`/admin/users/${userId}/status`, { status, reason })
    ElMessage.success(successMessage)
    await fetchCurrentTab()
  } catch (error) {
    if (!isDialogCancel(error)) {
      throw error
    }
  }
}

onMounted(fetchCompanyUsers)
</script>

<template>
  <div class="section-grid admin-page">
    <section class="page-heading">
      <span class="eyebrow">用户管理</span>
      <h2 class="page-title">企业与求职者账号维护</h2>
      <p class="page-subtitle">查看账号基础信息、注册时间与当前状态，并对异常账号执行启用、禁用或逻辑删除。</p>
    </section>

    <article class="surface-card table-card" v-loading="loading">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="企业用户" name="companies">
          <section class="filter-panel">
            <div class="filter-grid">
              <el-form-item label="关键词">
                <el-input v-model.trim="companyFilters.keyword" placeholder="企业名称 / 联系人 / 邮箱 / 电话" clearable />
              </el-form-item>
              <el-form-item label="审核状态">
                <el-select v-model="companyFilters.auditStatus" placeholder="全部审核状态" clearable>
                  <el-option label="待审核" value="PENDING" />
                  <el-option label="审核通过" value="APPROVED" />
                  <el-option label="审核驳回" value="REJECTED" />
                </el-select>
              </el-form-item>
              <el-form-item label="账号状态">
                <el-select v-model="companyFilters.userStatus" placeholder="默认排除已删除" clearable>
                  <el-option label="正常" value="ACTIVE" />
                  <el-option label="已禁用" value="DISABLED" />
                  <el-option label="已删除" value="DELETED" />
                </el-select>
              </el-form-item>
            </div>
            <div class="filter-actions">
              <el-button @click="resetCompanyFilters">重置</el-button>
              <el-button type="primary" @click="companyFilters.pageNum = 1; fetchCompanyUsers()">查询</el-button>
            </div>
          </section>

          <el-table :data="companyUsers" empty-text="当前没有符合条件的企业用户">
            <el-table-column label="企业信息" min-width="260">
              <template #default="{ row }">
                <strong>{{ row.companyName }}</strong>
                <div class="table-note">{{ row.contactPerson }} · {{ row.phone }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="220" />
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
            <el-table-column label="操作" min-width="240" fixed="right">
              <template #default="{ row }">
                <div class="action-group">
                  <el-button
                    v-if="row.userStatus === 'ACTIVE'"
                    size="small"
                    type="warning"
                    plain
                    @click="updateUserStatus(row.userId, 'DISABLED', '企业账号已禁用')"
                  >
                    禁用
                  </el-button>
                  <el-button
                    v-if="row.userStatus === 'DISABLED'"
                    size="small"
                    type="success"
                    plain
                    @click="updateUserStatus(row.userId, 'ACTIVE', '企业账号已启用')"
                  >
                    启用
                  </el-button>
                  <el-button
                    v-if="row.userStatus !== 'DELETED'"
                    size="small"
                    type="danger"
                    plain
                    @click="updateUserStatus(row.userId, 'DELETED', '企业账号已删除')"
                  >
                    删除
                  </el-button>
                  <span v-if="row.userStatus === 'DELETED'" class="table-note">已删除</span>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-card__footer">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="companyFilters.pageNum"
              :page-size="companyFilters.pageSize"
              :total="companyFilters.total"
              @current-change="(page: number) => { companyFilters.pageNum = page; fetchCompanyUsers() }"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="求职者用户" name="jobseekers">
          <section class="filter-panel">
            <div class="filter-grid">
              <el-form-item label="关键词">
                <el-input v-model.trim="jobseekerFilters.keyword" placeholder="姓名 / 电话 / 邮箱 / 学历 / 求职方向" clearable />
              </el-form-item>
              <el-form-item label="账号状态">
                <el-select v-model="jobseekerFilters.userStatus" placeholder="默认排除已删除" clearable>
                  <el-option label="正常" value="ACTIVE" />
                  <el-option label="已禁用" value="DISABLED" />
                  <el-option label="已删除" value="DELETED" />
                </el-select>
              </el-form-item>
            </div>
            <div class="filter-actions">
              <el-button @click="resetJobseekerFilters">重置</el-button>
              <el-button type="primary" @click="jobseekerFilters.pageNum = 1; fetchJobseekerUsers()">查询</el-button>
            </div>
          </section>

          <el-table :data="jobseekerUsers" empty-text="当前没有符合条件的求职者用户">
            <el-table-column label="求职者信息" min-width="240">
              <template #default="{ row }">
                <strong>{{ row.fullName || row.displayName }}</strong>
                <div class="table-note">{{ row.phone }} · {{ row.email }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="highestEducation" label="最高学历" min-width="120" />
            <el-table-column prop="desiredPositionCategory" label="求职方向" min-width="160" />
            <el-table-column label="账号状态" min-width="120">
              <template #default="{ row }">
                <el-tag :type="userStatusType(row.userStatus)">{{ userStatusLabel(row.userStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="240" fixed="right">
              <template #default="{ row }">
                <div class="action-group">
                  <el-button
                    v-if="row.userStatus === 'ACTIVE'"
                    size="small"
                    type="warning"
                    plain
                    @click="updateUserStatus(row.userId, 'DISABLED', '求职者账号已禁用')"
                  >
                    禁用
                  </el-button>
                  <el-button
                    v-if="row.userStatus === 'DISABLED'"
                    size="small"
                    type="success"
                    plain
                    @click="updateUserStatus(row.userId, 'ACTIVE', '求职者账号已启用')"
                  >
                    启用
                  </el-button>
                  <el-button
                    v-if="row.userStatus !== 'DELETED'"
                    size="small"
                    type="danger"
                    plain
                    @click="updateUserStatus(row.userId, 'DELETED', '求职者账号已删除')"
                  >
                    删除
                  </el-button>
                  <span v-if="row.userStatus === 'DELETED'" class="table-note">已删除</span>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-card__footer">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="jobseekerFilters.pageNum"
              :page-size="jobseekerFilters.pageSize"
              :total="jobseekerFilters.total"
              @current-change="(page: number) => { jobseekerFilters.pageNum = page; fetchJobseekerUsers() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
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
