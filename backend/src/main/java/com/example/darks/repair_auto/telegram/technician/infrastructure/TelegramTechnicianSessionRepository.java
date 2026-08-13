package com.example.darks.repair_auto.telegram.technician.infrastructure;

import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSession;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramTechnicianSessionRepository extends JpaRepository<TelegramTechnicianSession, Long> {

    @EntityGraph(attributePaths = "technician")
    Optional<TelegramTechnicianSession> findByTelegramUserId(Long telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from TelegramTechnicianSession s
            left join fetch s.technician
            where s.telegramUserId = :telegramUserId
            """)
    Optional<TelegramTechnicianSession> findByTelegramUserIdForUpdate(@Param("telegramUserId") Long telegramUserId);

    @Query("""
            select s from TelegramTechnicianSession s
            where s.technician.id = :technicianId
            """)
    List<TelegramTechnicianSession> findByTechnicianId(@Param("technicianId") Long technicianId);
}
