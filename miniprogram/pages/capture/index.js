const api = require('../../services/api')
const { findAnimal } = require('../../services/animals')

Page({
  data: {
    source: 'camera',
    sourceLabel: '拍照',
    animal: findAnimal(),
    draft: null,
    choosing: false,
    error: ''
  },

  onLoad(query) {
    const source = query.source === 'album' ? 'album' : 'camera'
    const animal = findAnimal(query.animalId)
    let draft = api.getDraft()
    if (!draft || !draft.tempFilePath || draft.animalId !== animal.id || draft.source !== source) {
      draft = null
    }
    this.setData({
      source,
      animal,
      sourceLabel: source === 'camera' ? '拍照' : '相册',
      draft
    })
  },

  chooseMedia() {
    if (this.data.choosing) return
    this.setData({ choosing: true, error: '' })
    const sourceType = this.data.source === 'camera' ? ['camera'] : ['album']
    const finish = (file) => {
      const draft = api.saveDraft({
        tempFilePath: file.tempFilePath,
        size: file.size || 1,
        source: this.data.source,
        animalId: this.data.animal.id,
        contentType: 'image/jpeg'
      })
      this.setData({ draft, choosing: false })
    }
    const fail = (error) => {
      const message = error && error.errMsg ? error.errMsg : ''
      this.setData({ choosing: false, error: message.indexOf('cancel') >= 0 ? '' : '请检查相机或相册权限后再试。' })
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType,
        sizeType: ['compressed'],
        success: (res) => {
          const file = res.tempFiles && res.tempFiles[0]
          if (!file) {
            this.setData({ choosing: false, error: '没有选到照片' })
            return
          }
          finish(file)
        },
        fail
      })
      return
    }
    wx.chooseImage({
      count: 1,
      sourceType,
      sizeType: ['compressed'],
      success: (res) => finish({ tempFilePath: res.tempFilePaths[0], size: 1 }),
      fail
    })
  },

  continueFlow() {
    if (!this.data.draft || !this.data.draft.tempFilePath) return
    wx.navigateTo({ url: '/pages/processing/index' })
  }
})
