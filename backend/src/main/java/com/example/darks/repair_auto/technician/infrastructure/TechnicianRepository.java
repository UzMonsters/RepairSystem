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

    @Override
    Page<Technician> findAll(Specification<Technician> specification, Pageable pageable);
}
