<template>
  <view class="login-page">
    <view class="logo-section">
      <text class="logo">🍽️</text>
      <text class="app-name">知味生活</text>
      <text class="slogan">探索城市美食，用味蕾丈量世界</text>
    </view>

    <view class="login-form">
      <button class="wx-login-btn" @click="wxLogin" :loading="loading">
        <text class="wx-icon">💬</text>
        <text>微信一键登录</text>
      </button>
      <text class="agreement">
        登录即表示同意《用户协议》和《隐私政策》
      </text>
    </view>

    <button class="skip-btn" @click="skip">暂不登录，先逛逛</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { wxLogin as wxLoginApi } from '@/api/index.js'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()
const loading = ref(false)

const wxLogin = () => {
  loading.value = true
  uni.login({
    provider: 'weixin',
    success: async (loginRes) => {
      try {
        const data = await wxLoginApi(loginRes.code)
        userStore.setLogin(data.token, {
          id: data.id,
          openid: data.openid,
          nickname: '微信用户',
          avatar: ''
        })
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1500)
      } catch (e) {
        // 开发阶段：模拟登录
        const mockData = { id: 1001, openid: 'mock_' + Date.now(), token: 'dev_token_' + Date.now() }
        userStore.setLogin(mockData.token, { id: mockData.id, openid: mockData.openid, nickname: '开发测试用户' })
        uni.showToast({ title: '登录成功(开发模式)', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 1500)
      } finally {
        loading.value = false
      }
    },
    fail: () => {
      loading.value = false
      // 开发阶段：使用模拟登录
      const mockData = { id: 1001, openid: 'mock_' + Date.now(), token: 'dev_token_' + Date.now() }
      userStore.setLogin(mockData.token, { id: mockData.id, openid: mockData.openid, nickname: '开发测试用户' })
      uni.showToast({ title: '模拟登录成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1500)
    }
  })
}

const skip = () => uni.switchTab({ url: '/pages/index/index' })
</script>

<style lang="scss" scoped>
.login-page { min-height: 100vh; background: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60rpx; }
.logo-section { text-align: center; margin-bottom: 80rpx; }
.logo { font-size: 120rpx; display: block; margin-bottom: 16rpx; }
.app-name { font-size: 48rpx; font-weight: bold; color: #333; display: block; }
.slogan { font-size: 26rpx; color: #999; display: block; margin-top: 12rpx; }
.login-form { width: 100%; }
.wx-login-btn {
  width: 100%; height: 96rpx; line-height: 96rpx;
  background: linear-gradient(135deg, #07C160, #06AD56);
  color: #fff; border-radius: 48rpx; font-size: 32rpx;
  display: flex; align-items: center; justify-content: center; gap: 12rpx;
  border: none;
}
.wx-icon { font-size: 36rpx; }
.agreement { display: block; text-align: center; margin-top: 24rpx; font-size: 22rpx; color: #ccc; }
.skip-btn {
  margin-top: 48rpx; background: none; color: #999; font-size: 28rpx;
  border: none;
}
</style>
