package com.example.darks.repair_auto.technician.infrastructure;

import com.example.darks.repair_auto.technician.domain.Technician;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicianRepository extends JpaRepository<Technician, Long>, JpaSpecificationExecutor<Technician> {

    Optional<Technician> findByPhone(String phone);

    Optional<Technician> findByTelegramUserId(Long telegramUserId);

    java.util.List<Technician> findByEmailIgnoreCaseAndActiveTrue(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Technician t
            where t.id = :id
            """)
    Optional<Technician> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Technician t
            where t.telegramUserId = :telegramUserId
            """)
    Optional<Technician> findByTelegramUserIdForUpdate(@Param("telegramUserId") Long telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Technician t
            where lower(t.email) = lower(:email)
              and t.active = true
            """)
    java.util.List<Technician> findActiveByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from Technician t
            where t.phone = :phone
            """)
    Optional<Technician> findByPhoneForUpdate(@Param("phone") String phone);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            update Technician t
            set t.authVersion = t.authVersion + 1,
                t.updatedAt = :now
            where t.id = :id
            """)
    int incrementAuthVersion(@Param("id") Long id, @Param("now") java.time.OffsetDateTime now);

    @Override
    Page<Technician> findAll(Specification<Technician> specification, Pageable pageable);
}
