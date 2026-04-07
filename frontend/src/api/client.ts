import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '../types'

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('recruitment-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload.code !== 200) {
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    response.data = payload.data
    return response
  },
  (error) => {
    const message = error?.response?.data?.message || error?.message || '网络异常'
    if (error?.response?.status === 401) {
      localStorage.removeItem('recruitment-token')
      localStorage.removeItem('recruitment-user')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default client
