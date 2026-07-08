<template>
  <div class="individual-container">
    <div class="page-header">
      <h2>个体管理</h2>
      <div class="header-actions">
        <el-select v-model="searchForm.speciesId" placeholder="选择物种" class="species-select" @change="handleSearch">
          <el-option label="全部物种" value="" />
          <el-option v-for="species in speciesList" :key="species.id" :label="species.name" :value="species.id" />
        </el-select>
        <el-button type="primary" @click="showAddDialog = true">
          <el-icon><Plus /></el-icon>
          添加个体
        </el-button>
      </div>
    </div>
    
    <div class="search-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索个体名称或编号" clearable @keyup.enter="handleSearch">
        <template #append>
          <el-button @click="handleSearch"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
    </div>
    
    <el-table :data="tableData" border class="individual-table" @row-click="handleRowClick">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="个体名称" />
      <el-table-column prop="code" label="个体编号" />
      <el-table-column prop="speciesName" label="所属物种" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.gender === 'male' ? 'primary' : 'success'">
            {{ scope.row.gender === 'male' ? '雄性' : '雌性' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
            {{ scope.row.status === 'active' ? '活跃' : '非活跃' }}
          </el-tag>
        </template>
      </el-table-column>
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
    
    <el-dialog :title="dialogTitle" v-model="showAddDialog" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="个体名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入个体名称" />
        </el-form-item>
        <el-form-item label="个体编号" prop="code">
          <el-input v-model="form.code" placeholder="请输入个体编号" />
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
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入个体描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="'active'" :inactive-value="'inactive'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
    
    <el-dialog title="个体详情" v-model="showDetailDialog" width="600px">
      <div v-if="detailData" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">个体名称</span>
          <span class="detail-value">{{ detailData.name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">个体编号</span>
          <span class="detail-value">{{ detailData.code }}</span>
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
          <span class="detail-label">年龄</span>
          <span class="detail-value">{{ detailData.age }}岁</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span class="detail-value">
            <el-tag :type="detailData.status === 'active' ? 'success' : 'danger'">
              {{ detailData.status === 'active' ? '活跃' : '非活跃' }}
            </el-tag>
          </span>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import request from '../utils/request'
import axios from 'axios'

const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const dialogTitle = ref('添加个体')
const formRef = ref(null)

const speciesList = ref([])
const allIndividuals = ref([])

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
  code: '',
  speciesId: '',
  gender: 'male',
  age: 0,
  description: '',
  status: 'active'
})

const rules = {
  name: [{ required: true, message: '请输入个体名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入个体编号', trigger: 'blur' }],
  speciesId: [{ required: true, message: '请选择所属物种', trigger: 'change' }]
}

const detailData = ref(null)

const filteredIndividuals = computed(() => {
  let result = allIndividuals.value
  if (searchForm.keyword) {
    const keyword = searchForm.keyword.toLowerCase()
    result = result.filter(item => 
      item.name.toLowerCase().includes(keyword) || 
      item.code.toLowerCase().includes(keyword)
    )
  }
  if (searchForm.speciesId) {
    result = result.filter(item => item.speciesId === searchForm.speciesId)
  }
  return result
})

const tableData = computed(() => {
  const start = (pagination.currentPage - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredIndividuals.value.slice(start, end)
})

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
  try {
    const response = await axios.get('/api/individuals', {
      params: {
        page: pagination.currentPage,
        size: pagination.pageSize,
        keyword: searchForm.keyword,
        speciesId: searchForm.speciesId
      }
    })
    allIndividuals.value = response.data.content
    pagination.total = response.data.totalElements
  } catch (error) {
    allIndividuals.value = [
      { id: 1, name: '小象甲', code: 'IND001', speciesId: 1, speciesName: '亚洲象', gender: 'male', age: 5, status: 'active', description: '性格温顺', createTime: '2024-01-15' },
      { id: 2, name: '母象乙', code: 'IND002', speciesId: 1, speciesName: '亚洲象', gender: 'female', age: 12, status: 'active', description: '群体领导者', createTime: '2024-02-20' },
      { id: 3, name: '幼象丙', code: 'IND003', speciesId: 2, speciesName: '非洲象', gender: 'male', age: 2, status: 'active', description: '活泼好动', createTime: '2024-03-10' },
      { id: 4, name: '老象丁', code: 'IND004', speciesId: 2, speciesName: '非洲象', gender: 'female', age: 25, status: 'inactive', description: '已退役', createTime: '2024-04-05' },
      { id: 5, name: '猴子戊', code: 'IND005', speciesId: 3, speciesName: '金丝猴', gender: 'male', age: 8, status: 'active', description: '擅长攀爬', createTime: '2024-05-18' }
    ]
    pagination.total = allIndividuals.value.length
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
    await axios.delete(`/api/individuals/${id}`)
    ElMessage.success('删除成功')
    loadIndividuals()
  } catch (error) {
    allIndividuals.value = allIndividuals.value.filter(item => item.id !== id)
    ElMessage.success('删除成功')
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  const species = speciesList.value.find(s => s.id === form.speciesId)
  const speciesName = species ? species.name : ''
  
  try {
    if (form.id) {
      await axios.put(`/api/individuals/${form.id}`, form)
      ElMessage.success('修改成功')
    } else {
      await axios.post('/api/individuals', form)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    resetForm()
    loadIndividuals()
  } catch (error) {
    if (form.id) {
      const index = allIndividuals.value.findIndex(item => item.id === form.id)
      if (index !== -1) {
        allIndividuals.value[index] = { ...form, speciesName }
      }
      ElMessage.success('修改成功')
    } else {
      form.id = Date.now()
      allIndividuals.value.push({ ...form, speciesName, createTime: new Date().toISOString().split('T')[0] })
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
  form.speciesId = ''
  form.gender = 'male'
  form.age = 0
  form.description = ''
  form.status = 'active'
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
</style>
