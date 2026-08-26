<template>
  <view class="profile-page">
    <!-- 用户头部 -->
    <view class="user-header" @click="goLogin" v-if="!userStore.isLogin">
      <image class="avatar" :src="defaultAvatar" mode="aspectFill" />
      <view class="user-text">
        <text class="login-hint">点击登录</text>
        <text class="login-sub">登录后享受更多服务</text>
      </view>
      <text class="arrow">→</text>
    </view>

    <view class="user-header" v-else>
      <image class="avatar" :src="userStore.userInfo?.avatar || defaultAvatar" mode="aspectFill" />
      <view class="user-text">
        <text class="nickname">{{ userStore.userInfo?.nickname || '用户' }}</text>
        <text class="level">Lv.{{ userStore.userInfo?.level || 1 }}</text>
      </view>
    </view>

    <!-- 数据统计 -->
    <view class="stats">
      <view class="stat-item" @click="goOrder">
        <text class="stat-num">{{ orderCount }}</text>
        <text class="stat-label">订单</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ collectCount }}</text>
        <text class="stat-label">收藏</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ followCount }}</text>
        <text class="stat-label">关注</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ growthValue }}</text>
        <text class="stat-label">成长值</text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-section">
      <view class="menu-item" @click="goOrder">
        <text>📋 我的订单</text>
        <text class="menu-arrow">→</text>
      </view>
      <view class="menu-item" @click="goCoupon">
        <text>🎫 优惠券</text>
        <text class="menu-arrow">→</text>
      </view>
      <view class="menu-item" @click="doSign">
        <text>📅 每日签到</text>
        <text class="menu-val">{{ signedToday ? '已签到' : '签到领积分' }}</text>
      </view>
      <view class="menu-item" @click="goMyNotes">
        <text>📝 我的笔记</text>
        <text class="menu-arrow">→</text>
      </view>
      <view class="menu-item" @click="goAddress">
        <text>📍 收货地址</text>
        <text class="menu-arrow">→</text>
      </view>
      <view class="menu-item">
        <text>🤖 AI客服</text>
        <text class="menu-arrow">→</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <button class="logout-btn" v-if="userStore.isLogin" @click="logout">退出登录</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user.js'
import { signToday, getSignToday } from '@/api/index.js'

const userStore = useUserStore()
const defaultAvatar = '/static/icons/profile.png'
const orderCount = ref(0)
const collectCount = ref(0)
const followCount = ref(0)
const growthValue = ref(0)
const signedToday = ref(false)

const goLogin = () => uni.navigateTo({ url: '/pages/login/login' })
const goOrder = () => uni.navigateTo({ url: '/pages/order/order' })
const goCoupon = () => uni.showToast({ title: '优惠券功能开发中', icon: 'none' })
const goMyNotes = () => uni.switchTab({ url: '/pages/note/note' })
const goAddress = () => uni.showToast({ title: '地址管理开发中', icon: 'none' })

const doSign = async () => {
  if (!userStore.isLogin) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    await signToday()
    signedToday.value = true
    uni.showToast({ title: '签到成功！+5成长值', icon: 'success' })
    growthValue.value += 5
  } catch (e) {
    uni.showToast({ title: e.message || '签到失败', icon: 'none' })
  }
}

const logout = () => {
  uni.showModal({
    title: '退出登录',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.showToast({ title: '已退出登录', icon: 'success' })
      }
    }
  })
}

onMounted(async () => {
  if (userStore.isLogin) {
    try {
      const today = await getSignToday()
      signedToday.value = today?.signed || false
    } catch (e) { console.log(e) }
  }
})
</script>

<style lang="scss" scoped>
.profile-page { min-height: 100vh; background: $bg-color; padding-bottom: 80rpx; }
.user-header {
  display: flex; align-items: center; padding: 48rpx 32rpx;
  background: linear-gradient(135deg, $primary-color, $primary-light);
}
.avatar { width: 100rpx; height: 100rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,0.3); margin-right: 20rpx; background: #f0f0f0; }
.user-text { flex: 1; }
.login-hint { color: #fff; font-size: 36rpx; font-weight: bold; display: block; }
.login-sub { color: rgba(255,255,255,0.8); font-size: 24rpx; }
.nickname { color: #fff; font-size: 34rpx; font-weight: bold; display: block; }
.level { color: rgba(255,255,255,0.8); font-size: 24rpx; }
.arrow { color: rgba(255,255,255,0.6); font-size: 36rpx; }
.stats {
  display: flex; background: #fff; margin: -20rpx 24rpx 20rpx;
  border-radius: 16rpx; padding: 24rpx 0; box-shadow: $shadow;
}
.stat-item { flex: 1; text-align: center; }
.stat-num { font-size: 36rpx; font-weight: bold; color: #333; display: block; }
.stat-label { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.menu-section { background: #fff; margin: 0 24rpx 16rpx; border-radius: 16rpx; overflow: hidden; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 28rpx 24rpx; border-bottom: 1rpx solid #f5f5f5; font-size: 28rpx; }
.menu-arrow { color: #ccc; }
.menu-val { color: $primary-color; font-size: 26rpx; }
.logout-btn { width: calc(100% - 48rpx); height: 88rpx; line-height: 88rpx; background: #fff; color: $error-color; border-radius: 16rpx; font-size: 28rpx; margin: 40rpx 24rpx; border: none; }
</style>
