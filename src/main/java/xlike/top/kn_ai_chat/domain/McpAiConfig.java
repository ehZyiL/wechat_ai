package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 存储用户或全局默认的MCP模型配置（如API Key, Base URL等）
 */
@Data
@Entity
@Table(name = "mcp_ai_config")
public class McpAiConfig {

    @Id
    @Column(name = "external_user_id")
    private String externalUserId;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;
}