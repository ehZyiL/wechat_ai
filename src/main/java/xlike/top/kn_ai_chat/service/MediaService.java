package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.enums.MediaType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 媒体素材服务
 * <p>
 * 负责与微信的临时素材接口进行交互，包括上传和下载。
 * @author Administrator
 */
@Service
public class MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);
    
    // 【最终修正】更精确的正则表达式，优先匹配 RFC 5987 标准的 filename*，并停止在分号
    private static final Pattern FILENAME_STAR_PATTERN = Pattern.compile("filename\\*=([^;]+)", Pattern.CASE_INSENSITIVE);
    // 回退匹配非标准的 filename
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final AccessTokenManager accessTokenManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用于封装下载结果的 record
     *
     * @param file     下载的、带有正确扩展名的临时文件
     * @param filename 从响应头中解析出的最准确的文件名
     */
    public record DownloadedMedia(File file, String filename) {}

    public MediaService(RestTemplate restTemplate, AccessTokenManager accessTokenManager) {
        this.restTemplate = restTemplate;
        this.accessTokenManager = accessTokenManager;
    }

    public Optional<String> uploadTemporaryMedia(File file, MediaType mediaType) {
        if (file == null || !file.exists()) {
            logger.error("上传文件不存在！");
            return Optional.empty();
        }

        String accessToken = accessTokenManager.getAccessToken();
        String url = "https://qyapi.weixin.qq.com/cgi-bin/media/upload?access_token=" + accessToken + "&type=" + mediaType.getTypeName();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("media", new FileSystemResource(file));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(url, requestEntity, String.class);
            logger.info("上传临时素材响应: {}", response);
            JsonNode root = objectMapper.readTree(response);
            if (root.has("errcode") && root.get("errcode").asInt() == 0 && root.has("media_id")) {
                String mediaId = root.get("media_id").asText();
                return Optional.of(mediaId);
            } else {
                logger.error("上传临时素材失败: {}", response);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("调用上传临时素材接口异常", e);
            return Optional.empty();
        }
    }

    public Optional<DownloadedMedia> downloadTemporaryMedia(String mediaId) {
        String accessToken = accessTokenManager.getAccessToken();
        String url = "https://qyapi.weixin.qq.com/cgi-bin/media/get?access_token=" + accessToken + "&media_id=" + mediaId;

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                
                String filename = parseFilenameFromDisposition(disposition);
                logger.info("从响应头中智能解析到文件名: {}", filename);

                String extension = getFileExtension(filename);
                File tempFile = File.createTempFile("wechat-media-", extension.isEmpty() ? ".tmp" : "." + extension);
                tempFile.deleteOnExit();

                byte[] mediaBytes = response.getBody();
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(mediaBytes);
                }

                logger.info("成功下载临时素材 {} 到 {}", mediaId, tempFile.getAbsolutePath());
                return Optional.of(new DownloadedMedia(tempFile, filename));
            } else {
                 if (response.getBody() != null) {
                    String errorResponse = new String(response.getBody());
                    logger.error("下载临时素材 {} 失败，响应: {}", mediaId, errorResponse);
                } else {
                    logger.error("下载临时素材 {} 失败，状态码: {}", mediaId, response.getStatusCode());
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("下载临时素材 {} 时发生未知异常", mediaId, e);
            return Optional.empty();
        }
    }

    /**
     * 【最终修正】从 Content-Disposition 头中智能解析文件名的辅助方法
     */
    private String parseFilenameFromDisposition(String disposition) {
        if (disposition == null) {
            return "unknown.tmp";
        }

        // 优先尝试解析 RFC 5987 的 filename*
        Matcher starMatcher = FILENAME_STAR_PATTERN.matcher(disposition);
        if (starMatcher.find()) {
            String value = starMatcher.group(1).trim();
            // 格式是: UTF-8''encoded-name
            String[] parts = value.split("''");
            if (parts.length == 2) {
                try {
                    String charset = parts[0];
                    String encodedName = parts[1];
                    return URLDecoder.decode(encodedName, charset);
                } catch (UnsupportedEncodingException e) {
                    logger.error("不支持的编码格式，解析filename*失败: {}", value, e);
                }
            }
        }

        // 如果失败，回退到解析非标准的 filename
        Matcher plainMatcher = FILENAME_PATTERN.matcher(disposition);
        if (plainMatcher.find()) {
            try {
                String garbledName = plainMatcher.group(1);
                // 尝试修复常见的UTF-8被误解为ISO-8859-1的乱码
                return new String(garbledName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            } catch (Exception e) {
                 logger.warn("尝试修复filename乱码时出错，返回原始值: {}", plainMatcher.group(1));
                 return plainMatcher.group(1);
            }
        }

        return "unknown.tmp";
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == fileName.length() - 1) {
            return ""; 
        }
        return fileName.substring(lastIndex + 1);
    }
}