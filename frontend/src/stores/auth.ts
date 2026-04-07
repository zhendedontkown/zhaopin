import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import client from '../api/client'
import type { RoleCode, UserProfileResponse, UserSession } from '../types'

export const AUTH_STORAGE_KEY = 'recruitment-user'
export const AUTH_TOKEN_KEY = 'recruitment-token'

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
      ]
    }

    if (role.value === 'ADMIN') {
      return [
        { key: 'dashboard', label: '工作台', path: '/dashboard' },
        { key: 'admin', label: '系统管理', path: '/admin' },
      ]
    }

    return [
      { key: 'dashboard', label: '工作台', path: '/dashboard' },
      { key: 'jobs', label: '岗位管理', path: '/jobs' },
      { key: 'applications', label: '投递处理', path: '/applications' },
      { key: 'chat', label: '在线沟通', path: '/chat' },
    ]
  })

  function setSession(nextSession: UserSession | null) {
    session.value = nextSession
    if (nextSession) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession))
      localStorage.setItem(AUTH_TOKEN_KEY, nextSession.token)
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      localStorage.removeItem(AUTH_TOKEN_KEY)
      profile.value = null
      profileLoaded.value = false
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
    profile.value = nextProfile
    profileLoaded.value = true

    const tokenRole = getRoleFromToken(session.value.token)
    if (session.value && tokenRole && session.value.role !== tokenRole) {
      setSession({ ...session.value, role: tokenRole })
    }

    return nextProfile
  }

  function logout() {
    setSession(null)
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
  }
})

function loadStoredSession(): UserSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as UserSession
    const tokenRole = getRoleFromToken(parsed.token)
    return tokenRole ? { ...parsed, role: tokenRole } : parsed
  } catch {
    return null
  }
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
