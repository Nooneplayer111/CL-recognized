<template>
  <div class="page">
    <div class="page-header">
      <h2>审计日志</h2>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column prop="actor" label="操作者" width="160" />
      <el-table-column prop="action" label="动作" width="160" />
      <el-table-column prop="resourceType" label="对象" width="150" />
      <el-table-column prop="resourceId" label="编号" min-width="180" />
      <el-table-column prop="detail" label="说明" min-width="180" />
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
    const res = await request.get('/admin/audit-events')
    rows.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '审计日志加载失败')
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
