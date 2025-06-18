package xlike.top.kn_ai_chat.handler;

import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.LotteryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
@Component
public class LotteryHandler implements MessageHandler {

    private final LotteryService lotteryService;
    private static final List<String> KEYWORDS = Arrays.asList("大乐透", "双色球", "今日中奖号码", "查彩票", "开奖");

    public LotteryHandler(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    @Override
    public boolean canHandle(String content) {
        return KEYWORDS.stream().anyMatch(content::contains);
    }

    // ▼▼▼【核心修改】: 重写 handle 方法的逻辑 ▼▼▼
    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        List<String> results = new ArrayList<>();

        // 如果指令明确或通用，则检查大乐透
        if (content.contains("大乐透") || !content.contains("双色球")) {
            lotteryService.getLotteryResult("dlt").ifPresent(results::add);
        }

        // 如果指令明确或通用，则检查双色球
        if (content.contains("双色球") || !content.contains("大乐透")) {
            lotteryService.getLotteryResult("ssq").ifPresent(results::add);
        }

        if (!results.isEmpty()) {
            // 使用 TextReply 包装返回结果
            return Optional.of(new TextReply(String.join("\n\n---\n\n", results)));
        } else {
            // 使用 TextReply 包装返回结果
            return Optional.of(new TextReply("今日无相关彩票开奖信息。"));
        }
    }

    @Override
    public int getOrder() {
        // 高优先级
        return 1;
    }
}