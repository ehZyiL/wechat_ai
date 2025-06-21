package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import xlike.top.kn_ai_chat.enums.MatchType;

import java.time.LocalDateTime;

/**
 * @author Administrator
 */
@Data
@Entity
public class CustomReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;

    @Column(nullable = false, length = 255)
    private String keyword;

    @Column(nullable = false, length = 2048)
    private String reply;

    @Column(length = 255)
    private String externalUserId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}