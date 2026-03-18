import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { changePassword, getMe, login, logout } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('course-platform-demo-token') || '')
  const user = ref(token.value ? JSON.parse(localStorage.getItem('course-platform-demo-user') || 'null') : null)

  const isLoggedIn = computed(() => Boolean(token.value))

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('course-platform-demo-token')
    localStorage.removeItem('course-platform-demo-user')
  }

  async function loginByPassword(payload) {
    const data = await login(payload)
    token.value = data.token
    user.value = data.user
    localStorage.setItem('course-platform-demo-token', data.token)
    localStorage.setItem('course-platform-demo-user', JSON.stringify(data.user))
    return data.user
  }

  async function refreshMe() {
    if (!token.value) return null
    user.value = await getMe()
    localStorage.setItem('course-platform-demo-user', JSON.stringify(user.value))
    return user.value
  }

  async function updatePassword(newPassword) {
    await changePassword({ newPassword })
    if (user.value) {
      user.value = { ...user.value, firstLoginResetRequired: false }
      localStorage.setItem('course-platform-demo-user', JSON.stringify(user.value))
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
