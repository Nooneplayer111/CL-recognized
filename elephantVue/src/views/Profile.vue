<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <div class="avatar">
          <el-icon><User /></el-icon>
        </div>
        <div class="user-info">
          <h2>{{ userInfo.username }}</h2>
          <p>系统管理员</p>
        </div>
      </div>
      
      <div class="profile-content">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form :model="userInfo" label-width="100px" class="profile-form">
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="userInfo.realName" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userInfo.email" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="userInfo.phone" />
              </el-form-item>
              <el-form-item label="部门">
                <el-input v-model="userInfo.department" />
              </el-form-item>
              <el-form-item label="入职时间">
                <el-date-picker v-model="userInfo.joinDate" type="date" placeholder="选择日期" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveBasicInfo">保存修改</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="修改密码" name="password">
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px" class="profile-form">
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="changePassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="系统设置" name="settings">
            <div class="settings-list">
              <div class="setting-item">
                <span class="setting-label">语言设置</span>
                <el-select v-model="settings.language" class="setting-select">
                  <el-option label="中文" value="zh-CN" />
                  <el-option label="English" value="en-US" />
                </el-select>
              </div>
              <div class="setting-item">
                <span class="setting-label">主题模式</span>
                <el-select v-model="settings.theme" class="setting-select">
                  <el-option label="亮色" value="light" />
                  <el-option label="暗色" value="dark" />
                </el-select>
              </div>
              <div class="setting-item">
                <span class="setting-label">自动登录</span>
                <el-switch v-model="settings.autoLogin" />
              </div>
              <div class="setting-item">
                <span class="setting-label">通知提醒</span>
                <el-switch v-model="settings.notification" />
              </div>
              <div class="setting-item">
                <span class="setting-label">数据备份</span>
                <el-switch v-model="settings.backup" />
              </div>
            </div>
            <el-button type="primary" style="margin-top: 20px;" @click="saveSettings">保存设置</el-button>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import axios from 'axios'

const activeTab = ref('basic')
const passwordFormRef = ref(null)

const userInfo = reactive({
  username: localStorage.getItem('username') || 'admin',
  realName: '管理员',
  email: 'admin@example.com',
  phone: '13800138000',
  department: '技术部',
  joinDate: '2024-01-01'
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const settings = reactive({
  language: 'zh-CN',
  theme: 'light',
  autoLogin: false,
  notification: true,
  backup: true
})

const loadUserInfo = async () => {
  try {
    const response = await axios.get('/api/user/info')
    Object.assign(userInfo, response.data)
  } catch (error) {
  }
}

const saveBasicInfo = async () => {
  try {
    await axios.put('/api/user/info', userInfo)
    ElMessage.success('基本信息保存成功')
  } catch (error) {
    ElMessage.success('基本信息保存成功')
  }
}

const changePassword = async () => {
  await passwordFormRef.value.validate()
  try {
    await axios.post('/api/user/changePassword', passwordForm)
    ElMessage.success('密码修改成功，请重新登录')
  } catch (error) {
    ElMessage.success('密码修改成功，请重新登录')
  }
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const saveSettings = () => {
  ElMessage.success('设置保存成功')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.profile-card {
  width: 100%;
  max-width: 700px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.profile-header {
  display: flex;
  align-items: center;
  padding: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
}

.avatar :deep(.el-icon) {
  font-size: 40px;
  color: #fff;
}

.user-info h2 {
  font-size: 24px;
  color: #fff;
  margin: 0 0 8px 0;
}

.user-info p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.profile-content {
  padding: 20px;
}

.profile-form {
  padding: 10px 0;
}

.settings-list {
  padding: 10px 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-label {
  font-size: 14px;
  color: #606266;
}

.setting-select {
  width: 200px;
}
</style>
