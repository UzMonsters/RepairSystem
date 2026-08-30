package com.example.darks.repair_auto.identity.infrastructure.persistence;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthIdentity;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobileAuthIdentityRepository extends JpaRepository<MobileAuthIdentity, Long> {

    @Query("""
            select i from MobileAuthIdentity i
            where i.actorType = :actorType
              and i.provider = :provider
              and i.providerSubject = :providerSubject
              and i.disabledAt is null
            """)
    Optional<MobileAuthIdentity> findActiveByProvider(
            @Param("actorType") ActorType actorType,
            @Param("provider") MobileAuthProvider provider,
            @Param("providerSubject") String providerSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from MobileAuthIdentity i
            where i.actorType = :actorType
              and i.provider = :provider
              and i.providerSubject = :providerSubject
              and i.disabledAt is null
            """)
    Optional<MobileAuthIdentity> findActiveByProviderForUpdate(
            @Param("actorType") ActorType actorType,
            @Param("provider") MobileAuthProvider provider,
            @Param("providerSubject") String providerSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from MobileAuthIdentity i
            where i.actorType = :actorType
              and i.provider = :provider
              and i.disabledAt is null
              and (
                    (:actorType = com.example.darks.repair_auto.identity.domain.ActorType.CUSTOMER and i.customer.id = :actorId)
                 or (:actorType = com.example.darks.repair_auto.identity.domain.ActorType.TECHNICIAN and i.technician.id = :actorId)
              )
            """)
    Optional<MobileAuthIdentity> findActiveActorProviderForUpdate(
            @Param("actorType") ActorType actorType,
            @Param("actorId") Long actorId,
            @Param("provider") MobileAuthProvider provider);

    @Query("""
            select i from MobileAuthIdentity i
            where i.actorType = :actorType
              and i.disabledAt is null
              and (
                    (:actorType = com.example.darks.repair_auto.identity.domain.ActorType.CUSTOMER and i.customer.id = :actorId)
                 or (:actorType = com.example.darks.repair_auto.identity.domain.ActorType.TECHNICIAN and i.technician.id = :actorId)
              )
            order by i.provider asc
            """)
    List<MobileAuthIdentity> findActiveForActor(
            @Param("actorType") ActorType actorType,
            @Param("actorId") Long actorId);
}
