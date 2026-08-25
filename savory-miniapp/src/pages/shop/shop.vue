<template>
  <view class="shop-page">
    <!-- 店铺头部 -->
    <view class="shop-header">
      <image class="shop-bg" :src="shop.logo || defaultBg" mode="aspectFill" />
      <view class="shop-mask" />
      <view class="shop-header-info">
        <text class="shop-title">{{ shop.name }}</text>
        <text class="shop-desc">{{ shop.description || '品质美食' }}</text>
        <view class="shop-meta">
          <text>🕐 {{ shop.businessHours || '09:00-22:00' }}</text>
          <text>📍 {{ shop.address || '查看地图' }}</text>
        </view>
      </view>
    </view>

    <!-- 分类Tab -->
    <view class="category-tabs">
      <scroll-view scroll-x class="tab-scroll">
        <view class="tab-item" v-for="c in categories" :key="c.id"
              :class="{ active: activeCate === c.id }"
              @click="switchCate(c)">
          {{ c.name }}
        </view>
      </scroll-view>
    </view>

    <!-- 菜品列表 -->
    <scroll-view scroll-y class="dish-area" :style="{ height: scrollH + 'px' }">
      <!-- 菜品分类 -->
      <view class="dish-group" v-for="c in activeDishCategories" :key="c.id">
        <view class="group-title">{{ c.name }}</view>
        <view class="dish-row" v-for="d in c.dishes" :key="d.id">
          <image class="dish-img" :src="d.image || defaultImg" mode="aspectFill" />
          <view class="dish-content">
            <text class="dish-name">{{ d.name }}</text>
            <text class="dish-desc">{{ d.description || '美味可口' }}</text>
            <text class="dish-sales">月售{{ d.sales || 0 }}份</text>
            <view class="dish-bottom">
              <text class="dish-price">¥{{ d.price }}</text>
              <view class="add-btn" @click="addDish(d)">+</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 套餐分类 -->
      <view class="dish-group" v-for="c in activeSetmealCategories" :key="c.id">
        <view class="group-title">{{ c.name }}</view>
        <view class="dish-row" v-for="s in c.setmeals" :key="s.id">
          <image class="dish-img" :src="s.image || defaultImg" mode="aspectFill" />
          <view class="dish-content">
            <text class="dish-name">{{ s.name }}</text>
            <text class="dish-desc">{{ s.description || '超值套餐' }}</text>
            <view class="dish-bottom">
              <text class="dish-price">¥{{ s.price }}</text>
              <view class="add-btn setmeal-btn" @click="addSetmeal(s)">+</view>
            </view>
          </view>
        </view>
      </view>

      <view class="empty" v-if="!hasDishes">暂无菜品</view>
    </scroll-view>

    <!-- 底部购物车栏 -->
    <view class="cart-bar" v-if="cartStore.items.length > 0">
      <view class="cart-icon-wrap" @click="goCart">
        <text class="cart-icon">🛒</text>
        <text class="cart-badge">{{ cartStore.items.length }}</text>
      </view>
      <text class="cart-total">¥{{ cartStore.totalPrice.toFixed(2) }}</text>
      <button class="checkout-btn" @click="goCart">去结算</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMerchantDetail, getCategoryList, getDishList, getSetmealList } from '@/api/index.js'
import { useCartStore } from '@/store/cart.js'

const cartStore = useCartStore()
const defaultBg = '/static/icons/shop-default.png'
const defaultImg = '/static/icons/dish-default.png'

const shopId = ref(0)
const shop = ref({})
const categories = ref([])
const dishes = ref([])
const setmeals = ref([])
const activeCate = ref(0)

const dishCategories = computed(() => categories.value.filter(c => c.type === 1))
const setmealCategories = computed(() => categories.value.filter(c => c.type === 2))

const activeDishCategories = computed(() => {
  return dishCategories.value
    .filter(c => !activeCate.value || c.id === activeCate.value)
    .map(c => ({
      ...c,
      dishes: dishes.value.filter(d => d.categoryId === c.id)
    }))
    .filter(c => c.dishes.length > 0)
})

const activeSetmealCategories = computed(() => {
  return setmealCategories.value
    .filter(c => !activeCate.value || c.id === activeCate.value)
    .map(c => ({
      ...c,
      setmeals: setmeals.value.filter(s => s.categoryId === c.id)
    }))
    .filter(c => c.setmeals.length > 0)
})

const hasDishes = computed(() =>
  activeDishCategories.value.length > 0 || activeSetmealCategories.value.length > 0
)

const scrollH = computed(() => {
  const sys = uni.getSystemInfoSync()
  return sys.windowHeight - 380
})

const switchCate = (cate) => {
  activeCate.value = activeCate.value === cate.id ? 0 : cate.id
}

const addDish = async (dish) => {
  try {
    await cartStore.add({
      dishId: dish.id, merchantId: shopId.value, name: dish.name,
      image: dish.image, amount: dish.price, number: 1
    })
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '请先登录', icon: 'none' })
  }
}

const addSetmeal = async (s) => {
  try {
    await cartStore.add({
      setmealId: s.id, merchantId: shopId.value, name: s.name,
      image: s.image, amount: s.price, number: 1
    })
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '请先登录', icon: 'none' })
  }
}

const goCart = () => uni.switchTab({ url: '/pages/cart/cart' })

onMounted(async () => {
  const pages = getCurrentPages()
  const options = pages[pages.length - 1].$page.options || {}
  shopId.value = Number(options.id)
  try {
    const [shopRes, cates, dishList, setmealList] = await Promise.all([
      getMerchantDetail(shopId.value),
      getCategoryList(shopId.value),
      getDishList(null),
      getSetmealList(null)
    ])
    shop.value = shopRes
    categories.value = cates || []
    dishes.value = dishList || []
    setmeals.value = setmealList || []
  } catch (e) {
    console.log('加载店铺失败', e)
  }
  cartStore.fetchCart()
})
</script>

<style lang="scss" scoped>
.shop-page { padding-bottom: 120rpx; background: $bg-color; min-height: 100vh; }
.shop-header { position: relative; height: 320rpx; overflow: hidden; }
.shop-bg { width: 100%; height: 100%; }
.shop-mask {
  position: absolute; inset: 0;
  background: linear-gradient(transparent 30%, rgba(0,0,0,0.6));
}
.shop-header-info { position: absolute; bottom: 20rpx; left: 24rpx; right: 24rpx; }
.shop-title { color: #fff; font-size: 40rpx; font-weight: bold; display: block; }
.shop-desc { color: rgba(255,255,255,0.85); font-size: 26rpx; display: block; margin-top: 4rpx; }
.shop-meta { display: flex; gap: 24rpx; margin-top: 8rpx; font-size: 22rpx; color: rgba(255,255,255,0.75); }
.category-tabs { background: #fff; padding: 0 24rpx; }
.tab-scroll { white-space: nowrap; display: flex; }
.tab-item {
  display: inline-block; padding: 20rpx 28rpx; font-size: 28rpx;
  color: #666; position: relative; flex-shrink: 0;
}
.tab-item.active { color: $primary-color; font-weight: 600; }
.tab-item.active::after {
  content: ''; position: absolute; bottom: 6rpx; left: 50%;
  transform: translateX(-50%); width: 32rpx; height: 4rpx;
  background: $primary-color; border-radius: 2rpx;
}
.dish-area { padding: 16rpx 24rpx; }
.group-title { font-size: 30rpx; font-weight: bold; padding: 16rpx 0 8rpx; }
.dish-row {
  display: flex; padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0;
}
.dish-img { width: 160rpx; height: 160rpx; border-radius: 12rpx; margin-right: 16rpx; background: #f0f0f0; }
.dish-content { flex: 1; display: flex; flex-direction: column; }
.dish-name { font-size: 30rpx; font-weight: 600; }
.dish-desc { font-size: 24rpx; color: #999; margin-top: 4rpx; }
.dish-sales { font-size: 22rpx; color: #ccc; margin-top: 4rpx; }
.dish-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: auto; }
.dish-price { font-size: 32rpx; font-weight: bold; color: $primary-color; }
.add-btn {
  width: 48rpx; height: 48rpx; border-radius: 50%; background: $primary-color;
  color: #fff; font-size: 32rpx; display: flex; align-items: center;
  justify-content: center; line-height: 1;
}
.setmeal-btn { background: #722ED1; }
.cart-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  display: flex; align-items: center; padding: 16rpx 24rpx;
  background: #fff; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.08);
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.cart-icon-wrap { position: relative; margin-right: 16rpx; }
.cart-icon { font-size: 48rpx; }
.cart-badge {
  position: absolute; top: -8rpx; right: -12rpx;
  min-width: 32rpx; height: 32rpx; line-height: 32rpx;
  background: $error-color; color: #fff; font-size: 20rpx;
  border-radius: 16rpx; text-align: center; padding: 0 6rpx;
}
.cart-total { flex: 1; font-size: 32rpx; font-weight: bold; color: $primary-color; }
.checkout-btn {
  height: 72rpx; line-height: 72rpx; padding: 0 32rpx;
  background: linear-gradient(135deg, $primary-color, $primary-light);
  color: #fff; border-radius: 36rpx; font-size: 28rpx; border: none;
}
.empty { text-align: center; padding: 80rpx 0; color: #999; font-size: 28rpx; }
</style>
