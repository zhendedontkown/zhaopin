import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '../types'

const AUTH_STORAGE_KEY = 'recruitment-user'
const AUTH_TOKEN_KEY = 'recruitment-token'
const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized'

declare module 'axios' {
  interface AxiosRequestConfig {
    skipErrorToast?: boolean
  }
}

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

function resolveStoredToken() {
  const rawSession = localStorage.getItem(AUTH_STORAGE_KEY)
  const rawToken = localStorage.getItem(AUTH_TOKEN_KEY)

  if (!rawSession) {
    if (rawToken) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
    }
    return null
  }

  try {
    const parsed = JSON.parse(rawSession) as { token?: string }
    const sessionToken = typeof parsed.token === 'string' ? parsed.token : ''

    if (!sessionToken) {
      return rawToken || null
    }

    if (rawToken !== sessionToken) {
      localStorage.setItem(AUTH_TOKEN_KEY, sessionToken)
    }

    return sessionToken
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    localStorage.removeItem(AUTH_TOKEN_KEY)
    return null
  }
}

client.interceptors.request.use((config) => {
  const token = resolveStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload.code !== 200) {
      if (!response.config.skipErrorToast) {
        ElMessage.error(payload.message || '请求失败')
      }
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    response.data = payload.data
    return response
  },
  (error) => {
    const message = error?.response?.data?.message || error?.message || '网络异常'
    if (error?.response?.status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
      localStorage.removeItem(AUTH_STORAGE_KEY)
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT))
      }
    }
    if (!error?.config?.skipErrorToast) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export default client
