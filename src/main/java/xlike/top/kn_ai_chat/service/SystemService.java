package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.repository.MessageLogRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 负责执行系统级别指令的服务
 * @author xlike
 */
@Service
public class SystemService {

    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);
    private final MessageLogRepository messageLogRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public SystemService(MessageLogRepository messageLogRepository) {
        this.messageLogRepository = messageLogRepository;
    }

    public String clearHistory(String userId) {
        try {
            messageLogRepository.deleteByFromUserOrToUser(userId, userId);
            logger.info("已成功清空用户 [{}] 的历史对话记录。", userId);
            return "✅ 已清空与您的历史对话记录。";
        } catch (Exception e) {
            logger.error("清空用户 [{}] 的历史对话记录时发生错误。", userId, e);
            return "❌ 清空历史对话记录失败，请稍后再试。";
        }
    }

    public String getChatStats(String userId) {
        try {
            long count = messageLogRepository.countByFromUserOrToUser(userId, userId);
            logger.info("查询到用户 [{}] 的历史对话共 {} 条。", userId, count);
            return String.format("📈 我与您的对话共计 %d 条。", count);
        } catch (Exception e) {
            logger.error("查询用户 [{}] 的对话统计时发生错误。", userId, e);
            return "❌ 查询对话统计失败，请稍后再试。";
        }
    }

    /**
     *  获取指定用户发送的消息总数
     * @param userId 用户的 externalUserId
     * @return 该用户发送的消息总数
     */
    public long countQuestionsFromUser(String userId) {
        return messageLogRepository.countByFromUser(userId);
    }

    /**
     * 获取指定用户最近的提问记录
     * @param userId 用户的 externalUserId
     * @return 格式化后的提问记录字符串
     */
    public String getUserQuestions(String userId) {
        try {
            // 查询最近的10条提问记录
            List<MessageLog> userMessages = messageLogRepository.findByFromUserOrderByTimestampDesc(userId, PageRequest.of(0, 10));
            if (userMessages.isEmpty()) {
                return "您还没有问过任何问题。";
            }
            StringBuilder sb = new StringBuilder("您最近的提问记录如下：\n");
            for (int i = 0; i < userMessages.size(); i++) {
                MessageLog msg = userMessages.get(i);
                sb.append(String.format("%d. [%s]: %s\n",
                        i + 1,
                        msg.getTimestamp().format(FORMATTER),
                        msg.getContent()));
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
            logger.error("查询用户 [{}] 的提问记录时发生错误。", userId, e);
            return "❌ 查询提问记录失败，请稍后再试。";
        }
    }
}