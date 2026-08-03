package com.example.darks.repair_auto.repair.request.infrastructure;

import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairRequestRepository
        extends JpaRepository<RepairRequest, Long>, JpaSpecificationExecutor<RepairRequest> {

    @EntityGraph(attributePaths = {"customer", "category", "createdByUser"})
    @Override
    Page<RepairRequest> findAll(Specification<RepairRequest> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "category", "createdByUser"})
    Optional<RepairRequest> findWithRelationsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from RepairRequest r
            join fetch r.customer
            join fetch r.category
            left join fetch r.createdByUser
            where r.id = :id
            """)
    Optional<RepairRequest> findByIdForUpdate(@Param("id") Long id);

    Optional<RepairRequest> findBySourceReference(String sourceReference);

    @EntityGraph(attributePaths = {"customer", "category"})
    @Query("""
            select r from RepairRequest r
            where r.customer.id = :customerId
              and r.status = com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus.COMPLETED
              and not exists (
                  select review.id from RepairReview review
                  where review.repairRequest.id = r.id
              )
            order by r.updatedAt desc, r.id desc
            """)
    List<RepairRequest> findCompletedUnreviewedForCustomer(
            @Param("customerId") Long customerId,
            Pageable pageable);
}
