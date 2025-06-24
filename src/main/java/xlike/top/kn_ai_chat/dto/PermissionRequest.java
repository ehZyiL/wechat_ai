package xlike.top.kn_ai_chat.dto;

import lombok.Data;

/**
 * @author xlike
 */
@Data
public class PermissionRequest {
    private String externalUserId;
    private Long mcpConfigId;
    private boolean grant;
}