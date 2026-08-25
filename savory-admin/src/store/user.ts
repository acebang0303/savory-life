import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, logoutApi } from '@/api'
import type { EmployeeLoginDTO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const name = ref<string>(localStorage.getItem('userName') || '')
  const userId = ref<number>(Number(localStorage.getItem('userId')) || 0)

  async function login(data: EmployeeLoginDTO) {
    const res = await loginApi(data)
    token.value = res.data.token
    name.value = res.data.name
    userId.value = res.data.id
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userName', res.data.name)
    localStorage.setItem('userId', String(res.data.id))
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      name.value = ''
      userId.value = 0
      localStorage.clear()
    }
  }

  return { token, name, userId, login, logout }
})
