package xlike.top.kn_ai_chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/**
 * @author xlike
 */
@Data
public class McpAddRequest {

    @NotEmpty(message = "mcpServers不能为空")
    private Map<String, @Valid McpDefinition> mcpServers;
}