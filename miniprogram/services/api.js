const SESSION_KEY = 'visitor.session'
const DRAFT_KEY = 'visitor.draft'

function baseUrl() {
  const app = getApp()
  return (app && app.globalData && app.globalData.apiBase) || 'http://localhost:8082'
}

function session() {
  return wx.getStorageSync(SESSION_KEY) || null
}

function authHeader() {
  const current = session()
  return current && current.token ? { Authorization: 'Bearer ' + current.token } : {}
}

function request({ url, method, data, header, timeout }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl() + url,
      method: method || 'GET',
      data: data,
      timeout: timeout || 30000,
      header: Object.assign({ 'Content-Type': 'application/json' }, authHeader(), header || {}),
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync(SESSION_KEY)
          reject(new Error('游客会话已失效，请返回首页重试'))
          return
        }
        const body = res.data || {}
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 1) {
          resolve(body.data)
          return
        }
        reject(new Error(body.msg || '请求未完成'))
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

function ensureSession() {
  const current = session()
  if (current && current.token && new Date(current.expiresAt).getTime() > Date.now()) {
    return Promise.resolve(current)
  }
  return request({ url: '/v1/visitor-sessions', method: 'POST', data: {}, header: {} }).then((data) => {
    wx.setStorageSync(SESSION_KEY, data)
    return data
  })
}

function saveDraft(draft) {
  wx.setStorageSync(DRAFT_KEY, draft)
  return draft
}

function getDraft() {
  return wx.getStorageSync(DRAFT_KEY) || null
}

function clearDraft() {
  wx.removeStorageSync(DRAFT_KEY)
}

function readFile(filePath) {
  return new Promise((resolve, reject) => {
    wx.getFileSystemManager().readFile({
      filePath,
      success: (res) => resolve(res.data),
      fail: reject
    })
  })
}

function contentTypeOf(fileName) {
  const lower = String(fileName || '').toLowerCase()
  if (lower.endsWith('.png')) return 'image/png'
  if (lower.endsWith('.webp')) return 'image/webp'
  if (lower.endsWith('.mp4')) return 'video/mp4'
  if (lower.endsWith('.mov')) return 'video/quicktime'
  return 'image/jpeg'
}

/**
 * 图片和视频走同一条链路：登记媒体、上传字节、完成、创建任务。
 * 页面只提交图片。视频能力留在这里，当前没有入口按钮。
 */
function submitMedia(draft, onProgress) {
  const fileName = String(draft.tempFilePath).split('/').pop() || 'photo.jpg'
  const contentType = draft.contentType || contentTypeOf(fileName)
  if (onProgress) onProgress(10)
  return ensureSession()
    .then(() => request({
      url: '/v1/media-upload-sessions',
      method: 'POST',
      data: {
        fileName,
        contentType,
        sizeBytes: draft.size || 1,
        source: draft.source || 'album'
      }
    }))
    .then((upload) => readFile(draft.tempFilePath).then((bytes) => ({ upload, bytes, contentType })))
    .then(({ upload, bytes, contentType: type }) => {
      if (onProgress) onProgress(40)
      return request({
        url: '/v1/media-upload-sessions/' + upload.id + '/content',
        method: 'PUT',
        data: bytes,
        header: { 'Content-Type': type },
        timeout: 120000
      }).then(() => upload)
    })
    .then((upload) => {
      if (onProgress) onProgress(70)
      return request({ url: '/v1/media-upload-sessions/' + upload.id + '/complete', method: 'POST', data: {} })
    })
    .then((ready) => {
      if (onProgress) onProgress(90)
      return request({
        url: '/v1/recognition-tasks',
        method: 'POST',
        data: { mediaAssetId: ready.mediaAssetId },
        header: { 'Idempotency-Key': ready.mediaAssetId },
        timeout: 20000
      })
    })
}

function getTask(taskId) {
  return request({ url: '/v1/recognition-tasks/' + taskId })
}

function getHistory() {
  return request({ url: '/v1/me/recognition-history' })
}

function deleteHistory(taskId) {
  return request({ url: '/v1/me/recognition-history/' + taskId, method: 'DELETE' })
}

function submitFeedback(taskId, verdict) {
  return request({
    url: '/v1/recognition-tasks/' + taskId + '/feedback',
    method: 'PUT',
    data: { verdict }
  })
}

function listElephants() {
  return request({ url: '/v1/public/elephants' })
}

function getElephant(id) {
  return request({ url: '/v1/public/elephants/' + id })
}

function requestPrivacy() {
  return ensureSession().then(() => request({ url: '/v1/privacy-requests', method: 'POST', data: { type: 'ACCESS' } }))
}

module.exports = {
  ensureSession,
  saveDraft,
  getDraft,
  clearDraft,
  submitMedia,
  getTask,
  getHistory,
  deleteHistory,
  submitFeedback,
  listElephants,
  getElephant,
  requestPrivacy
}
