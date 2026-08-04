package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.domain.TelegramUserContext;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TelegramUserContextRepository extends JpaRepository<TelegramUserContext, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TelegramUserContext> findByTelegramUserId(Long telegramUserId);
}
