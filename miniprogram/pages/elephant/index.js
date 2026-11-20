const api = require('../../services/api')

Page({
  data: { loading: true, error: '', profile: null },

  onLoad(query) {
    this.profileId = query.id || ''
    this.load()
  },

  load() {
    if (!this.profileId) {
      this.setData({ loading: false, error: '没有这条公开档案。' })
      return
    }
    this.setData({ loading: true, error: '' })
    api.getElephant(this.profileId).then((profile) => {
      this.setData({ loading: false, profile })
    }).catch((error) => {
      this.setData({ loading: false, error: error.message || '公开档案暂不可用', profile: null })
    })
  },

  retry() {
    this.load()
  },

  startRecognition() {
    wx.redirectTo({ url: '/pages/home/index' })
  }
})
