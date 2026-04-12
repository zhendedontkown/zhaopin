import { createRouter, createWebHistory } from 'vue-router'
import { defaultRouteByRole, useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginPage.vue'),
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: () => {
          const authStore = useAuthStore()
          return defaultRouteByRole(authStore.role)
        },
      },
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardPage.vue'), meta: { roles: ['COMPANY'] } },
      { path: 'jobs', name: 'jobs', component: () => import('../views/JobsPage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
      { path: 'resume', name: 'resume', component: () => import('../views/ResumePage.vue'), meta: { roles: ['JOBSEEKER'] } },
      { path: 'applications', name: 'applications', component: () => import('../views/ApplicationsPage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
      {
        path: 'admin',
        component: () => import('../views/AdminPage.vue'),
        meta: { roles: ['ADMIN'] },
        children: [
          { path: '', redirect: '/admin/dashboard' },
          { path: 'dashboard', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboardPage.vue') },
          { path: 'users', name: 'admin-users', component: () => import('../views/admin/AdminUsersPage.vue') },
          { path: 'company-audits', name: 'admin-company-audits', component: () => import('../views/admin/AdminCompanyAuditsPage.vue') },
          { path: 'jobs', name: 'admin-jobs', component: () => import('../views/admin/AdminJobsPage.vue') },
          { path: 'applications', name: 'admin-applications', component: () => import('../views/admin/AdminApplicationsPage.vue') },
        ],
      },
      { path: 'chat', name: 'chat', component: () => import('../views/ChatPage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
      { path: 'profile', name: 'profile', component: () => import('../views/ProfilePage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

async function ensureProfile(authStore: ReturnType<typeof useAuthStore>) {
  try {
    await authStore.fetchProfile()
    return true
  } catch {
    return authStore.isLoggedIn
  }
}

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!to.meta.requiresAuth) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      const ready = await ensureProfile(authStore)
      if (ready && authStore.isLoggedIn) {
        return defaultRouteByRole(authStore.role)
      }
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return '/login'
  }

  const ready = await ensureProfile(authStore)
  if (!ready || !authStore.isLoggedIn) {
    return '/login'
  }

  const requiredRoles = (to.meta.roles as string[] | undefined) ?? []
  if (requiredRoles.length && !requiredRoles.includes(authStore.role)) {
    return defaultRouteByRole(authStore.role)
  }

  return true
})
