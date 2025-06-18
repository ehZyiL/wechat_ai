package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Administrator
 */
@Service
public class LotteryService {

    private static final Logger logger = LoggerFactory.getLogger(LotteryService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 彩票号码
    private static final Set<String> MY_NUMBERS_FRONT = new HashSet<>(Arrays.asList("01", "06", "12", "14", "25"));
    private static final Set<String> MY_NUMBERS_BACK = new HashSet<>(Arrays.asList("09", "11"));
    private static final Set<String> MY_NUMBERS_RED = new HashSet<>(Arrays.asList("01", "06", "12", "14", "25", "09"));
    private static final String MY_NUMBER_BLUE = "11";

    // API 地址
    private static final String DLT_API = "http://api.huiniao.top/interface/home/lotteryHistory?type=dlt&page=1&limit=1";
    private static final String SSQ_API = "http://api.huiniao.top/interface/home/lotteryHistory?type=ssq&page=1&limit=1";

    /**
     * 获取最新的彩票数据。
     * @param lotteryType "dlt" 或 "ssq"
     * @return 如果当天有开奖结果，则返回包含格式化消息的Optional，否则返回Optional.empty()
     */
    public Optional<String> getLotteryResult(String lotteryType) {
        String apiUrl = "dlt".equalsIgnoreCase(lotteryType) ? DLT_API : SSQ_API;
        String lotteryName = "dlt".equalsIgnoreCase(lotteryType) ? "大乐透" : "双色球";

        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.get("code").asInt() != 1 || !root.has("data") || !root.get("data").has("last")) {
                logger.error("获取{}数据失败: {}", lotteryName, response);
                // 对于API错误，我们仍然返回提示信息
                return Optional.of("获取" + lotteryName + "最新开奖数据失败，请稍后再试。");
            }

            JsonNode lastDraw = root.get("data").get("last");
            String drawDateStr = lastDraw.get("day").asText();
            LocalDate drawDate = LocalDate.parse(drawDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // ▼▼▼【核心修改】▼▼▼
            // 检查开奖日期是否是今天，如果不是，则返回一个空的Optional，表示“没有结果”
            if (!drawDate.equals(LocalDate.now())) {
                return Optional.empty();
            }

            // 如果是今天的结果，则格式化并返回
            return Optional.of(formatLotteryMessage(lastDraw, lotteryType));

        } catch (IOException e) {
            logger.error("请求{}接口或解析数据出错", lotteryName, e);
            return Optional.of("查询" + lotteryName + "开奖结果时发生内部错误。");
        }
    }

    // formatLotteryMessage 和 checkWin 方法保持不变...
    private String formatLotteryMessage(JsonNode drawData, String lotteryType) {
        String code = drawData.get("code").asText();
        String day = drawData.get("day").asText();
        
        if ("dlt".equalsIgnoreCase(lotteryType)) {
            String[] frontDraw = {
                drawData.get("one").asText(), drawData.get("two").asText(), drawData.get("three").asText(),
                drawData.get("four").asText(), drawData.get("five").asText()
            };
            String[] backDraw = {drawData.get("six").asText(), drawData.get("seven").asText()};
            String prize = checkDltWin(new HashSet<>(Arrays.asList(frontDraw)), new HashSet<>(Arrays.asList(backDraw)));

            return String.format(
                "🎉 大乐透 开奖公告 🎉\n\n" +
                "期号：%s\n" +
                "日期：%s\n" +
                "开奖号码：\n前区: %s\n后区: %s\n\n" +
                "我的号码：\n前区: %s\n后区: %s\n\n" +
                "结果：%s",
                code, day,
                String.join(", ", frontDraw), String.join(", ", backDraw),
                String.join(", ", MY_NUMBERS_FRONT), String.join(", ", MY_NUMBERS_BACK),
                prize
            );
        } else { // SSQ
            String[] redDraw = {
                drawData.get("one").asText(), drawData.get("two").asText(), drawData.get("three").asText(),
                drawData.get("four").asText(), drawData.get("five").asText(), drawData.get("six").asText()
            };
            String blueDraw = drawData.get("seven").asText();
            String prize = checkSsqWin(new HashSet<>(Arrays.asList(redDraw)), blueDraw);

            return String.format(
                "🎉 双色球 开奖公告 🎉\n\n" +
                "期号：%s\n" +
                "日期：%s\n" +
                "开奖号码：\n红球: %s\n蓝球: %s\n\n" +
                "我的号码：\n红球: %s\n蓝球: %s\n\n" +
                "结果：%s",
                code, day,
                String.join(", ", redDraw), blueDraw,
                String.join(", ", MY_NUMBERS_RED), MY_NUMBER_BLUE,
                prize
            );
        }
    }

    private String checkDltWin(Set<String> frontDraw, Set<String> backDraw) {
        long frontMatch = MY_NUMBERS_FRONT.stream().filter(frontDraw::contains).count();
        long backMatch = MY_NUMBERS_BACK.stream().filter(backDraw::contains).count();

        if (frontMatch == 5 && backMatch == 2) return "一等奖";
        if (frontMatch == 5 && backMatch == 1) return "二等奖";
        if (frontMatch == 5 && backMatch == 0) return "三等奖";
        if (frontMatch == 4 && backMatch == 2) return "四等奖";
        if (frontMatch == 4 && backMatch == 1) return "五等奖";
        if (frontMatch == 3 && backMatch == 2) return "六等奖";
        if (frontMatch == 4 && backMatch == 0) return "七等奖";
        if ((frontMatch == 3 && backMatch == 1) || (frontMatch == 2 && backMatch == 2)) return "八等奖";
        if ((frontMatch == 3 && backMatch == 0) || (frontMatch == 1 && backMatch == 2) || (frontMatch == 2 && backMatch == 1) || (frontMatch == 0 && backMatch == 2)) return "九等奖";
        
        return "很遗憾，未中奖，再接再厉！";
    }

    private String checkSsqWin(Set<String> redDraw, String blueDraw) {
        long redMatch = MY_NUMBERS_RED.stream().filter(redDraw::contains).count();
        boolean blueMatch = MY_NUMBER_BLUE.equals(blueDraw);

        if (redMatch == 6 && blueMatch) return "一等奖";
        if (redMatch == 6 && !blueMatch) return "二等奖";
        if (redMatch == 5 && blueMatch) return "三等奖";
        if ((redMatch == 5 && !blueMatch) || (redMatch == 4 && blueMatch)) return "四等奖";
        if ((redMatch == 4 && !blueMatch) || (redMatch == 3 && blueMatch)) return "五等奖";
        if ((redMatch == 2 && blueMatch) || (redMatch == 1 && blueMatch) || (redMatch == 0 && blueMatch)) return "六等奖";

        return "很遗憾，未中奖，再接再厉！";
    }
}