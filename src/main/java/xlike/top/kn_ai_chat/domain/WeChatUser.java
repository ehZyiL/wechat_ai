package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author xlike
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "we_chat_user")
public class WeChatUser {
    @Id
    @Column(name = "external_user_id")
    private String externalUserId;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "avatar", length = 512)
    private String avatar;
    
    //存储API返回的完整JSON信息
    @Column(name = "info", columnDefinition = "TEXT")
    private String info;

    // 是否被拉黑，默认为 false
    @Column(name = "blocked")
    private boolean blocked = false;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}