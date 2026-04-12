<script setup lang="ts">
import { Bell, ChatRound, Crop, DataBoard, Files, Search, Setting, SwitchButton, User } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'
import type { NotificationRecord } from '../types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const drawerVisible = ref(false)
const markingAllRead = ref(false)

const isJobseeker = computed(() => authStore.role === 'JOBSEEKER')
const isCompany = computed(() => authStore.role === 'COMPANY')
const usesTopbarLayout = computed(() => isJobseeker.value || isCompany.value)
const topbarIdentity = computed(() =>
  authStore.profile?.displayName || authStore.session?.displayName || (isCompany.value ? '企业端' : '求职者端'),
)

const currentPage = computed(() => {
  if (route.path === '/jobs') {
    return {
      title: authStore.role === 'COMPANY' ? '岗位管理' : '岗位大厅',
      description: authStore.role === 'COMPANY'
        ? '集中管理岗位信息、状态流转和招聘节奏。'
        : '快速浏览岗位、筛选条件并找到更匹配的机会。',
    }
  }

  if (route.path === '/applications') {
    return {
      title: authStore.role === 'COMPANY' ? '投递处理' : '投递记录',
      description: authStore.role === 'COMPANY'
        ? '查看候选人投递、推进状态并进入沟通。'
        : '追踪每一条投递的处理进展和反馈结果。',
    }
  }

  if (route.path === '/resume') {
    return {
      title: '简历创作',
      description: '在统一的创作工作台里编辑信息，并实时预览最终投递效果。',
    }
  }

  if (route.path === '/chat') {
    return {
      title: '在线沟通',
      description: '围绕岗位和候选人进行一对一实时消息沟通。',
    }
  }

  if (route.path === '/profile') {
    return {
      title: '我的',
      description: authStore.role === 'COMPANY'
        ? '集中查看企业账号资料、认证状态和招聘快捷入口，并在这里维护基础信息与账号安全。'
        : '集中查看求职账号资料、简历完成度和常用入口，并在这里维护个人资料与账号安全。',
    }
  }

  if (route.path.startsWith('/admin/dashboard')) {
    return {
      title: '数据统计',
      description: '集中查看平台用户、岗位、投递与审核等核心运营数据。',
    }
  }

  if (route.path.startsWith('/admin/users')) {
    return {
      title: '用户管理',
      description: '统一维护企业与求职者账号状态，处理异常账号与无效用户。',
    }
  }

  if (route.path.startsWith('/admin/company-audits')) {
    return {
      title: '企业审核',
      description: '审核企业注册资料，保障平台招聘主体真实可靠。',
    }
  }

  if (route.path.startsWith('/admin/jobs')) {
    return {
      title: '岗位管理',
      description: '监管企业发布岗位，及时处理违规、异常或虚假招聘信息。',
    }
  }

  if (route.path.startsWith('/admin/applications')) {
    return {
      title: '投递记录管理',
      description: '从平台视角查看投递链路，排查异常数据并核对业务流程。',
    }
  }

  if (route.path === '/admin') {
    return {
      title: '系统管理',
      description: '处理企业审核、岗位监管和系统整体运行情况。',
    }
  }

  if (authStore.role === 'ADMIN') {
    return {
      title: '系统管理',
      description: '处理企业审核、岗位监管和系统整体运行情况。',
    }
  }

  return {
    title: '工作台',
    description: '围绕当前身份整理关键任务、最新进展和值得优先处理的事项。',
  }
})

const roleLabel = computed(() => {
  if (authStore.role === 'ADMIN') return '管理员'
  if (authStore.role === 'COMPANY') return '企业用户'
  if (authStore.role === 'JOBSEEKER') return '求职者'
  return '访客'
})

function iconFor(path: string) {
  if (path === '/dashboard') return DataBoard
  if (path === '/jobs') return Search
  if (path === '/resume') return Crop
  if (path === '/applications') return Files
  if (path === '/admin/dashboard') return DataBoard
  if (path === '/admin/users') return User
  if (path === '/admin/company-audits') return Setting
  if (path === '/admin/jobs') return Search
  if (path === '/admin/applications') return Files
  if (path === '/profile') return User
  return ChatRound
}

async function openNotifications() {
  drawerVisible.value = true
  await notificationStore.fetchNotifications()
}

async function markAllNotificationsRead() {
  if (notificationStore.unreadCount === 0) return
  markingAllRead.value = true
  try {
    await notificationStore.markAllRead()
  } finally {
    markingAllRead.value = false
  }
}

async function handleNotificationClick(item: NotificationRecord) {
  const isChatNotification = item.type === 'NEW_MESSAGE' || item.title === '收到新的聊天消息'
  const hasApplicationTarget = Boolean(item.relatedApplicationId)

  if (item.readFlag === 0) {
    await notificationStore.markRead(item.id)
  }

  if (isChatNotification && item.relatedUserId) {
    drawerVisible.value = false
    await router.push({
      path: '/chat',
      query: { peerUserId: String(item.relatedUserId) },
    })
    return
  }

  if (hasApplicationTarget) {
    drawerVisible.value = false
    await router.push({
      path: '/applications',
      query: { focusApplicationId: String(item.relatedApplicationId) },
    })
  }
}

function logout() {
  notificationStore.reset()
  authStore.logout()
  void router.push('/login')
}

async function syncNotifications(token?: string) {
  if (!token) {
    notificationStore.reset()
    return
  }

  try {
    await notificationStore.fetchNotifications()
    notificationStore.connect(token)
  } catch {
    notificationStore.reset()
  }
}

watch(
  () => authStore.session?.token ?? '',
  (nextToken, previousToken) => {
    if (!nextToken) {
      notificationStore.reset()
      return
    }

    if (previousToken && previousToken !== nextToken) {
      notificationStore.reset()
    }

    void syncNotifications(nextToken)
  },
  { immediate: true },
)

onMounted(async () => {
  try {
    await authStore.fetchProfile()
  } catch {
    if (!authStore.isLoggedIn) {
      await router.push('/login')
    }
  }
})
</script>

<template>
  <div :class="['layout-shell', usesTopbarLayout ? 'is-topbar-layout' : '']">
    <template v-if="usesTopbarLayout">
      <header class="jobseeker-topbar" :data-role="authStore.role" :title="topbarIdentity">
        <div class="jobseeker-brand">
          <div class="brand-mark">HR</div>
          <div class="jobseeker-brand__copy">
            <strong>企业在线招聘系统</strong>
            <span>{{ authStore.profile?.displayName || authStore.session?.displayName || '工作空间' }}</span>
          </div>
        </div>

        <nav class="jobseeker-menu">
          <button
            v-for="item in authStore.menuItems"
            :key="item.path"
            :class="['jobseeker-menu__item', route.path === item.path ? 'is-active' : '']"
            @click="router.push(item.path)"
          >
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <div class="header-actions">
          <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0">
            <el-button circle :icon="Bell" @click="openNotifications" />
          </el-badge>
          <el-button circle :icon="SwitchButton" @click="logout" />
        </div>
      </header>

      <main class="content-shell">
        <router-view />
      </main>
    </template>

    <template v-else>
      <aside class="layout-aside">
        <div class="brand-block">
          <div class="brand-mark">HR</div>
          <div class="brand-copy">
            <strong>企业在线招聘系统</strong>
            <p>覆盖企业审核、岗位监管与系统维护的一体化管理后台。</p>
          </div>
        </div>

        <nav class="menu-list">
          <button
            v-for="item in authStore.menuItems"
            :key="item.path"
            :class="['menu-item', route.path === item.path ? 'is-active' : '']"
            @click="router.push(item.path)"
          >
            <el-icon><component :is="iconFor(item.path)" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <div class="identity-panel">
          <div class="identity-top">
            <el-tag type="success" effect="dark">{{ roleLabel }}</el-tag>
            <span>{{ authStore.workspaceLabel || '管理员工作区' }}</span>
          </div>
          <strong>{{ authStore.profile?.displayName || authStore.session?.displayName || '未命名用户' }}</strong>
          <p>{{ authStore.profile?.email || '暂未获取邮箱信息' }}</p>
        </div>
      </aside>

      <div class="layout-main">
        <header class="layout-header glass-card">
          <div class="header-copy">
            <span class="eyebrow">{{ authStore.workspaceLabel || '管理员工作区' }}</span>
            <h1 class="header-title">{{ currentPage.title }}</h1>
            <p class="header-subtitle">{{ currentPage.description }}</p>
          </div>

          <div class="header-actions">
            <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0">
              <el-button circle :icon="Bell" @click="openNotifications" />
            </el-badge>
            <el-button circle :icon="SwitchButton" @click="logout" />
          </div>
        </header>

        <main class="content-shell">
          <router-view />
        </main>
      </div>
    </template>

    <el-drawer v-model="drawerVisible" title="站内通知" size="420px">
      <div class="drawer-toolbar">
        <span>未读 {{ notificationStore.unreadCount }} 条</span>
        <el-button
          text
          type="primary"
          :disabled="notificationStore.unreadCount === 0"
          :loading="markingAllRead"
          @click="markAllNotificationsRead"
        >
          全部已读
        </el-button>
      </div>

      <div class="drawer-list">
        <el-empty v-if="!notificationStore.notifications.length" description="当前没有新的通知" />
        <button
          v-for="item in notificationStore.notifications"
          :key="item.id"
          class="drawer-item"
          @click="handleNotificationClick(item)"
        >
          <div class="drawer-item-head">
            <strong>{{ item.title }}</strong>
            <el-tag v-if="item.readFlag === 0" type="warning">未读</el-tag>
          </div>
          <p>{{ item.content }}</p>
          <span>{{ item.createdAt?.replace('T', ' ') }}</span>
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
  padding: 20px;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 18px;
  align-items: start;
  align-content: start;
}

.layout-shell.is-topbar-layout {
  grid-template-columns: 1fr;
  gap: 10px;
}

.layout-aside {
  display: grid;
  gap: 24px;
  align-content: start;
  padding: 26px 20px;
  border-radius: var(--radius-2xl);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 24%),
    var(--nav-bg);
  color: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-panel);
}

.brand-block,
.jobseeker-brand {
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 14px;
  align-items: center;
}

.jobseeker-brand {
  grid-template-columns: 34px 1fr;
  gap: 12px;
  padding: 6px 10px 6px 6px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.16));
  border: 1px solid rgba(255, 255, 255, 0.34);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.34),
    0 4px 14px rgba(148, 163, 184, 0.06);
  backdrop-filter: blur(10px);
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #38bdf8, #0369a1);
  color: white;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-copy strong,
.jobseeker-brand__copy strong {
  display: block;
  font-size: 18px;
}

.brand-copy p {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
  line-height: 1.65;
}

.jobseeker-brand__copy {
  display: grid;
  gap: 2px;
}

.jobseeker-brand .brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fb923c, #fdba74);
  font-size: 15px;
}

.jobseeker-brand__copy strong {
  color: #0f172a;
  font-size: 15px;
}

.jobseeker-brand__copy span {
  color: rgba(51, 65, 85, 0.62);
  font-size: 11px;
}

.menu-list {
  display: grid;
  gap: 8px;
}

.menu-item {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: transparent;
  color: rgba(255, 255, 255, 0.78);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: 160ms ease;
}

.menu-item:hover,
.menu-item.is-active {
  background: rgba(14, 165, 233, 0.12);
  border-color: rgba(56, 189, 248, 0.18);
  color: white;
}

.identity-panel {
  margin-top: auto;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: grid;
  gap: 10px;
}

.identity-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.identity-top span,
.identity-panel p {
  color: rgba(255, 255, 255, 0.68);
  font-size: 13px;
}

.identity-panel strong {
  font-size: 17px;
}

.identity-panel p {
  margin: 0;
  line-height: 1.5;
}

.layout-main {
  display: grid;
  gap: 18px;
}

.layout-header,
.jobseeker-topbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 26px 28px;
  border-radius: var(--radius-2xl);
}

.jobseeker-topbar {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  box-sizing: border-box;
  height: 64px;
  min-height: 64px;
  padding: 8px 14px;
  border-radius: 16px;
  isolation: isolate;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.26), rgba(255, 255, 255, 0.12));
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.36),
    0 8px 20px rgba(148, 163, 184, 0.06);
  backdrop-filter: blur(11px) saturate(120%);
  overflow: hidden;
}

.jobseeker-topbar::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 50%, rgba(96, 165, 250, 0.07), transparent 28%),
    radial-gradient(circle at 88% 50%, rgba(125, 211, 252, 0.08), transparent 26%),
    linear-gradient(120deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.02) 28%, rgba(59, 130, 246, 0.06) 52%, rgba(14, 165, 233, 0.05) 72%, rgba(168, 85, 247, 0.04));
  background-size: 140% 140%, 140% 140%, 220% 220%;
  animation: jobseeker-nav-gradient 24s ease-in-out infinite alternate;
  opacity: 0.76;
  pointer-events: none;
  z-index: 0;
}

.jobseeker-topbar::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(96, 165, 250, 0.06), rgba(14, 165, 233, 0.16), rgba(168, 85, 247, 0.08));
  opacity: 0.78;
  z-index: 0;
}

.header-copy {
  display: grid;
  gap: 10px;
}

.header-title {
  margin: 0;
  font-size: 34px;
  line-height: 1;
  letter-spacing: -0.04em;
}

.header-subtitle {
  margin: 0;
  max-width: 720px;
  color: var(--text-muted);
  line-height: 1.7;
}

.jobseeker-menu {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  align-items: center;
  flex: 1;
  position: relative;
  z-index: 1;
}

.jobseeker-menu__item {
  position: relative;
  min-height: 40px;
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(51, 65, 85, 0.84);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-weight: 800;
  font-size: 14px;
  letter-spacing: 0.01em;
  line-height: 1;
  transition:
    border-color 180ms ease,
    background-color 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.jobseeker-menu__item:hover {
  color: #0f172a;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.26), rgba(255, 255, 255, 0.12));
  border-color: rgba(148, 163, 184, 0.34);
  box-shadow: 0 8px 16px rgba(148, 163, 184, 0.1);
  transform: translateY(-1px);
}

.jobseeker-menu__item:focus-visible {
  outline: none;
  border-color: rgba(14, 165, 233, 0.46);
  box-shadow:
    0 0 0 3px rgba(96, 165, 250, 0.16),
    0 8px 18px rgba(148, 163, 184, 0.1);
}

.jobseeker-menu__item.is-active {
  border-color: rgba(148, 163, 184, 0.42);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.78));
  color: #0f172a;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 10px 22px rgba(148, 163, 184, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.38);
  backdrop-filter: blur(9px);
  transform: translateY(-1px);
}

.jobseeker-menu__item.is-active::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(96, 165, 250, 0.07), rgba(14, 165, 233, 0.03), rgba(168, 85, 247, 0.04));
  pointer-events: none;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  position: relative;
  z-index: 1;
}

:deep(.jobseeker-topbar .el-button.is-circle) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.32), rgba(255, 255, 255, 0.14)) !important;
  border-color: rgba(255, 255, 255, 0.24) !important;
  color: rgba(51, 65, 85, 0.88) !important;
  width: 36px !important;
  height: 36px !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.38),
    0 4px 10px rgba(148, 163, 184, 0.05) !important;
  backdrop-filter: blur(9px);
}

:deep(.jobseeker-topbar .el-button.is-circle:hover) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.58), rgba(255, 255, 255, 0.24)) !important;
  border-color: rgba(255, 255, 255, 0.34) !important;
}

:deep(.jobseeker-topbar .el-badge__content) {
  box-shadow: none;
}

@keyframes jobseeker-nav-gradient {
  0% {
    background-position: 0% 50%, 100% 50%, 0% 50%;
  }

  50% {
    background-position: 35% 48%, 68% 52%, 50% 50%;
  }

  100% {
    background-position: 100% 50%, 0% 50%, 100% 50%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .jobseeker-topbar::before {
    animation: none;
  }
}

.content-shell {
  display: grid;
  gap: 18px;
}

.drawer-list {
  display: grid;
  gap: 12px;
}

.drawer-toolbar {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--text-muted);
  font-size: 13px;
}

.drawer-item {
  width: 100%;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.drawer-item:hover {
  border-color: rgba(3, 105, 161, 0.18);
  transform: translateY(-1px);
  box-shadow: 0 12px 28px rgba(3, 105, 161, 0.08);
}

.drawer-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.drawer-item p {
  margin: 10px 0;
  color: var(--text-main);
  line-height: 1.7;
}

.drawer-item span {
  color: var(--text-muted);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .layout-shell {
    grid-template-columns: 1fr;
  }

  .layout-header,
  .jobseeker-topbar {
    flex-direction: column;
  }

  .jobseeker-topbar {
    grid-template-columns: 1fr;
    justify-items: start;
    height: auto;
    min-height: unset;
  }

  .jobseeker-menu {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 760px) {
  .layout-shell {
    padding: 12px;
  }

  .header-title {
    font-size: 28px;
  }

  .jobseeker-menu__item {
    width: 100%;
    justify-content: center;
  }
}
</style>
