function agentBase() {
  const app = getApp()
  return (app && app.globalData && app.globalData.agentBase) || 'http://127.0.0.1:8090'
}

function authHeader() {
  const current = wx.getStorageSync('visitor.session') || null
  return current && current.token ? { Authorization: 'Bearer ' + current.token } : {}
}

function request({ url, method, data }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: agentBase() + url,
      method: method || 'GET',
      data: data,
      timeout: 45000,
      header: Object.assign({ 'Content-Type': 'application/json' }, authHeader()),
      success(res) {
        const body = res.data || {}
        if (body.code === 1) {
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

function getAnimal(animalId) {
  return request({ url: '/v1/chat/animals/' + animalId })
}

function createSession(animalId) {
  return request({ url: '/v1/chat/sessions', method: 'POST', data: { animalId } })
}

function listSessions(animalId) {
  return request({ url: '/v1/chat/sessions?animalId=' + animalId })
}

function getMessages(sessionId) {
  return request({ url: '/v1/chat/sessions/' + sessionId + '/messages' })
}

function postMessage(sessionId, text) {
  return request({
    url: '/v1/chat/sessions/' + sessionId + '/messages',
    method: 'POST',
    data: { text }
  })
}

module.exports = { getAnimal, createSession, listSessions, getMessages, postMessage }
