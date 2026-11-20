const app = getApp()

Page({
  data: {
    imagePath: '',
    hasImage: false,
    videoPath: '',
    hasVideo: false,
    videoDuration: '00:00',
    showResult: false,
    loading: false,
    loadingText: '正在处理...',
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
    console.log('页面加载完成')
  },

  // 从相册选择图片
  chooseImage() {
    const that = this
    wx.chooseImage({
      count: 1,
      sizeType: ['original'],
      sourceType: ['album'],
      success(res) {
        const tempFilePath = res.tempFilePaths[0]
        that.setData({
          imagePath: tempFilePath,
          hasImage: true,
          showResult: false
        })
        // 自动进行识别
        that.predictImage(tempFilePath, 'album')
      },
      fail(err) {
        console.error('选择图片失败:', err)
        wx.showToast({
          title: '选择图片失败',
          icon: 'none'
        })
      }
    })
  },

  // 拍照识别
  takePhoto() {
    const that = this
    wx.chooseImage({
      count: 1,
      sizeType: ['original'],
      sourceType: ['camera'],
      success(res) {
        const tempFilePath = res.tempFilePaths[0]
        that.setData({
          imagePath: tempFilePath,
          hasImage: true,
          showResult: false
        })
        // 自动进行识别
        that.predictImage(tempFilePath, 'camera')
      },
      fail(err) {
        console.error('拍照失败:', err)
        wx.showToast({
          title: '拍照失败',
          icon: 'none'
        })
      }
    })
  },

  // 录制视频
  recordVideo() {
    const that = this
    wx.chooseVideo({
      sourceType: ['camera'],
      maxDuration: 15,
      camera: 'back',
      success(res) {
        console.log('录制完成:', res)
        that.setData({
          videoPath: res.tempFilePath,
          hasVideo: true,
          videoDuration: that.formatDuration(res.duration)
        })
        that.predictVideo(res.tempFilePath)
      },
      fail(err) {
        console.error('录制失败:', err)
        wx.showToast({
          title: '录制失败',
          icon: 'none'
        })
      }
    })
  },

  // 格式化时长
  formatDuration(seconds) {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  },

  // 移除视频
  removeVideo() {
    this.setData({
      videoPath: '',
      hasVideo: false,
      videoDuration: '00:00'
    })
  },

  // 移除图片
  removeImage() {
    this.setData({
      imagePath: '',
      hasImage: false,
      showResult: false,
      result: {
        elephant_name: '',
        confidence: 0,
        all_probs: {}
      }
    })
  },

  // 图片转Base64
  imageToBase64(filePath) {
    return new Promise((resolve, reject) => {
      wx.getFileSystemManager().readFile({
        filePath: filePath,
        encoding: 'base64',
        success(res) {
          resolve(res.data)
        },
        fail(err) {
          reject(err)
        }
      })
    })
  },

  // 调用API识别图片
  async predictImage(imagePath, sourceType = 'album') {
    const that = this

    // 显示加载状态
    that.setData({
      loading: true,
      loadingText: '正在识别图片...'
    })

    try {
      console.log('开始识别，图片路径:', imagePath)

      // 将图片转换为Base64
      console.log('正在转换图片为Base64...')
      const base64Data = await that.imageToBase64(imagePath)
      console.log('图片转换完成，大小:', base64Data.length, '字节')

      // 发送请求到后端（使用Promise包装）
      console.log('正在发送请求到:', `${app.globalData.apiUrl}/predict`)

      const result = await that.sendPredictRequest(base64Data)
      console.log('识别结果:', result)

      // 保存识别记录到 Java 后端（失败不影响结果展示）
      that.saveRecognitionRecord({
        mediaType: 'image',
        source: 'miniprogram',
        sourceType: sourceType,
        operatorName: '小程序用户',
        imageBase64: base64Data,
        predictedName: result.elephant_name,
        confidence: result.confidence,
        allProbs: result.all_probs,
        status: 'success'
      }).catch(err => console.warn('保存识别记录失败:', err))

      // 使用本地存储传递数据（避免URL参数长度限制）
      wx.setStorageSync('predictResult', result)
      wx.setStorageSync('predictImage', imagePath)

      // 跳转到结果页面
      wx.navigateTo({
        url: '/pages/result/result'
      })

    } catch (error) {
      console.error('识别错误:', error)
      wx.showToast({
        title: error.message || '识别失败',
        icon: 'none',
        duration: 3000
      })
    } finally {
      that.setData({
        loading: false
      })
    }
  },

  // 视频识别
  async predictVideo(videoPath) {
    const that = this
    that.setData({ loading: true, loadingText: '正在上传视频...' })

    try {
      console.log('开始处理视频:', videoPath)

      // 获取文件信息
      const fileInfo = await that.getFileInfo(videoPath)
      console.log('文件大小:', fileInfo.size)

      // 限制文件大小（5MB，因为Base64编码会增加约33%体积）
      if (fileInfo.size > 5 * 1024 * 1024) {
        throw new Error('视频文件过大，请录制更短的视频（建议5-8秒以内）')
      }

      that.setData({ loadingText: '正在识别视频中的大象...' })

      // 转换视频为Base64
      const base64Data = await that.fileToBase64(videoPath)
      console.log('视频转换完成，大小:', base64Data.length)

      // 发送请求
      that.setData({ loadingText: '正在跟踪识别...' })
      const result = await that.sendVideoRequest(base64Data)

      console.log('视频识别结果:', result)

      // 保存视频识别记录
      const primaryElephant = (result.elephants && result.elephants.length > 0) ? result.elephants[0] : null
      that.saveRecognitionRecord({
        mediaType: 'video',
        source: 'miniprogram',
        sourceType: 'camera',
        operatorName: '小程序用户',
        processedVideoUrl: result.video_url || '',
        predictedName: primaryElephant ? primaryElephant.name : ('识别到' + (result.elephant_count || 0) + '头大象'),
        confidence: primaryElephant ? primaryElephant.confidence : null,
        elephantCount: result.elephant_count,
        frameCount: result.frame_count,
        elephants: result.elephants,
        status: 'success'
      }).catch(err => console.warn('保存视频识别记录失败:', err))

      // 只保存非视频数据到storage（避免超出10MB限制）
      const { output_video, ...metaData } = result
      wx.setStorageSync('videoResult', metaData)
      wx.setStorageSync('videoPath', videoPath)

      // 将处理后的视频单独保存（如果小于6MB）
      if (output_video && output_video.length < 6 * 1024 * 1024) {
        wx.setStorageSync('processedVideo', output_video)
      } else if (output_video) {
        // 如果视频太大，只保存元数据，不保存视频
        console.log('处理后的视频太大，不保存到storage')
      }

      // 跳转到视频结果页面
      wx.navigateTo({ url: '/pages/video-result/video-result' })

    } catch (error) {
      console.error('视频处理错误:', error)
      wx.showToast({ title: error.message || '视频处理失败', icon: 'none', duration: 3000 })
    } finally {
      that.setData({ loading: false })
    }
  },

  // 获取文件信息
  getFileInfo(filePath) {
    return new Promise((resolve, reject) => {
      wx.getFileInfo({
        filePath: filePath,
        success(res) { resolve(res) },
        fail(err) { reject(err) }
      })
    })
  },

  // 文件转Base64
  fileToBase64(filePath) {
    return new Promise((resolve, reject) => {
      wx.getFileSystemManager().readFile({
        filePath: filePath,
        encoding: 'base64',
        success(res) { resolve(res.data) },
        fail(err) { reject(err) }
      })
    })
  },

  // 发送预测请求（Promise版本）
  sendPredictRequest(base64Data) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${app.globalData.apiUrl}/predict`,
        method: 'POST',
        header: {
          'Content-Type': 'application/json'
        },
        data: {
          image_base64: base64Data
        },
        timeout: 30000,
        success(res) {
          if (res.statusCode === 200 && res.data) {
            resolve(res.data)
          } else {
            reject(new Error(res.data?.detail || '识别失败'))
          }
        },
        fail(err) {
          console.error('请求失败:', err)
          reject(new Error('网络请求失败: ' + (err.errMsg || '未知错误')))
        }
      })
    })
  },

  // 保存识别记录到 Java 后端
  saveRecognitionRecord(recordData) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${app.globalData.javaApiUrl}/open/recognition-records`,
        method: 'POST',
        header: {
          'Content-Type': 'application/json'
        },
        data: recordData,
        timeout: 30000,
        success(res) {
          if (res.statusCode === 200 && res.data && res.data.code === 1) {
            resolve(res.data.data)
          } else {
            reject(new Error(res.data?.msg || '保存识别记录失败'))
          }
        },
        fail(err) {
          reject(new Error('保存识别记录请求失败: ' + (err.errMsg || '未知错误')))
        }
      })
    })
  },

  // 发送视频识别请求
  sendVideoRequest(base64Data) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: `${app.globalData.apiUrl}/predict-video`,
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        data: { video_base64: base64Data },
        timeout: 300000,  // 增加到5分钟（300秒）
        success(res) {
          if (res.statusCode === 200 && res.data) resolve(res.data)
          else reject(new Error(res.data?.detail || '视频处理失败'))
        },
        fail(err) { reject(new Error('网络请求失败: ' + (err.errMsg || '未知错误'))) }
      })
    })
  },

  // 处理识别结果
  handleResult(data) {
    console.log('识别结果:', data)

    // 转换概率为数组并排序
    const sortedProbs = Object.keys(data.all_probs).map(name => ({
      name: name,
      prob: data.all_probs[name]
    })).sort((a, b) => b.prob - a.prob)

    // 设置置信度等级
    let confLevelClass = ''
    let confLevelText = ''
    if (data.confidence > 80) {
      confLevelClass = 'high'
      confLevelText = '高置信度'
    } else if (data.confidence > 60) {
      confLevelClass = 'medium'
      confLevelText = '中等置信度'
    } else {
      confLevelClass = 'low'
      confLevelText = '低置信度'
    }

    this.setData({
      result: data,
      sortedProbs: sortedProbs,
      showResult: true,
      confLevelClass: confLevelClass,
      confLevelText: confLevelText
    })
  },

  // 获取概率条样式类
  getProbClass(prob) {
    if (prob > 80) return 'high'
    if (prob > 50) return 'medium'
    return 'low'
  }
})