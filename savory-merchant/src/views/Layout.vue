<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo"><el-icon :size="22"><Shop /></el-icon><span>知味生活</span></div>
      <el-menu :default-active="route.path" router background-color="#2d5a27" text-color="#bfcbd9" active-text-color="#67c23a">
        <el-menu-item index="/dashboard"><el-icon><DataAnalysis /></el-icon><span>经营概览</span></el-menu-item>
        <el-menu-item index="/order"><el-icon><Document /></el-icon><span>订单处理</span></el-menu-item>
        <el-menu-item index="/dish"><el-icon><Food /></el-icon><span>菜品管理</span></el-menu-item>
        <el-menu-item index="/shop"><el-icon><Setting /></el-icon><span>店铺设置</span></el-menu-item>
        <el-menu-item index="/assistant"><el-icon><ChatDotRound /></el-icon><span>AI 经营助手</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ route.meta.title }}</span>
        <div><span style="margin-right:12px;color:#606266">{{ userName }}</span><el-button text @click="logout">退出</el-button></div>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute(); const router = useRouter()
const userName = ref(localStorage.getItem('userName') || '')
function logout() { localStorage.clear(); router.push('/login') }
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #2d5a27; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; gap: 8px; border-bottom: 1px solid rgba(255,255,255,0.1); }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom:1px solid #e6e6e6; padding:0 24px; }
.main { background: #f0f2f5; min-height: calc(100vh - 60px); }
</style>
