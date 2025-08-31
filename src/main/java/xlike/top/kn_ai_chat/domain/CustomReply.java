package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import xlike.top.kn_ai_chat.enums.MatchType;

import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "custom_reply")
public class CustomReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType;

    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;

    @Column(name = "reply", nullable = false, length = 2048)
    private String reply;

    @Column(name = "external_user_id", length = 255)
    private String externalUserId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}