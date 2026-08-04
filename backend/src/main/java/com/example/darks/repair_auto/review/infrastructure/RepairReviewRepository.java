package com.example.darks.repair_auto.review.infrastructure;

import com.example.darks.repair_auto.review.domain.RepairReview;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairReviewRepository
        extends JpaRepository<RepairReview, Long>, JpaSpecificationExecutor<RepairReview> {

    boolean existsByRepairRequestId(Long repairRequestId);

    Optional<RepairReview> findByRepairRequestId(Long repairRequestId);

    @EntityGraph(attributePaths = {
            "repairRequest",
            "repairRequest.category",
            "customer",
            "technician"
    })
    @Override
    Page<RepairReview> findAll(Specification<RepairReview> specification, Pageable pageable);

    @EntityGraph(attributePaths = {
            "repairRequest",
            "repairRequest.category",
            "customer",
            "technician"
    })
    @Query("select r from RepairReview r where r.id = :id")
    Optional<RepairReview> findDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "repairRequest",
            "repairRequest.category",
            "customer",
            "technician"
    })
    @Query("select r from RepairReview r where r.repairRequest.id = :requestId and r.customer.id = :customerId")
    Optional<RepairReview> findByRequestIdAndCustomerId(
            @Param("requestId") Long requestId,
            @Param("customerId") Long customerId);
}
