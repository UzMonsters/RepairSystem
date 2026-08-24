package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.EmailVerificationChallenge;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from EmailVerificationChallenge c
            where c.id = :id
            """)
    Optional<EmailVerificationChallenge> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select c from EmailVerificationChallenge c
            where c.customer.id = :customerId
              and c.consumedAt is null
              and c.expiresAt > :now
            order by c.createdAt desc
            limit 1
            """)
    Optional<EmailVerificationChallenge> findTopActiveByCustomer(
            @Param("customerId") Long customerId,
            @Param("now") java.time.OffsetDateTime now);

    @Query("""
            select c from EmailVerificationChallenge c
            where c.technician.id = :technicianId
              and c.consumedAt is null
              and c.expiresAt > :now
            order by c.createdAt desc
            limit 1
            """)
    Optional<EmailVerificationChallenge> findTopActiveByTechnician(
            @Param("technicianId") Long technicianId,
            @Param("now") java.time.OffsetDateTime now);
}
