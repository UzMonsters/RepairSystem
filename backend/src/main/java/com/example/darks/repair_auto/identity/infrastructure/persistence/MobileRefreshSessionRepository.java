package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.MobileRefreshSession;
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

public interface MobileRefreshSessionRepository extends JpaRepository<MobileRefreshSession, Long> {

    Optional<MobileRefreshSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from MobileRefreshSession s
            where s.tokenHash = :tokenHash
            """)
    Optional<MobileRefreshSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update MobileRefreshSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.tokenFamilyId = :tokenFamilyId
              and s.revokedAt is null
            """)
    int revokeFamily(
            @Param("tokenFamilyId") UUID tokenFamilyId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);

    @Modifying
    @Query("""
            update MobileRefreshSession s
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
            update MobileRefreshSession s
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
            update MobileRefreshSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.customer.id = :customerId
              and (s.mobileSession is null or s.mobileSession.id <> :excludeSessionId)
              and s.revokedAt is null
            """)
    int revokeOtherSessionsForCustomer(
            @Param("customerId") Long customerId,
            @Param("excludeSessionId") UUID excludeSessionId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);

    @Modifying
    @Query("""
            update MobileRefreshSession s
            set s.revokedAt = :now,
                s.revocationReason = :reason,
                s.updatedAt = :now
            where s.technician.id = :technicianId
              and (s.mobileSession is null or s.mobileSession.id <> :excludeSessionId)
              and s.revokedAt is null
            """)
    int revokeOtherSessionsForTechnician(
            @Param("technicianId") Long technicianId,
            @Param("excludeSessionId") UUID excludeSessionId,
            @Param("now") OffsetDateTime now,
            @Param("reason") String reason);

    List<MobileRefreshSession> findByTokenFamilyId(UUID tokenFamilyId);

    List<MobileRefreshSession> findByCustomerId(Long customerId);

    List<MobileRefreshSession> findByTechnicianId(Long technicianId);
}
