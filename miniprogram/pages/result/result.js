Page({
  data: {
    imagePath: '',
    result: {
      elephant_name: '',
      confidence: 0,
      all_probs: {}
    },
    sortedProbs: [],
    confLevelClass: '',
    confLevelText: ''
  },

  onLoad() {
    console.log('结果页面加载')
    
    try {
      // 从本地存储获取数据
      const result = wx.getStorageSync('predictResult')
      const imagePath = wx.getStorageSync('predictImage')
      
      console.log('从存储读取结果:', result)
      
      if (!result || !result.all_probs) {
        throw new Error('没有找到识别结果')
      }
      
      // 转换概率为数组并排序，同时添加样式类和格式化文本，只保留前3个
      const sortedProbs = Object.keys(result.all_probs).map(name => {
        const prob = result.all_probs[name]
        let className = 'low'
        if (prob > 80) {
          className = 'high'
        } else if (prob > 50) {
          className = 'medium'
        }
        return {
          name: name,
          prob: prob,
          class: className,
          probText: prob.toFixed(1)
        }
      }).sort((a, b) => b.prob - a.prob).slice(0, 3)

      console.log('排序后的概率（前3）:', sortedProbs)

      // 设置置信度等级
      let confLevelClass = ''
      let confLevelText = ''
      if (result.confidence > 80) {
        confLevelClass = 'high'
        confLevelText = '高置信度'
      } else if (result.confidence > 60) {
        confLevelClass = 'medium'
        confLevelText = '中等置信度'
      } else {
        confLevelClass = 'low'
        confLevelText = '低置信度'
      }

      this.setData({
        result: result,
        sortedProbs: sortedProbs,
        confLevelClass: confLevelClass,
        confLevelText: confLevelText,
        imagePath: imagePath
      })

      // 清除存储的数据
      wx.removeStorageSync('predictResult')
      wx.removeStorageSync('predictImage')
      
    } catch (error) {
      console.error('解析数据失败:', error)
      wx.showToast({
        title: error.message || '数据解析失败',
        icon: 'none'
      })
    }
  },

  // 返回首页重新识别
  goBack() {
    wx.navigateBack({
      delta: 1
    })
  }
})