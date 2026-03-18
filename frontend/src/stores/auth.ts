import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, UserRole } from '@/types'
import { mockLogin, mockRegister, mockSendSms } from '@/mock'
import { useRouter } from 'vue-router'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(null)

  // Initialize from localStorage
  const storedUser = localStorage.getItem('edu_user')
  const storedToken = localStorage.getItem('edu_access_token')
  if (storedUser && storedToken) {
    user.value = JSON.parse(storedUser)
    accessToken.value = storedToken
  }

  const isLoggedIn = computed(() => !!accessToken.value)

  async function login(phone: string, code: string) {
    const result = await mockLogin(phone, code)
    user.value = result.user
    accessToken.value = result.accessToken
    localStorage.setItem('edu_user', JSON.stringify(result.user))
    localStorage.setItem('edu_access_token', result.accessToken)
  }

  async function register(phone: string, code: string, username: string, role: UserRole) {
    const result = await mockRegister(phone, code, username, role)
    user.value = result.user
    accessToken.value = result.accessToken
    localStorage.setItem('edu_user', JSON.stringify(result.user))
    localStorage.setItem('edu_access_token', result.accessToken)
  }

  async function sendSms(phone: string) {
    return mockSendSms(phone)
  }

  function logout() {
    user.value = null
    accessToken.value = null
    localStorage.removeItem('edu_user')
    localStorage.removeItem('edu_access_token')
  }

  return {
    user,
    accessToken,
    isLoggedIn,
    login,
    register,
    sendSms,
    logout
  }
})
