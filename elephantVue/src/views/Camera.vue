<template>
  <div class="camera-container">
    <div class="page-header">
      <h2>摄像头管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加摄像头
      </el-button>
    </div>
    
    <div class="camera-grid">
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
    
    <el-dialog :title="dialogTitle" v-model="showAddDialog" width="500px">
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
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoCamera, MapLocation, Monitor } from '@element-plus/icons-vue'
import axios from 'axios'

const showAddDialog = ref(false)
const dialogTitle = ref('添加摄像头')
const formRef = ref(null)

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

const loadCameras = async () => {
  try {
    const response = await axios.get('/api/cameras')
    cameraList.value = response.data
  } catch (error) {
    cameraList.value = [
      {
        id: 1,
        name: '监控点A-东门',
        ipAddress: '192.168.1.101',
        location: '园区东门入口',
        port: 8080,
        status: 'online',
        previewUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=security%20camera%20view%20of%20wildlife%20park%20entrance&image_size=landscape_16_9'
      },
      {
        id: 2,
        name: '监控点B-象舍',
        ipAddress: '192.168.1.102',
        location: '大象馆区域',
        port: 8080,
        status: 'online',
        previewUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=security%20camera%20view%20of%20elephant%20enclosure&image_size=landscape_16_9'
      },
      {
        id: 3,
        name: '监控点C-猴山',
        ipAddress: '192.168.1.103',
        location: '金丝猴展示区',
        port: 8080,
        status: 'offline',
        previewUrl: ''
      },
      {
        id: 4,
        name: '监控点D-北门',
        ipAddress: '192.168.1.104',
        location: '园区北门出口',
        port: 8080,
        status: 'online',
        previewUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=security%20camera%20view%20of%20park%20gate&image_size=landscape_16_9'
      }
    ]
  }
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
    await axios.delete(`/api/cameras/${id}`)
    ElMessage.success('删除成功')
    loadCameras()
  } catch (error) {
    cameraList.value = cameraList.value.filter(item => item.id !== id)
    ElMessage.success('删除成功')
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  try {
    if (form.id) {
      await axios.put(`/api/cameras/${form.id}`, form)
      ElMessage.success('修改成功')
    } else {
      await axios.post('/api/cameras', form)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadCameras()
  } catch (error) {
    if (form.id) {
      const index = cameraList.value.findIndex(item => item.id === form.id)
      if (index !== -1) {
        cameraList.value[index] = { ...form, status: cameraList.value[index].status }
      }
      ElMessage.success('修改成功')
    } else {
      form.id = Date.now()
      form.status = 'offline'
      cameraList.value.push({ ...form })
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
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
