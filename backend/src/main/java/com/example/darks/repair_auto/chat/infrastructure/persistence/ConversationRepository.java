package com.example.darks.repair_auto.chat.infrastructure.persistence;

import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.chat.domain.ConversationType;
import com.example.darks.repair_auto.identity.domain.ActorType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("select c from Conversation c where c.repairRequest.id = :requestId and c.conversationType = :type")
    Optional<Conversation> findByRepairRequestIdAndConversationType(
            @Param("requestId") Long requestId,
            @Param("type") ConversationType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Long id);

    @Query("select distinct c from Conversation c left join fetch c.participants where c.id = :id")
    Optional<Conversation> findWithParticipantsById(@Param("id") Long id);

    @Query("""
        select c from Conversation c
        join c.participants p
        where p.actorType = :actorType and p.actorId = :actorId and p.leftAt is null
        order by c.updatedAt desc
    """)
    Page<Conversation> findActiveByParticipant(
            @Param("actorType") ActorType actorType,
            @Param("actorId") Long actorId,
            Pageable pageable);

    @Query("select c from Conversation c where c.repairRequest.id = :requestId order by c.createdAt asc")
    List<Conversation> findByRepairRequestId(@Param("requestId") Long requestId);
}
