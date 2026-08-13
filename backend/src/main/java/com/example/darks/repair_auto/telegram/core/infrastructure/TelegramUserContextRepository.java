package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.domain.TelegramUserContext;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramUserContextRepository extends JpaRepository<TelegramUserContext, Long> {

    Optional<TelegramUserContext> findByTelegramUserId(Long telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from TelegramUserContext c
            where c.telegramUserId = :telegramUserId
            """)
    Optional<TelegramUserContext> findByTelegramUserIdForUpdate(@Param("telegramUserId") Long telegramUserId);
}
