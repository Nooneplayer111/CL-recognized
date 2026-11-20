Page({
  data: {
    speciesList: [
      {
        id: 'elephant',
        name: '大象',
        icon: '🐘',
        desc: '基于深度学习的个体识别',
        available: true,
        url: '/pages/elephant/elephant'
      },
      {
        id: 'tiger',
        name: '老虎',
        icon: '🐯',
        desc: '敬请期待',
        available: false,
        url: ''
      },
      {
        id: 'panda',
        name: '熊猫',
        icon: '🐼',
        desc: '敬请期待',
        available: false,
        url: ''
      },
      {
        id: 'giraffe',
        name: '长颈鹿',
        icon: '🦒',
        desc: '敬请期待',
        available: false,
        url: ''
      }
    ]
  },

  onSpeciesTap(e) {
    const { available, url, name } = e.currentTarget.dataset

    if (!available) {
      wx.showToast({
        title: `${name}识别功能即将上线`,
        icon: 'none'
      })
      return
    }

    wx.navigateTo({ url })
  }
})
