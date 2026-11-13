# 没有模型/服务器时也能开始联调

此目录给出**虚构**的 WebSocket 事件、REST 卡片补偿查询、打卡与商城回包，适合前端先做页面状态机。Mock 中的 animal ID、modelScore、points 等都是演示值，不是已有系统准确率、实际动物信息、实际积分余额。

运行本地 JSON/契约基础检查（无需依赖或网络）：

```bash
python test_mock_contracts.py
```

真实对接时请以 `03_api_contracts.md` 的协议和 Java 后端返回为准，不可通过 Mock 中的 `eligibleForCheckin` 或 `awardedPoints` 字段由客户端直接给用户发奖。小程序只负责展示和提交**可信 recognitionJobId**；是否符合签到资格由服务端决定。需要测试低置信度、失败、未知、重复打卡、超时和断线的事件，可从当前文件复制一个额外 fixture，并保持 `sessionId`、`recognitionJobId`、`eventId` 的绑定一致。
