package com.example.darks.repair_auto.repair.execution.infrastructure;

import com.example.darks.repair_auto.repair.execution.domain.RepairRequestStatusHistory;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairRequestStatusHistoryRepository extends JpaRepository<RepairRequestStatusHistory, Long> {

    @EntityGraph(attributePaths = {"repairRequest", "changedByUser"})
    List<RepairRequestStatusHistory> findByRepairRequestIdOrderByChangedAtDescIdDesc(Long requestId);

    @EntityGraph(attributePaths = {"repairRequest"})
    List<RepairRequestStatusHistory> findByRepairRequestIdOrderByChangedAtAscIdAsc(Long requestId);

    long countByRepairRequestIdAndToStatus(Long requestId, RepairRequestStatus status);
}
