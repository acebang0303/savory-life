// 订单状态映射
export const orderStatusMap = {
  1: { text: '待支付', color: '#FF6B35' },
  2: { text: '待接单', color: '#1890FF' },
  3: { text: '备货中', color: '#722ED1' },
  4: { text: '待取餐', color: '#13C2C2' },
  5: { text: '已完成', color: '#52C41A' },
  6: { text: '已取消', color: '#999999' },
  7: { text: '已退款', color: '#999999' }
}

// 支付状态映射
export const payStatusMap = {
  0: '未支付',
  1: '已支付',
  2: '已退款'
}

// 格式化时间
export const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const M = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return y + '-' + M + '-' + day + ' ' + h + ':' + m
}

// 格式化价格
export const formatPrice = (price) => {
  return Number(price).toFixed(2)
}
