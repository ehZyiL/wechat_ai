package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.McpConfig;
import xlike.top.kn_ai_chat.dto.BatchPermissionRequest;
import xlike.top.kn_ai_chat.dto.PermissionRequest;
import xlike.top.kn_ai_chat.service.McpService;

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

    /**
     * 【重要修正】
     * 修复了参数绑定问题。
     * 整个请求的数据现在都从 @RequestBody 中获取，不再混用 @RequestParam。
     */
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


}
