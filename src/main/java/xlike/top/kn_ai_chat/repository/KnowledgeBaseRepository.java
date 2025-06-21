package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional; // 导入 @Transactional
import xlike.top.kn_ai_chat.domain.Knowledge;

import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
public interface KnowledgeBaseRepository extends JpaRepository<Knowledge, Long> {

    List<Knowledge> findByExternalUserId(String externalUserId);

    Optional<Knowledge> findByIdAndExternalUserId(Long id, String externalUserId);

    /**
     * 根据用户ID删除其所有知识库条目
     * @param externalUserId 用户的 external_userid
     */
    @Transactional
    void deleteByExternalUserId(String externalUserId);
}