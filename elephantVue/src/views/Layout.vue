<template>
  <div class="layout-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>物种识别</h2>
      </div>
      <el-menu :default-active="activeMenu" class="sidebar-menu" background-color="#2a3f5f" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/species" @click="handleMenuClick('/species')">
          <el-icon><Menu /></el-icon>
          <span>物种管理</span>
        </el-menu-item>
        <el-menu-item index="/individual" @click="handleMenuClick('/individual')">
          <el-icon><User /></el-icon>
          <span>个体管理</span>
        </el-menu-item>
        <el-menu-item index="/camera" @click="handleMenuClick('/camera')">
          <el-icon><VideoCamera /></el-icon>
          <span>摄像头管理</span>
        </el-menu-item>
        <el-menu-item index="/profile" @click="handleMenuClick('/profile')">
          <el-icon><Setting /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </aside>
    
    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <el-icon class="toggle-btn"><Menu /></el-icon>
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Menu, User, VideoCamera, Setting, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const username = ref(localStorage.getItem('username') || '用户')
const activeMenu = ref('/species')

const pageTitleMap = {
  '/species': '物种管理',
  '/individual': '个体管理',
  '/camera': '摄像头管理',
  '/profile': '个人中心'
}

const pageTitle = computed(() => {
  return pageTitleMap[route.path] || '物种个体识别系统'
})

const handleMenuClick = (path) => {
  activeMenu.value = path
  router.push(path)
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    ElMessage.success('退出成功')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  activeMenu.value = route.path
})
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 200px;
  background-color: #2a3f5f;
  flex-shrink: 0;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #1f2d44;
}

.logo h2 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
  height: calc(100% - 60px);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
  overflow: hidden;
}

.header {
  height: 60px;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-info span {
  font-size: 14px;
  color: #606266;
}

.user-info :deep(.el-icon) {
  font-size: 16px;
  color: #606266;
}

main :deep(.el-main) {
  padding: 20px;
  overflow: auto;
}
</style>
