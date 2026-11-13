-- 当前项目所需表。先在 IDEA 或 mysql 客户端选中服务器上已有的库再执行。
-- Docker 里的库名是环境变量 MYSQL_DATABASE，不要依赖下面这行的库名。
-- USE animal-recognition;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(64) NOT NULL,
  `real_name` VARCHAR(64) NULL,
  `email` VARCHAR(128) NULL,
  `phone` VARCHAR(32) NULL,
  `department` VARCHAR(64) NULL,
  `join_date` DATE NULL,
  `status` VARCHAR(32) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `species` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `description` VARCHAR(500) NULL,
  `image_url` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_species_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `individual` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `species_id` BIGINT NULL,
  `species_name` VARCHAR(64) NULL,
  `gender` VARCHAR(32) NULL,
  `age` INT NULL,
  `description` VARCHAR(1000) NULL,
  `status` VARCHAR(32) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_individual_code` (`code`),
  KEY `idx_individual_species` (`species_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `camera` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `ip_address` VARCHAR(64) NULL,
  `location` VARCHAR(128) NULL,
  `port` INT NULL,
  `username` VARCHAR(64) NULL,
  `password` VARCHAR(128) NULL,
  `status` VARCHAR(32) NULL,
  `preview_url` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recognition_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `media_type` VARCHAR(16) NULL,
  `source` VARCHAR(32) NULL,
  `source_type` VARCHAR(32) NULL,
  `operator_name` VARCHAR(64) NULL,
  `user_id` BIGINT NULL,
  `media_url` VARCHAR(500) NULL,
  `processed_video_url` VARCHAR(500) NULL,
  `predicted_name` VARCHAR(128) NULL,
  `confidence` DECIMAL(6,2) NULL,
  `all_probs` TEXT NULL,
  `elephant_count` INT NULL,
  `frame_count` INT NULL,
  `elephants` TEXT NULL,
  `individual_id` BIGINT NULL,
  `status` VARCHAR(32) NULL,
  `error_message` VARCHAR(500) NULL,
  `create_time` DATETIME NULL,
  `update_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_record_user` (`user_id`),
  KEY `idx_record_individual` (`individual_id`),
  KEY `idx_record_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `visitor_session` (
  `id` VARCHAR(64) NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitor_token` (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `media_upload` (
  `id` VARCHAR(64) NOT NULL,
  `media_asset_id` VARCHAR(64) NOT NULL,
  `session_id` VARCHAR(64) NOT NULL,
  `file_name` VARCHAR(255) NULL,
  `content_type` VARCHAR(100) NULL,
  `size_bytes` BIGINT NULL,
  `source` VARCHAR(32) NULL,
  `state` VARCHAR(32) NOT NULL,
  `storage_path` VARCHAR(500) NULL,
  `media_type` VARCHAR(16) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `expires_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_asset` (`media_asset_id`),
  KEY `idx_upload_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `recognition_task` (
  `id` VARCHAR(64) NOT NULL,
  `session_id` VARCHAR(64) NOT NULL,
  `media_asset_id` VARCHAR(64) NOT NULL,
  `idempotency_key` VARCHAR(128) NULL,
  `media_type` VARCHAR(16) NOT NULL,
  `task_state` VARCHAR(32) NOT NULL,
  `result_kind` VARCHAR(32) NULL,
  `review_state` VARCHAR(32) NOT NULL,
  `predicted_name` VARCHAR(128) NULL,
  `confidence` DECIMAL(6,2) NULL,
  `individual_id` BIGINT NULL,
  `record_id` BIGINT NULL,
  `quality_reason` VARCHAR(255) NULL,
  `error_message` VARCHAR(500) NULL,
  `feedback_verdict` VARCHAR(32) NULL,
  `visible` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_session` (`session_id`),
  KEY `idx_task_review` (`review_state`, `task_state`),
  KEY `idx_task_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `actor` VARCHAR(128) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `resource_type` VARCHAR(64) NOT NULL,
  `resource_id` VARCHAR(64) NOT NULL,
  `detail` VARCHAR(500) NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
