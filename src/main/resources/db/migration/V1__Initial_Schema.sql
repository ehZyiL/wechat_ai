-- V1__Initial_Schema.sql
-- 微信AI助手数据库初始架构

-- 创建we_chat_user表
CREATE TABLE we_chat_user
(
    external_user_id VARCHAR(255) PRIMARY KEY,
    nickname         VARCHAR(255),
    avatar           VARCHAR(512),
    info             TEXT,
    blocked          BOOLEAN DEFAULT FALSE,
    last_updated     TIMESTAMP
);

-- 创建ai_config表
CREATE TABLE ai_config
(
    external_user_id VARCHAR(255) PRIMARY KEY,
    ai_base_url      VARCHAR(512) NOT NULL,
    ai_api_key       VARCHAR(512) NOT NULL,
    ai_model         VARCHAR(255) NOT NULL,
    system_prompt    TEXT         NOT NULL,
    sf_base_url      VARCHAR(512) NOT NULL,
    sf_image_model   VARCHAR(255) NOT NULL,
    sf_tts_model     VARCHAR(255) NOT NULL,
    sf_stt_model     VARCHAR(255) NOT NULL,
    sf_voice         VARCHAR(255) NOT NULL,
    sf_vlm_model     VARCHAR(255) NOT NULL,
    rag_enabled      BOOLEAN DEFAULT FALSE,
    rag_model        VARCHAR(255),
    rag_base_url     VARCHAR(512),
    rag_api_key      VARCHAR(512),
    last_modified    TIMESTAMP    NOT NULL
);

-- 创建chat_message表
CREATE TABLE chat_message
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL,
    sender_type      VARCHAR(20)  NOT NULL CHECK (sender_type IN ('ADMIN', 'USER')),
    message_type     VARCHAR(20)  NOT NULL CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')),
    content          TEXT         NOT NULL,
    meta             TEXT,
    timestamp        TIMESTAMP    NOT NULL
);

-- 创建knowledge_base表
CREATE TABLE knowledge_base
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL,
    file_name        VARCHAR(500) NOT NULL,
    content          TEXT         NOT NULL,
    created_at       TIMESTAMP    NOT NULL
);

-- 创建message_log表
CREATE TABLE message_log
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id    VARCHAR(255) UNIQUE,
    from_user VARCHAR(255),
    to_user   VARCHAR(255),
    msg_type  VARCHAR(50),
    content   TEXT,
    timestamp TIMESTAMP
);

-- 创建custom_reply表
CREATE TABLE custom_reply
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_type       VARCHAR(20)   NOT NULL CHECK (match_type IN ('EXACT', 'FUZZY', 'REGEX')),
    keyword          VARCHAR(255)  NOT NULL,
    reply            VARCHAR(2048) NOT NULL,
    external_user_id VARCHAR(255),
    create_time      TIMESTAMP     NOT NULL
);

-- 创建keyword_config表
CREATE TABLE keyword_config
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL,
    handler_name     VARCHAR(255) NOT NULL,
    keywords         TEXT         NOT NULL,
    last_modified    TIMESTAMP    NOT NULL
);

-- 创建wechat_kf_account表
CREATE TABLE wechat_kf_account
(
    open_kfid VARCHAR(255) PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    avatar    VARCHAR(512) NOT NULL
);

-- 创建user_mcp_permission表
CREATE TABLE user_mcp_permission
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL,
    mcp_config_id    BIGINT       NOT NULL,
    UNIQUE (external_user_id, mcp_config_id)
);

-- 创建mcp_config表
CREATE TABLE mcp_config
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    type         VARCHAR(50)  NOT NULL,
    url          VARCHAR(512) NOT NULL,
    sse_endpoint VARCHAR(512) NOT NULL
);

-- 创建manual_transfer_request表
CREATE TABLE manual_transfer_request
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id VARCHAR(255) NOT NULL UNIQUE,
    last_message     TEXT         NOT NULL,
    request_time     TIMESTAMP    NOT NULL,
    resolved         BOOLEAN DEFAULT FALSE
);

-- 创建mcp_ai_config表
CREATE TABLE mcp_ai_config
(
    external_user_id VARCHAR(255) PRIMARY KEY,
    base_url         VARCHAR(512) NOT NULL,
    api_key          VARCHAR(512) NOT NULL,
    model            VARCHAR(255) NOT NULL,
    last_modified    TIMESTAMP    NOT NULL
);

-- 添加外键约束
-- PostgreSQL语法
ALTER TABLE user_mcp_permission
    ADD CONSTRAINT fk_user_mcp_permission_mcp_config
        FOREIGN KEY (mcp_config_id) REFERENCES mcp_config (id) ON DELETE CASCADE;

-- 创建索引以提高查询性能
CREATE INDEX idx_chat_message_external_user_id ON chat_message (external_user_id);
CREATE INDEX idx_chat_message_timestamp ON chat_message (timestamp);
CREATE INDEX idx_knowledge_base_external_user_id ON knowledge_base (external_user_id);
CREATE INDEX idx_knowledge_base_created_at ON knowledge_base (created_at);
CREATE INDEX idx_custom_reply_external_user_id ON custom_reply (external_user_id);
CREATE INDEX idx_custom_reply_keyword ON custom_reply (keyword);
CREATE INDEX idx_keyword_config_external_user_id ON keyword_config (external_user_id);
CREATE INDEX idx_message_log_msg_id ON message_log (msg_id);
CREATE INDEX idx_message_log_timestamp ON message_log (timestamp);
CREATE INDEX idx_user_mcp_permission_external_user_id ON user_mcp_permission (external_user_id);
CREATE INDEX idx_user_mcp_permission_mcp_config_id ON user_mcp_permission (mcp_config_id);
CREATE INDEX idx_manual_transfer_request_external_user_id ON manual_transfer_request (external_user_id);
CREATE INDEX idx_manual_transfer_request_request_time ON manual_transfer_request (request_time);
CREATE INDEX idx_ai_config_external_user_id ON ai_config (external_user_id);
CREATE INDEX idx_mcp_ai_config_external_user_id ON mcp_ai_config (external_user_id);
