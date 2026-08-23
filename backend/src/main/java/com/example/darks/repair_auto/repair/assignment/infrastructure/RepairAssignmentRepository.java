package com.example.darks.repair_auto.repair.assignment.infrastructure;

import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface RepairAssignmentRepository extends JpaRepository<RepairAssignment, Long> {

    Collection<AssignmentStatus> ACTIVE_STATUSES = List.of(AssignmentStatus.PENDING, AssignmentStatus.ACCEPTED);

    @EntityGraph(attributePaths = {"repairRequest", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.repairRequest.id = :requestId and a.status in :statuses
            """)
    Optional<RepairAssignment> findActiveByRequestId(@Param("requestId") Long requestId,
            @Param("statuses") Collection<AssignmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @EntityGraph(attributePaths = {"repairRequest", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.repairRequest.id = :requestId and a.status in :statuses
            """)
    Optional<RepairAssignment> findActiveByRequestIdForUpdate(
            @Param("requestId") Long requestId,
            @Param("statuses") Collection<AssignmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @EntityGraph(attributePaths = {"repairRequest", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.id = :assignmentId
            """)
    Optional<RepairAssignment> findByIdForUpdate(@Param("assignmentId") Long assignmentId);

    @EntityGraph(attributePaths = {"repairRequest", "technician", "assignedByUser"})
    List<RepairAssignment> findByRepairRequestIdOrderByCreatedAtDesc(Long requestId);

    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.technician.id = :technicianId and a.status = :status
            order by a.createdAt desc
            """)
    List<RepairAssignment> findByTechnicianIdAndStatusOrderByCreatedAtDesc(
            @Param("technicianId") Long technicianId,
            @Param("status") AssignmentStatus status);

    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.technician.id = :technicianId and a.status in :statuses
            order by a.createdAt desc
            """)
    List<RepairAssignment> findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
            @Param("technicianId") Long technicianId,
            @Param("statuses") Collection<AssignmentStatus> statuses);

    long countByTechnicianIdAndStatusIn(Long technicianId, Collection<AssignmentStatus> statuses);

    long countByTechnicianIdAndStatus(Long technicianId, AssignmentStatus status);

    @EntityGraph(attributePaths = {"repairRequest", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.repairRequest.id = :requestId and a.status = :status
            order by a.closedAt desc, a.id desc
            """)
    Optional<RepairAssignment> findLatestCompletedByRequestId(
            @Param("requestId") Long requestId,
            @Param("status") AssignmentStatus status);

    @EntityGraph(attributePaths = {"repairRequest", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.repairRequest.id = :requestId
              and a.technician.id = :technicianId
              and a.status in :statuses
            order by a.createdAt desc
            """)
    List<RepairAssignment> findByRepairRequestIdAndTechnicianIdAndStatusInOrderByCreatedAtDesc(
            @Param("requestId") Long requestId,
            @Param("technicianId") Long technicianId,
            @Param("statuses") Collection<AssignmentStatus> statuses);

    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category", "technician", "assignedByUser"})
    @Query("""
            select a from RepairAssignment a
            where a.repairRequest.id = :requestId
              and a.technician.id = :technicianId
            order by a.createdAt desc
            """)
    List<RepairAssignment> findByRepairRequestIdAndTechnicianIdOrderByCreatedAtDesc(
            @Param("requestId") Long requestId,
            @Param("technicianId") Long technicianId);

    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category", "technician"})
    @Query(value = """
            select a from RepairAssignment a
            where a.technician.id = :technicianId and a.status in :statuses
            """,
            countQuery = """
            select count(a) from RepairAssignment a
            where a.technician.id = :technicianId and a.status in :statuses
            """)
    Page<RepairAssignment> findJobsByTechnicianIdAndStatusIn(
            @Param("technicianId") Long technicianId,
            @Param("statuses") Collection<AssignmentStatus> statuses,
            Pageable pageable);

    @EntityGraph(attributePaths = {"repairRequest", "repairRequest.customer", "repairRequest.category", "technician"})
    @Query("""
            select a from RepairAssignment a
            where a.technician.id = :technicianId
              and a.status in :statuses
              and a.scheduledVisitAt >= :from
              and a.scheduledVisitAt <= :to
            order by a.scheduledVisitAt asc
            """)
    List<RepairAssignment> findSchedule(
            @Param("technicianId") Long technicianId,
            @Param("statuses") Collection<AssignmentStatus> statuses,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to);
}
