const api = require('../../services/api')

Page({
  data: { items: [], job: null, error: '' },

  onShow() {
    this.load()
  },

  load() {
    api.ensureSession()
      .then(() => api.getHistory())
      .then((history) => this.setData({ items: history.items || [], error: '' }))
      .catch((error) => this.setData({ error: error.message || '记录加载失败' }))
  },

  deleteOne(event) {
    const taskId = event.currentTarget.dataset.id
    api.deleteHistory(taskId).then((job) => {
      this.setData({ job })
      this.load()
    }).catch((error) => this.setData({ error: error.message || '删除未完成' }))
  },

  requestPrivacyJob() {
    api.requestPrivacy().then((job) => {
      this.setData({ job, error: '' })
    }).catch((error) => this.setData({ error: error.message || '申请没有提交' }))
  },

  retry() {
    this.load()
  },

  goHistory() {
    wx.redirectTo({ url: '/pages/home/index' })
  }
})
