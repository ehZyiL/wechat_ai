package xlike.top.kn_ai_chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.CustomReply;
import xlike.top.kn_ai_chat.enums.MatchType;
import xlike.top.kn_ai_chat.repository.CustomReplyRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
public class CustomReplyService {

    private final CustomReplyRepository customReplyRepository;

    private String normalizeKeyword(String input) {
        if (input == null) return null;
        return input.toLowerCase().replaceAll("[\\p{P}\\p{S}\\s]", "");
    }

    /**
     * 【核心】根据规则列表和匹配方式查找回复
     * @param content 原始消息内容
     * @param rules   规则列表
     * @return 匹配到的回复
     */
    private Optional<String> findMatch(String content, List<CustomReply> rules) {
        String normalizedContent = normalizeKeyword(content);
        String lowerCaseContent = content.toLowerCase();

        for (CustomReply rule : rules) {
            String keyword = rule.getKeyword();
            if (rule.getMatchType() == MatchType.EQUALS) {
                // "等于"逻辑：使用标准化后的内容进行精确比较
                if (keyword.equals(normalizedContent)) {
                    return Optional.of(rule.getReply());
                }
            } else if (rule.getMatchType() == MatchType.CONTAINS) {
                // "包含"逻辑：使用小写内容进行包含比较
                if (lowerCaseContent.contains(keyword)) {
                    return Optional.of(rule.getReply());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 【升级】分层查找，并支持两种匹配模式
     */
    public Optional<String> findReplyForKeyword(String content, String userId) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        // 1. 查找并匹配用户专属规则
        List<CustomReply> userRules = customReplyRepository.findByExternalUserId(userId);
        Optional<String> userReply = findMatch(content, userRules);
        if (userReply.isPresent()) {
            return userReply;
        }

        // 2. 查找并匹配全局规则
        List<CustomReply> globalRules = customReplyRepository.findByExternalUserIdIsNull();
        return findMatch(content, globalRules);
    }

    public List<CustomReply> findAll() {
        return customReplyRepository.findAll();
    }

    /**
     * 【升级】保存规则时，根据匹配方式处理关键词
     */
    @Transactional
    public CustomReply save(CustomReply customReply) {
        String keyword = customReply.getKeyword();
        if (keyword == null || keyword.isBlank()) {
             throw new IllegalArgumentException("关键词不能为空");
        }
        
        // 如果是“等于”匹配，关键词需要标准化
        if (customReply.getMatchType() == MatchType.EQUALS) {
            String normalized = normalizeKeyword(keyword);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("关键词不能只包含标点/空格");
            }
            customReply.setKeyword(normalized);
        } else {
            // 如果是“包含”匹配，关键词存入时也转为小写，以便后续统一比较
             customReply.setKeyword(keyword.toLowerCase().trim());
        }
        
        return customReplyRepository.save(customReply);
    }

    @Transactional
    public void deleteById(Long id) {
        customReplyRepository.deleteById(id);
    }
}