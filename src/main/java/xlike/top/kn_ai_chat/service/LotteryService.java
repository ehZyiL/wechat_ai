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
 * @author xlike
 */
@Service
public class LotteryService {

    private static final Logger logger = LoggerFactory.getLogger(LotteryService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * 【修改】格式化彩票信息，只包含官方开奖结果
     * @param drawData 开奖数据节点
     * @param lotteryType 彩票类型
     * @return 格式化后的公告字符串
     */
    private String formatLotteryMessage(JsonNode drawData, String lotteryType) {
        String code = drawData.get("code").asText();
        String day = drawData.get("day").asText();
        
        if ("dlt".equalsIgnoreCase(lotteryType)) {
            String[] frontDraw = {
                drawData.get("one").asText(), drawData.get("two").asText(), drawData.get("three").asText(),
                drawData.get("four").asText(), drawData.get("five").asText()
            };
            String[] backDraw = {drawData.get("six").asText(), drawData.get("seven").asText()};

            return String.format(
                "🎉 大乐透 开奖公告 🎉\n\n" +
                "期号：%s\n" +
                "日期：%s\n\n" +
                "开奖号码：\n" +
                "前区: %s\n" +
                "后区: %s",
                code, day,
                String.join(", ", frontDraw), String.join(", ", backDraw)
            );
        } else { // SSQ
            String[] redDraw = {
                drawData.get("one").asText(), drawData.get("two").asText(), drawData.get("three").asText(),
                drawData.get("four").asText(), drawData.get("five").asText(), drawData.get("six").asText()
            };
            String blueDraw = drawData.get("seven").asText();

            return String.format(
                "🎉 双色球 开奖公告 🎉\n\n" +
                "期号：%s\n" +
                "日期：%s\n\n" +
                "开奖号码：\n" +
                "红球: %s\n" +
                "蓝球: %s",
                code, day,
                String.join(", ", redDraw), blueDraw
            );
        }
    }
}