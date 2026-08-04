package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.RefreshSession;
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

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from RefreshSession s
            join fetch s.user
            where s.tokenHash = :tokenHash
            """)
    Optional<RefreshSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    List<RefreshSession> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshSession s
            set s.revokedAt = :now, s.revocationReason = :reason
            where s.user.id = :userId and s.revokedAt is null
            """)
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now, @Param("reason") String reason);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshSession s
            set s.revokedAt = :now, s.revocationReason = :reason
            where s.tokenFamilyId = :familyId and s.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") OffsetDateTime now, @Param("reason") String reason);
}
