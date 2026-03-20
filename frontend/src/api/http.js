import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_STORAGE_KEY = 'course-platform-token'
const USER_STORAGE_KEY = 'course-platform-user'

function clearAuthState() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  localStorage.removeItem(USER_STORAGE_KEY)
}

function isTokenError(payload) {
  return payload?.code === 4010 || (/token/i.test(payload?.message || '') && [4000, 4010].includes(payload?.code))
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload.code !== 0) {
      if (isTokenError(payload)) {
        clearAuthState()
        if (window.location.pathname !== '/login') {
          window.location.assign('/login')
        }
      }
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || 'request failed'))
    }
    return payload.data
  },
  (error) => {
    if (isTokenError(error.response?.data)) {
      clearAuthState()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    return Promise.reject(error)
  },
)

export default http
