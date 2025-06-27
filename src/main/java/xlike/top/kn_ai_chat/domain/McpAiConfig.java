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
    private String externalUserId;

    @Column(nullable = false)
    private String baseUrl;

    @Column(nullable = false)
    private String apiKey;

    @Column(nullable = false)
    private String model;
    
    @Column(nullable = false)
    private LocalDateTime lastModified;
}