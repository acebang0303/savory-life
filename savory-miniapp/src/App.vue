<template>
  <view class="app">
    <slot />
  </view>
</template>

<script setup>
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 登录策略：浏览类接口游客可访问（api 层 silent 模式），交易/互动操作时再引导登录
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
