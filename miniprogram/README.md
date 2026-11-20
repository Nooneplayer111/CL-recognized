# 大象识别微信小程序

## 项目结构

```
miniprogram/
├── app.js          # 小程序入口文件
├── app.json        # 小程序配置文件
├── app.wxss        # 全局样式
└── pages/
    └── index/
        ├── index.js    # 页面逻辑
        ├── index.json  # 页面配置
        ├── index.wxml  # 页面结构
        └── index.wxss  # 页面样式
```

## 开发环境

### 1. 安装微信开发者工具

下载地址：https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html

### 2. 配置开发环境

1. 打开微信开发者工具
2. 选择"小程序" → "导入项目"
3. 选择 `miniprogram` 文件夹作为项目目录
4. 填写AppID（如需发布，请在微信公众平台申请）

## 配置说明

### 修改API地址

在 `app.js` 中修改后端API地址：

```javascript
globalData: {
    apiUrl: 'http://localhost:8000',  // 本地开发环境
    // apiUrl: 'https://your-server.com',  // 生产环境
    elephantClasses: []
}
```

### 注意事项

1. **开发阶段**：使用 `http://localhost:8000` 作为API地址
2. **预览测试**：需要将后端服务部署到公网，或使用内网穿透工具
3. **发布上线**：需要将API部署到HTTPS服务器

## 功能特点

- ✅ 拍照识别：使用手机摄像头拍照进行大象识别
- ✅ 相册选择：从相册选择图片进行识别
- ✅ 实时显示：显示识别结果和置信度
- ✅ 概率分布：展示所有类别的概率分布

## 调试说明

### 1. 启动后端服务

在 `best_elephant_model` 目录下运行：

```bash
# Windows
start_api.bat

# macOS/Linux
./start_api.sh
```

### 2. 启动小程序调试

1. 确保后端服务正在运行
2. 在微信开发者工具中打开项目
3. 点击"编译"按钮开始调试

### 3. 常见问题

**Q: 无法连接到后端？**
- 检查后端服务是否启动
- 检查API地址是否正确
- 确保没有防火墙阻止连接

**Q: 拍照权限被拒绝？**
- 在手机设置中允许小程序访问相机
- 确保在 `app.json` 中配置了相机权限

**Q: 预览时无法访问本地服务？**
- 使用内网穿透工具（如 ngrok）
- 将后端部署到公网服务器

## 发布流程

1. 在微信公众平台注册小程序账号
2. 获取AppID并配置到项目中
3. 将后端API部署到HTTPS服务器
4. 在开发者工具中上传代码
5. 提交审核并发布

## 技术栈

- 框架：微信小程序原生框架
- 语言：JavaScript
- 样式：WXSS（类似CSS）
- 后端：FastAPI + Python

## 联系信息

如有问题，请联系开发者。