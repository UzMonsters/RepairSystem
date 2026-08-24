package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.PhoneOtpChallenge;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhoneOtpChallengeRepository extends JpaRepository<PhoneOtpChallenge, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from PhoneOtpChallenge c
            where c.id = :id
            """)
    Optional<PhoneOtpChallenge> findByIdForUpdate(@Param("id") UUID id);

    Optional<PhoneOtpChallenge> findTopByPhoneAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone,
            OffsetDateTime now);
}
