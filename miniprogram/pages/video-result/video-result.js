const app = getApp()

Page({
  data: {
    videoUrl: '',
    elephantCount: 0,
    frameCount: 0,
    elephants: [],
    loading: true,
    downloading: false,
    downloaded: false,
    downloadProgress: 0
  },

  onLoad() {
    this.loadResult()
  },

  loadResult() {
    try {
      const result = wx.getStorageSync('videoResult')
      
      console.log('视频结果:', result)
      
      // 处理大象数据，计算置信度百分比（后端返回的可能已经是百分比值）
      const elephants = (result.elephants || []).map(item => {
        let confidence = item.confidence || 0
        // 如果置信度大于100，说明后端已经返回的是百分比，直接使用
        // 如果置信度小于等于1，说明是小数形式，需要乘以100
        const percent = confidence > 100 ? confidence : 
                        confidence <= 1 ? confidence * 100 : confidence
        return {
          ...item,
          confidencePercent: percent.toFixed(1)
        }
      })
      
      this.setData({
        videoUrl: result.video_url || '',
        elephantCount: result.elephant_count || 0,
        frameCount: result.frame_count || 0,
        elephants: elephants,
        loading: false
      })
    } catch (error) {
      console.error('加载结果失败:', error)
      wx.showToast({ title: '加载结果失败', icon: 'none' })
      this.setData({ loading: false })
    }
  },

  downloadVideo() {
    if (!this.data.videoUrl) {
      wx.showToast({ title: '没有可下载的视频', icon: 'none' })
      return
    }

    const that = this
    that.setData({ downloading: true, downloaded: false, downloadProgress: 0 })

    wx.showLoading({ title: '正在下载...' })

    // 使用下载API下载视频
    wx.downloadFile({
      url: this.data.videoUrl.replace('0.0.0.0', '192.168.100.1'),
      timeout: 120000,
      success(res) {
        if (res.statusCode === 200) {
          console.log('视频下载成功:', res.tempFilePath)
          
          // 保存到相册
          wx.saveVideoToPhotosAlbum({
            filePath: res.tempFilePath,
            success() {
              wx.hideLoading()
              that.setData({ downloading: false, downloaded: true })
              wx.showToast({ title: '保存成功！视频已保存到相册', icon: 'success', duration: 3000 })
            },
            fail(err) {
              wx.hideLoading()
              that.setData({ downloading: false })
              console.error('保存到相册失败:', err)
              // 如果用户拒绝权限，提示用户手动保存
              if (err.errMsg && err.errMsg.includes('auth denied')) {
                wx.showModal({
                  title: '保存失败',
                  content: '请在系统设置中开启相册权限，或手动保存视频',
                  showCancel: false
                })
              } else {
                wx.showToast({ title: '保存失败: ' + err.errMsg, icon: 'none' })
              }
            }
          })
        } else {
          wx.hideLoading()
          that.setData({ downloading: false })
          wx.showToast({ title: '下载失败', icon: 'none' })
        }
      },
      fail(err) {
        wx.hideLoading()
        that.setData({ downloading: false })
        console.error('下载失败:', err)
        wx.showToast({ title: '下载失败: ' + err.errMsg, icon: 'none' })
      }
    })
  },

  goBack() {
    wx.navigateBack()
  }
})