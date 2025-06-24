package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "user_mcp_permission", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"externalUserId", "mcp_config_id"})
})
public class UserMcpPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String externalUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mcp_config_id", nullable = false)
    private McpConfig mcpConfig;
}