# 物种个体识别系统 - 前端接口文档

## 基础信息

- **API 基础路径**: `/api`
- **后端服务地址**: `http://localhost:8080` (开发环境通过 Vite 代理转发)
- **内容类型**: `application/json`
- **认证方式**: Token 认证，登录成功后将 token 存储在 localStorage

---

## 一、登录认证

### 1.1 用户登录

**前端代码位置**: [Login.vue](src/views/Login.vue#L85-L100)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/login` |
| **Method** | POST |
| **Content-Type** | `application/json` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**请求示例**:
```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin"
  }
}
```

---

## 二、物种管理

### 2.1 获取物种列表

**前端代码位置**: [Species.vue](src/views/Species.vue#L85-L114)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/species` |
| **Method** | GET |

**响应示例**:
```json
[
  {
    "id": 1,
    "name": "亚洲象",
    "code": "ASIAN_ELE",
    "description": "亚洲象是亚洲现存的最大陆生动物",
    "imageUrl": "http://example.com/images/asian_elephant.jpg"
  },
  {
    "id": 2,
    "name": "非洲象",
    "code": "AFRICAN_ELE",
    "description": "非洲象是陆地上最大的哺乳动物",
    "imageUrl": "http://example.com/images/african_elephant.jpg"
  }
]
```

### 2.2 添加物种

**前端代码位置**: [Species.vue](src/views/Species.vue#L142-L170)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/species` |
| **Method** | POST |
| **Content-Type** | `application/json` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 物种名称 |
| code | String | 是 | 物种编码（唯一标识） |
| description | String | 否 | 物种描述 |
| imageUrl | String | 否 | 物种图片URL |

**请求示例**:
```json
{
  "name": "金丝猴",
  "code": "GOLDEN_MONKEY",
  "description": "金丝猴是中国特有的珍稀动物",
  "imageUrl": ""
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "添加成功",
  "data": {
    "id": 3,
    "name": "金丝猴",
    "code": "GOLDEN_MONKEY",
    "description": "金丝猴是中国特有的珍稀动物",
    "imageUrl": ""
  }
}
```

### 2.3 更新物种

**前端代码位置**: [Species.vue](src/views/Species.vue#L142-L170)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/species/{id}` |
| **Method** | PUT |
| **Content-Type** | `application/json` |

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 物种ID |

**请求体**: 同添加物种

### 2.4 删除物种

**前端代码位置**: [Species.vue](src/views/Species.vue#L126-L140)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/species/{id}` |
| **Method** | DELETE |

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 物种ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

### 2.5 上传图片

**前端代码位置**: [Species.vue](src/views/Species.vue#L43-L49)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/upload` |
| **Method** | POST |
| **Content-Type** | `multipart/form-data` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | File | 是 | 图片文件 |

**响应示例**:
```json
{
  "code": 200,
  "message": "上传成功",
  "url": "http://example.com/images/uploaded_xxx.jpg"
}
```

---

## 三、个体管理

### 3.1 获取个体列表（分页）

**前端代码位置**: [Individual.vue](src/views/Individual.vue#L222-L244)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/individuals` |
| **Method** | GET |

**查询参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 是 | 页码（从0或1开始，根据后端实现） |
| size | Integer | 是 | 每页条数 |
| keyword | String | 否 | 搜索关键词（匹配名称或编号） |
| speciesId | Long | 否 | 物种ID（筛选条件） |

**响应示例**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "小象甲",
      "code": "IND001",
      "speciesId": 1,
      "speciesName": "亚洲象",
      "gender": "male",
      "age": 5,
      "status": "active",
      "description": "性格温顺",
      "createTime": "2024-01-15"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

### 3.2 添加个体

**前端代码位置**: [Individual.vue](src/views/Individual.vue#L293-L324)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/individuals` |
| **Method** | POST |
| **Content-Type** | `application/json` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 个体名称 |
| code | String | 是 | 个体编号（唯一标识） |
| speciesId | Long | 是 | 所属物种ID |
| gender | String | 是 | 性别（male/female） |
| age | Integer | 否 | 年龄 |
| description | String | 否 | 描述信息 |
| status | String | 否 | 状态（active/inactive），默认active |

**请求示例**:
```json
{
  "name": "幼象丙",
  "code": "IND003",
  "speciesId": 2,
  "gender": "male",
  "age": 2,
  "description": "活泼好动",
  "status": "active"
}
```

### 3.3 更新个体

**前端代码位置**: [Individual.vue](src/views/Individual.vue#L293-L324)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/individuals/{id}` |
| **Method** | PUT |
| **Content-Type** | `application/json` |

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 个体ID |

**请求体**: 同添加个体

### 3.4 删除个体

**前端代码位置**: [Individual.vue](src/views/Individual.vue#L277-L291)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/individuals/{id}` |
| **Method** | DELETE |

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 个体ID |

### 3.5 获取物种列表（用于下拉框）

**前端代码位置**: [Individual.vue](src/views/Individual.vue#L209-L220)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/species` |
| **Method** | GET |

**响应示例**:
```json
[
  {
    "id": 1,
    "name": "亚洲象"
  },
  {
    "id": 2,
    "name": "非洲象"
  }
]
```

---

## 四、摄像头管理

### 4.1 获取摄像头列表

**前端代码位置**: [Camera.vue](src/views/Camera.vue#L95-L139)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/cameras` |
| **Method** | GET |

**响应示例**:
```json
[
  {
    "id": 1,
    "name": "监控点A-东门",
    "ipAddress": "192.168.1.101",
    "location": "园区东门入口",
    "port": 8080,
    "username": "admin",
    "password": "******",
    "status": "online",
    "previewUrl": "http://192.168.1.101:8080/stream"
  }
]
```

### 4.2 添加摄像头

**前端代码位置**: [Camera.vue](src/views/Camera.vue#L163-L192)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/cameras` |
| **Method** | POST |
| **Content-Type** | `application/json` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 摄像头名称 |
| ipAddress | String | 是 | IP地址 |
| location | String | 是 | 安装位置 |
| port | Integer | 否 | 端口，默认8080 |
| username | String | 否 | 登录用户名 |
| password | String | 否 | 登录密码 |

**请求示例**:
```json
{
  "name": "监控点E-南门",
  "ipAddress": "192.168.1.105",
  "location": "园区南门入口",
  "port": 8080,
  "username": "admin",
  "password": "123456"
}
```

### 4.3 更新摄像头

**前端代码位置**: [Camera.vue](src/views/Camera.vue#L163-L192)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/cameras/{id}` |
| **Method** | PUT |
| **Content-Type** | `application/json` |

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 摄像头ID |

**请求体**: 同添加摄像头

### 4.4 删除摄像头

**前端代码位置**: [Camera.vue](src/views/Camera.vue#L147-L161)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/cameras/{id}` |
| **Method** | DELETE |

**路径参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 摄像头ID |

---

## 五、用户管理

### 5.1 获取用户信息

**前端代码位置**: [Profile.vue](src/views/Profile.vue#L149-L155)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/user/info` |
| **Method** | GET |
| **Authorization** | Bearer Token |

**响应示例**:
```json
{
  "username": "admin",
  "realName": "管理员",
  "email": "admin@example.com",
  "phone": "13800138000",
  "department": "技术部",
  "joinDate": "2024-01-01"
}
```

### 5.2 更新用户信息

**前端代码位置**: [Profile.vue](src/views/Profile.vue#L157-L164)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/user/info` |
| **Method** | PUT |
| **Content-Type** | `application/json` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（不可修改，仅用于标识） |
| realName | String | 否 | 真实姓名 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| department | String | 否 | 部门 |
| joinDate | String | 否 | 入职时间（yyyy-MM-dd） |

### 5.3 修改密码

**前端代码位置**: [Profile.vue](src/views/Profile.vue#L166-L177)

| 属性 | 值 |
|------|-----|
| **URL** | `/api/user/changePassword` |
| **Method** | POST |
| **Content-Type** | `application/json` |

**请求体**:

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码（至少6位） |
| confirmPassword | String | 是 | 确认新密码 |

**请求示例**:
```json
{
  "oldPassword": "123456",
  "newPassword": "654321",
  "confirmPassword": "654321"
}
```

---

## 六、数据模型定义

### 6.1 物种 (Species)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 主键ID |
| name | String | 物种名称 |
| code | String | 物种编码（唯一） |
| description | String | 物种描述 |
| imageUrl | String | 图片URL |

### 6.2 个体 (Individual)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 主键ID |
| name | String | 个体名称 |
| code | String | 个体编号（唯一） |
| speciesId | Long | 所属物种ID |
| speciesName | String | 所属物种名称（冗余字段，便于展示） |
| gender | String | 性别（male/female） |
| age | Integer | 年龄 |
| description | String | 描述信息 |
| status | String | 状态（active/inactive） |
| createTime | String | 创建时间（yyyy-MM-dd） |

### 6.3 摄像头 (Camera)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 主键ID |
| name | String | 摄像头名称 |
| ipAddress | String | IP地址 |
| location | String | 安装位置 |
| port | Integer | 端口 |
| username | String | 登录用户名 |
| password | String | 登录密码 |
| status | String | 在线状态（online/offline） |
| previewUrl | String | 预览画面URL |

### 6.4 用户 (User)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| username | String | 用户名（唯一） |
| realName | String | 真实姓名 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| department | String | 部门 |
| joinDate | String | 入职时间 |

---

## 七、统一响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 失败响应

```json
{
  "code": 500,
  "message": "操作失败，原因描述",
  "data": null
}
```

### 分页响应

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "number": 0,
  "size": 10
}
```

---

## 八、接口调用流程图

```
┌─────────────┐     POST /api/login     ┌─────────────┐
│   登录页面   │ ──────────────────────> │   后端服务   │
│   Login.vue │ <────────────────────── │             │
└─────────────┘     返回token           └─────────────┘
         │
         ▼ (存储token到localStorage)
┌─────────────┐     GET /api/species    ┌─────────────┐
│   物种管理   │ ──────────────────────> │   后端服务   │
│  Species.vue│ <────────────────────── │             │
└─────────────┘     返回物种列表        └─────────────┘
         │
         ▼
┌─────────────┐     GET /api/individuals ┌─────────────┐
│   个体管理   │ ───────────────────────> │   后端服务   │
│ Individual.vue│ <───────────────────── │             │
└─────────────┘     返回个体分页数据     └─────────────┘
         │
         ▼
┌─────────────┐     GET /api/cameras    ┌─────────────┐
│ 摄像头管理   │ ──────────────────────> │   后端服务   │
│  Camera.vue │ <────────────────────── │             │
└─────────────┘     返回摄像头列表      └─────────────┘
         │
         ▼
┌─────────────┐     GET /api/user/info ┌─────────────┐
│   个人中心   │ ──────────────────────> │   后端服务   │
│  Profile.vue│ <────────────────────── │             │
└─────────────┘     返回用户信息        └─────────────┘
```

---

## 九、注意事项

1. **认证**: 所有接口（除登录外）需要在请求头携带 `Authorization: Bearer <token>`
2. **跨域**: 后端需要配置 CORS 支持前端域名
3. **文件上传**: 图片上传接口使用 `multipart/form-data` 格式
4. **分页**: 前端分页参数从1开始，后端需根据实际实现调整
5. **错误处理**: 前端已实现请求失败时使用本地 mock 数据兜底，后端接口就绪后可移除 mock 数据
