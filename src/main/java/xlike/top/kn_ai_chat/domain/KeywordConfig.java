package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @author Administrator
 */
@Data
@Entity
@Table(name = "keyword_config")
public class KeywordConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String externalUserId;

    @Column(nullable = false)
    private String handlerName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String keywords;

    @Column(nullable = false)
    private LocalDateTime lastModified;
}