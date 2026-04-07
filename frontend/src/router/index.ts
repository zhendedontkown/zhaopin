import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardPage.vue'), meta: { roles: ['COMPANY', 'ADMIN'] } },
      { path: 'jobs', name: 'jobs', component: () => import('../views/JobsPage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
      { path: 'resume', name: 'resume', component: () => import('../views/ResumePage.vue'), meta: { roles: ['JOBSEEKER'] } },
      { path: 'applications', name: 'applications', component: () => import('../views/ApplicationsPage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
      { path: 'admin', name: 'admin', component: () => import('../views/AdminPage.vue'), meta: { roles: ['ADMIN'] } },
      { path: 'chat', name: 'chat', component: () => import('../views/ChatPage.vue'), meta: { roles: ['JOBSEEKER', 'COMPANY'] } },
    ],
  },
]

function defaultRouteByRole(role: string) {
  if (role === 'JOBSEEKER') return '/jobs'
  if (role === 'ADMIN') return '/admin'
  return '/dashboard'
}

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!to.meta.requiresAuth) {
    if (to.path === '/login' && authStore.isLoggedIn) {
      await authStore.fetchProfile()
      return defaultRouteByRole(authStore.role)
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return '/login'
  }

  await authStore.fetchProfile()

  const requiredRoles = (to.meta.roles as string[] | undefined) ?? []
  if (requiredRoles.length && !requiredRoles.includes(authStore.role)) {
    return defaultRouteByRole(authStore.role)
  }

  return true
})
