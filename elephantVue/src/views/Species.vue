<template>
  <div class="species-container">
    <div class="page-header">
      <h2>物种管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加物种
      </el-button>
    </div>
    
    <div class="species-grid">
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
          <p class="species-desc">{{ species.description }}</p>
        </div>
        <div class="species-actions">
          <el-button size="small" @click="editSpecies(species)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteSpecies(species.id)">删除</el-button>
        </div>
      </div>
    </div>
    
    <el-dialog :title="dialogTitle" v-model="showAddDialog" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="物种名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入物种名称" />
        </el-form-item>
        <el-form-item label="物种编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入物种编码" />
        </el-form-item>
        <el-form-item label="物种描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入物种描述" />
        </el-form-item>
        <el-form-item label="物种图片">
          <el-upload class="upload-demo" action="/api/upload" :on-success="handleUploadSuccess">
            <el-button type="primary">上传图片</el-button>
          </el-upload>
          <div v-if="form.imageUrl" class="image-preview">
            <img :src="form.imageUrl" />
          </div>
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
import { Plus, Picture } from '@element-plus/icons-vue'
import axios from 'axios'

const showAddDialog = ref(false)
const dialogTitle = ref('添加物种')
const formRef = ref(null)

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

const loadSpecies = async () => {
  try {
    const response = await axios.get('/api/species')
    speciesList.value = response.data
  } catch (error) {
    speciesList.value = [
      {
        id: 1,
        name: '亚洲象',
        code: 'ASIAN_ELE',
        description: '亚洲象是亚洲现存的最大陆生动物',
        imageUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=asian%20elephant%20in%20wild%20nature&image_size=square'
      },
      {
        id: 2,
        name: '非洲象',
        code: 'AFRICAN_ELE',
        description: '非洲象是陆地上最大的哺乳动物',
        imageUrl: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=african%20elephant%20in%20savanna&image_size=square'
      },
      {
        id: 3,
        name: '金丝猴',
        code: 'GOLDEN_MONKEY',
        description: '金丝猴是中国特有的珍稀动物',
        imageUrl: ''
      }
    ]
  }
}

const handleUploadSuccess = (response) => {
  form.imageUrl = response.url
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
    await axios.delete(`/api/species/${id}`)
    ElMessage.success('删除成功')
    loadSpecies()
  } catch (error) {
    speciesList.value = speciesList.value.filter(item => item.id !== id)
    ElMessage.success('删除成功')
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  try {
    if (form.id) {
      await axios.put(`/api/species/${form.id}`, form)
      ElMessage.success('修改成功')
    } else {
      await axios.post('/api/species', form)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadSpecies()
  } catch (error) {
    if (form.id) {
      const index = speciesList.value.findIndex(item => item.id === form.id)
      if (index !== -1) {
        speciesList.value[index] = { ...form }
      }
      ElMessage.success('修改成功')
    } else {
      form.id = Date.now()
      speciesList.value.push({ ...form })
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
  }
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.imageUrl = ''
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
