package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库条目实体类
 * <p>
 * 用于存储用户上传的文件内容，实现用户隔离的知识库。
 * @author xlike
 */
@Data
@Entity
@Table(name = "knowledge_base")
public class Knowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的微信用户的 external_userid，用于数据隔离
     */
    @Column(nullable = false, updatable = false)
    private String externalUserId;

    /**
     * 上传时的原始文件名
     */
    @Column(nullable = false)
    private String fileName;

    /**
     * 从文件中提取的纯文本内容
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 记录创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}