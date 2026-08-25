const BASE_URL = 'http://localhost:8080'

// 请求拦截：自动携带 token
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    const header = {
      'Content-Type': 'application/json'
    }
    if (token) {
      header['Authorization'] = token
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: (res) => {
        if (res.data.code === 1) {
          resolve(res.data.data)
        } else if (res.data.code === 0 && res.data.msg === '用户未登录') {
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('未登录'))
        } else {
          reject(new Error(res.data.msg || '请求失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

// ===== 认证 =====
export const wxLogin = (code) => request({
  url: '/user/user/login',
  method: 'POST',
  data: { code }
})

// ===== 店铺 =====
export const getMerchantList = () => request({
  url: '/user/merchant/list'
})

export const getMerchantDetail = (id) => request({
  url: '/user/merchant/' + id
})

export const getCategoryList = (merchantId, type) => request({
  url: '/user/category/list',
  data: { merchantId, type }
})

export const getDishList = (categoryId) => request({
  url: '/user/dish/list',
  data: { categoryId }
})

export const getSetmealList = (categoryId) => request({
  url: '/user/setmeal/list',
  data: { categoryId }
})

export const searchDish = (keyword) => request({
  url: '/user/dish/search',
  data: { keyword }
})

// ===== 购物车 =====
export const addToCart = (item) => request({
  url: '/user/cart/add',
  method: 'POST',
  data: item
})

export const getCartList = () => request({
  url: '/user/cart/list'
})

export const updateCartNum = (field, number) => request({
  url: '/user/cart/' + field + '/number',
  method: 'PUT',
  data: { number }
})

export const deleteCartItem = (field) => request({
  url: '/user/cart/' + field,
  method: 'DELETE'
})

export const clearCart = () => request({
  url: '/user/cart/clear',
  method: 'DELETE'
})

// ===== 订单 =====
export const submitOrder = (data) => request({
  url: '/user/order/submit',
  method: 'POST',
  data
})

export const getOrderPage = (page, pageSize, status) => request({
  url: '/user/order/page',
  data: { page, pageSize, status }
})

export const cancelOrder = (id) => request({
  url: '/user/order/' + id + '/cancel',
  method: 'PUT'
})

export const payOrder = (orderId) => request({
  url: '/user/order/pay',
  method: 'POST',
  data: { orderId }
})

// ===== 地址 =====
export const getAddressList = () => request({
  url: '/user/address'
})

export const addAddress = (data) => request({
  url: '/user/address',
  method: 'POST',
  data
})

// ===== 优惠券 =====
export const getUserCouponList = (page, pageSize) => request({
  url: '/user/coupon/list',
  data: { page, pageSize }
})

export const receiveCoupon = (templateId) => request({
  url: '/user/coupon/receive/' + templateId,
  method: 'POST'
})

// ===== 签到 =====
export const signToday = () => request({
  url: '/user/sign',
  method: 'POST'
})

export const getSignToday = () => request({
  url: '/user/sign/today'
})

export const getSignMonth = () => request({
  url: '/user/sign/month'
})

// ===== 笔记社区 =====
export const getNoteFeed = (page, pageSize) => request({
  url: '/user/note/feed',
  data: { page, pageSize }
})

export const getNoteHot = (page, pageSize) => request({
  url: '/user/note/hot',
  data: { page, pageSize }
})

export const publishNote = (data) => request({
  url: '/user/note',
  method: 'POST',
  data
})

export const likeNote = (id) => request({
  url: '/user/note/' + id + '/like',
  method: 'POST'
})

export const collectNote = (id) => request({
  url: '/user/note/' + id + '/collect',
  method: 'POST'
})

export const followUser = (userId) => request({
  url: '/user/follow/' + userId,
  method: 'POST'
})

// ===== 秒杀 =====
export const getSeckillList = () => request({
  url: '/user/seckill/list'
})

export const buySeckill = (id) => request({
  url: '/user/seckill/' + id + '/buy',
  method: 'POST'
})

// ===== AI推荐 =====
export const getAiRecommend = (userId, topN = 10) => request({
  url: '/ai/recommend/dish',
  data: { userId, topN }
})

export default {
  wxLogin, getMerchantList, getMerchantDetail, getCategoryList,
  getDishList, getSetmealList, searchDish,
  addToCart, getCartList, updateCartNum, deleteCartItem, clearCart,
  submitOrder, getOrderPage, cancelOrder, payOrder,
  getAddressList, addAddress,
  getUserCouponList, receiveCoupon,
  signToday, getSignToday, getSignMonth,
  getNoteFeed, getNoteHot, publishNote, likeNote, collectNote, followUser,
  getSeckillList, buySeckill, getAiRecommend
}
