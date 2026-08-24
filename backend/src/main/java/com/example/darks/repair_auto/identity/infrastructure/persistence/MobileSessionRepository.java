package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobileSessionRepository extends JpaRepository<MobileSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from MobileSession s
            where s.id = :id
            """)
    Optional<MobileSession> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select s from MobileSession s
            where s.actorType = :actorType
              and s.clientType = :clientType
              and (
                    (:actorType = com.example.darks.repair_auto.identity.domain.ActorType.CUSTOMER and s.customer.id = :actorId)
                 or (:actorType = com.example.darks.repair_auto.identity.domain.ActorType.TECHNICIAN and s.technician.id = :actorId)
              )
            order by s.createdAt desc
            """)
    List<MobileSession> findForActor(
            @Param("actorType") ActorType actorType,
            @Param("actorId") Long actorId,
            @Param("clientType") PushClientType clientType);

    @Modifying
    @Query("""
            update MobileSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.customer.id = :customerId
              and s.revokedAt is null
            """)
    int revokeAllForCustomer(
            @Param("customerId") Long customerId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);

    @Modifying
    @Query("""
            update MobileSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.technician.id = :technicianId
              and s.revokedAt is null
            """)
    int revokeAllForTechnician(
            @Param("technicianId") Long technicianId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);

    @Modifying
    @Query("""
            update MobileSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.customer.id = :customerId
              and s.id <> :excludeSessionId
              and s.revokedAt is null
            """)
    int revokeOtherSessionsForCustomer(
            @Param("customerId") Long customerId,
            @Param("excludeSessionId") UUID excludeSessionId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);

    @Modifying
    @Query("""
            update MobileSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.technician.id = :technicianId
              and s.id <> :excludeSessionId
              and s.revokedAt is null
            """)
    int revokeOtherSessionsForTechnician(
            @Param("technicianId") Long technicianId,
            @Param("excludeSessionId") UUID excludeSessionId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);
}
