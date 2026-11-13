# 源码审查差异与风险清单（2026-11-06）

本清单基于用户上传的 `elephantJava (2).zip`。定位路径以压缩包外层 `elephantJava/` 为根；所有引用的行号来自本次解压的文件版本，后续代码变更后会移动。**状态分类**：`已存在` 表示看见实现，`缺失` 表示当前压缩包未找到，`计划中` 表示只见于原始 docs，`待验证` 表示缺少外部环境/数据。没有把文档中的设计当作已经写好的代码。

## A. 已确认技术栈与功能

| 对象 | 已核查位置 | 确认情况 |
| --- | --- | --- |
| 后端构建 | `elephantJava/pom.xml` | Spring Boot `4.0.6`、Java `17`、MyBatis-Plus `3.5.16`、MySQL 驱动；尚无 Redis、LangChain4j、WebSocket Starter 依赖 |
| 个体档案 | `entity/Individual.java:15–45` | `id/name/code/species_id/species_name/gender/age/description/status`；无 `avatar_url`、出生日期/年龄评估来源 |
| 物种档案 | `entity/Species.java:15–34` | `name/code/description/image_url`；该 `image_url` 属于物种不是个体头像 |
| 识别写入 | `controller/OpenRecognitionController.java:18–34` | `/open/recognition-records` 匿名 POST 后调用 `saveRecord` |
| 旧姓名关联 | `service/impl/RecognitionRecordServiceImpl.java:135–136,179–188` | 用 `predictedName` 全库查 `Individual.name`，`LIMIT 1`，无物种约束 |
| 当前小程序识别 | `miniprogram/pages/elephant/elephant.js:153–197,305–355` | 客户端 Base64 请求 Python `/predict`，再用客户端提供的数据向开放接口写识别记录 |
| 识别结果 UI | `miniprogram/pages/result/result.js:18–67` | 从本地存储获取 `elephant_name/confidence/all_probs`，只展示预测结果和概率排名 |
| 物种入口 | `miniprogram/pages/species-select/species-select.js:3–51` | 只有大象 `available: true`，其余为“敬请期待” |
| 打卡页面 | `miniprogram/pages/checkin/checkin.js:1–3` | 空页面，尚无真实打卡逻辑 |
| 小程序授权 | `miniprogram/**` 与 `app.js` | 未发现 `wx.login`/游客账户建立或请求 token；不能把员工登录视为游客登录 |
| 员工登录 | `controller/UserController.java:38–56`、`service/impl/UserServiceImpl.java:35–53` | 账号密码查询员工 `user`，签发 JWT；密码使用简单 MD5 比对 |
| 鉴权范围 | `config/WebMvcConfiguration.java:32–38` | 放行 `/user/login` 和 `/open/**`，其他接口使用 JWT 拦截器 |
| 身份上下文 | `interceptor/JwtTokenUserInterceptor.java:53–81`、`contex/BaseContext.java` | 通过 ThreadLocal 存储 Long ID，未看到 `afterCompletion` 清理调用，也未区分游客/员工主体 |
| Vue 管理端 | `elephantVue/package.json`、`elephantVue/src/router/index.js` | Vue 3/Vite/Element Plus，现有 Species/Individual/Camera/RecognitionRecord/Profile 页面，无商城/告警页面 |
| 摄像头管理 | `controller/CameraController.java`、`service/impl/CameraServiceImpl.java` | 仅 CRUD/预览地址；返回 VO 的密码用 `******` 脱敏，但 Controller 日志会打印请求 DTO |
| 原要求：AI | `docs/小程序与管理端Agent接入设计.md` | **计划中**：两阶段视觉、WS、Tool Calling、管理端异常和 SSE；明确不需要在卡片阶段使用 RAG |
| 原要求：打卡 | `docs/打卡讨论与积分商城实现方案.md` | **计划中**：Redis 打卡/排行、同物种当日讨论、积分商城、Redisson 特价锁 |

## B. 风险分级、修正建议与验收依据

| 编号 | 优先级 | 问题及证据 | 修正方式 | 最小验收 |
| --- | --- | --- | --- | --- |
| S01 | **P0** | 匿名 `/open/recognition-records` 接受客户端 `predictedName/confidence`，不验证模型结果；识别记录可能被构造 | 由 Java 服务端执行模型请求并形成 `recognition_job`；旧匿名接口只保留不发分的兼容记录 | 用 curl 自造高置信度 JSON，不能得到任何积分 |
| S02 | **P0** | 小程序缺游客认证，`/user/login` 是员工入口 | `visitor_user` + `wx.login` 后端换 openid + 独立 Visitor JWT；员工与游客在接口过滤器层隔离 | 游客令牌无法访问员工接口；员工令牌不可冒充游客签到 |
| S03 | **P0** | `UserController.java:40` 打印整个登录 DTO；`JwtTokenUserInterceptor.java:70` 记录原始 JWT；`CameraController` 打印含密码 DTO | 删除/脱敏这些日志；增加敏感字段日志单测/静态扫描 | 日志无明文密码、JWT、摄像头秘密 |
| S04 | **P0** | `BaseContext` 设置 ThreadLocal 后未看到请求结束删除，异步线程不能天然继承正确主体 | `afterCompletion` 清理；异步任务明确传入身份快照而非 ThreadLocal；WebSocket 握手独立校验 | 用线程复用测试不存在前一请求身份残留 |
| S05 | **P0** | `application.yaml` 存在已配置的 DB/JWT 敏感项（数值未复印），ZIP 同时包含 `.git`、私有工程设置 | 从源码外部读取凭据；公开/分享前检查 Git 历史并轮换可能暴露的密钥，排除 `.git/node_modules/target/.idea/project.private.config.json` | 清洁代码包的凭据扫描通过 |
| S06 | **P0** | 文档欲在现有员工 `user.points` 上记游客积分，但用户表目前是员工账号表 | 新建 `visitor_user` + `point_wallet` + `point_log`；账本不依赖员工用户表 | 游客和员工主键相同时不串账 |
| S07 | **P1** | 全局 `name LIMIT 1` 匹配动物有跨物种串身份风险 | 新模型用 `individual.code+species_id`；旧模型 fallback 按 `name+species_id` 且必须唯一 | 建立两个同名动物测试返回正确目标或不确定 |
| S08 | **P1** | `individual` 有 `age INT`，但无 `age_as_of`、`age_source`、`avatar_url` | 扩表并对无来源字段输出“待核实”；物种 `image_url` 只能用于物种图 | 年龄/头像空值时 UI 和问答正确降级 |
| S09 | **P1** | 文档 `model_endpoint` 使用 `UNIQUE(task, species_code)`，而 SPECIES 模型 `species_code=NULL` | 改成 `routing_key NOT NULL`，如 `all`、`elephant`；或使用单独固定键 | 两条 `(SPECIES,all)` 不可能同时插入 |
| S10 | **P1** | 文档给的 75/40/10 阈值只是经验规则，没有验证集支持 | 阈值可配置，按物种与模型版本记录；提供 unknown 和 uncertain | 验证集上实测结果/误判率有记录 |
| S11 | **P1** | 如果先用 Redis Lua 登记、再 MySQL 发积分，Redis 成功 + DB 失败造成账本不一致 | 推荐 MySQL 打卡/余额同事务，Redis 投影；如保留 Lua-first 必须 outbox/补偿和对账 | 模拟 DB/Redis 故障可恢复、不会重复发积分 |
| S12 | **P1** | 原 Lua 只检查第一个 SET 的 TTL 再统一过期；若部分 key 丢失并重建，ZSET 等可能无 TTL | 每个 Key 都单独确认、设置/修复 `EXPIREAT`；或 Redis 只做可重建缓存 | 删除部分 Key 后重建，过期时刻仍正确 |
| S13 | **P1** | 原 Feed Redis 只以毫秒分数做 `cursor`；同一毫秒多条会在下一页被跳过 | **权威 SQL 双字段游标 `(create_time,id)`**；Redis 只做加速缓存 | 同毫秒 3 条评论、size=1，三页全取到 |
| S14 | **P1** | 商城只查每个用户订单数不能防并发超限（尤其 NORMAL 不加 Redisson） | `point_goods_quota` 唯一行 + 条件更新，和余额库存同一个 MySQL 事务；锁只做可选优化 | 同人并发不同 requestId 仍不超过 `limit_per_user` |
| S15 | **P1** | `requestId` 先查后写仍有竞态 | 数据库唯一键兜底，捕获并发唯一冲突后回查原订单 | 同一 requestId 50 个并发仅有一笔订单 |
| S16 | **P1** | `MediaStorageUtil.java:27–51` Base64 解码后直接保存，缺少解码后大小/格式/授权保护；上传 Controller 主要看声明 MIME | 文件大小、魔数/实际解码、格式白名单、限流；识别原图和公开头像分储 | 伪装图片、超大 Base64、恶意内容被拒绝 |
| S17 | **P1** | `model_endpoint.endpoint_url` 可配置模型 URL，若供不可信用户操控可能成为 SSRF 入口 | 仅有权限的员工配置，host/scheme/端口 allowlist，不允许任意外网地址；加超时与熔断 | 不能请求内网管理地址或云元数据地址 |
| S18 | **P1** | `individual` 的唯一编号目前在 Service 中先 count 再 insert，不能完全避免并发插入重复编码 | 根据业务主键策略增加 DB 唯一索引（全局 code 或 `(species_id,code)`） | 同时写同 code 只能成功一条 |
| S19 | **P2** | `UserServiceImpl.java:47–50,111–116` 的密码 MD5 无盐，不适合长线部署 | 迁移带盐强哈希 BCrypt/Argon2；过渡时登录成功触发逐步哈希升级 | 新密码不以 MD5 存储、旧账号可迁移 |
| S20 | **P2** | 小程序 `elephant.wxml` 混用 `:class` 等 Vue 风格绑定，且直接在模板调用 `toFixed()` | 清理为 WXML 兼容绑定、JS 中预格式化属性 | 微信开发者工具无 WXML 警告，页面无空白 |
| S21 | **P2** | 仅有 Java `contextLoads` 测试，缺少识别逻辑、账本、并发、UI 回归 | 增 JUnit、MockWebServer、MySQL/Redis 集成测试、微信真机测试 | 每一阶段有可复现验收报告 |
| S22 | **P2** | 异常行为服务仅在文档与专利方案中描述，上传包未见设备侧模型代码 | 按模拟 JSON 开发接收/分级，待真实设备到位再联调；不虚报已实现模型 | 可区分 Mock 与真实来源及准确度指标 |

## C. 文档之间/文档与代码之间的冲突

1. **原 Agent 设计：当前阶段不加 Chroma**；用户的新诉求：希望 RAG/Chroma。处理：保留 Tool Calling 作为基础，把 Chroma 定位为**审核的长文知识增强**，不要反过来强制将每个短结构字段只放向量库。
2. **原打卡设计：先使用匿名识别记录 + `user.points`**；现有 `user` 是员工账号表，小程序没有登录。处理：先上游客账号与可信 `recognition_job`；改用游客 `point_wallet`。
3. **原注册表：两个 `SPECIES,NULL` 可以同时存在**（MySQL 唯一键对 NULL 的处理）；处理：非空 `routing_key`。
4. **原榜单：Redis Lua 成功不等于 MySQL 积分成功**；处理：同库事务确定排名/积分，Redis 作为可重建投影；如老师指定 Lua，则配可恢复对账。
5. **原 Feed：同一毫秒时 Redis score cursor 与 SQL 双游标行为不一致**；处理：SQL 双字段统一为真实分页语义。
6. **原商城：特价有锁，普通无锁，但都要满足限购**；处理：限购使用数据库条件原子更新，覆盖 NORMAL 与 SPECIAL。
7. **原管理端：期待边缘行为模型真实上线**；实际 ZIP 没有；处理：列为条件性 P7，不阻断游客 MVP。

## D. 仍需实际环境验证，不应凭源码确定

- Python `predict` 对真实图片的输出稳定性、0–100 置信度含义、个体类别是否与 `individual.name` 一一对应；ZIP 没有 Python 模型源码和真实数据。
- MySQL 当前表结构和历史数据是否与 `recognition_record.sql` 一致；目前只能看到一个建表脚本，不能假设所有旧表已有唯一键。
- 微信小程序真机是否已配置合法 HTTPS/WSS 域名，能否完成 `wx.login` 及 WebSocket 鉴权。
- 完整 Maven/Node 编译、启动、Redis/MySQL 实战测试；未连接这些环境，不能声称端到端通过。
- AI 账号可用模型 ID、地域、配额与调用费用；以实际百炼控制台配置为准。

## E. 最先提交的三个 Pull Request

1. `security/visitor-auth-and-trusted-recognition`：修日志/ThreadLocal、独立游客身份、可信识别凭证（先 DB Mock）。
2. `feature/animal-card`：Java 旧模型适配、编号匹配、头像与年龄源、新增卡片 API、更新小程序结果页。
3. `feature/visitor-ai-tools`：四个安全只读工具、Agent 会话、百炼服务端代理和回归问答；可选 RAG 后续另开 PR。

PR 检查：每个要有数据库迁移说明、Mock 测试、异常路径、截图和回滚方式；不要提交 `.git`、`node_modules`、`target`、私密配置或任何真实密钥。
