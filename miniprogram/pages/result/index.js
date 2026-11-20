const api = require('../../services/api')

Page({
  data: {
    loading: true,
    error: '',
    deleted: false,
    task: null,
    pendingReview: false,
    feedbackError: '',
    feedbackOptions: [
      { value: 'CORRECT', label: '正确' },
      { value: 'INCORRECT', label: '不正确' },
      { value: 'UNSURE', label: '不确定' }
    ]
  },

  onLoad(query) {
    this.taskId = query.taskId || ''
    this.load()
  },

  load() {
    if (!this.taskId) {
      this.setData({ loading: false, error: '没有找到这条识别记录。' })
      return
    }
    this.setData({ loading: true, error: '' })
    api.getTask(this.taskId).then((task) => {
      const pendingReview = task.taskState === 'REVIEW_PENDING' || task.resultKind === 'PENDING_REVIEW'
      this.setData({ loading: false, task, pendingReview })
    }).catch((error) => {
      const deleted = (error.message || '').indexOf('删除') >= 0
      this.setData({ loading: false, error: error.message || '结果暂不可用', deleted })
    })
  },

  retryLoad() {
    this.load()
  },

  openProfile(event) {
    const id = event.currentTarget.dataset.id || (this.data.task && this.data.task.publicProfileRef)
    if (!id) return
    wx.navigateTo({ url: `/pages/elephant/index?id=${id}` })
  },

  retry() {
    wx.redirectTo({ url: '/pages/home/index' })
  },

  submitFeedback(event) {
    const verdict = event.currentTarget.dataset.value
    this.setData({ feedbackError: '' })
    api.submitFeedback(this.taskId, verdict).then((feedback) => {
      const task = Object.assign({}, this.data.task, { feedback })
      this.setData({ task })
    }).catch((error) => {
      this.setData({ feedbackError: error.message || '反馈没有提交成功' })
    })
  },

  goHistory() {
    wx.redirectTo({ url: '/pages/home/index' })
  }
})
