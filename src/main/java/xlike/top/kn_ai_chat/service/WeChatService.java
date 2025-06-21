package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.dispatcher.MessageDispatcher;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.repository.MessageLogRepository;
import xlike.top.kn_ai_chat.reply.*;
import xlike.top.kn_ai_chat.service.MediaService.DownloadedMedia;
import xlike.top.kn_ai_chat.utils.FileContentReader;
import xlike.top.kn_ai_chat.utils.WeChatUtils;
import xlike.top.kn_ai_chat.utils.XmlParseUtil;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WeChatService {

    private static final Logger logger = LoggerFactory.getLogger(WeChatService.class);

    @Value("${wechat.token}")
    private String token;
    @Value("${wechat.encoding-aes-key}")
    private String encodingAesKey;

    private final MessageLogRepository messageLogRepository;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final MessageDispatcher messageDispatcher;
    private final AccessTokenManager accessTokenManager;
    private final MediaService mediaService;
    private final SiliconFlowService siliconFlowService;
    private final FormatFileService formatFileService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final UserConfigService userConfigService;
    private final WeChatUserService weChatUserService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private byte[] encodingAesKeyBytes;
    
    // 用于消息去重的Redis Key前缀
    private static final String PROCESSED_MSG_ID_KEY_PREFIX = "wechat:processed_msgid:";
    // 用于存储消息拉取游标的Redis Key
    private static final String MSG_CURSOR_KEY = "wechat:msg_cursor";

    public WeChatService(
            MessageLogRepository messageLogRepository,
            RestTemplate restTemplate,
            RedisTemplate<String, String> redisTemplate,
            MessageDispatcher messageDispatcher,
            AccessTokenManager accessTokenManager,
            MediaService mediaService,
            SiliconFlowService siliconFlowService,
            FormatFileService formatFileService,
            KnowledgeBaseService knowledgeBaseService,
            UserConfigService userConfigService,
            WeChatUserService weChatUserService) {
        this.messageLogRepository = messageLogRepository;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.messageDispatcher = messageDispatcher;
        this.accessTokenManager = accessTokenManager;
        this.mediaService = mediaService;
        this.siliconFlowService = siliconFlowService;
        this.formatFileService = formatFileService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.userConfigService = userConfigService;
        this.weChatUserService = weChatUserService;
    }

    @PostConstruct
    public void init() {
        this.encodingAesKeyBytes = WeChatUtils.base64Decode(encodingAesKey);
        if (encodingAesKeyBytes.length != 32) {
            throw new IllegalArgumentException("EncodingAESKey 解码后长度应为32字节");
        }
    }

    public String processMessage(String xmlData, String msgSignature, String timestamp, String nonce) {
        try {
            String encryptedMsg = XmlParseUtil.extractEncryptPart(xmlData);
            String[] array = {token, timestamp, nonce, encryptedMsg};
            Arrays.sort(array);
            String sortedStr = String.join("", array);
            String signature = WeChatUtils.sha1(sortedStr);

            if (!signature.equals(msgSignature)) {
                logger.warn("签名验证失败! received_signature: {}, calculated_signature: {}", msgSignature, signature);
                return "error";
            }

            byte[] decryptedBytes = WeChatUtils.decrypt(WeChatUtils.base64Decode(encryptedMsg), this.encodingAesKeyBytes);
            String decryptedXml = new String(decryptedBytes, StandardCharsets.UTF_8);
            logger.info("解密后的消息: {}", decryptedXml);

            Map<String, String> messageMap = XmlParseUtil.xmlToMap(decryptedXml);
            String event = messageMap.get("Event");

            if ("kf_msg_or_event".equals(event)) {
                // 异步处理消息，防止阻塞微信回调
                syncAndProcessMessages(); 
            } else {
                logger.info("接收到非拉取类型的事件，忽略处理: {}", event);
            }

            return "success";
        } catch (DataIntegrityViolationException e) {
            logger.info("数据库唯一键冲突，消息可能已被处理，本次请求忽略。错误: {}", e.getMessage());
            return "success";
        } catch (Exception e) {
            logger.error("处理微信消息失败", e);
            return "error";
        }
    }
    
    /**
     * 异步地、分页地拉取和处理消息。
     * 该方法会从Redis读取上一次的游标，并仅处理最新的一条消息，然后保存新的游标。
     */
    @Async
    public void syncAndProcessMessages() {
        // 从Redis中获取上次保存的游标
        String cursor = redisTemplate.opsForValue().get(MSG_CURSOR_KEY);
        
        // 循环拉取，直到没有更多消息
        while (true) {
            String accessToken = accessTokenManager.getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/sync_msg?access_token=" + accessToken;

            Map<String, Object> requestBody = new HashMap<>();
            if (cursor != null) {
                requestBody.put("cursor", cursor);
            }
        
            try {
                String response = restTemplate.postForObject(url, requestBody, String.class);
                logger.debug("拉取消息响应(sync_msg): {}", response);
                JsonNode root = objectMapper.readTree(response);

                if (root.path("errcode").asInt() != 0) {
                    logger.error("拉取消息(sync_msg)失败: {}", response);
                    break;
                }
                // 获取新的游标并立即保存到Redis，无论本次是否处理了消息。
                // 这是为了确保下次能从正确的位置开始拉取。
                String nextCursor = root.path("next_cursor").asText(null);
                if (nextCursor != null) {
                    redisTemplate.opsForValue().set(MSG_CURSOR_KEY, nextCursor);
                    cursor = nextCursor;
                }
                JsonNode msgList = root.path("msg_list");
                if (msgList.isArray() && !msgList.isEmpty()) {
                    JsonNode latestMessage = msgList.get(msgList.size() - 1);
                    processSinglePulledMessage(latestMessage);
                }

                // 如果微信服务器明确告知没有更多消息了，就退出循环
                if (root.path("has_more").asInt() == 0) {
                    logger.info("没有更多消息，结束本次拉取。");
                    break;
                }

            } catch (Exception e) {
                logger.error("调用拉取消息(sync_msg)接口失败", e);
                break;
            }
        }
    }
    
    /**
     * 处理单条从API拉取的消息。包含幂等性检查。
     */
    private void processSinglePulledMessage(JsonNode msgNode) {
        // 对于事件类型的消息，如 "enter_session", "user_recall_msg"，它们没有 msgid，直接跳过
        if (!msgNode.has("msgid")) {
            return;
        }

        String msgId = msgNode.get("msgid").asText();
        String redisKey = PROCESSED_MSG_ID_KEY_PREFIX + msgId;
    
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey)) || messageLogRepository.existsByMsgId(msgId)) {
            logger.info("消息 {} 已被处理过，跳过。", msgId);
            return;
        }
    
        String msgType = msgNode.get("msgtype").asText();
    
        String externalUserId = msgNode.has("external_userid") ? msgNode.get("external_userid").asText() :
                (msgNode.has("event") && msgNode.get("event").has("external_userid") ? msgNode.get("event").get("external_userid").asText() : "UNKNOWN_USER");
        String openKfid = msgNode.has("open_kfid") ? msgNode.get("open_kfid").asText() :
                (msgNode.has("event") && msgNode.get("event").has("open_kfid") ? msgNode.get("event").get("open_kfid").asText() : "UNKNOWN_KFID");
    
        if (!"UNKNOWN_USER".equals(externalUserId)) {
            weChatUserService.getOrCreateUser(externalUserId);
        }
        // 检查用户是否被拉黑，这一步已在MessageDispatcher中实现，此处为双重保险
        if(weChatUserService.isUserBlocked(externalUserId)) {
             logger.info("用户 [{}] 已被拉黑，拒绝回复此消息: {}", externalUserId, msgId);
             return;
        }

        switch (msgType) {
            case "text":
                handleTextMessage(msgNode, externalUserId, openKfid);
                break;
            case "image":
                handleImageMessage(msgNode, externalUserId, openKfid);
                break;
            case "voice":
                handleVoiceMessage(msgNode, externalUserId, openKfid);
                break;
            case "file":
                handleFileMessage(msgNode, externalUserId, openKfid);
                break;
            default:
                logger.info("接收到未处理的消息类型: {}, 跳过处理。", msgType);
                break;
        }
        
        redisTemplate.opsForValue().set(redisKey, "processed", 48, TimeUnit.HOURS);
    }
    

    private void handleTextMessage(JsonNode msgNode, String externalUserId, String openKfid) {
        String userContent = msgNode.get("text").get("content").asText().trim();
        saveMessageLog(msgNode.get("msgid").asText(), externalUserId, openKfid, "text", userContent);
        logger.info("用户 [{}] 发送消息: {}", externalUserId, userContent);

        List<MessageLog> history = messageLogRepository.findByFromUserOrToUserOrderByTimestampDesc(externalUserId, externalUserId, PageRequest.of(0, 10));
        Collections.reverse(history);

        Optional<Reply> replyOpt = messageDispatcher.dispatch(externalUserId, openKfid, userContent, history);
        replyOpt.ifPresent(reply -> sendReply(reply, externalUserId, openKfid));
    }

    private void handleImageMessage(JsonNode msgNode, String externalUserId, String openKfid) {
        String mediaId = msgNode.get("image").get("media_id").asText();
        String msgId = msgNode.get("msgid").asText();
        logger.info("用户 [{}] 发送图片, media_id: {}", externalUserId, mediaId);

        Optional<DownloadedMedia> downloadedMediaOpt = mediaService.downloadTemporaryMedia(mediaId);
        if (downloadedMediaOpt.isPresent()) {
            File imageFile = downloadedMediaOpt.get().file();
            
            Optional<String> descriptionOpt = siliconFlowService.analyzeImage(imageFile, "请详细描述这张图片的内容", externalUserId);
            imageFile.delete(); 

            String replyContent;
            if (descriptionOpt.isPresent() && !descriptionOpt.get().isBlank()) {
                replyContent = descriptionOpt.get();
                saveMessageLog(msgId, externalUserId, openKfid, "image", replyContent);
            } else {
                replyContent = "抱歉，我暂时无法理解这张图片的内容。";
            }
            sendTextMessage(externalUserId, openKfid, replyContent);
            saveMessageLog(null, openKfid, externalUserId, "text", replyContent);

        } else {
            sendTextMessage(externalUserId, openKfid, "图片下载失败，无法处理。");
        }
    }

    private void handleVoiceMessage(JsonNode msgNode, String externalUserId, String openKfid) {
        String mediaId = msgNode.get("voice").get("media_id").asText();
        String msgId = msgNode.get("msgid").asText();
        logger.info("用户 [{}] 发送语音, media_id: {}", externalUserId, mediaId);
    
        Optional<DownloadedMedia> downloadedMediaOpt = mediaService.downloadTemporaryMedia(mediaId);
        if (downloadedMediaOpt.isEmpty()) {
            sendTextMessage(externalUserId, openKfid, "抱歉，你的语音文件下载失败了。");
            return;
        }
    
        File amrFile = downloadedMediaOpt.get().file();
        Optional<File> mp3FileOpt = formatFileService.convertToMp3(amrFile);
        amrFile.delete(); 
    
        if (mp3FileOpt.isEmpty()) {
            sendTextMessage(externalUserId, openKfid, "抱歉，语音格式转换失败，无法识别。");
            return;
        }
    
        File mp3File = mp3FileOpt.get();
        Optional<String> transcribedTextOpt = siliconFlowService.transcribeAudio(mp3File, externalUserId);
        mp3File.delete(); 
    
        if (transcribedTextOpt.isEmpty() || transcribedTextOpt.get().isBlank()) {
            sendTextMessage(externalUserId, openKfid, "抱歉，你的语音我没听清，可以再说一遍吗？");
            return;
        }
    
        String transcribedText = transcribedTextOpt.get();
        logger.info("语音识别结果: {}", transcribedText);
    
        saveMessageLog(msgId, externalUserId, openKfid, "voice", transcribedText);
        
        List<MessageLog> history = messageLogRepository.findByFromUserOrToUserOrderByTimestampDesc(externalUserId, externalUserId, PageRequest.of(0, 10));
        Collections.reverse(history);
    
        Optional<Reply> replyOpt = messageDispatcher.dispatch(externalUserId, openKfid, transcribedText, history);
        replyOpt.ifPresent(reply -> sendReply(reply, externalUserId, openKfid));
    }

    private void handleFileMessage(JsonNode msgNode, String externalUserId, String openKfid) {
        String mediaId = msgNode.get("file").get("media_id").asText();
        String msgId = msgNode.get("msgid").asText();

        Optional<DownloadedMedia> downloadedMediaOpt = mediaService.downloadTemporaryMedia(mediaId);

        String replyContent;
        if (downloadedMediaOpt.isPresent()) {
            DownloadedMedia downloadedMedia = downloadedMediaOpt.get();
            File file = downloadedMedia.file();
            String originalFilename = downloadedMedia.filename();

            logger.info("用户 [{}] 发送文件 '{}'，临时存储为 '{}'", externalUserId, originalFilename, file.getName());

            try {
                String fileContent = FileContentReader.readFileContent(file);
                if (fileContent.isBlank()) {
                    replyContent = "文件 '" + originalFilename + "' 内容为空，已跳过。";
                } else {
                    saveMessageLog(msgId, externalUserId, openKfid, "file", fileContent);
                    knowledgeBaseService.addFileToKnowledgeBase(file, originalFilename, externalUserId);
                    replyContent = "✅ 文件 '" + originalFilename + "' 已成功添加到您的知识库！";
                }
            } catch (IOException e) {
                logger.error("为用户 [{}] 读取文件 '{}' 内容失败。", externalUserId, originalFilename, e);
                replyContent = "❌ 文件 '" + originalFilename + "' 内容解析失败：" + e.getMessage();
            } finally {
                file.delete();
            }
        } else {
            replyContent = "❌ 文件下载失败，无法存入知识库。";
        }

        sendTextMessage(externalUserId, openKfid, replyContent);
        saveMessageLog(null, openKfid, externalUserId, "text", replyContent);
    }
    
    private void saveMessageLog(String msgId, String fromUser, String toUser, String msgType, String content) {
        MessageLog log = new MessageLog();
        if (msgId != null && !msgId.isEmpty()) {
            log.setMsgId(msgId);
        }
        log.setFromUser(fromUser);
        log.setToUser(toUser);
        log.setMsgType(msgType);
        log.setContent(content);
        log.setTimestamp(LocalDateTime.now());
        try {
            messageLogRepository.save(log);
        } catch (DataIntegrityViolationException e) {
            logger.warn("保存MessageLog时出现唯一键冲突，MsgId: {}. 该消息可能已被处理。", msgId);
        }
    }
    
    private void sendReply(Reply reply, String externalUserId, String openKfid) {
        switch (reply) {
            case TextReply textReply -> {
                logger.info("准备发送给用户 [{}] 的文本: {}", externalUserId, textReply.content());
                sendTextMessage(externalUserId, openKfid, textReply.content());
                saveMessageLog(null, openKfid, externalUserId, "text", textReply.content());
            }
            case ImageReply imageReply -> {
                logger.info("准备发送给用户 [{}] 的图片, media_id: {}", externalUserId, imageReply.mediaId());
                sendImageMessage(externalUserId, openKfid, imageReply.mediaId());
                saveMessageLog(null, openKfid, externalUserId, "image", "media_id: " + imageReply.mediaId());
            }
            case VoiceReply voiceReply -> {
                logger.info("准备发送给用户 [{}] 的语音, media_id: {}", externalUserId, voiceReply.mediaId());
                sendVoiceMessage(externalUserId, openKfid, voiceReply.mediaId());
                saveMessageLog(null, openKfid, externalUserId, "voice", "media_id: " + voiceReply.mediaId());
            }
            case VideoReply videoReply -> {
                logger.info("准备发送给用户 [{}] 的视频, media_id: {}", externalUserId, videoReply.mediaId());
                sendVideoMessage(externalUserId, openKfid, videoReply.mediaId());
                saveMessageLog(null, openKfid, externalUserId, "video", "media_id: " + videoReply.mediaId());
            }
            case FileReply fileReply -> {
                logger.info("准备发送给用户 [{}] 的文件, media_id: {}", externalUserId, fileReply.mediaId());
                sendFileMessage(externalUserId, openKfid, fileReply.mediaId());
                saveMessageLog(null, openKfid, externalUserId, "file", "media_id: " + fileReply.mediaId());
            }
        }
    }

    public void sendTextMessage(String toUser, String openKfid, String content) {
        Map<String, Object> contentMap = Map.of("content", content);
        sendRequest("text", toUser, openKfid, contentMap);
    }

    public void sendImageMessage(String toUser, String openKfid, String mediaId) {
        Map<String, Object> contentMap = Map.of("media_id", mediaId);
        sendRequest("image", toUser, openKfid, contentMap);
    }
    
    public void sendVoiceMessage(String toUser, String openKfid, String mediaId) {
        Map<String, Object> contentMap = Map.of("media_id", mediaId);
        sendRequest("voice", toUser, openKfid, contentMap);
    }

    public void sendVideoMessage(String toUser, String openKfid, String mediaId) {
        Map<String, Object> contentMap = Map.of("media_id", mediaId);
        sendRequest("video", toUser, openKfid, contentMap);
    }

    public void sendFileMessage(String toUser, String openKfid, String mediaId) {
        Map<String, Object> contentMap = Map.of("media_id", mediaId);
        sendRequest("file", toUser, openKfid, contentMap);
    }
    
    private void sendRequest(String msgType, String toUser, String openKfid, Map<String, Object> contentMap) {
        String accessToken = accessTokenManager.getAccessToken();
        String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/send_msg?access_token=" + accessToken;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("touser", toUser);
        requestBody.put("open_kfid", openKfid);
        requestBody.put("msgtype", msgType);
        requestBody.put(msgType, contentMap);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String response = restTemplate.postForObject(url, jsonBody, String.class);
            logger.info("发送 {} 消息响应: {}", msgType, response);
            JsonNode root = objectMapper.readTree(response);
            if (root.get("errcode").asInt() != 0) {
                logger.error("发送 {} 消息失败: {}", msgType, response);
            }
        } catch (Exception e) {
            logger.error("调用发送 {} 消息接口失败", msgType, e);
        }
    }
}