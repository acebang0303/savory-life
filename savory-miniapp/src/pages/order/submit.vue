<template>
  <view class="submit-page">
    <view class="addr-section" @click="chooseAddr">
      <view class="addr-info" v-if="address">
        <text class="addr-contact">{{ address.consignee }} {{ address.phone }}</text>
        <text class="addr-detail">{{ address.provinceName }}{{ address.cityName }}{{ address.districtName }}{{ address.detail }}</text>
      </view>
      <view class="addr-empty" v-else>
        <text>请选择收货地址</text>
      </view>
      <text class="addr-arrow">→</text>
    </view>

    <view class="goods-section">
      <text class="section-title">商品明细</text>
      <view class="goods-item" v-for="item in items" :key="item.dishId || item.setmealId">
        <text>{{ item.name }} x{{ item.number }}</text>
        <text>¥{{ (item.amount * item.number).toFixed(2) }}</text>
      </view>
    </view>

    <view class="coupon-section">
      <text>优惠券</text>
      <text class="coupon-val">无可用</text>
    </view>

    <view class="total-section">
      <text>实付金额</text>
      <text class="total-price">¥{{ totalPrice }}</text>
    </view>

    <button class="pay-btn" @click="confirmPay">确认支付 ¥{{ totalPrice }}</button>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAddressList, submitOrder } from '@/api/index.js'
import { useCartStore } from '@/store/cart.js'

const cartStore = useCartStore()
const address = ref(null)
const items = ref([])
const totalPrice = ref(0)

onMounted(async () => {
  items.value = cartStore.items
  totalPrice.value = cartStore.totalPrice.toFixed(2)
  try {
    const addrs = await getAddressList()
    if (addrs && addrs.length > 0) {
      address.value = addrs.find(a => a.isDefault === 1) || addrs[0]
    }
  } catch (e) { console.log('加载地址失败', e) }
})

const chooseAddr = () => {
  uni.chooseAddress({
    success: (res) => {
      address.value = {
        consignee: res.userName,
        phone: res.telNumber,
        provinceName: res.provinceName,
        cityName: res.cityName,
        districtName: res.countyName,
        detail: res.detailInfo
      }
    }
  })
}

const confirmPay = async () => {
  if (!address.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中' })
  try {
    await submitOrder({
      addressBookId: address.value.id || 1,
      merchantId: items.value[0]?.merchantId,
      amount: Number(totalPrice.value)
    })
    uni.hideLoading()
    await cartStore.clear()
    uni.showToast({ title: '下单成功' })
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '失败' })
  }
}
</script>

<style lang="scss" scoped>
.submit-page { min-height: 100vh; background: $bg-color; padding-bottom: 140rpx; }
.addr-section { display: flex; align-items: center; padding: 24rpx; background: #fff; margin: 16rpx 24rpx; border-radius: 12rpx; }
.addr-info { flex: 1; }
.addr-contact { font-size: 30rpx; font-weight: 600; display: block; }
.addr-detail { font-size: 24rpx; color: #666; margin-top: 4rpx; display: block; }
.addr-empty { flex: 1; color: #999; }
.addr-arrow { font-size: 32rpx; color: #ccc; }
.goods-section { background: #fff; margin: 0 24rpx 16rpx; border-radius: 12rpx; padding: 20rpx; }
.section-title { font-size: 28rpx; font-weight: 600; display: block; margin-bottom: 12rpx; }
.goods-item { display: flex; justify-content: space-between; padding: 8rpx 0; font-size: 26rpx; }
.coupon-section { display: flex; justify-content: space-between; background: #fff; margin: 0 24rpx 16rpx; border-radius: 12rpx; padding: 20rpx; font-size: 26rpx; }
.coupon-val { color: #999; }
.total-section { display: flex; justify-content: space-between; background: #fff; margin: 0 24rpx; border-radius: 12rpx; padding: 20rpx; }
.total-price { font-size: 36rpx; font-weight: bold; color: $primary-color; }
.pay-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: linear-gradient(135deg, $primary-color, $primary-light);
  color: #fff; border-radius: 44rpx; font-size: 32rpx; border: none;
}
</style>
