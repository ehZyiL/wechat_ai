package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "keyword_config")
public class KeywordConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @Column(name = "handler_name", nullable = false)
    private String handlerName;

    @Column(name = "keywords", columnDefinition = "TEXT", nullable = false)
    private String keywords;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;
}