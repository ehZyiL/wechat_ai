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

@Service
public class ScheduledPushTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledPushTask.class);

    private final LotteryService lotteryService;
    private final WeChatService weChatService;

    @Value("${wechat.push.open-kfid}")
    private String PUSH_OPEN_KFID;

    @Value("${wechat.push.external-user-id}")
    private String PUSH_EXTERNAL_USER_ID;

    private static final List<DayOfWeek> DLT_DRAW_DAYS = Arrays.asList(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
    private static final List<DayOfWeek> SSQ_DRAW_DAYS = Arrays.asList(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY);


    public ScheduledPushTask(LotteryService lotteryService, WeChatService weChatService) {
        this.lotteryService = lotteryService;
        this.weChatService = weChatService;
    }

    @Scheduled(cron = "0 43 22 * * ?")
    public void pushLotteryResult() {
        logger.info("开始执行每日彩票开奖推送任务...");
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if (DLT_DRAW_DAYS.contains(today)) {
            logger.info("今天是大乐透开奖日，正在获取结果...");
            Optional<String> dltResultOpt = lotteryService.getLotteryResult("dlt");
            
            dltResultOpt.ifPresent(result -> {
                logger.info("大乐透开奖结果已获取，准备推送...");
                weChatService.sendTextMessage(PUSH_EXTERNAL_USER_ID, PUSH_OPEN_KFID.trim(), result);
                logger.info("大乐透开奖结果已推送给用户 {}", PUSH_EXTERNAL_USER_ID);
            });
        }

        if (SSQ_DRAW_DAYS.contains(today)) {
            logger.info("今天是双色球开奖日，正在获取结果...");
            Optional<String> ssqResultOpt = lotteryService.getLotteryResult("ssq");
            
            ssqResultOpt.ifPresent(result -> {
                logger.info("双色球开奖结果已获取，准备推送...");
                weChatService.sendTextMessage(PUSH_EXTERNAL_USER_ID, PUSH_OPEN_KFID.trim(), result);
                logger.info("双色球开奖结果已推送给用户 {}", PUSH_EXTERNAL_USER_ID);
            });
        }
        
        logger.info("每日彩票开奖推送任务执行完毕。");
    }
}