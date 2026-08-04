package com.example.darks.repair_auto.repair.execution.infrastructure;

import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepairExecutionRepository extends JpaRepository<RepairExecution, Long> {

    @EntityGraph(attributePaths = {
            "repairRequest",
            "startedByUser",
            "diagnosisUpdatedByUser",
            "completedByUser",
            "cancelledByUser"
    })
    Optional<RepairExecution> findByRepairRequestId(Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "repairRequest",
            "startedByUser",
            "diagnosisUpdatedByUser",
            "completedByUser",
            "cancelledByUser"
    })
    @Query("""
            select e from RepairExecution e
            where e.repairRequest.id = :requestId
            """)
    Optional<RepairExecution> findByRepairRequestIdForUpdate(@Param("requestId") Long requestId);
}
