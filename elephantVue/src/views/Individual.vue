<template>
  <div class="individual-container">
    <div class="page-header">
      <h2>个体管理</h2>
      <div class="header-actions">
        <el-select v-model="searchForm.speciesId" placeholder="选择物种" class="species-select" clearable @change="handleSearch">
          <el-option label="全部物种" value="" />
          <el-option v-for="species in speciesList" :key="species.id" :label="species.name" :value="species.id" />
        </el-select>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          添加个体
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索个体名称" clearable @keyup.enter="handleSearch" @clear="handleSearch">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
    </div>

    <el-table v-loading="loading" :data="tableData" border class="individual-table" @row-click="handleRowClick">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="个体名称" />
      <el-table-column label="图片" width="90">
        <template #default="scope">
          <img v-if="scope.row.imageUrl" class="table-photo" :src="scope.row.imageUrl" alt="" />
          <span v-else class="table-photo-empty">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="speciesName" label="所属物种" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.gender === 'male' ? 'primary' : 'success'">
            {{ scope.row.gender === 'male' ? '雄性' : '雌性' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="birthDate" label="出生日期" width="120" />
      <el-table-column prop="birthPlace" label="出生地" />
      <el-table-column prop="personality" label="性格" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click.stop="editIndividual(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click.stop="deleteIndividual(scope.row.id)">删除</el-button>
          <el-button size="small" type="info" @click.stop="viewDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog :title="dialogTitle" v-model="showAddDialog" width="600px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="个体名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入个体名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="所属物种" prop="speciesId">
          <el-select v-model="form.speciesId" placeholder="请选择物种">
            <el-option v-for="species in speciesList" :key="species.id" :label="species.name" :value="species.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio label="male">雄性</el-radio>
            <el-radio label="female">雌性</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择出生日期" :disabled-date="disableFutureDate" clearable />
        </el-form-item>
        <el-form-item label="出生地" prop="birthPlace">
          <el-input v-model="form.birthPlace" placeholder="请输入出生地" maxlength="128" />
        </el-form-item>
        <el-form-item label="性格" prop="personality">
          <el-input v-model="form.personality" placeholder="请输入性格" maxlength="255" />
        </el-form-item>
        <el-form-item label="父亲" prop="father">
          <el-input v-model="form.father" placeholder="请输入父亲名字" maxlength="64" />
        </el-form-item>
        <el-form-item label="母亲" prop="mother">
          <el-input v-model="form.mother" placeholder="请输入母亲名字" maxlength="64" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入个体描述" maxlength="1000" />
        </el-form-item>
        <el-form-item label="个体图片">
          <el-upload
            action="/api/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            accept="image/*"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
          >
            <el-button type="primary">上传图片</el-button>
          </el-upload>
          <el-button v-if="form.imageUrl" class="remove-image" @click="form.imageUrl = ''">移除图片</el-button>
          <div v-if="form.imageUrl" class="image-preview">
            <img :src="form.imageUrl" alt="个体图片预览" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="个体详情" v-model="showDetailDialog" width="600px">
      <div v-if="detailData" class="detail-content">
        <div class="detail-photo">
          <img v-if="detailData.imageUrl" :src="detailData.imageUrl" alt="个体图片" />
          <span v-else>未上传图片</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">个体名称</span>
          <span class="detail-value">{{ detailData.name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">所属物种</span>
          <span class="detail-value">{{ detailData.speciesName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">性别</span>
          <span class="detail-value">{{ detailData.gender === 'male' ? '雄性' : '雌性' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">出生日期</span>
          <span class="detail-value">{{ detailData.birthDate || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">出生地</span>
          <span class="detail-value">{{ detailData.birthPlace || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">性格</span>
          <span class="detail-value">{{ detailData.personality || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">父亲</span>
          <span class="detail-value">{{ detailData.father || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">母亲</span>
          <span class="detail-value">{{ detailData.mother || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">描述</span>
          <span class="detail-value">{{ detailData.description || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ detailData.createTime }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import request from '../utils/request'

const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const dialogTitle = ref('添加个体')
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const speciesList = ref([])
const tableData = ref([])

const searchForm = reactive({
  keyword: '',
  speciesId: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null,
  name: '',
  speciesId: null,
  gender: 'male',
  birthDate: '',
  birthPlace: '',
  personality: '',
  father: '',
  mother: '',
  description: '',
  imageUrl: ''
})

const rules = {
  name: [{ required: true, message: '请输入个体名称', trigger: 'blur' }],
  speciesId: [{ required: true, message: '请选择所属物种', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

const detailData = ref(null)

const uploadHeaders = computed(() => ({
  token: localStorage.getItem('token') || ''
}))

const buildPayload = () => ({
  name: form.name,
  speciesId: form.speciesId,
  gender: form.gender,
  birthDate: form.birthDate || null,
  birthPlace: form.birthPlace || null,
  personality: form.personality || null,
  father: form.father || null,
  mother: form.mother || null,
  description: form.description || null,
  imageUrl: form.imageUrl || null
})

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

const disableFutureDate = (date) => date.getTime() > Date.now()

const loadSpecies = async () => {
  try {
    const data = await request.get('/species')
    speciesList.value = Array.isArray(data) ? data : (data?.data || [])
  } catch (error) {
    speciesList.value = []
    ElMessage.error(error.message || '获取物种列表失败')
  }
}

const loadIndividuals = async () => {
  loading.value = true
  try {
    const data = await request.get('/individuals', {
      params: {
        page: pagination.currentPage,
        size: pagination.pageSize,
        keyword: searchForm.keyword || undefined,
        speciesId: searchForm.speciesId || undefined
      }
    })
    tableData.value = data.content || []
    pagination.total = data.totalElements || 0
  } catch (error) {
    tableData.value = []
    pagination.total = 0
    ElMessage.error(error.message || '获取个体列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadIndividuals()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadIndividuals()
}

const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadIndividuals()
}

const handleRowClick = (row) => {
  viewDetail(row)
}

const viewDetail = (row) => {
  detailData.value = { ...row }
  showDetailDialog.value = true
}

const openAddDialog = () => {
  dialogTitle.value = '添加个体'
  resetForm()
  showAddDialog.value = true
}

const editIndividual = (row) => {
  dialogTitle.value = '编辑个体'
  Object.assign(form, row)
  showAddDialog.value = true
}

const deleteIndividual = async (id) => {
  await ElMessageBox.confirm('确定要删除该个体吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await request.delete(`/individuals/${id}`)
    ElMessage.success('删除成功')
    loadIndividuals()
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
      await request.put(`/individuals/${form.id}`, payload)
      ElMessage.success('修改成功')
    } else {
      await request.post('/individuals', payload)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadIndividuals()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.speciesId = null
  form.gender = 'male'
  form.birthDate = ''
  form.birthPlace = ''
  form.personality = ''
  form.father = ''
  form.mother = ''
  form.description = ''
  form.imageUrl = ''
  formRef.value?.clearValidate()
}

onMounted(() => {
  loadSpecies()
  loadIndividuals()
})
</script>

<style scoped>
.individual-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.species-select {
  width: 150px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-bar :deep(.el-input) {
  width: 400px;
}

.individual-table {
  background: #fff;
  border-radius: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  padding: 10px 0;
}

.detail-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-label {
  width: 100px;
  color: #909399;
  font-weight: 500;
}

.detail-value {
  flex: 1;
  color: #303133;
}

.table-photo {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}

.table-photo-empty {
  color: #909399;
}

.remove-image {
  margin-left: 8px;
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

.detail-photo {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  margin-bottom: 8px;
  background: #f5f7fa;
  color: #909399;
}

.detail-photo img {
  max-width: 100%;
  max-height: 240px;
  object-fit: contain;
}
</style>
