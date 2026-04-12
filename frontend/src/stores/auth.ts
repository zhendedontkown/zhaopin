import { computed, ref } from 'vue'
import { defineStore, type Pinia } from 'pinia'
import type { Router } from 'vue-router'
import client, { resetUnauthorizedHandling } from '../api/client'
import type { RoleCode, UserProfileResponse, UserSession } from '../types'

export const AUTH_STORAGE_KEY = 'recruitment-user'
export const AUTH_TOKEN_KEY = 'recruitment-token'
export const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized'

let authLifecycleBound = false

export function defaultRouteByRole(role: RoleCode | string) {
  if (role === 'JOBSEEKER') return '/jobs'
  if (role === 'ADMIN') return '/admin/dashboard'
  return '/dashboard'
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<UserSession | null>(loadStoredSession())
  const profile = ref<UserProfileResponse | null>(null)
  const profileLoaded = ref(false)

  const isLoggedIn = computed(() => Boolean(session.value?.token))
  const role = computed<RoleCode>(() => session.value?.role ?? '')
  const workspaceLabel = computed(() => profile.value?.workspaceLabel ?? '')

  const menuItems = computed(() => {
    if (role.value === 'JOBSEEKER') {
      return [
        { key: 'jobs', label: '岗位大厅', path: '/jobs' },
        { key: 'resume', label: '简历创作', path: '/resume' },
        { key: 'applications', label: '投递记录', path: '/applications' },
        { key: 'chat', label: '在线沟通', path: '/chat' },
        { key: 'profile', label: '我的', path: '/profile' },
      ]
    }

    if (role.value === 'ADMIN') {
      return [
        { key: 'admin-dashboard', label: '数据统计', path: '/admin/dashboard' },
        { key: 'admin-users', label: '用户管理', path: '/admin/users' },
        { key: 'admin-company-audits', label: '企业审核', path: '/admin/company-audits' },
        { key: 'admin-jobs', label: '岗位管理', path: '/admin/jobs' },
        { key: 'admin-applications', label: '投递记录', path: '/admin/applications' },
      ]
    }

    return [
      { key: 'dashboard', label: '工作台', path: '/dashboard' },
      { key: 'jobs', label: '岗位管理', path: '/jobs' },
      { key: 'applications', label: '投递处理', path: '/applications' },
      { key: 'chat', label: '在线沟通', path: '/chat' },
      { key: 'profile', label: '我的', path: '/profile' },
    ]
  })

  function setSession(nextSession: UserSession | null) {
    const sessionChanged = session.value?.token !== nextSession?.token || session.value?.userId !== nextSession?.userId

    if (sessionChanged) {
      profile.value = null
      profileLoaded.value = false
    }

    session.value = nextSession

    if (nextSession) {
      resetUnauthorizedHandling()
      persistSession(nextSession)
      return
    }

    clearStoredAuth()
  }

  function applyProfile(nextProfile: UserProfileResponse | null) {
    profile.value = nextProfile
    profileLoaded.value = Boolean(nextProfile)

    if (!session.value || !nextProfile) return

    const tokenRole = getRoleFromToken(session.value.token)
    const nextRole = tokenRole || nextProfile.primaryRole || session.value.role
    if (session.value.displayName !== nextProfile.displayName || session.value.role !== nextRole) {
      setSession({
        ...session.value,
        displayName: nextProfile.displayName,
        role: nextRole,
      })
    }
  }

  async function login(payload: { account: string; password: string }) {
    const response = await client.post('/auth/login', payload)
    setSession(response.data as UserSession)
    await fetchProfile(true)
  }

  async function fetchProfile(force = false) {
    if (!session.value?.token) return null
    if (profileLoaded.value && !force) return profile.value

    const response = await client.get('/auth/me')
    const nextProfile = response.data as UserProfileResponse
    applyProfile(nextProfile)
    return nextProfile
  }

  function logout() {
    setSession(null)
  }

  function handleUnauthorized() {
    setSession(null)
  }

  function hydrateFromStorage() {
    const nextSession = loadStoredSession()
    if (nextSession) {
      setSession(nextSession)
    } else {
      handleUnauthorized()
    }
  }

  return {
    session,
    profile,
    profileLoaded,
    role,
    workspaceLabel,
    menuItems,
    isLoggedIn,
    login,
    fetchProfile,
    logout,
    setSession,
    applyProfile,
    handleUnauthorized,
    hydrateFromStorage,
  }
})

export function initializeAuthLifecycle(pinia: Pinia, router?: Router) {
  if (authLifecycleBound || typeof window === 'undefined') {
    return
  }

  authLifecycleBound = true

  const authStore = useAuthStore(pinia)
  const syncRouteWithAuth = async () => {
    const currentRoute = router?.currentRoute.value

    if (!authStore.isLoggedIn) {
      if (currentRoute?.meta.requiresAuth && currentRoute.path !== '/login') {
        await router?.replace('/login')
      }
      return
    }

    try {
      await authStore.fetchProfile(true)
    } catch {
      if (currentRoute?.meta.requiresAuth && currentRoute.path !== '/login') {
        await router?.replace('/login')
      }
      return
    }

    if (!currentRoute?.meta.requiresAuth || !currentRoute.meta.roles) {
      return
    }

    const allowedRoles = currentRoute.meta.roles as string[]
    if (!allowedRoles.includes(authStore.role)) {
      await router?.replace(defaultRouteByRole(authStore.role))
    }
  }

  window.addEventListener(AUTH_UNAUTHORIZED_EVENT, () => {
    authStore.handleUnauthorized()
    void syncRouteWithAuth()
  })

  window.addEventListener('storage', (event) => {
    if (event.key && event.key !== AUTH_STORAGE_KEY && event.key !== AUTH_TOKEN_KEY) {
      return
    }

    authStore.hydrateFromStorage()
    void syncRouteWithAuth()
  })
}

function loadStoredSession(): UserSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  const rawToken = localStorage.getItem(AUTH_TOKEN_KEY)

  if (!raw) {
    if (rawToken) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
    }
    return null
  }

  try {
    const parsed = JSON.parse(raw) as UserSession
    if (!parsed?.token) {
      clearStoredAuth()
      return null
    }

    const tokenRole = getRoleFromToken(parsed.token)
    const normalized = tokenRole ? { ...parsed, role: tokenRole } : parsed
    persistSession(normalized)
    return normalized
  } catch {
    clearStoredAuth()
    return null
  }
}

function persistSession(nextSession: UserSession) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession))
  localStorage.setItem(AUTH_TOKEN_KEY, nextSession.token)
}

function clearStoredAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY)
  localStorage.removeItem(AUTH_TOKEN_KEY)
}

function getRoleFromToken(token?: string): RoleCode {
  if (!token) return ''

  try {
    const [, payload] = token.split('.')
    if (!payload) return ''
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = JSON.parse(decodeBase64(normalized)) as { roles?: RoleCode[] }
    return decoded.roles?.[0] ?? ''
  } catch {
    return ''
  }
}

function decodeBase64(value: string) {
  if (typeof window !== 'undefined' && typeof window.atob === 'function') {
    const padded = value + '='.repeat((4 - (value.length % 4)) % 4)
    return decodeURIComponent(
      Array.from(window.atob(padded))
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join(''),
    )
  }

  return ''
}
