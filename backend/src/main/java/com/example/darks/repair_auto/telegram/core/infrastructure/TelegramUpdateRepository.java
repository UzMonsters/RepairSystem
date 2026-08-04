package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.domain.TelegramUpdateRecord;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramUpdateRepository extends JpaRepository<TelegramUpdateRecord, Long> {

    Optional<TelegramUpdateRecord> findByTelegramUpdateId(Long telegramUpdateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from TelegramUpdateRecord u
            where u.telegramUpdateId = :telegramUpdateId
            """)
    Optional<TelegramUpdateRecord> findByTelegramUpdateIdForUpdate(@Param("telegramUpdateId") Long telegramUpdateId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into telegram_updates (
                telegram_update_id,
                status,
                update_type,
                received_at,
                attempt_count,
                created_at,
                updated_at,
                version
            )
            values (:telegramUpdateId, 'RECEIVED', :updateType, :now, 1, :now, :now, 0)
            on conflict (telegram_update_id) do nothing
            """, nativeQuery = true)
    int insertReceivedIfAbsent(
            @Param("telegramUpdateId") Long telegramUpdateId,
            @Param("updateType") String updateType,
            @Param("now") OffsetDateTime now);
}
