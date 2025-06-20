package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.CustomReply;
import xlike.top.kn_ai_chat.service.CustomReplyService;

import java.util.List;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/admin/api/custom-replies")
@RequiredArgsConstructor
public class CustomReplyController {

    private final CustomReplyService customReplyService;

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    // 获取所有规则
    @GetMapping
    public ResponseEntity<List<CustomReply>> getAll(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(customReplyService.findAll());
    }

    // 新增一个规则
    @PostMapping
    public ResponseEntity<CustomReply> create(@RequestBody CustomReply customReply, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(customReplyService.save(customReply));
    }

    // 删除一个规则
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        customReplyService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}