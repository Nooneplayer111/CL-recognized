package com.elephantjava.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VisitorChainSchema implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public VisitorChainSchema(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS visitor_session (
                  id VARCHAR(64) PRIMARY KEY,
                  token_hash CHAR(64) NOT NULL,
                  expires_at DATETIME NOT NULL,
                  created_at DATETIME NOT NULL,
                  UNIQUE KEY uk_visitor_token (token_hash)
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS media_upload (
                  id VARCHAR(64) PRIMARY KEY,
                  media_asset_id VARCHAR(64) NOT NULL,
                  session_id VARCHAR(64) NOT NULL,
                  file_name VARCHAR(255),
                  content_type VARCHAR(100),
                  size_bytes BIGINT,
                  source VARCHAR(32),
                  state VARCHAR(32) NOT NULL,
                  storage_path VARCHAR(500),
                  media_type VARCHAR(16) NOT NULL,
                  created_at DATETIME NOT NULL,
                  expires_at DATETIME NOT NULL,
                  UNIQUE KEY uk_media_asset (media_asset_id)
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS recognition_task (
                  id VARCHAR(64) PRIMARY KEY,
                  session_id VARCHAR(64) NOT NULL,
                  media_asset_id VARCHAR(64) NOT NULL,
                  idempotency_key VARCHAR(128),
                  media_type VARCHAR(16) NOT NULL,
                  task_state VARCHAR(32) NOT NULL,
                  result_kind VARCHAR(32),
                  review_state VARCHAR(32) NOT NULL,
                  predicted_name VARCHAR(128),
                  confidence DECIMAL(6,2),
                  species_id BIGINT,
                  individual_id BIGINT,
                  record_id BIGINT,
                  quality_reason VARCHAR(255),
                  error_message VARCHAR(500),
                  feedback_verdict VARCHAR(32),
                  visible TINYINT NOT NULL DEFAULT 1,
                  created_at DATETIME NOT NULL,
                  updated_at DATETIME NOT NULL,
                  KEY idx_task_session (session_id),
                  KEY idx_task_review (review_state, task_state)
                )""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS audit_event (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  actor VARCHAR(128) NOT NULL,
                  action VARCHAR(64) NOT NULL,
                  resource_type VARCHAR(64) NOT NULL,
                  resource_id VARCHAR(64) NOT NULL,
                  detail VARCHAR(500),
                  created_at DATETIME NOT NULL
                )""");
        addColumnIfMissing("individual", "image_url", "VARCHAR(500) NULL");
        addColumnIfMissing("recognition_task", "species_id", "BIGINT NULL");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        }
    }
}
