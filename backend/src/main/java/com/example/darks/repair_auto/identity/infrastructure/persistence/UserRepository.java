package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    @Query("""
            select count(u) from User u
            where u.role = 'ADMIN' and u.active = true
            """)
    long countActiveAdmins();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from User u
            where u.id = :id
            """)
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from User u
            where u.role = 'ADMIN' and u.active = true
            order by u.id
            """)
    List<User> findActiveAdminsForUpdate();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
            set u.authVersion = u.authVersion + 1, u.updatedAt = :updatedAt
            where u.id = :id
            """)
    int incrementAuthVersion(@Param("id") Long id, @Param("updatedAt") OffsetDateTime updatedAt);

    @Override
    Page<User> findAll(Specification<User> specification, Pageable pageable);
}
