Page({
  data: {},

  onLoad(query) {
    this.next = query.next || ''
    this.source = query.source === 'album' ? 'album' : 'camera'
    this.animalId = query.animalId || 'asian-elephant'
  },

  accept() {
    wx.setStorageSync('visitor.privacy.accepted', true)
    if (this.next === 'capture') {
      wx.redirectTo({ url: `/pages/capture/index?source=${this.source}&animalId=${this.animalId}` })
      return
    }
    if (this.next === 'chat') {
      wx.navigateBack({
        fail: () => wx.redirectTo({ url: `/pages/animal-chat/index?animalId=${this.animalId}` })
      })
      return
    }
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/home/index' }) })
  },

  decline() {
    wx.removeStorageSync('visitor.pendingCapture')
    wx.redirectTo({ url: '/pages/home/index' })
  }
})
