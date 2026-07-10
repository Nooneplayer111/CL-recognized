import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['token'] = token
  }
  return config
}, error => {
  return Promise.reject(error)
})

request.interceptors.response.use(response => {
  const res = response.data
  if (res.code === 0) {
    return Promise.reject(new Error(res.msg || 'Error'))
  }
  return res
}, error => {
  if (error.response?.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    window.location.href = '/login'
    return Promise.reject(error)
  }
  const message = error.response?.data?.msg || error.message || '请求失败'
  return Promise.reject(new Error(message))
})

export default request