const api = require('../../services/api')
const agent = require('../../services/agent')
const { ANIMALS } = require('../../services/animals')

Page({
  data: {
    animal: null,
    canCapture: false,
    welcome: '',
    greeting: '',
    suggestions: [],
    showSuggestions: true,
    messages: [],
    draftText: '',
    sending: false,
    sendError: '',
    pageError: '',
    showHistory: false,
    historyItems: [],
    historyLoading: false,
    historyError: '',
    anchor: ''
  },

  onLoad(query) {
    const animal = ANIMALS.find((item) => item.id === query.animalId)
    this.animalId = animal ? animal.id : ''
    this.seq = 0
    this.pendingText = ''
    this.choosing = false
    if (!animal) {
      this.setData({ pageError: '没有这种动物' })
      return
    }
    wx.setNavigationBarTitle({ title: animal.name + '助手' })
    this.setData({
      animal,
      canCapture: animal.route === 'elephant',
      welcome: '你好，欢迎来到' + animal.name + '家园',
      greeting: '看看都想要了解些什么样的' + animal.name + '呀？'
    })
    this.prepare()
  },

  onShow() {
    if (!this.animalId) return
    const pending = wx.getStorageSync('visitor.pendingCapture')
    if (!pending || pending.animalId !== this.animalId) return
    if (!wx.getStorageSync('visitor.privacy.accepted')) return
    wx.removeStorageSync('visitor.pendingCapture')
    this.chooseAndSubmit(pending.source === 'album' ? 'album' : 'camera')
  },

  prepare() {
    if (!this.animalId) return
    this.setData({ pageError: '' })
    api.ensureSession().catch(() => null).then(() => {
      agent.getAnimal(this.animalId).then((profile) => {
        this.setData({
          welcome: profile.welcome || this.data.welcome,
          greeting: profile.greeting || this.data.greeting,
          suggestions: profile.suggestions || []
        })
      }).catch(() => {})
      return agent.createSession(this.animalId)
    }).then((session) => {
      if (!session) return
      this.setData({ sessionId: session.sessionId, pageError: '' })
    }).catch((error) => {
      this.setData({ pageError: error.message || '对话服务连接失败' })
    })
  },

  nextKey() {
    this.seq += 1
    return 'k' + this.seq
  },

  scrollToEnd() {
    const messages = this.data.messages
    const anchor = this.data.sending
      ? 'typing'
      : (messages.length ? 'm' + messages[messages.length - 1].key : 'welcome')
    this.setData({ anchor: '' }, () => {
      wx.nextTick(() => this.setData({ anchor }))
    })
  },

  onInput(event) {
    this.setData({ draftText: event.detail.value })
  },

  sendDraft() {
    this.sendText(this.data.draftText, false)
  },

  askSuggestion(event) {
    this.sendText(event.currentTarget.dataset.text, false)
  },

  retrySend() {
    this.sendText(this.pendingText, true)
  },

  sendText(text, retrying) {
    const content = String(text || '').trim()
    if (!content || this.data.sending) return
    if (!this.data.sessionId) {
      this.setData({ sendError: '对话还没准备好' })
      return
    }
    if (!retrying) {
      const messages = this.data.messages.concat([{ key: this.nextKey(), role: 'user', content }])
      this.setData({
        draftText: '',
        messages,
        showSuggestions: false,
        sendError: '',
        sending: true
      })
    } else {
      this.setData({ sendError: '', sending: true })
    }
    this.pendingText = content
    this.scrollToEnd()
    agent.postMessage(this.data.sessionId, content).then((data) => {
      const messages = this.data.messages.concat([{
        key: 's' + data.messageId,
        role: 'assistant',
        content: data.reply
      }])
      this.pendingText = ''
      this.setData({ messages, sending: false })
      this.scrollToEnd()
    }).catch((error) => {
      this.setData({ sending: false, sendError: error.message || '没有发送成功' })
      this.scrollToEnd()
    })
  },

  openHistory() {
    if (!this.animalId) return
    this.setData({ showHistory: true, historyLoading: true, historyError: '' })
    agent.listSessions(this.animalId).then((data) => {
      this.setData({ historyItems: data.items || [], historyLoading: false })
    }).catch((error) => {
      this.setData({ historyLoading: false, historyError: error.message || '对话记录加载失败' })
    })
  },

  closeHistory() {
    this.setData({ showHistory: false })
  },

  noop() {},

  startFresh() {
    if (!this.animalId || this.data.sending) return
    agent.createSession(this.animalId).then((session) => {
      this.pendingText = ''
      this.setData({
        sessionId: session.sessionId,
        messages: [],
        showSuggestions: true,
        sendError: '',
        showHistory: false,
        draftText: ''
      })
    }).catch((error) => {
      this.setData({ historyError: error.message || '没有开新对话' })
    })
  },

  openSession(event) {
    const sessionId = event.currentTarget.dataset.id
    if (!sessionId || this.data.sending) return
    agent.getMessages(sessionId).then((data) => {
      const messages = (data.messages || []).map((item) => ({
        key: 's' + item.id,
        role: item.role,
        content: item.content
      }))
      this.pendingText = ''
      this.setData({
        sessionId: data.sessionId,
        messages,
        showSuggestions: !messages.some((item) => item.role === 'user'),
        sendError: '',
        showHistory: false,
        draftText: ''
      })
      this.scrollToEnd()
    }).catch((error) => {
      this.setData({ historyError: error.message || '这条对话没有打开' })
    })
  },

  startCapture(event) {
    const source = event.currentTarget.dataset.source === 'album' ? 'album' : 'camera'
    if (!this.data.canCapture) {
      wx.showModal({
        title: '拍照识别尚未开放',
        content: '该动物拍照识别尚未开放，可以先提问。',
        showCancel: false
      })
      return
    }
    if (!wx.getStorageSync('visitor.privacy.accepted')) {
      wx.setStorageSync('visitor.pendingCapture', { source, animalId: this.animalId })
      wx.navigateTo({
        url: `/pages/privacy/index?next=chat&source=${source}&animalId=${this.animalId}`
      })
      return
    }
    this.chooseAndSubmit(source)
  },

  chooseAndSubmit(source) {
    if (this.choosing) return
    this.choosing = true
    const sourceType = source === 'album' ? ['album'] : ['camera']
    const finish = (file) => {
      api.saveDraft({
        tempFilePath: file.tempFilePath,
        size: file.size || 1,
        source,
        animalId: this.animalId,
        contentType: 'image/jpeg'
      })
      const messages = this.data.messages.concat([{
        key: this.nextKey(),
        role: 'local',
        content: '已提交识别'
      }])
      this.setData({ messages })
      this.choosing = false
      this.scrollToEnd()
      wx.navigateTo({ url: '/pages/processing/index' })
    }
    const fail = (error) => {
      this.choosing = false
      const message = error && error.errMsg ? error.errMsg : ''
      if (message.indexOf('cancel') >= 0) return
      wx.showModal({
        title: '没有选到照片',
        content: '请检查相机或相册权限后再试。',
        showCancel: false
      })
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
            this.choosing = false
            wx.showModal({ title: '没有选到照片', content: '请再选一次。', showCancel: false })
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
  }
})
