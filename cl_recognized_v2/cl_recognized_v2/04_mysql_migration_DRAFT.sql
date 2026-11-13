-- CL-recognized v2 数据库迁移建议草案 (MySQL 8, InnoDB, utf8mb4)
-- 2026-11-06
-- !!! 不是已验证的生产升级脚本。禁止无备份直接在真实数据库执行。
-- !!! 当前 ZIP 只提供 recognition_record.sql；其余真实旧表 DDL 和版本未上传。
-- !!! DBA/项目负责人应先 SHOW CREATE TABLE, 对照字段、唯一约束、数据量，
-- !!! 再拆成可回滚的 Flyway/Liquibase 迁移，并在测试库验证旧数据兼容和回滚。
-- !!! 本脚本的全新表结构是“建议”，而不是源码已存在的表。

-- 1. 游客用户：与原员工表 `user` 分离；服务端保管微信身份，禁止客户端自报用户 ID。
CREATE TABLE IF NOT EXISTS visitor_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  wechat_openid VARCHAR(128) NOT NULL,
  display_name VARCHAR(64) NOT NULL DEFAULT '游客',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_visitor_openid (wechat_openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序游客账户；不复用员工 user';

-- 2. 稳定的模型注册表：非空 routing_key 才能保证 SPECIES 只有一条。
-- task=SPECIES routing_key=all; task=INDIVIDUAL routing_key=elephant/tiger...
CREATE TABLE IF NOT EXISTS model_endpoint (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task VARCHAR(32) NOT NULL,
  routing_key VARCHAR(64) NOT NULL,
  species_id BIGINT NULL,
  endpoint_url VARCHAR(255) NOT NULL,
  timeout_ms INT NOT NULL DEFAULT 30000,
  auto_route_min_confidence DECIMAL(5,2) NOT NULL DEFAULT 75.00,
  model_version VARCHAR(64) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_model_route (task, routing_key),
  KEY idx_model_species (species_id),
  CONSTRAINT ck_model_timeout CHECK (timeout_ms BETWEEN 500 AND 120000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可控内网模型注册表；endpoint 必须经服务端域名白名单验证';

-- 3. 识别会话：使用显式 principal_type + principal_id 防止员工/游客 Long ID 相撞。
CREATE TABLE IF NOT EXISTS agent_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  principal_type VARCHAR(16) NOT NULL COMMENT 'VISITOR/STAFF',
  principal_id BIGINT NOT NULL,
  scene VARCHAR(16) NOT NULL COMMENT 'VISITOR/OPS',
  individual_id BIGINT NULL,
  species_id BIGINT NULL,
  recognition_job_id BIGINT NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_agent_owner (principal_type, principal_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool',
  content TEXT NOT NULL COMMENT '已脱敏的摘要，不存敏感工具原文',
  tool_name VARCHAR(64) NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_message_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  payload JSON NOT NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_event_session (session_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 后端可信识别任务。可引用旧 recognition_record, 但不能由小程序直接提交 model_score。
CREATE TABLE IF NOT EXISTS recognition_job (
  id BIGINT NOT NULL AUTO_INCREMENT,
  visitor_user_id BIGINT NOT NULL,
  session_id BIGINT NULL,
  request_id VARCHAR(64) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'QUEUED'
    COMMENT 'QUEUED/RUNNING/RECOGNIZED/SPECIES_ONLY/UNCERTAIN/UNKNOWN/ERROR',
  species_id BIGINT NULL,
  individual_id BIGINT NULL,
  individual_code VARCHAR(64) NULL,
  recognition_record_id BIGINT NULL,
  model_version VARCHAR(64) NULL,
  model_score DECIMAL(6,2) NULL COMMENT '模型原始评分，不一定是概率',
  eligible_for_checkin TINYINT NOT NULL DEFAULT 0,
  trusted_source VARCHAR(24) NOT NULL DEFAULT 'SERVER_PIPELINE',
  error_code VARCHAR(64) NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_rec_job_request (visitor_user_id, request_id),
  KEY idx_rec_job_owner (visitor_user_id, create_time),
  KEY idx_rec_job_individual (individual_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 个体档案扩展：下面两条是一次性 ALTER，先在测试库检查旧结构，再执行。
-- individual 旧表已存在。没有已授权年龄信息时保留 NULL；不要把 species.image_url 当个体头像。
ALTER TABLE individual
  ADD COLUMN avatar_url VARCHAR(512) NULL COMMENT '个体专属头像，可为空',
  ADD COLUMN birth_date DATE NULL COMMENT '有明确来源时才填准确生日',
  ADD COLUMN age_as_of DATE NULL COMMENT '原 age INT 代表截至哪一天的年龄',
  ADD COLUMN age_source VARCHAR(255) NULL COMMENT '年龄来源或估计依据';

-- 如果实际旧表已有重名列，请不要重复执行 ALTER；改用增量迁移编号。
ALTER TABLE recognition_record
  ADD COLUMN species_id BIGINT NULL,
  ADD COLUMN recognition_job_id BIGINT NULL,
  ADD COLUMN source_trust VARCHAR(24) NOT NULL DEFAULT 'LEGACY_UNVERIFIED'
    COMMENT 'SERVER_VERIFIED/LEGACY_UNVERIFIED';

-- 6. MySQL 权威打卡 + 排名分配计数器：不依赖 Redis 保证积分安全。
CREATE TABLE IF NOT EXISTS checkin_day_counter (
  species_id BIGINT NOT NULL,
  biz_date CHAR(8) NOT NULL COMMENT 'Asia/Shanghai yyyyMMdd',
  last_rank BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (species_id, biz_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS species_checkin (
  id BIGINT NOT NULL AUTO_INCREMENT,
  visitor_user_id BIGINT NOT NULL,
  species_id BIGINT NOT NULL,
  biz_date CHAR(8) NOT NULL,
  recognition_job_id BIGINT NOT NULL,
  rank_no BIGINT NOT NULL,
  points_awarded INT NOT NULL,
  checkin_time DATETIME(3) NOT NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_checkin_once (visitor_user_id, species_id, biz_date),
  UNIQUE KEY uk_checkin_job (recognition_job_id),
  UNIQUE KEY uk_checkin_rank (species_id, biz_date, rank_no),
  KEY idx_checkin_rank (species_id, biz_date, rank_no),
  KEY idx_checkin_user_date (visitor_user_id, biz_date),
  CONSTRAINT ck_checkin_positive_rank CHECK (rank_no > 0),
  CONSTRAINT ck_checkin_points CHECK (points_awarded >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 游客积分账本：与员工 user 分离。
CREATE TABLE IF NOT EXISTS point_wallet (
  visitor_user_id BIGINT NOT NULL,
  balance BIGINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (visitor_user_id),
  CONSTRAINT ck_wallet_nonnegative CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS point_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  visitor_user_id BIGINT NOT NULL,
  change_amount BIGINT NOT NULL,
  balance_after BIGINT NOT NULL,
  biz_type VARCHAR(32) NOT NULL COMMENT 'CHECKIN/EXCHANGE/REFUND/ADJUST',
  biz_id VARCHAR(80) NOT NULL,
  remark VARCHAR(255) NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_point_business (visitor_user_id, biz_type, biz_id),
  KEY idx_point_user_time (visitor_user_id, create_time),
  CONSTRAINT ck_point_balance CHECK (balance_after >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. MySQL 事务事件，Redis 榜单及 Chroma 文档重建用它来可靠重试。
CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_type VARCHAR(64) NOT NULL,
  business_key VARCHAR(128) NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'NEW',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_time DATETIME(3) NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event (event_type, business_key),
  KEY idx_outbox_status (status, next_retry_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 同物种同日讨论：数据库是正文权威；游标按 (create_time,id) 双字段。
CREATE TABLE IF NOT EXISTS species_comment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  species_id BIGINT NOT NULL,
  visitor_user_id BIGINT NOT NULL,
  biz_date CHAR(8) NOT NULL,
  content VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING/PUBLISHED/REJECTED/DELETED',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_feed_keyset (species_id, biz_date, status, create_time, id),
  KEY idx_feed_author (visitor_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 积分商城：MySQL 防超卖、独立用户限购计数器、订单幂等。
CREATE TABLE IF NOT EXISTS point_goods (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  cover_url VARCHAR(512) NULL,
  description VARCHAR(500) NULL,
  points_price BIGINT NOT NULL,
  stock INT NOT NULL,
  goods_type VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(16) NOT NULL DEFAULT 'OFF',
  limit_per_user INT NOT NULL DEFAULT 1,
  sale_start DATETIME(3) NULL,
  sale_end DATETIME(3) NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_goods_sale (status, goods_type, sale_start, sale_end),
  CONSTRAINT ck_goods_price CHECK (points_price > 0),
  CONSTRAINT ck_goods_stock CHECK (stock >= 0),
  CONSTRAINT ck_goods_limit CHECK (limit_per_user > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS point_goods_quota (
  visitor_user_id BIGINT NOT NULL,
  goods_id BIGINT NOT NULL,
  redeemed_count INT NOT NULL DEFAULT 0,
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (visitor_user_id, goods_id),
  CONSTRAINT ck_redeemed_count CHECK (redeemed_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS point_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  visitor_user_id BIGINT NOT NULL,
  goods_id BIGINT NOT NULL,
  goods_name VARCHAR(64) NOT NULL,
  points_cost BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  request_id VARCHAR(64) NOT NULL COMMENT '同一个客户端动作重试必须不变',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_request (visitor_user_id, request_id),
  KEY idx_order_user (visitor_user_id, create_time, id),
  KEY idx_order_goods (goods_id),
  CONSTRAINT ck_order_points CHECK (points_cost > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. RAG 的权威文档元数据：Chroma 是副本，审核资料主数据保存在这里。
-- 只有 visibility=PUBLIC AND status=PUBLISHED 的文档才能进入游客 Chroma。
CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGINT NOT NULL AUTO_INCREMENT,
  source_type VARCHAR(32) NOT NULL,
  source_id VARCHAR(128) NOT NULL,
  source_version INT NOT NULL DEFAULT 1,
  individual_id BIGINT NULL,
  species_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  visibility VARCHAR(16) NOT NULL DEFAULT 'INTERNAL',
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_source (source_type, source_id),
  KEY idx_knowledge_individual (individual_id, status, visibility),
  KEY idx_knowledge_species (species_id, status, visibility)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 管理端异常事件是可选工作包，外部 Python 模型交付后再具体确定字段。
-- 下方是最小事件主表草案，不含轨迹原始大数组；媒体请用对象存储 key 管理。
CREATE TABLE IF NOT EXISTS abnormal_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  device_event_id VARCHAR(80) NOT NULL,
  camera_id BIGINT NOT NULL,
  track_id VARCHAR(64) NULL,
  individual_id BIGINT NULL,
  species_id BIGINT NULL,
  behavior_type VARCHAR(32) NOT NULL,
  probability DECIMAL(6,4) NOT NULL,
  occurred_at DATETIME(3) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  handle_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  clip_storage_key VARCHAR(512) NULL,
  keyframe_storage_key VARCHAR(512) NULL,
  raw_json JSON NOT NULL,
  agent_summary TEXT NULL,
  suggestion TEXT NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_event_id (device_event_id),
  KEY idx_abnormal_camera (camera_id, occurred_at),
  KEY idx_abnormal_status (handle_status, severity, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 推荐的高并发打卡事务伪 SQL (Java Service 中执行，不能作为裸脚本调用):
-- BEGIN;
-- INSERT IGNORE INTO checkin_day_counter(species_id,biz_date,last_rank) VALUES (:sid,:day,0);
-- SELECT last_rank FROM checkin_day_counter WHERE species_id=:sid AND biz_date=:day FOR UPDATE;
-- SELECT * FROM species_checkin WHERE visitor_user_id=:uid AND species_id=:sid AND biz_date=:day;
--   (如已存在，返回原记录；请确保上面的行锁仍被本事务持有)
-- UPDATE checkin_day_counter SET last_rank = last_rank + 1 WHERE species_id=:sid AND biz_date=:day;
-- INSERT INTO species_checkin(...,rank_no,points_awarded) VALUES (..., old_last_rank+1, award(old_last_rank+1));
-- UPDATE point_wallet SET balance=balance+:award, version=version+1 WHERE visitor_user_id=:uid;
-- INSERT INTO point_log(...,biz_type,biz_id) VALUES (...,'CHECKIN',:sid + ':' + :day);
-- INSERT INTO outbox_event(event_type,business_key,payload) VALUES ('CHECKIN_RANK_UPDATED',:checkinId,...);
-- COMMIT;
-- 任何失败都 ROLLBACK，Redis 投影在 COMMIT 之后异步写入。

-- 14. 推荐的库存与积分扣减 SQL（在一次 MySQL 事务中；不接受前端价格）:
-- UPDATE point_goods SET stock=stock-1 WHERE id=:goodsId AND status='ON' AND stock>0;
-- UPDATE point_goods_quota SET redeemed_count=redeemed_count+1
-- WHERE visitor_user_id=:uid AND goods_id=:goodsId AND redeemed_count<:limit;
-- UPDATE point_wallet SET balance=balance-:cost, version=version+1
-- WHERE visitor_user_id=:uid AND balance>=:cost;
-- INSERT INTO point_order(...) VALUES (...);
-- INSERT INTO point_log(...) VALUES (...);
-- 每次检查 affectedRows==1；扣减失败回滚全部操作。
-- 注意：先 INSERT IGNORE 初始化 quota 唯一行，并始终在同一事务中操作；
-- 需规定所有兑换交易统一锁顺序，避免跨商品与多账户事务死锁。
-- 幂等 key 的并发冲突在外层查询已提交订单，不可在异常回滚后再重复扣款。
