<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>物种个体识别系统</h1>
        <p>{{ mode === 'login' ? '请登录您的账户' : '创建新账户' }}</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名（选填）" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" @click="handleSubmit" :loading="loading">
            {{ mode === 'login' ? '登录' : '注册并进入' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="switch-row">
        <span>{{ mode === 'login' ? '没有账号？' : '已有账号？' }}</span>
        <el-button link type="primary" @click="switchMode">
          {{ mode === 'login' ? '去注册' : '去登录' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import router from '../router'
import request from '../utils/request'

const formRef = ref(null)
const loading = ref(false)
const mode = ref('login')
const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules = computed(() => {
  const username = [
    { required: true, message: '用户名不能为空', trigger: 'blur' }
  ]
  const password = [
    { required: true, message: '密码不能为空', trigger: 'blur' }
  ]
  if (mode.value === 'login') {
    return { username, password }
  }
  return {
    username: [
      {
        validator: (rule, value, callback) => {
          const username = (value || '').trim()
          if (!username) {
            callback(new Error('用户名不能为空'))
            return
          }
          if (!/^[A-Za-z0-9_]{2,32}$/.test(username)) {
            callback(new Error('用户名需为2-32位字母、数字或下划线'))
            return
          }
          callback()
        },
        trigger: 'blur'
      }
    ],
    realName: [
      { max: 64, message: '真实姓名不能超过64个字', trigger: 'blur' }
    ],
    password: [
      ...password,
      { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '两次输入的密码不一致', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  }
})

const switchMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  const isRegister = mode.value === 'register'

  try {
    const response = await request.post(isRegister ? '/user/register' : '/user/login', isRegister ? {
      username: form.username.trim(),
      realName: form.realName.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword
    } : {
      username: form.username,
      password: form.password
    })

    localStorage.setItem('token', response.data.token)
    localStorage.setItem('username', response.data.userName)
    ElMessage.success(isRegister ? '注册成功' : '登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || (isRegister ? '注册失败' : '登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
}

.login-form {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
}

.switch-row {
  text-align: center;
  font-size: 14px;
  color: #909399;
}
</style>
