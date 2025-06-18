package xlike.top.kn_ai_chat.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.service.LotteryService;
import xlike.top.kn_ai_chat.service.WeChatService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
@Service
public class ScheduledPushTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledPushTask.class);

    private final LotteryService lotteryService;
    private final WeChatService weChatService;

    // --- 需要主动推送的用户信息 ---
    // 从 application.yml 中读取客服和用户ID
    @Value("${wechat.push.open-kfid}")
    private String PUSH_OPEN_KFID;

    @Value("${wechat.push.external-user-id}")
    private String PUSH_EXTERNAL_USER_ID;

    // 大乐透和双色球的开奖日
    private static final List<DayOfWeek> DLT_DRAW_DAYS = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
    private static final List<DayOfWeek> SSQ_DRAW_DAYS = Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY);


    public ScheduledPushTask(LotteryService lotteryService, WeChatService weChatService) {
        this.lotteryService = lotteryService;
        this.weChatService = weChatService;
    }

    /**
     * 定时任务，每天晚上22:05执行，检查彩票结果并推送
     * cron表达式: "0 5 22 * * ?" 意为：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 43 22 * * ?")
    public void pushLotteryResult() {
        logger.info("开始执行每日彩票开奖推送任务...");
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        // 检查今天是否是大乐透开奖日
        if (DLT_DRAW_DAYS.contains(today)) {
            logger.info("今天是大乐透开奖日，正在获取结果...");
            // getLotteryResult 现在返回 Optional<String>
            Optional<String> dltResultOpt = lotteryService.getLotteryResult("dlt");
            
            // 使用 ifPresent 方法，只有在 Optional 不为空时才执行发送操作
            dltResultOpt.ifPresent(result -> {
                logger.info("大乐透开奖结果已获取，准备推送...");
                weChatService.sendTextMessage(PUSH_EXTERNAL_USER_ID, PUSH_OPEN_KFID.trim(), result);
                logger.info("大乐透开奖结果已推送给用户 {}", PUSH_EXTERNAL_USER_ID);
            });
        }

        // 检查今天是否是双色球开奖日
        if (SSQ_DRAW_DAYS.contains(today)) {
            logger.info("今天是双色球开奖日，正在获取结果...");
            // getLotteryResult 现在返回 Optional<String>
            Optional<String> ssqResultOpt = lotteryService.getLotteryResult("ssq");
            
            // 同样使用 ifPresent 方法处理
            ssqResultOpt.ifPresent(result -> {
                logger.info("双色球开奖结果已获取，准备推送...");
                weChatService.sendTextMessage(PUSH_EXTERNAL_USER_ID, PUSH_OPEN_KFID.trim(), result);
                logger.info("双色球开奖结果已推送给用户 {}", PUSH_EXTERNAL_USER_ID);
            });
        }
        
        logger.info("每日彩票开奖推送任务执行完毕。");
    }
}