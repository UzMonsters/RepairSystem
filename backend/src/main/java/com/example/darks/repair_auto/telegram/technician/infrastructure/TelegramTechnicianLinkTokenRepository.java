package com.example.darks.repair_auto.telegram.technician.infrastructure;

import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianLinkToken;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TelegramTechnicianLinkTokenRepository extends JpaRepository<TelegramTechnicianLinkToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"technician", "createdByUser"})
    Optional<TelegramTechnicianLinkToken> findByTokenHash(String tokenHash);

    Optional<TelegramTechnicianLinkToken> findFirstByTechnicianIdAndUsedAtIsNullAndRevokedAtIsNull(Long technicianId);
}
