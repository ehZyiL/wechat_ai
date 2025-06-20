package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.CustomReply;

import java.util.List;

/**
 * @author Administrator
 */
public interface CustomReplyRepository extends JpaRepository<CustomReply, Long> {

    /**
     * 【修改】查找指定用户的所有规则
     */
    List<CustomReply> findByExternalUserId(String externalUserId);

    /**
     * 【修改】查找所有全局规则
     */
    List<CustomReply> findByExternalUserIdIsNull();

}