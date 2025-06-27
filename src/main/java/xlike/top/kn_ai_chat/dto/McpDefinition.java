package xlike.top.kn_ai_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;


@Data
public class McpDefinition {

    @NotBlank(message = "类型(type)不能为空")
    @Pattern(regexp = "sse", message = "类型(type)当前只支持 'sse'")
    private String type;

    @NotBlank(message = "URL不能为空")
    @URL(message = "必须是有效的URL格式")
    private String url;
}