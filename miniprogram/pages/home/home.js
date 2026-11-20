Page({
  data: {
    menuItems: [
      {
        id: 'identify',
        title: '个体识别',
        desc: 'AI 智能识别动物个体',
        icon: '🔍',
        color: '#2563eb',
        url: '/pages/species-select/species-select'
      },
      {
        id: 'news',
        title: '动物资讯',
        desc: '了解园区动物最新动态',
        icon: '📰',
        color: '#16a34a',
        url: '/pages/news/news'
      },
      {
        id: 'checkin',
        title: '打卡物种',
        desc: '记录您的物种发现之旅',
        icon: '✅',
        color: '#f59e0b',
        url: '/pages/checkin/checkin'
      },
      {
        id: 'map',
        title: '动物地图',
        desc: '探索园区动物分布位置',
        icon: '🗺️',
        color: '#8b5cf6',
        url: '/pages/map/map'
      }
    ]
  },

  onMenuTap(e) {
    const { url } = e.currentTarget.dataset
    wx.navigateTo({ url })
  }
})
