package xlike.top.kn_ai_chat.handler;

import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.LotteryService;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
@Component
public class LotteryHandler implements MessageHandler {

    private final LotteryService lotteryService;
    private final UserConfigService userConfigService;

    public LotteryHandler(LotteryService lotteryService, UserConfigService userConfigService) {
        this.lotteryService = lotteryService;
        this.userConfigService = userConfigService;
    }

    /**
     * 修改了方法签名，并将 "default" 替换为 externalUserId
     */
    @Override
    public boolean canHandle(String content, String externalUserId) {
        List<String> keywords = userConfigService.getKeywordsForHandler(externalUserId, this.getClass().getSimpleName());
        return keywords.stream().anyMatch(content::contains);
    }

    /**
     * 修改了 handle 方法，将 "default" 替换为 externalUserId
     */
    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        List<String> results = new ArrayList<>();
        List<String> keywords = userConfigService.getKeywordsForHandler(externalUserId, this.getClass().getSimpleName());

        if (content.contains("大乐透") || (!content.contains("双色球") && keywords.contains("查彩票"))) {
            lotteryService.getLotteryResult("dlt").ifPresent(results::add);
        }

        if (content.contains("双色球") || (!content.contains("大乐透") && keywords.contains("查彩票"))) {
            lotteryService.getLotteryResult("ssq").ifPresent(results::add);
        }

        if (!results.isEmpty()) {
            return Optional.of(new TextReply(String.join("\n\n---\n\n", results)));
        } else {
            return Optional.of(new TextReply("今日无相关彩票开奖信息。"));
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}