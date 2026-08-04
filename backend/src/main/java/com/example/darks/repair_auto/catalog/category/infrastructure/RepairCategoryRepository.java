package com.example.darks.repair_auto.catalog.category.infrastructure;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import java.util.Collection;
import java.util.List;
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

public interface RepairCategoryRepository
        extends JpaRepository<RepairCategory, Long>, JpaSpecificationExecutor<RepairCategory> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from RepairCategory c
            where c.id = :id
            """)
    Optional<RepairCategory> findByIdForUpdate(@Param("id") Long id);

    List<RepairCategory> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from RepairCategory c
            where c.id in :ids
            order by c.id
            """)
    List<RepairCategory> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Override
    Page<RepairCategory> findAll(Specification<RepairCategory> specification, Pageable pageable);
}
