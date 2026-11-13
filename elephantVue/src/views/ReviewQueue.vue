<template>
  <div class="page">
    <div class="page-header">
      <h2>复核队列</h2>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="id" label="任务" min-width="180" />
      <el-table-column prop="predictedName" label="机器候选" min-width="120" />
      <el-table-column prop="confidence" label="置信度" width="100" />
      <el-table-column prop="statusLabel" label="当前结论" width="120" />
      <el-table-column prop="createdAt" label="进入时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openConfirm(scope.row)">确认个体</el-button>
          <el-button size="small" @click="markUnknown(scope.row)">无法确认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="确认个体" v-model="visible" width="420px">
      <el-select v-model="individualId" placeholder="选择已发布个体" filterable style="width: 100%">
        <el-option v-for="item in individuals" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const loading = ref(false)
const rows = ref([])
const individuals = ref([])
const visible = ref(false)
const individualId = ref(null)
const currentId = ref('')

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/review-tasks')
    rows.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '复核队列加载失败')
  } finally {
    loading.value = false
  }
}

const loadIndividuals = async () => {
  const data = await request.get('/individuals', { params: { page: 1, size: 100 } })
  individuals.value = data.content || []
}

const openConfirm = (row) => {
  currentId.value = row.id
  individualId.value = row.individualId || null
  visible.value = true
}

const confirm = async () => {
  if (!individualId.value) {
    ElMessage.warning('请选择个体')
    return
  }
  await request.post(`/admin/review-tasks/${currentId.value}/conclude`, {
    decision: 'CONFIRM',
    individualId: individualId.value
  })
  visible.value = false
  ElMessage.success('已确认')
  load()
}

const markUnknown = async (row) => {
  await request.post(`/admin/review-tasks/${row.id}/conclude`, { decision: 'UNKNOWN' })
  ElMessage.success('已标为无法确认')
  load()
}

onMounted(() => {
  load()
  loadIndividuals().catch(() => {})
})
</script>

<style scoped>
.page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
