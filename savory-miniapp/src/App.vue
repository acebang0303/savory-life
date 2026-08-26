<template>
  <view class="app">
    <slot />
  </view>
</template>

<script setup>
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 检查登录状态
  const token = uni.getStorageSync('token')
  if (!token) {
    // 未登录时引导登录（不强制，游客可浏览）
    console.log('游客模式 - 部分功能需要登录后使用')
  }
})
</script>

<style lang="scss">
@import '@/uni.scss';

page {
  background-color: #FFF8F1;
  font-family: 'PingFang SC', 'HarmonyOS Sans SC', -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: 28rpx;
  color: #33261E;
  line-height: 1.6;
}

.app {
  min-height: 100vh;
}
</style>
