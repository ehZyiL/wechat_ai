package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.McpConfig;
import xlike.top.kn_ai_chat.dto.BatchPermissionRequest;
import xlike.top.kn_ai_chat.dto.McpAddRequest;
import xlike.top.kn_ai_chat.dto.PermissionRequest;
import xlike.top.kn_ai_chat.service.McpService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/mcp")
public class McpController {

    private final McpService mcpService;

    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    @GetMapping("/configs")
    public ResponseEntity<List<McpConfig>> getAllConfigs(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(mcpService.getAllMcpConfigs());
    }

    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<Long>>> getPermissions(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(mcpService.getPermissionsGroupedByUser());
    }

    @PostMapping("/permissions/update")
    public ResponseEntity<Void> updatePermission(@RequestBody PermissionRequest request, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        try {
            if (request.isGrant()) {
                mcpService.grantPermission(request.getExternalUserId(), request.getMcpConfigId());
            } else {
                mcpService.revokePermission(request.getExternalUserId(), request.getMcpConfigId());
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 用于删除一个MCP配置
     * @param id 要删除的MCP配置的ID
     * @param session 用于权限验证
     * @return 成功或失败的响应
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMcpConfiguration(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "未授权的访问"));
        }
        try {
            mcpService.deleteMcpConfig(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "MCP配置成功删除。"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "删除失败，服务器内部错误。"));
        }
    }
    
    @PostMapping("/permissions/batch")
    public ResponseEntity<Void> batchUpdatePermissions(@RequestBody BatchPermissionRequest request, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        try {
            mcpService.batchUpdatePermissions(request.getMcpConfigId(), request.isGrant());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 用于从前端模态框接收JSON，并新增MCP配置
     * @param request 包含MCP服务器信息的请求对象
     * @param session 用于权限验证
     * @return 成功或失败的响应
     */
    @PostMapping("/add")
    public ResponseEntity<?> addMcpConfiguration(@Valid @RequestBody McpAddRequest request, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "未授权的访问"));
        }
        try {
            mcpService.addMcpConfig(request);
            return ResponseEntity.ok(Collections.singletonMap("message", "MCP配置成功添加。"));
        } catch (RuntimeException e) {
            // 捕获业务逻辑中抛出的已知异常，并返回给前端
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            // 捕获其他未知异常
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "服务器内部错误，请联系管理员。"));
        }
    }
}