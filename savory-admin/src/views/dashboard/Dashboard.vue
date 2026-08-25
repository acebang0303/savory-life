<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ stats.todayOrders }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日收入</div>
          <div class="stat-value">¥{{ stats.todayRevenue }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待处理订单</div>
          <div class="stat-value">{{ stats.pendingOrders }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">已完成订单</div>
          <div class="stat-value">{{ stats.completedOrders }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>数据概览 · {{ stats.date }}</template>
          <div ref="chartRef" style="height: 360px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { getOrderStatistics } from '@/api'
import * as echarts from 'echarts'

const chartRef = ref<HTMLDivElement>()
const stats = reactive({
  todayOrders: 0,
  todayRevenue: '0.00',
  pendingOrders: 0,
  completedOrders: 0,
  date: ''
})

onMounted(async () => {
  try {
    const res = await getOrderStatistics()
    Object.assign(stats, res.data)
  } catch (e) {
    console.error('获取统计数据失败', e)
  }

  // 渲染图表
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] },
      yAxis: { type: 'value' },
      series: [
        { name: '订单数', type: 'line', smooth: true, data: [82, 93, 90, 94, 100, 130, 120] },
        { name: '收入(百元)', type: 'line', smooth: true, data: [34, 41, 38, 42, 47, 58, 52] }
      ]
    })
  }
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
</style>
