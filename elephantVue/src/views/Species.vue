<template>
  <div class="species-container">
    <div class="page-header">
      <h2>物种管理</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        添加物种
      </el-button>
    </div>

    <div v-loading="loading" class="species-grid">
      <el-empty v-if="!loading && speciesList.length === 0" description="暂无物种数据" />
      <div v-for="species in speciesList" :key="species.id" class="species-card">
        <div class="species-image">
          <img v-if="species.imageUrl" :src="species.imageUrl" :alt="species.name" />
          <div v-else class="image-placeholder">
            <el-icon><Picture /></el-icon>
            <span>暂无图片</span>
          </div>
        </div>
        <div class="species-info">
          <h3>{{ species.name }}</h3>
          <p class="species-code">编码: {{ species.code }}</p>
          <p class="species-desc">{{ species.description || '暂无描述' }}</p>
        </div>
        <div class="species-actions">
          <el-button size="small" @click="editSpecies(species)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteSpecies(species.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-dialog :title="dialogTitle" v-model="showAddDialog" width="500px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="物种名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入物种名称" />
        </el-form-item>
        <el-form-item label="物种编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入物种编码" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="物种描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入物种描述" />
        </el-form-item>
        <el-form-item label="物种图片">
          <el-upload
            class="upload-demo"
            action="/api/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            accept="image/*"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
          >
            <el-button type="primary">上传图片</el-button>
          </el-upload>
          <div v-if="form.imageUrl" class="image-preview">
            <img :src="form.imageUrl" alt="物种图片预览" />
          </div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture } from '@element-plus/icons-vue'
import request from '../utils/request'

const showAddDialog = ref(false)
const dialogTitle = ref('添加物种')
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  imageUrl: ''
})

const rules = {
  name: [{ required: true, message: '请输入物种名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入物种编码', trigger: 'blur' }]
}

const speciesList = ref([])

// 上传接口需要手动携带 token
const uploadHeaders = computed(() => ({
  token: localStorage.getItem('token') || ''
}))

const buildPayload = () => ({
  name: form.name,
  code: form.code,
  description: form.description,
  imageUrl: form.imageUrl
})

const loadSpecies = async () => {
  loading.value = true
  try {
    const data = await request.get('/species')
    speciesList.value = Array.isArray(data) ? data : (data?.data || [])
  } catch (error) {
    speciesList.value = []
    ElMessage.error(error.message || '获取物种列表失败')
  } finally {
    loading.value = false
  }
}

const handleUploadSuccess = (response) => {
  if (response.code === 1 && response.url) {
    form.imageUrl = response.url
    ElMessage.success('图片上传成功')
    return
  }
  ElMessage.error(response.message || '图片上传失败')
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败，请稍后重试')
}

const openAddDialog = () => {
  dialogTitle.value = '添加物种'
  resetForm()
  showAddDialog.value = true
}

const editSpecies = (species) => {
  dialogTitle.value = '编辑物种'
  Object.assign(form, species)
  showAddDialog.value = true
}

const deleteSpecies = async (id) => {
  await ElMessageBox.confirm('确定要删除该物种吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await request.delete(`/species/${id}`)
    ElMessage.success('删除成功')
    loadSpecies()
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
      await request.put(`/species/${form.id}`, payload)
      ElMessage.success('修改成功')
    } else {
      await request.post('/species', payload)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadSpecies()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.imageUrl = ''
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadSpecies()
})
</script>

<style scoped>
.species-container {
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

.species-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  min-height: 200px;
}

.species-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.species-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.species-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background-color: #f5f7fa;
}

.species-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.image-placeholder :deep(.el-icon) {
  font-size: 48px;
  margin-bottom: 8px;
}

.species-info {
  padding: 16px;
}

.species-info h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 8px 0;
}

.species-code {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px 0;
}

.species-desc {
  font-size: 14px;
  color: #606266;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.species-actions {
  padding: 0 16px 16px;
  display: flex;
  gap: 8px;
}

.image-preview {
  margin-top: 12px;
  max-width: 200px;
}

.image-preview img {
  width: 100%;
  height: auto;
  border-radius: 4px;
}
</style>
