package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.WeChatKfAccount;

/**
 * 微信客服账号数据仓库
 * @author xlike
 */
public interface WeChatKfAccountRepository extends JpaRepository<WeChatKfAccount, String> {
}