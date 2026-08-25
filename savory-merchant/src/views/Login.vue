<template>
  <div class="login-container">
    <div class="login-card">
      <h1>知味生活</h1>
      <p>商家管理中心</p>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" placeholder="请输入用户名" size="large" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" @keyup.enter="login" show-password /></el-form-item>
        <el-form-item><el-button type="primary" size="large" :loading="loading" block @click="login">登 录</el-button></el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'merchant01', password: '123456' })

async function login() {
  loading.value = true
  try {
    const res = await http.post('/employee/login', form)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userName', res.data.name)
    localStorage.setItem('empId', res.data.id)
    ElMessage.success('登录成功')
    router.push('/')
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.login-card { width: 400px; padding: 40px; background: #fff; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
h1 { text-align: center; font-size: 28px; color: #303133; margin-bottom: 4px; }
p { text-align: center; color: #909399; margin-bottom: 32px; }
</style>
