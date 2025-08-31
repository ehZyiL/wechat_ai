package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "ai_config")
public class AiConfig {

    @Id
    @Column(name = "external_user_id")
    private String externalUserId;

    @Column(name = "ai_base_url", nullable = false)
    private String aiBaseUrl;

    @Column(name = "ai_api_key", nullable = false)
    private String aiApiKey;

    @Column(name = "ai_model", nullable = false)
    private String aiModel;

    @Column(name = "system_prompt", columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    @Column(name = "sf_base_url", nullable = false)
    private String sfBaseUrl;

    @Column(name = "sf_image_model", nullable = false)
    private String sfImageModel;

    @Column(name = "sf_tts_model", nullable = false)
    private String sfTtsModel;

    @Column(name = "sf_stt_model", nullable = false)
    private String sfSttModel;

    @Column(name = "sf_voice", nullable = false)
    private String sfVoice;

    @Column(name = "sf_vlm_model", nullable = false)
    private String sfVlmModel;

    @Column(name = "rag_enabled", nullable = false)
    private boolean ragEnabled = false;

    // 新增Rag字段
    @Column(name = "rag_model")
    private String ragModel;

    @Column(name = "rag_base_url")
    private String ragBaseUrl;

    @Column(name = "rag_api_key")
    private String ragApiKey;


    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;
}