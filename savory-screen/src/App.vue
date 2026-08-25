<template>
  <div class="screen">
    <header class="header"><h1>知味生活 · SavoryLife 数据大屏</h1><div class="time">{{ time }}</div></header>
    <main class="grid">
      <!-- 左上：实时订单数 -->
      <div class="card"><h3>📊 实时订单量</h3><div ref="orderRef" class="chart"></div></div>
      <!-- 中上：核心指标 -->
      <div class="card row-span-2">
        <h3>📈 核心指标</h3>
        <div class="kpi-grid">
          <div class="kpi"><span class="kpi-val">{{ kpi.todayOrders }}</span><span class="kpi-label">今日订单</span></div>
          <div class="kpi"><span class="kpi-val">¥{{ kpi.todayRevenue }}</span><span class="kpi-label">今日营收</span></div>
          <div class="kpi"><span class="kpi-val">{{ kpi.pendingOrders }}</span><span class="kpi-label">待处理</span></div>
          <div class="kpi"><span class="kpi-val">{{ kpi.onlineShops }}</span><span class="kpi-label">在线商户</span></div>
        </div>
        <!-- 热力图 -->
        <div ref="heatRef" class="chart" style="height:200px;margin-top:16px"></div>
      </div>
      <!-- 右上：营收趋势 -->
      <div class="card"><h3>💰 营收趋势</h3><div ref="revenueRef" class="chart"></div></div>
      <!-- 左下：订单地图 -->
      <div class="card col-span-2 row-span-2"><h3>🗺️ 订单热力地图</h3><div ref="mapRef" class="chart" style="height:calc(100% - 40px)"></div></div>
      <!-- 右下：实时滚动 -->
      <div class="card"><h3>🔔 最新订单</h3><div class="scroll-list">
        <div v-for="(o,i) in recentOrders" :key="i" class="order-item">{{o.time}} - {{o.shop}} · ¥{{o.amount}}</div>
      </div></div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const time = ref('')
const orderRef = ref(); const heatRef = ref(); const revenueRef = ref(); const mapRef = ref()
const kpi = reactive({ todayOrders: 286, todayRevenue: '12,580', pendingOrders: 23, onlineShops: 48 })
const recentOrders = ref([
  { time: '19:45:12', shop: '知味川菜馆', amount: 128 },
  { time: '19:44:38', shop: '江南小厨', amount: 58 },
  { time: '19:43:05', shop: '王品牛排', amount: 280 },
  { time: '19:42:21', shop: '绿茶餐厅', amount: 86 },
  { time: '19:41:50', shop: '海底捞', amount: 156 }
])

// 更新时间
const timer = setInterval(() => { time.value = new Date().toLocaleString('zh-CN') }, 1000)
onUnmounted(() => clearInterval(timer))

onMounted(() => {
  // 实时订单量 - 柱状图
  const oC = echarts.init(orderRef.value)
  oC.setOption({ grid: { top: 10, right: 10, bottom: 20, left: 40 }, xAxis: { type: 'category', data: ['12:00','13:00','14:00','15:00','16:00','17:00','18:00','19:00'], axisLabel: { color: '#7b8ba3', fontSize: 10 } }, yAxis: { type: 'value', axisLabel: { color: '#7b8ba3' } }, series: [{ data: [15,22,18,20,25,35,48,52], type: 'bar', itemStyle: { color: '#409eff', borderRadius: [4,4,0,0] } }] })

  // 营收趋势
  const rC = echarts.init(revenueRef.value)
  rC.setOption({ grid: { top: 10, right: 10, bottom: 20, left: 50 }, xAxis: { type: 'category', data: ['12日','13日','14日','15日','16日','17日','18日'], axisLabel: { color: '#7b8ba3', fontSize: 10 } }, yAxis: { type: 'value', axisLabel: { color: '#7b8ba3' } }, series: [{ data: [9800,11000,10500,12000,13500,14000,12580], type: 'line', smooth: true, lineStyle: { color: '#67c23a' }, areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(103,194,58,0.3)'},{offset:1,color:'rgba(103,194,58,0)'}]) } }] })

  // 热力图（模拟24小时×7天）
  const hC = echarts.init(heatRef.value)
  const hours = ['00:00','02:00','04:00','06:00','08:00','10:00','12:00','14:00','16:00','18:00','20:00','22:00']
  const days = ['周一','周二','周三','周四','周五','周六','周日']
  const heatData: [number,number,number][] = []
  for (let h = 0; h < 12; h++) for (let d = 0; d < 7; d++) heatData.push([h, d, Math.floor(Math.random() * 100)])
  hC.setOption({ grid: { top: 10, right: 20, bottom: 20, left: 50 }, xAxis: { type: 'category', data: hours, axisLabel: { color: '#7b8ba3', fontSize: 9 } }, yAxis: { type: 'category', data: days, axisLabel: { color: '#7b8ba3', fontSize: 9 } }, visualMap: { min: 0, max: 100, calculable: true, orient: 'horizontal', left: 'center', bottom: 0, inRange: { color: ['#0a1628','#235894','#409eff','#a0d2ff'] }, show: false }, series: [{ type: 'heatmap', data: heatData, label: { show: false } }] })

  // 订单热力地图（散点图模拟）
  const mC = echarts.init(mapRef.value)
  const cities = ['杭州','宁波','温州','嘉兴','湖州','绍兴','金华']
  const geoData: {name:string,value:number[]}[] = []
  for (let i = 0; i < 50; i++) {
    const ci = Math.floor(Math.random() * cities.length)
    geoData.push({ name: cities[ci], value: [118 + Math.random() * 3, 28.5 + Math.random() * 2.5, Math.floor(Math.random() * 20)] })
  }
  mC.setOption({
    geo: { map: 'china', roam: true, center: [120.15, 30.28], zoom: 5, itemStyle: { areaColor: '#0e1f3d', borderColor: '#1a3a5c' }, label: { show: true, color: '#7b8ba3' } },
    series: [{ type: 'effectScatter', coordinateSystem: 'geo', data: geoData, symbolSize: (v: number[]) => v[2] * 2, rippleEffect: { brushType: 'stroke' }, itemStyle: { color: '#409eff' } }]
  })
})
</script>

<style>
.screen { width: 100vw; height: 100vh; background: #0a1628; padding: 8px 16px; }
.header { display: flex; justify-content: space-between; align-items: center; padding: 8px 0 12px; border-bottom: 1px solid rgba(64,158,255,0.3); margin-bottom: 12px; }
.header h1 { font-size: 24px; background: linear-gradient(90deg, #409eff, #67c23a); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.header .time { font-size: 16px; color: #409eff; font-family: monospace; }
.grid { display: grid; grid-template-columns: 1fr 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 12px; height: calc(100vh - 70px); }
.card { background: rgba(14,31,61,0.8); border: 1px solid rgba(64,158,255,0.2); border-radius: 8px; padding: 12px 16px; }
.card h3 { font-size: 14px; color: #409eff; margin-bottom: 8px; }
.chart { height: calc(100% - 40px); }
.row-span-2 { grid-row: span 2; }
.col-span-2 { grid-column: span 2; }
.kpi-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px; }
.kpi { background: rgba(64,158,255,0.1); border-radius: 8px; padding: 12px; text-align: center; }
.kpi-val { display: block; font-size: 32px; font-weight: 700; color: #409eff; }
.kpi-label { display: block; font-size: 12px; color: #7b8ba3; margin-top: 4px; }
.scroll-list { overflow-y: auto; height: calc(100% - 40px); }
.order-item { padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 13px; color: #c0c9d4; }
</style>
