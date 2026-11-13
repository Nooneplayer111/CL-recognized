# 独立实验：百炼 + Chroma 文本 RAG（可选，不是仓库集成）

> 本目录完全使用虚构动物个体与虚构观察记录，仅用于说明如何构建文字知识索引、在检索时限定个体 ID、返回来源。**它不能识别动物图片，也不能调用 CL-recognized 当前 MySQL、微信登录或真实业务 API**。不要把这里的 `FAKE_ANIMALS` 当成真实档案。

## 为什么它排在动物卡片与只读工具之后？

个体 ID/头像/年龄/积分余额均是结构化权威数据，必须直接从 Java 业务服务/数据库获得。Chroma 只负责长文本知识，如经审核的观察笔记、行为描述与合法可使用的科普。Agent 的工具先查询 MySQL，遇到知识类问题再检索 Chroma。

## 安装（本机 Python 3.10+；建议独立虚拟环境）

```bash
python -m venv .venv
# Windows PowerShell: .venv\Scripts\Activate.ps1
# Linux/macOS: source .venv/bin/activate
python -m pip install -r requirements.txt
```

执行前需在**开发机器或服务器的环境变量**中设置：

- `DASHSCOPE_API_KEY`：自己的百炼 API Key，不要贴入代码、微信群、截图或日志。
- `BAILIAN_BASE_URL`：**与 Key 地域一致**的阿里云百炼 OpenAI 兼容 API 基础 URL，以当前官方文档与账户控制台为准。
- 可选 `BAILIAN_EMBED_MODEL`，默认 `text-embedding-v4`。
- 可选 `BAILIAN_CHAT_MODEL`，默认 `qwen-plus`（需要以账户实际开放的模型为准）。

示例仅为命令格式，不提供真实 Token/URL：

```bash
python rag_poc.py --index --db-path ./chroma_data
python rag_poc.py --animal-id E001 --question '这只动物有什么可核实的特征？' --db-path ./chroma_data
```

预期输出是虚构资料的检索来源和模型文本；若调用失败，应先检查 Key、URL 地域、模型开通状态及账单。由于没有你的凭据，此演示**仅执行过本地语法检查，没有实际发起付费模型调用或验证输出**。

## 真正接入 CL-recognized 的步骤

1. 不要将 Python POC 的 `FAKE_ANIMALS` 作为业务事实；改由 Java `getAnimalProfile(individualId)` Tool 返回已授权的数据库记录。
2. 确定 `knowledge_document` 的来源、审核状态、可公开级别、版本和撤回机制；从 MySQL 已审核内容生成 Embedding，写入 Chroma 的业务 ID 元数据。
3. 已识别个体的相关检索必须过滤 `individualId`，科普类则按 `speciesId` 过滤；对结果添加明确来源，资料不足不得自行编造。
4. 如果 Java 直接集成向量服务，可采用与版本兼容的 LangChain4j/Chroma 方案；也可把 RAG 封装在独立 Python 服务，由 Java 授权调用。**不要仅因 POC 使用 Python，就强迫整个业务后端改为 Python。**
5. 数据更新或撤回后应执行索引同步并验证查询不再返回旧文档。控制向量维度、Embedding 模型版本和 Chroma Collection 的一致性。

## 安全与边界

- 无需将原始小程序图片、保育敏感位置、真实游客身份、密码、订单数据放入 Chroma。
- 检索文档是**不可信输入数据**，其中的“请忽略之前指令”不能被解释为系统命令。
- RAG 回复仍需事实一致性测试；即使已有检索来源，大模型也可能产生错误。个人数据和受保护野生动物位置信息不得自动转发给外部模型。
- `.venv/`、`chroma_data/`、日志、API Key 与密钥管理文件均不要上传 Git。
