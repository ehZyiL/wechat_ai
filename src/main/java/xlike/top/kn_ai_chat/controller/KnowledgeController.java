package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.Knowledge;
import xlike.top.kn_ai_chat.service.KnowledgeBaseService;

import java.util.List;

/**
 * @author xlike
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    @GetMapping("/{externalUserId}")
    public ResponseEntity<List<Knowledge>> getUserFiles(@PathVariable String externalUserId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        List<Knowledge> files = knowledgeBaseService.listFilesForUser(externalUserId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<String> deleteFile(@PathVariable Long fileId, @RequestParam String externalUserId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        String result = knowledgeBaseService.deleteFileForUser(fileId, externalUserId);
        if (result.startsWith("❌")) {
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }
}