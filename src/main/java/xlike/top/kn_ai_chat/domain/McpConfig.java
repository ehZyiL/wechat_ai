package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "mcp_config")
public class McpConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "sse_endpoint", nullable = false, length = 512)
    private String sseEndpoint;
}