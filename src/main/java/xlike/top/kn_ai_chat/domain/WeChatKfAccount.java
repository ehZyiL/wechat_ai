package xlike.top.kn_ai_chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 微信客服账号实体类
 * 用于存储从API获取的客服账号信息。
 */
@Data
@Entity
@Table(name = "wechat_kf_account")
public class WeChatKfAccount {

    /**
     * 客服账号ID (open_kfid)，作为主键。
     */
    @Id
    @Column(name = "open_kfid")
    private String openKfid;

    /**
     * 客服账号名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 客服账号头像URL
     */
    @Column(nullable = false, length = 512)
    private String avatar;
}