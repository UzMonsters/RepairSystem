package com.example.darks.repair_auto.telegram.customer.infrastructure;

import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramCustomerSessionRepository extends JpaRepository<TelegramCustomerSession, Long> {

    Optional<TelegramCustomerSession> findByTelegramUserId(Long telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from TelegramCustomerSession s
            left join fetch s.customer
            left join fetch s.createdRequest
            where s.telegramUserId = :telegramUserId
            """)
    Optional<TelegramCustomerSession> findByTelegramUserIdForUpdate(@Param("telegramUserId") Long telegramUserId);
}
