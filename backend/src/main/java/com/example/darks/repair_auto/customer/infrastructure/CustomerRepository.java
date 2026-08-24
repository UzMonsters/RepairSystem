package com.example.darks.repair_auto.customer.infrastructure;

import com.example.darks.repair_auto.customer.domain.Customer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByTelegramUserId(Long telegramUserId);

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Customer c
            where c.phone = :phone
            """)
    Optional<Customer> findByPhoneForUpdate(@Param("phone") String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Customer c
            where c.telegramUserId = :telegramUserId
            """)
    Optional<Customer> findByTelegramUserIdForUpdate(@Param("telegramUserId") Long telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Customer c
            where c.id = :id
            """)
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            update Customer c
            set c.authVersion = c.authVersion + 1,
                c.updatedAt = :now
            where c.id = :id
            """)
    int incrementAuthVersion(@Param("id") Long id, @Param("now") java.time.OffsetDateTime now);

    @Override
    Page<Customer> findAll(Specification<Customer> specification, Pageable pageable);
}
