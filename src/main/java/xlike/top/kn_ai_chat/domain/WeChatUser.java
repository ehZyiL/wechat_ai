package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data; // 推荐使用 Lombok 来简化代码
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author Administrator
 */
@Data
@NoArgsConstructor
@Entity
public class WeChatUser {
    @Id
    private String externalUserId;

    private String nickname;

    @Column(length = 512) // 头像URL可能较长
    private String avatar;
    
    // info 字段可以保留用于存储API返回的完整JSON信息
    @Column(columnDefinition = "TEXT")
    private String info;

    // 是否被拉黑，默认为 false
    private boolean blocked = false;

    private LocalDateTime lastUpdated;
}