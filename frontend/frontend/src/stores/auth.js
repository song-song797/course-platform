import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { changePassword, getMe, login, logout } from '../api/auth'

const TOKEN_STORAGE_KEY = 'course-platform-token'
const USER_STORAGE_KEY = 'course-platform-user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_STORAGE_KEY) || '')
  const user = ref(token.value ? JSON.parse(localStorage.getItem(USER_STORAGE_KEY) || 'null') : null)

  const isLoggedIn = computed(() => Boolean(token.value))

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    localStorage.removeItem(USER_STORAGE_KEY)
  }

  async function loginByPassword(payload) {
    const data = await login(payload)
    token.value = data.token
    user.value = data.user
    localStorage.setItem(TOKEN_STORAGE_KEY, data.token)
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(data.user))
    return data.user
  }

  async function refreshMe() {
    if (!token.value) return null
    user.value = await getMe()
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user.value))
    return user.value
  }

  async function updatePassword(newPassword) {
    await changePassword({ newPassword })
    if (user.value) {
      user.value = { ...user.value, firstLoginResetRequired: false }
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user.value))
    }
  }

  async function signOut() {
    if (token.value) {
      await logout()
    }
    clearAuth()
  }

  return {
    token,
    user,
    isLoggedIn,
    clearAuth,
    loginByPassword,
    refreshMe,
    updatePassword,
    signOut,
  }
})
