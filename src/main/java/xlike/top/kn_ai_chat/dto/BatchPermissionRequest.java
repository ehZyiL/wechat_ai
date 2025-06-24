package xlike.top.kn_ai_chat.dto;

import lombok.Data;

/**
 * @author xlike
 */
@Data
public class BatchPermissionRequest {
    private Long mcpConfigId;
    private boolean grant;
}