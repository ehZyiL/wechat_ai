package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.UserMcpPermission;
import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
public interface UserMcpPermissionRepository extends JpaRepository<UserMcpPermission, Long> {

    // 检查特定用户对特定MCP连接的权限
    boolean existsByExternalUserIdAndMcpConfigName(String externalUserId, String mcpConfigName);

    // 查找用户的全部权限
    List<UserMcpPermission> findByExternalUserId(String externalUserId);

    // 查找特定权限记录，用于撤销
    Optional<UserMcpPermission> findByExternalUserIdAndMcpConfigId(String externalUserId, Long mcpConfigId);
    
    // 根据用户和配置ID删除权限
    @Transactional
    void deleteByExternalUserIdAndMcpConfigId(String externalUserId, Long mcpConfigId);

    // 根据 MCP 配置 ID 查找所有权限记录
    List<UserMcpPermission> findByMcpConfigId(Long mcpConfigId);

    // 根据 MCP 配置 ID 批量删除权限记录，并返回删除的数量
    @Transactional
    long deleteByMcpConfigId(Long mcpConfigId);
}