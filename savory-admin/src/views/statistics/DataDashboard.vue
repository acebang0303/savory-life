<template>
  <div class="dashboard">
    <!-- Row 1 -->
    <div class="grid">
      <div class="cell">
        <div class="card-title">今日概览</div>
        <div class="stat-row">
          <el-statistic title="今日订单数" :value="stats.todayOrders" :value-style="{ color: '#fff', fontSize: '26px', fontWeight: 700 }" />
          <el-statistic title="今日营收" :value="'¥' + stats.todayRevenue" :value-style="{ color: '#fff', fontSize: '26px', fontWeight: 700 }" />
          <el-statistic title="待处理订单" :value="stats.pendingOrders" :value-style="{ color: '#f56c6c', fontSize: '26px', fontWeight: 700 }" />
          <el-statistic title="已完成订单" :value="stats.completedOrders" :value-style="{ color: '#67c23a', fontSize: '26px', fontWeight: 700 }" />
        </div>
      </div>

      <div class="cell">
        <div class="card-title">订单趋势</div>
        <v-chart :option="lineOption" style="height:300px" />
      </div>

      <div class="cell">
        <div class="card-title">订单状态</div>
        <v-chart :option="pieOption" style="height:300px" />
      </div>
    </div>

    <!-- Row 2 -->
    <div class="grid">
      <div class="cell">
        <div class="card-title">商户排行</div>
        <v-chart :option="barOption" style="height:300px" />
      </div>

      <div class="cell">
        <div class="card-title">菜品排行</div>
        <v-chart :option="hBarOption" style="height:300px" />
      </div>

      <div class="cell">
        <div class="card-title">社区数据</div>
        <div class="stat-row">
          <el-statistic title="笔记总数" :value="4" :value-style="{ color: '#fff', fontSize: '26px', fontWeight: 700 }" />
          <el-statistic title="评价总数" :value="5" :value-style="{ color: '#fff', fontSize: '26px', fontWeight: 700 }" />
          <el-statistic title="用户总数" :value="5" :value-style="{ color: '#fff', fontSize: '26px', fontWeight: 700 }" />
          <el-statistic title="今日新增" :value="2" :value-style="{ color: '#67c23a', fontSize: '26px', fontWeight: 700 }" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { getOrderStatistics } from '@/api'

const stats = reactive({
  todayOrders: 0,
  todayRevenue: '0.00',
  pendingOrders: 0,
  completedOrders: 0
})

// ============ 订单趋势 - line chart ============
const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const orderData = Array.from({ length: 7 }, () => Math.floor(Math.random() * 61) + 20)

const lineOption = ref({
  tooltip: { trigger: 'axis' },
  legend: {
    textStyle: { color: '#8899aa' },
    top: 0
  },
  grid: { left: 10, right: 20, top: 40, bottom: 10, containLabel: true },
  xAxis: {
    type: 'category',
    data: days,
    axisLine: { lineStyle: { color: '#334455' } },
    axisLabel: { color: '#8899aa' }
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: '#334455' } },
    axisLabel: { color: '#8899aa' },
    splitLine: { lineStyle: { color: '#1a2f44' } }
  },
  series: [
    {
      name: '订单数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      data: orderData,
      itemStyle: { color: '#409eff' },
      lineStyle: { width: 3 }
    }
  ]
})

// ============ 订单状态 - pie chart ============
const pieOption = ref({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    textStyle: { color: '#8899aa' }
  },
  series: [
    {
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['40%', '52%'],
      avoidLabelOverlap: false,
      padAngle: 2,
      itemStyle: { borderRadius: 4 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#fff' }
      },
      data: [
        { value: 5, name: '待支付', itemStyle: { color: '#e6a23c' } },
        { value: 3, name: '待接单', itemStyle: { color: '#409eff' } },
        { value: 4, name: '备货中', itemStyle: { color: '#909399' } },
        { value: 35, name: '已完成', itemStyle: { color: '#67c23a' } },
        { value: 8, name: '已取消', itemStyle: { color: '#f56c6c' } }
      ]
    }
  ]
})

// ============ 商户排行 - bar chart ============
const barOption = ref({
  tooltip: { trigger: 'axis' },
  grid: { left: 10, right: 20, top: 20, bottom: 10, containLabel: true },
  xAxis: {
    type: 'category',
    data: ['张记面馆', '老王烧烤', '蜀味川菜', '外婆家'],
    axisLabel: { color: '#8899aa', rotate: 15 },
    axisLine: { lineStyle: { color: '#334455' } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#8899aa' },
    splitLine: { lineStyle: { color: '#1a2f44' } }
  },
  series: [
    {
      type: 'bar',
      barWidth: 36,
      data: [
        { value: 320, itemStyle: { color: '#409eff' } },
        { value: 280, itemStyle: { color: '#67c23a' } },
        { value: 210, itemStyle: { color: '#e6a23c' } },
        { value: 180, itemStyle: { color: '#f56c6c' } }
      ],
      label: { show: true, position: 'top', color: '#8899aa' }
    }
  ]
})

// ============ 菜品排行 - horizontal bar chart ============
const dishNames = ['青岛啤酒', '羊肉串', '蛋炒饭', '麻婆豆腐', '红烧牛肉面']
const dishValues = [4500, 3200, 2300, 1560, 1523]

const hBarOption = ref({
  tooltip: { trigger: 'axis' },
  grid: { left: 10, right: 40, top: 10, bottom: 10, containLabel: true },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#8899aa' },
    splitLine: { lineStyle: { color: '#1a2f44' } }
  },
  yAxis: {
    type: 'category',
    data: dishNames,
    axisLabel: { color: '#8899aa' },
    axisLine: { lineStyle: { color: '#334455' } },
    inverse: true
  },
  series: [
    {
      type: 'bar',
      barWidth: 18,
      data: dishValues.map((v, i) => ({
        value: v,
        itemStyle: {
          color: ['#f56c6c', '#e6a23c', '#67c23a', '#409eff', '#909399'][i]
        }
      })),
      label: { show: true, position: 'right', color: '#8899aa' }
    }
  ]
})

onMounted(async () => {
  try {
    const res = await getOrderStatistics()
    if (res && res.data) {
      Object.assign(stats, res.data)
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
})
</script>

<style scoped>
.dashboard {
  background: #0a1628;
  min-height: 100vh;
  padding: 20px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.cell {
  background: #111d32;
  border: 1px solid #1e3550;
  border-radius: 8px;
  padding: 16px 20px;
}

.card-title {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #1e3550;
  position: relative;
}

.card-title::before {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 40px;
  height: 2px;
  background: #409eff;
  border-radius: 1px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 20px;
}

.stat-row :deep(.el-statistic__head) {
  color: #667a94;
  font-size: 13px;
  margin-bottom: 4px;
}

.stat-row :deep(.el-statistic__content) {
  color: #fff;
}
</style>
