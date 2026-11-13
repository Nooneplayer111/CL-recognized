<template>
  <div class="page">
    <div class="page-header">
      <h2>识别事件</h2>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="id" label="任务" min-width="180" />
      <el-table-column label="来源" width="100">
        <template #default="scope">{{ scope.row.mediaType === 'video' ? '视频' : '图片' }}</template>
      </el-table-column>
      <el-table-column prop="taskState" label="任务状态" width="140" />
      <el-table-column prop="statusLabel" label="对外结论" width="120" />
      <el-table-column prop="predictedName" label="候选名称" min-width="120" />
      <el-table-column prop="confidence" label="置信度" width="100" />
      <el-table-column prop="reviewState" label="复核" width="120" />
      <el-table-column prop="createdAt" label="时间" width="170" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const loading = ref(false)
const rows = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/recognition-events')
    rows.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '识别事件加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
