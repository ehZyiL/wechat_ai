package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@Entity
@Table(name = "message_log")
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 【新增】微信消息的唯一ID，用于持久化幂等性判断
     */
    @Column(unique = true, nullable = true)
    private String msgId;

    private String fromUser;
    private String toUser;
    private String msgType;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;
}