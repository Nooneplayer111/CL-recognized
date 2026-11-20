const api = require('../../services/api')
const { ANIMALS } = require('../../services/animals')

Page({
  data: {
    sessionError: '',
    availableAnimals: ANIMALS,
    historyItems: [],
    visibleHistoryItems: [],
    hasMoreHistory: false,
    showAllHistory: false,
    historyLoading: true,
    historyError: '',
    recovery: null,
    elephants: []
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    this.setData({ historyLoading: true, historyError: '', sessionError: '' })
    api.ensureSession()
      .then(() => Promise.all([api.getHistory(), api.listElephants()]))
      .then(([history, elephants]) => {
        const items = history.items || []
        const recovery = items.find((item) => !item.terminal && item.taskState !== 'REVIEW_PENDING')
        this.setData({
          historyItems: items,
          elephants: elephants || [],
          recovery: recovery || null,
          historyLoading: false
        })
        this.applyHistory()
      })
      .catch((error) => {
        this.setData({
          sessionError: error.message || '服务连接失败',
          historyLoading: false,
          historyError: error.message || '记录加载失败'
        })
      })
  },

  applyHistory() {
    const historyItems = this.data.historyItems
    const visibleHistoryItems = this.data.showAllHistory ? historyItems : historyItems.slice(0, 3)
    this.setData({
      visibleHistoryItems,
      hasMoreHistory: historyItems.length > 3
    })
  },

  retrySession() {
    this.refresh()
  },

  refreshHistory() {
    this.refresh()
  },

  toggleHistory() {
    this.setData({ showAllHistory: !this.data.showAllHistory })
    this.applyHistory()
  },

  openAnimal(event) {
    const animalId = event.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/animal-chat/index?animalId=${animalId}` })
  },

  openRecovery() {
    if (!this.data.recovery) return
    wx.navigateTo({ url: `/pages/processing/index?taskId=${this.data.recovery.id}` })
  },

  openItem(event) {
    const item = this.data.historyItems.find((row) => row.id === event.currentTarget.dataset.id)
    if (!item) return
    const done = item.terminal || item.taskState === 'REVIEW_PENDING' || item.resultKind === 'PENDING_REVIEW'
    wx.navigateTo({
      url: done ? `/pages/result/index?taskId=${item.id}` : `/pages/processing/index?taskId=${item.id}`
    })
  },

  deleteItem(event) {
    const taskId = event.currentTarget.dataset.id
    wx.showModal({
      title: '删除这条识别记录？',
      content: '删除后首页不再显示。原始识别事实仍保留在管理端审计中。',
      confirmText: '确认删除',
      confirmColor: '#A4412D',
      success: (result) => {
        if (!result.confirm) return
        api.deleteHistory(taskId).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          this.refresh()
        }).catch((error) => {
          this.setData({ historyError: error.message || '删除未完成' })
        })
      }
    })
  },

  openProfile(event) {
    wx.navigateTo({ url: `/pages/elephant/index?id=${event.currentTarget.dataset.id}` })
  },

  openPrivacy() {
    wx.navigateTo({ url: '/pages/privacy/index' })
  },

  openDeletion() {
    wx.navigateTo({ url: '/pages/deletion/index' })
  }
})
