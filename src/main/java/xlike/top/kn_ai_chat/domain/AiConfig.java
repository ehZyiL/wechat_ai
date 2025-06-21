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
    private String externalUserId;

    @Column(nullable = false)
    private String aiBaseUrl;

    @Column(nullable = false)
    private String aiApiKey;

    @Column(nullable = false)
    private String aiModel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;

    @Column(nullable = false)
    private String sfBaseUrl;

    @Column(nullable = false)
    private String sfImageModel;

    @Column(nullable = false)
    private String sfTtsModel;

    @Column(nullable = false)
    private String sfSttModel;

    @Column(nullable = false)
    private String sfVoice;

    @Column(nullable = false)
    private String sfVlmModel;

    @Column(nullable = false)
    private LocalDateTime lastModified;
}