package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.ManualTransferRequest;
import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
public interface ManualTransferRepository extends JpaRepository<ManualTransferRequest, Long> {
    List<ManualTransferRequest> findByResolvedFalseOrderByRequestTimeDesc();
    Optional<ManualTransferRequest> findByExternalUserId(String externalUserId);
}