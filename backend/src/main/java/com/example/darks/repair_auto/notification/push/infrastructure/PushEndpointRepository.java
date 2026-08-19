package com.example.darks.repair_auto.notification.push.infrastructure;

import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushEndpointRepository extends JpaRepository<PushEndpoint, Long> {

    @EntityGraph(attributePaths = {"staffUser", "customer", "technician"})
    Optional<PushEndpoint> findByFirebaseAppKeyAndFcmRegistrationToken(
            PushFirebaseApp firebaseAppKey,
            String fcmRegistrationToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PushEndpoint p where p.firebaseAppKey = :firebaseAppKey and p.fcmRegistrationToken = :fcmRegistrationToken")
    Optional<PushEndpoint> findForUpdate(
            @Param("firebaseAppKey") PushFirebaseApp firebaseAppKey,
            @Param("fcmRegistrationToken") String fcmRegistrationToken);

    @EntityGraph(attributePaths = {"staffUser"})
    List<PushEndpoint> findByStaffUserIdAndEnabledTrue(Long staffUserId);

    @EntityGraph(attributePaths = {"customer"})
    List<PushEndpoint> findByCustomerIdAndEnabledTrue(Long customerId);

    @EntityGraph(attributePaths = {"technician"})
    List<PushEndpoint> findByTechnicianIdAndEnabledTrue(Long technicianId);

    @Modifying
    @Query("update PushEndpoint p set p.enabled = false, p.updatedAt = :now where p.staffUser.id = :staffUserId and p.enabled = true")
    int disableAllForStaff(@Param("staffUserId") Long staffUserId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("update PushEndpoint p set p.enabled = false, p.updatedAt = :now where p.customer.id = :customerId and p.enabled = true")
    int disableAllForCustomer(@Param("customerId") Long customerId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("update PushEndpoint p set p.enabled = false, p.updatedAt = :now where p.technician.id = :technicianId and p.enabled = true")
    int disableAllForTechnician(@Param("technicianId") Long technicianId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("update PushEndpoint p set p.enabled = false, p.updatedAt = :now where p.enabled = true and p.lastSeenAt < :threshold")
    int disableStaleEndpoints(@Param("threshold") OffsetDateTime threshold, @Param("now") OffsetDateTime now);

    long countByEnabledTrue();

    long countByEnabledFalse();
}
