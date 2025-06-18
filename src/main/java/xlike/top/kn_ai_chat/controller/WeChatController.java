package xlike.top.kn_ai_chat.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.service.WeChatService;
import xlike.top.kn_ai_chat.utils.WeChatUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/wechat")
public class WeChatController {

    private static final Logger logger = LoggerFactory.getLogger(WeChatController.class);

    @Value("${wechat.token}")
    private String token;

    @Value("${wechat.encoding-aes-key}")
    private String encodingAesKey;

    private byte[] encodingAesKeyBytes;

    private final WeChatService weChatService;

    public WeChatController(WeChatService weChatService) {
        this.weChatService = weChatService;
    }

    /**
     * 初始化，解码AES Key
     */
    @javax.annotation.PostConstruct
    public void init() {
        // Base64解码微信提供的AES Key
        this.encodingAesKeyBytes = WeChatUtils.base64Decode(encodingAesKey);
        if (encodingAesKeyBytes.length != 32) {
            throw new IllegalArgumentException("EncodingAESKey 解码后长度应为32字节");
        }
    }

    /**
     * 微信回调URL验证接口 (GET请求)
     * @param msgSignature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 待解密的随机字符串
     * @return 解密后的echostr
     */
    @GetMapping("/callback")
    public String verify(
            @RequestParam(name = "msg_signature") String msgSignature,
            @RequestParam(name = "timestamp") String timestamp,
            @RequestParam(name = "nonce") String nonce,
            @RequestParam(name = "echostr") String echostr) {

        logger.info("接收到微信验证请求: msg_signature={}, timestamp={}, nonce={}, echostr={}", msgSignature, timestamp, nonce, echostr);

        // 1. 构造用于签名的字符串
        String[] array = {token, timestamp, nonce, echostr};
        Arrays.sort(array);
        String sortedStr = String.join("", array);

        // 2. SHA1加密
        String calculatedSignature = WeChatUtils.sha1(sortedStr);

        // 3. 比较签名
        if (!calculatedSignature.equals(msgSignature)) {
            logger.error("签名验证失败! received_signature: {}, calculated_signature: {}", msgSignature, calculatedSignature);
            return "签名验证失败";
        }

        try {
            // 4. 解密echostr
            byte[] decryptedBytes = WeChatUtils.decrypt(WeChatUtils.base64Decode(echostr), encodingAesKeyBytes);
            String decryptedEchostr = new String(decryptedBytes, StandardCharsets.UTF_8);
            logger.info("验证成功，返回解密后的 echostr: {}", decryptedEchostr);
            // 5. 返回解密后的内容
            return decryptedEchostr;
        } catch (Exception e) {
            logger.error("echostr解密失败", e);
            return "解密失败";
        }
    }

    /**
     * 接收微信消息和事件的回调接口 (POST请求)
     * @param msgSignature 消息签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param xmlData 请求体（XML格式）
     * @return 处理结果
     */
    @PostMapping("/callback")
    public String handleMessage(
            @RequestParam(name = "msg_signature") String msgSignature,
            @RequestParam(name = "timestamp") String timestamp,
            @RequestParam(name = "nonce") String nonce,
            @RequestBody String xmlData) {
        logger.info("接收到微信消息: msg_signature={}, timestamp={}, nonce={}", msgSignature, timestamp, nonce);
        logger.debug("消息体: {}", xmlData);
        // 将所有参数传递给服务层进行处理
        return weChatService.processMessage(xmlData, msgSignature, timestamp, nonce);
    }
}