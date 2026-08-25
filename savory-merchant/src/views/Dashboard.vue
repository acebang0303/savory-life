<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-label">今日订单</div><div class="stat-value">{{ stats.todayOrders }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-label">今日营收</div><div class="stat-value">¥{{ stats.todayRevenue }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-label">待处理</div><div class="stat-value" style="color:#e6a23c">{{ stats.pendingOrders }}</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-label">已完成</div><div class="stat-value" style="color:#67c23a">{{ stats.completedOrders }}</div></el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="16">
        <el-card><template #header>近7天订单趋势</template><div ref="trendRef" style="height:300px"></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><template #header>热门菜品 Top5</template>
          <el-table :data="topDishes" stripe size="small" v-loading="loading">
            <el-table-column prop="name" label="菜品" />
            <el-table-column prop="sales" label="销量" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import http from '@/api/http'
import * as echarts from 'echarts'

const loading = ref(false)
const trendRef = ref<HTMLDivElement>()
const stats = reactive({ todayOrders: 0, todayRevenue: '0', pendingOrders: 0, completedOrders: 0 })
const topDishes = ref<{ name: string; sales: number }[]>([])

async function fetchStats() {
  loading.value = true
  try {
    const res = await http.get('/order/statistics')
    Object.assign(stats, res.data)
    const dishRes = await http.get('/dish/page', { params: { page: 1, pageSize: 5 } })
    topDishes.value = (dishRes.data.records || []).map((d: any) => ({ name: d.name, sales: d.sales || 0 }))
  } catch { /* use defaults */ }
  finally { loading.value = false }
}

onMounted(() => {
  fetchStats()
  if (trendRef.value) {
    const chart = echarts.init(trendRef.value)
    chart.setOption({
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value' },
      series: [{
        data: [22, 28, 25, 30, 35, 42, 38], type: 'line', smooth: true,
        areaStyle: { color: 'rgba(64,158,255,0.15)' },
        itemStyle: { color: '#409eff' }
      }]
    })
  }
})
</script>

<style scoped>
.dashboard { padding: 16px; }
.stat-label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
</style>
