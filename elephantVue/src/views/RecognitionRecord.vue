<template>
  <div class="record-container">
    <div class="page-header">
      <h2>识别记录</h2>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索识别结果或用户"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="searchForm.mediaType" placeholder="媒体类型" clearable class="type-select" @change="handleSearch">
        <el-option label="全部类型" value="" />
        <el-option label="图片识别" value="image" />
        <el-option label="视频识别" value="video" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border class="record-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="识别图片" width="120">
        <template #default="scope">
          <el-image
            v-if="scope.row.mediaUrl"
            :src="scope.row.mediaUrl"
            :preview-src-list="[scope.row.mediaUrl]"
            fit="cover"
            class="record-image"
          />
          <span v-else class="no-image">无图片</span>
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="识别用户" width="120" />
      <el-table-column prop="predictedName" label="识别结果" min-width="140" />
      <el-table-column label="置信度" width="100">
        <template #default="scope">
          <span v-if="scope.row.confidence != null">{{ scope.row.confidence }}%</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.mediaType === 'image' ? 'primary' : 'success'">
            {{ scope.row.mediaType === 'image' ? '图片' : '视频' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="source" label="来源" width="100">
        <template #default="scope">
          {{ scope.row.source === 'miniprogram' ? '小程序' : 'Web' }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="识别时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
          <el-button size="small" type="danger" @click="deleteRecord(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog title="识别记录详情" v-model="showDetailDialog" width="700px">
      <div v-if="detailData" class="detail-content">
        <div class="detail-image" v-if="detailData.mediaUrl">
          <img :src="detailData.mediaUrl" alt="识别图片" />
        </div>
        <div class="detail-row">
          <span class="detail-label">识别用户</span>
          <span class="detail-value">{{ detailData.operatorName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">识别结果</span>
          <span class="detail-value">{{ detailData.predictedName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">置信度</span>
          <span class="detail-value">{{ detailData.confidence != null ? detailData.confidence + '%' : '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">关联个体</span>
          <span class="detail-value">{{ detailData.individualName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">媒体类型</span>
          <span class="detail-value">{{ detailData.mediaType === 'image' ? '图片识别' : '视频识别' }}</span>
        </div>
        <div class="detail-row" v-if="detailData.mediaType === 'video'">
          <span class="detail-label">大象数量</span>
          <span class="detail-value">{{ detailData.elephantCount ?? '-' }}</span>
        </div>
        <div class="detail-row" v-if="detailData.mediaType === 'video'">
          <span class="detail-label">处理帧数</span>
          <span class="detail-value">{{ detailData.frameCount ?? '-' }}</span>
        </div>
        <div class="detail-row" v-if="detailData.processedVideoUrl">
          <span class="detail-label">处理视频</span>
          <span class="detail-value">
            <a :href="detailData.processedVideoUrl" target="_blank">{{ detailData.processedVideoUrl }}</a>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">识别时间</span>
          <span class="detail-value">{{ detailData.createTime || '-' }}</span>
        </div>
        <div class="detail-row" v-if="detailData.allProbs">
          <span class="detail-label">概率分布</span>
          <span class="detail-value detail-json">{{ detailData.allProbs }}</span>
        </div>
        <div class="detail-row" v-if="detailData.elephants">
          <span class="detail-label">视频结果</span>
          <span class="detail-value detail-json">{{ detailData.elephants }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const loading = ref(false)
const showDetailDialog = ref(false)
const tableData = ref([])
const detailData = ref(null)

const searchForm = reactive({
  keyword: '',
  mediaType: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const loadRecords = async () => {
  loading.value = true
  try {
    const data = await request.get('/recognition-records', {
      params: {
        page: pagination.currentPage,
        size: pagination.pageSize,
        keyword: searchForm.keyword || undefined,
        mediaType: searchForm.mediaType || undefined
      }
    })
    tableData.value = data.content || []
    pagination.total = data.totalElements || 0
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    ElMessage.error(error.message || '获取识别记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadRecords()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadRecords()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadRecords()
}

const viewDetail = async (row) => {
  try {
    const data = await request.get(`/recognition-records/${row.id}`)
    detailData.value = data
    showDetailDialog.value = true
  } catch (error) {
    ElMessage.error(error.message || '获取详情失败')
  }
}

const deleteRecord = async (id) => {
  await ElMessageBox.confirm('确定要删除该识别记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await request.delete(`/recognition-records/${id}`)
    ElMessage.success('删除成功')
    loadRecords()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.record-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search-input {
  width: 280px;
}

.type-select {
  width: 140px;
}

.record-table {
  background: #fff;
  border-radius: 8px;
}

.record-image {
  width: 80px;
  height: 60px;
  border-radius: 4px;
}

.no-image {
  color: #909399;
  font-size: 12px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  padding: 10px 0;
}

.detail-image {
  margin-bottom: 16px;
  text-align: center;
}

.detail-image img {
  max-width: 100%;
  max-height: 260px;
  border-radius: 8px;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  width: 100px;
  color: #909399;
  font-weight: 500;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  color: #303133;
  word-break: break-all;
}

.detail-json {
  white-space: pre-wrap;
  font-family: Consolas, monospace;
  font-size: 12px;
}
</style>
