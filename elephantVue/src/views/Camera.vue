<template>
  <div class="camera-container">
    <div class="page-header">
      <h2>摄像头管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        添加摄像头
      </el-button>
    </div>

    <div v-loading="loading" class="camera-grid">
      <el-empty v-if="!loading && cameraList.length === 0" description="暂无摄像头数据" />
      <div v-for="camera in cameraList" :key="camera.id" class="camera-card">
        <div class="camera-header">
          <div class="camera-status" :class="camera.status">
            <span class="status-dot"></span>
            <span>{{ camera.status === 'online' ? '在线' : '离线' }}</span>
          </div>
          <div class="camera-actions">
            <el-button size="small" @click="editCamera(camera)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCamera(camera.id)">删除</el-button>
          </div>
        </div>
        <div class="camera-preview">
          <img v-if="camera.previewUrl" :src="camera.previewUrl" alt="预览" />
          <div v-else class="preview-placeholder">
            <el-icon><VideoCamera /></el-icon>
            <span>{{ camera.status === 'online' ? '点击查看实时画面' : '摄像头离线' }}</span>
          </div>
        </div>
        <div class="camera-info">
          <h3>{{ camera.name }}</h3>
          <p><el-icon><MapLocation /></el-icon> {{ camera.location }}</p>
          <p><el-icon><Monitor /></el-icon> {{ camera.ipAddress }}</p>
        </div>
      </div>
    </div>

    <el-dialog :title="dialogTitle" v-model="showAddDialog" width="500px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="摄像头名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入摄像头名称" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ipAddress">
          <el-input v-model="form.ipAddress" placeholder="请输入IP地址" />
        </el-form-item>
        <el-form-item label="安装位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入安装位置" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoCamera, MapLocation, Monitor } from '@element-plus/icons-vue'
import request from '../utils/request'

const showAddDialog = ref(false)
const dialogTitle = ref('添加摄像头')
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  id: null,
  name: '',
  ipAddress: '',
  location: '',
  port: 8080,
  username: '',
  password: ''
})

const rules = {
  name: [{ required: true, message: '请输入摄像头名称', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  location: [{ required: true, message: '请输入安装位置', trigger: 'blur' }]
}

const cameraList = ref([])

const buildPayload = () => ({
  name: form.name,
  ipAddress: form.ipAddress,
  location: form.location,
  port: form.port,
  username: form.username,
  password: form.password
})

const loadCameras = async () => {
  loading.value = true
  try {
    const data = await request.get('/cameras')
    cameraList.value = Array.isArray(data) ? data : (data?.data || [])
  } catch (error) {
    cameraList.value = []
    ElMessage.error(error.message || '获取摄像头列表失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  dialogTitle.value = '添加摄像头'
  resetForm()
  showAddDialog.value = true
}

const editCamera = (camera) => {
  dialogTitle.value = '编辑摄像头'
  Object.assign(form, camera)
  showAddDialog.value = true
}

const deleteCamera = async (id) => {
  await ElMessageBox.confirm('确定要删除该摄像头吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await request.delete(`/cameras/${id}`)
    ElMessage.success('删除成功')
    loadCameras()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true

  try {
    const payload = buildPayload()
    if (form.id) {
      await request.put(`/cameras/${form.id}`, payload)
      ElMessage.success('修改成功')
    } else {
      await request.post('/cameras', payload)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadCameras()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.ipAddress = ''
  form.location = ''
  form.port = 8080
  form.username = ''
  form.password = ''
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadCameras()
})
</script>

<style scoped>
.camera-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.camera-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
  min-height: 200px;
}

.camera-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.camera-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f7fa;
}

.camera-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
}

.camera-status.online {
  background-color: #f0f9eb;
  color: #67c23a;
}

.camera-status.offline {
  background-color: #fef0f0;
  color: #f56c6c;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: currentColor;
}

.camera-preview {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background-color: #1a1a1a;
}

.camera-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
}

.preview-placeholder :deep(.el-icon) {
  font-size: 48px;
  margin-bottom: 8px;
}

.preview-placeholder span {
  font-size: 14px;
}

.camera-info {
  padding: 16px;
}

.camera-info h3 {
  font-size: 16px;
  color: #303133;
  margin: 0 0 10px 0;
}

.camera-info p {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  margin: 0 0 8px 0;
}

.camera-info p:last-child {
  margin-bottom: 0;
}

.camera-info :deep(.el-icon) {
  font-size: 14px;
}
</style>
