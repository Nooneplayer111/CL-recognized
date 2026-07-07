import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/species',
    children: [
      {
        path: 'species',
        name: 'Species',
        component: () => import('../views/Species.vue')
      },
      {
        path: 'individual',
        name: 'Individual',
        component: () => import('../views/Individual.vue')
      },
      {
        path: 'camera',
        name: 'Camera',
        component: () => import('../views/Camera.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    next()
  } else {
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
