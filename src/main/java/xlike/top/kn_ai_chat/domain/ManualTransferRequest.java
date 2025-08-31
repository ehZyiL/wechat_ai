package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "manual_transfer_request")
public class ManualTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_user_id", nullable = false, unique = true)
    private String externalUserId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "external_user_id", referencedColumnName = "external_user_id", insertable = false, updatable = false)
    private WeChatUser user;

    @Column(name = "last_message", nullable = false, columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;
}