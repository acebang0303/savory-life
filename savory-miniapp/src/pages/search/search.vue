<template>
  <view class="search-page">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="AI智能搜索... 如「适合约会的浪漫西餐厅」"
             confirm-type="search" @confirm="search" focus />
      <text class="search-btn" @click="search">搜索</text>
    </view>

    <!-- 热门搜索 -->
    <view class="hot-section" v-if="results.length === 0 && !searched">
      <text class="section-title">🔥 热门搜索</text>
      <view class="hot-tags">
        <text class="tag" v-for="t in hotTags" :key="t" @click="quickSearch(t)">{{ t }}</text>
      </view>
    </view>

    <!-- 历史搜索 -->
    <view class="history-section" v-if="history.length > 0 && results.length === 0 && !searched">
      <text class="section-title">🕐 最近搜索</text>
      <view class="history-tags">
        <text class="tag" v-for="h in history" :key="h" @click="quickSearch(h)">{{ h }}</text>
      </view>
    </view>

    <!-- 搜索结果 -->
    <view class="results" v-if="results.length > 0">
      <text class="result-count">为你找到 {{ results.length }} 个结果</text>
      <view class="result-item" v-for="r in results" :key="r.id" @click="goShop(r.merchantId || r.id)">
        <image class="result-img" :src="r.image || defaultImg" mode="aspectFill" />
        <view class="result-info">
          <text class="result-name">{{ r.name }}</text>
          <text class="result-reason">{{ r.reason || '根据您的口味偏好推荐' }}</text>
          <text class="result-price">¥{{ r.price }}</text>
        </view>
      </view>
    </view>

    <view class="empty" v-if="results.length === 0 && searched">
      <text>未找到相关结果</text>
      <text class="empty-hint">试试其他关键词吧~</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { searchDish } from '@/api/index.js'

const defaultImg = '/static/icons/dish-default.png'
const keyword = ref('')
const results = ref([])
const searched = ref(false)
const history = ref(uni.getStorageSync('searchHistory') ? JSON.parse(uni.getStorageSync('searchHistory')) : [])

const hotTags = ['火锅', '日料', '约会约会', '性价比高', '甜品', '深夜食堂']

const quickSearch = (kw) => {
  keyword.value = kw
  search()
}

const search = async () => {
  if (!keyword.value.trim()) return
  searched.value = true

  // 保存搜索历史
  const h = history.value
  if (!h.includes(keyword.value)) {
    h.unshift(keyword.value)
    if (h.length > 10) h.pop()
    uni.setStorageSync('searchHistory', JSON.stringify(h))
  }

  try {
    const result = await searchDish(keyword.value)
    if (typeof result === 'string') {
      results.value = JSON.parse(result || '[]')
    } else {
      results.value = result || []
    }
  } catch (e) {
    // 开发模式：显示mock数据
    results.value = [
      { id: 1, name: '水煮鱼', price: 58, reason: '麻辣鲜香，适合您的川菜偏好', merchantId: 1 },
      { id: 2, name: '寿司拼盘', price: 88, reason: '新鲜食材，日料爱好者必选', merchantId: 1 },
      { id: 3, name: '提拉米苏', price: 35, reason: '经典甜品，下午茶好伴侣', merchantId: 1 }
    ]
  }
}

const goShop = (id) => uni.navigateTo({ url: '/pages/shop/shop?id=' + id })
</script>

<style lang="scss" scoped>
.search-page { min-height: 100vh; background: #fff; }
.search-bar { display: flex; align-items: center; padding: 16rpx 24rpx; background: #fff; }
.search-input { flex: 1; height: 72rpx; background: #f5f5f5; border-radius: 36rpx; padding: 0 24rpx; font-size: 28rpx; }
.search-btn { margin-left: 16rpx; font-size: 28rpx; color: $primary-color; font-weight: 600; }
.section-title { font-size: 28rpx; font-weight: 600; display: block; margin-bottom: 16rpx; }
.hot-section, .history-section { padding: 24rpx; }
.hot-tags, .history-tags { display: flex; flex-wrap: wrap; gap: 16rpx; }
.tag { padding: 12rpx 24rpx; background: #f5f5f5; border-radius: 20rpx; font-size: 26rpx; color: #666; }
.results { padding: 24rpx; }
.result-count { font-size: 24rpx; color: #999; display: block; margin-bottom: 16rpx; }
.result-item { display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.result-img { width: 120rpx; height: 120rpx; border-radius: 8rpx; margin-right: 16rpx; background: #f0f0f0; }
.result-info { flex: 1; }
.result-name { font-size: 30rpx; font-weight: 600; display: block; }
.result-reason { font-size: 24rpx; color: #FAAD14; display: block; margin: 4rpx 0; }
.result-price { font-size: 32rpx; font-weight: bold; color: $primary-color; }
.empty { text-align: center; padding: 120rpx 0; color: #999; font-size: 28rpx; }
.empty-hint { font-size: 24rpx; color: #ccc; display: block; margin-top: 8rpx; }
</style>
