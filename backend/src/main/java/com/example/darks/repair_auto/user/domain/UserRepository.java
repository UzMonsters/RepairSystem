package com.example.darks.repair_auto.user.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
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

    @Override
    Page<User> findAll(Specification<User> specification, Pageable pageable);
}
