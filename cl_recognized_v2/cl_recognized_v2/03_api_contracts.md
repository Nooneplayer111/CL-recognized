# API 契约草案 v2：可直接用于 Postman / 前后端联调

> 这是一份 **待实现的接口协议**，不是声称仓库已有这些接口。尽量保持原 `/docs` 的接口名；新增字段是为了修复现有“客户端可伪造识别结果、无法绑定游客”的问题。正式开发后把本文件作为契约测试基准。

## 0. 约定

- 后端 HTTP JSON 普通返回沿用项目 `Result<T>`：`{"code":1,"msg":null,"data":...}`；失败使用实际 HTTP 4xx/5xx 且可保留 `code=0,msg,requestId`。
- `Visitor` 与 `Staff` 的 JWT 需要区分身份类型与权限；游客登录后请求头使用 `token: <visitor JWT>`（可逐步升级到标准 `Authorization: Bearer`）。**不能使用员工 token 代替游客**。
- `requestId`（请求追踪 ID）、`sessionId`、`recognitionJobId` 含义不同，不能混用。识别结果、奖励资格由 Java 服务端写入。除了明确声明为公开的字段，不返回摄像头账户、原始登录信息、精确内部位置。
- 日期：`bizDate` 以 `Asia/Shanghai` 计算，格式 `yyyyMMdd`；`occurredAt` 等跨系统时间优先返回 ISO 8601 带时区，数据库中明确统一 UTC/上海时区策略。
- 所有对象 ID 只作索引提示；请求时仍要查当前认证用户是否有访问权限。所有 JSON 以下都为**虚构样例**，不含真实个体数据。

## 1. 游客认证（必须新增）

### POST `/visitor/auth/wechat`

请求：

```json
{"code":"wx.login 获取的一次性 code"}
```

响应 `data`：

```json
{
  "visitorId": 101,
  "token": "<visitor-jwt-produced-by-server>",
  "expiresInSeconds": 7200,
  "displayName": "游客101"
}
```

服务端向微信换取身份并维护 `visitor_user`；这里只是契约，不包括真实登录密钥。开发环境可以有独立的**仅开发环境** Mock 登录端点，不可部署到生产。

## 2. 图片识别、会话、卡片（原 `/docs` 契约增强）

### POST `/user/agent/sessions`

请求：

```json
{"scene":"VISITOR"}
```

响应 `data`：

```json
{"sessionId":5001,"scene":"VISITOR","lastEventId":0}
```

不接受客户端指定 `visitorUserId`，后端从 JWT 读取。禁止游客创建 `OPS` session。

### POST `/user/agent/sessions/{sessionId}/images`

`multipart/form-data`: `file`（图片）、`sourceType=camera|album`。建议 `202 Accepted` + 业务 `Result`：

```json
{
  "code":1,
  "data":{
    "recognitionJobId":90001,
    "sessionId":5001,
    "jobStatus":"QUEUED",
    "requestId":"req-example-1"
  }
}
```

异步 `RecognitionPipeline` 先调用模型、后写权威任务结果，不信任用户提供的 `predictedName/individualId/confidence`。图片在 Java 验证格式、大小后保存到受控存储，不把 Base64 大图发给聊天模型。

### WebSocket `/user/agent/ws?sessionId=5001&afterEventId=0`

握手使用鉴权头/受限会话票据，**JWT 不放 URL 查询字符串**；每次订阅、断线重放都校验会话属于当前游客。事件按 `eventId` 严格递增，数据库 `agent_event` 先提交后推。客户端需以 eventId 去重。

事件样例（按时间顺序）：

```json
{"eventId":1,"sessionId":5001,"type":"stage","payload":{"stage":"species","status":"running"}}
{"eventId":2,"sessionId":5001,"type":"species","payload":{"speciesCode":"elephant","speciesName":"亚洲象","modelScore":91.2,"needConfirm":false}}
{"eventId":3,"sessionId":5001,"type":"stage","payload":{"stage":"individual","status":"running"}}
{"eventId":4,"sessionId":5001,"type":"individual","payload":{"recognitionJobId":90001,"status":"recognized","individualId":17,"individualCode":"ELE-003","modelScore":87.1}}
{"eventId":5,"sessionId":5001,"type":"profile","payload":{"animalCard":{"individualId":17,"individualCode":"ELE-003","name":"演示大象 A","speciesCode":"elephant","speciesName":"亚洲象","avatarUrl":null,"ageDisplay":"年龄暂未核实","ageSource":null,"gender":"unknown","status":"active","description":"来自数据库的演示描述","lastVerifiedAt":null}}}
{"eventId":6,"sessionId":5001,"type":"done","payload":{"recognitionJobId":90001,"status":"recognized","eligibleForCheckin":true}}
```

**重要**：`modelScore=87.1` 只能称为模型评分，除非已校准，否则不要展示成“有 87.1% 概率是它”。用户可见的 `eligibleForCheckin` 来自服务端验证逻辑。

低置信度/不可识别例：

```json
{"eventId":7,"sessionId":5001,"type":"species","payload":{"speciesCode":null,"needConfirm":true,"candidates":[{"speciesCode":"elephant","modelScore":60.2}]}}
{"eventId":8,"sessionId":5001,"type":"done","payload":{"recognitionJobId":90002,"status":"uncertain","eligibleForCheckin":false}}
```

### POST `/user/agent/sessions/{sessionId}/species-confirm`

```json
{"recognitionJobId":90002,"speciesCode":"elephant"}
```

Java 校验该 `speciesCode` 在该任务**原模型候选集**中，并且用户对任务有权限；人工选择后的结果标识为 `USER_CONFIRMED`，默认不允许自动领奖，业务另行审核可改变资格。

### GET `/user/agent/sessions/{sessionId}/recognitions/{jobId}`

这是推荐新增的**补偿读取接口**，使 WebSocket 断线或消息丢失时也能查最终结果。

```json
{
  "code":1,
  "data":{
    "recognitionJobId":90001,
    "status":"recognized",
    "speciesId":1,
    "individualId":17,
    "animalCard":{"individualId":17,"name":"演示大象 A","avatarUrl":null,"ageDisplay":"年龄暂未核实"},
    "eligibleForCheckin":true
  }
}
```

正式 API 不需要返回不允许公开的模型原始特征或内部候选 URL。

## 3. 问答 Agent 和可选 RAG

### POST `/user/agent/sessions/{sessionId}/messages`

```json
{"messageId":"mp-uuid-1","text":"这只象以前有什么观察记录？"}
```

响应立即：

```json
{"code":1,"data":{"messageId":"server-msg-201","accepted":true}}
```

在 WebSocket 里推：

```json
{"eventId":9,"sessionId":5001,"type":"tool","payload":{"label":"正在查询已审核的档案资料"}}
{"eventId":10,"sessionId":5001,"type":"token","payload":{"messageId":"server-msg-201","delta":"目前"}}
{"eventId":11,"sessionId":5001,"type":"token","payload":{"messageId":"server-msg-201","delta":"档案显示……"}}
{"eventId":12,"sessionId":5001,"type":"done","payload":{"messageId":"server-msg-201","sources":[{"sourceId":"doc-12","title":"已审核个体观察记录"}]}}
```

`tool` 只返回“正在查档案”等 UI 状态，不向游客输出工具参数原始 SQL、内部账号资料。若没有长文本资料，直接用 SQL tools 回答，`sources` 指向数据库权威档案来源，不必装作 RAG 命中了什么。

**建议的访客工具接口（内部 Java 方法，不对外裸开放）**：`getCurrentIndividual()`、`searchIndividualByName(name)`、`getSpeciesBrief(speciesCode)`、`getRecentRecognition()`，有长文本后加 `searchApprovedKnowledge(query)`。`getCurrentIndividual` 的 ID 由**服务端会话上下文**指定，不由 LLM 自由填入。

## 4. 每日物种打卡（基于可信 recognitionJobId）

### POST `/user/checkins`

```json
{"recognitionJobId":90001}
```

响应：

```json
{
  "code":1,
  "data":{
    "speciesId":1,
    "bizDate":"20261106",
    "firstCheckin":true,
    "rank":3,
    "pointsAwarded":20,
    "pointsBalance":120,
    "checkinAt":"2026-11-06T10:35:19+08:00"
  }
}
```

重复当日同物种打卡应返回原排名，`firstCheckin=false`、`pointsAwarded=0`，但 `rank=3` 仍保持。客户端不得自选 `speciesId` 来跳过可信识别；物种由 Java 从 `recognition_job` 读取。

### GET `/user/checkins/rank?speciesId=1&date=20261106`

```json
{
  "code":1,
  "data":{
    "speciesId":1,
    "speciesName":"亚洲象",
    "bizDate":"20261106",
    "total":3,
    "myRank":3,
    "list":[
      {"rank":1,"visitorId":103,"displayName":"游客103","checkinAt":"2026-11-06T08:00:00+08:00","pointsAwarded":50},
      {"rank":2,"visitorId":104,"displayName":"游客104","checkinAt":"2026-11-06T09:00:00+08:00","pointsAwarded":30},
      {"rank":3,"visitorId":101,"displayName":"游客101","checkinAt":"2026-11-06T10:35:19+08:00","pointsAwarded":20}
    ]
  }
}
```

### GET `/user/points`

`data`: `{"points":120}`，直接从 `point_wallet` 获取，不依赖 Redis 排名。

## 5. 当日物种 Feed（只能当天打过卡的人进入）

### POST `/user/feeds/{speciesId}/comments`

```json
{"content":"今天看到了演示大象 A，观察到它在活动。"}
```

`data`：`{"id":301,"speciesId":1,"bizDate":"20261106","status":"PENDING","content":"..."}`；如采用先审后发，作者可看到自己“待审核”，其他用户不能看到。`PUBLISHED` 后才进入公共列表。

### GET `/user/feeds/{speciesId}/comments?cursor=<opaque>&size=10`

```json
{
  "code":1,
  "data":{
    "list":[
      {"id":301,"displayName":"游客101","content":"示例内容","createdAt":"2026-11-06T11:00:00.123+08:00"}
    ],
    "nextCursor":"<base64url-encoding-of-createdAt-and-id>",
    "hasMore":false
  }
}
```

必须满足 `(create_time,id)` 双字段顺序，且对首页、第二页、最后一页、同毫秒多条数据一致。`cursor` 不能仅保存毫秒数。跨日 Feed 不混读，未打卡者请求应返回 403/业务代码 `CHECKIN_REQUIRED`，不返回空列表伪装没有人发言。

## 6. 商城与订单

### GET `/user/point-goods?type=SPECIAL`

`data`: `{"list":[{"id":3,"name":"演示纪念贴纸","pointsPrice":80,"stock":5,"goodsType":"SPECIAL","status":"ON","limitPerUser":1,"saleStart":null,"saleEnd":null}]}`。

### POST `/user/point-orders`

```json
{"goodsId":3,"requestId":"client-generated-idempotency-uuid"}
```

`data`: `{"orderNo":"PO-DEMO-001","status":"SUCCESS","pointsCost":80,"pointsBalance":40}`。

服务端**忽略客户端传来的价格与 userId**；同时校验库存、限购、余额、时间窗。`(visitor_id,requestId)` 唯一。对相同 requestId 的重试查询并返回原订单，不能双扣。

### GET `/user/point-orders?cursor=<opaque>&size=10`

返回同样的双字段 Keyset 游标，用户只能看到自己订单。

员工管理 API 可沿用原文档：`POST /point-goods`、`PUT /point-goods/{id}`、`GET /point-goods`、`GET /point-orders`，但必须使用员工权限。

## 7. 管理端异常（外部算法有真实输出后启用）

`POST /device/abnormal-events`：设备凭据 + 媒体 + JSON；设备的 `deviceEventId` 全局唯一用于幂等，设备 token 与员工 JWT/游客 JWT 分开。`GET /abnormal-events` / `POST /abnormal-events/{id}/handle` / `GET /abnormal-events/stream` 仅员工可用。`OPS` Agent 的工具清单不与游客混用，输出中不泄露摄像头密码。原文档“长停留 600s”只能当待验证业务告警规则，不能宣称已完成医学诊断。

## 8. 统一错误码建议

| code | HTTP | 说明 |
| --- | --- | --- |
| `NOT_AUTHENTICATED` | 401 | 缺少或失效 Token |
| `FORBIDDEN` | 403 | 无权访问其他游客会话 / 员工功能 |
| `SESSION_NOT_FOUND` | 404 | 会话不存在或不可见 |
| `RECOGNITION_NOT_TRUSTED` | 422 | 识别结果不符合打卡资格 |
| `MODEL_UNAVAILABLE` | 503 | Python 模型超时/不可用 |
| `CHECKIN_REQUIRED` | 403 | 今日还没对该物种完成打卡 |
| `INSUFFICIENT_POINTS` | 409 | 积分余额不足 |
| `OUT_OF_STOCK` | 409 | 兑换商品无库存 |
| `LIMIT_REACHED` | 409 | 超出该用户限购次数 |
| `RETRY_LATER` | 429/503 | 限流或抢兑拥堵，请稍后重试 |

## 9. 联调顺序

1. 服务端用 Mock 模型演示 `POST image -> stage/species/individual/profile/done`，小程序断开/重连补放没有重复卡片。
2. 用老大象 FastAPI 实测字段适配与超时处理；准确保存 `recognition_job` 和 `recognition_record`。
3. 导入测试游客登录 -> 打卡重复 -> 余额变化 -> 跨物种当天讨论资格。
4. 商城使用独立 requestId、多用户并发与同人并发分别验证库存/限购/重复单。
5. 百炼 Tool Calling 只读查询成功后，再开放 RAG 检索；始终先通过卡片、账本 API 的纯数据库测试。
