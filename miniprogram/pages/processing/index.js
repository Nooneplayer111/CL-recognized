const api = require('../../services/api')

Page({
  data: {
    phase: 'upload',
    uploadProgress: 0,
    task: null,
    taskId: '',
    error: '',
    retryable: false,
    recoveryPhase: '',
    canCreateNewAttempt: false,
    canOpenResult: false,
    lookupRetrying: false
  },

  onLoad(query) {
    this.stopped = false
    this.taskId = query.taskId || ''
    if (this.taskId) {
      this.setData({ phase: 'task', taskId: this.taskId })
      this.poll(Date.now())
      return
    }
    this.submitPhoto()
  },

  onUnload() {
    this.stopped = true
    this.clearTimer()
  },

  clearTimer() {
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = null
    }
  },

  submitPhoto() {
    const draft = api.getDraft()
    if (!draft || !draft.tempFilePath) {
      this.setData({ error: '请先选择照片。', retryable: false, phase: 'upload' })
      return
    }
    this.setData({ phase: 'upload', error: '', retryable: false, uploadProgress: 5, canCreateNewAttempt: false })
    api.submitMedia(draft, (uploadProgress) => {
      if (!this.stopped) this.setData({ uploadProgress })
    }).then((task) => {
      api.clearDraft()
      this.taskId = task.id
      this.setData({ phase: 'task', task, taskId: task.id, uploadProgress: 100 })
      this.follow(task, Date.now())
    }).catch((error) => {
      this.setData({ error: error.message || '照片没有提交成功', retryable: true, canCreateNewAttempt: false })
    })
  },

  poll(startedAt) {
    this.clearTimer()
    if (this.stopped || !this.taskId) return
    api.getTask(this.taskId).then((task) => {
      if (this.stopped) return
      this.follow(task, startedAt)
    }).catch((error) => {
      if (this.stopped) return
      this.setData({ lookupRetrying: true, error: error.message || '暂时没查到进度' })
      this.timer = setTimeout(() => this.poll(startedAt), 2000)
    })
  },

  follow(task, startedAt) {
    const done = task.terminal || task.taskState === 'REVIEW_PENDING' || task.resultKind === 'PENDING_REVIEW'
    this.setData({ task, canOpenResult: done, lookupRetrying: false, error: done ? '' : this.data.error })
    if (done) {
      this.openResult()
      return
    }
    if (Date.now() - startedAt >= 90000) {
      this.setData({
        recoveryPhase: 'timed_out',
        error: '识别仍在处理。可以稍后再查同一次任务，不会重新上传照片。',
        retryable: true,
        canCreateNewAttempt: false
      })
      return
    }
    this.timer = setTimeout(() => this.poll(startedAt), 1500)
  },

  retry() {
    this.setData({ error: '', retryable: false, recoveryPhase: '' })
    if (this.taskId) {
      this.poll(Date.now())
      return
    }
    this.submitPhoto()
  },

  openResult() {
    if (!this.taskId) return
    wx.redirectTo({ url: `/pages/result/index?taskId=${this.taskId}` })
  },

  createNewAttempt() {
    api.clearDraft()
    wx.redirectTo({ url: '/pages/home/index' })
  },

  goHistory() {
    wx.redirectTo({ url: '/pages/home/index' })
  },

  goHome() {
    wx.redirectTo({ url: '/pages/home/index' })
  }
})
