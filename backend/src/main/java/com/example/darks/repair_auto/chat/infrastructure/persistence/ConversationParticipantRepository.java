package com.example.darks.repair_auto.chat.infrastructure.persistence;

import com.example.darks.repair_auto.chat.domain.ConversationParticipant;
import com.example.darks.repair_auto.identity.domain.ActorType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    @Query("select p from ConversationParticipant p where p.conversation.id = :conversationId and p.actorType = :actorType and p.actorId = :actorId")
    Optional<ConversationParticipant> findByConversationIdAndActorTypeAndActorId(
            @Param("conversationId") Long conversationId,
            @Param("actorType") ActorType actorType,
            @Param("actorId") Long actorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ConversationParticipant p where p.conversation.id = :conversationId and p.actorType = :actorType and p.actorId = :actorId")
    Optional<ConversationParticipant> findByConversationIdAndActorTypeAndActorIdForUpdate(
            @Param("conversationId") Long conversationId,
            @Param("actorType") ActorType actorType,
            @Param("actorId") Long actorId);

    @Query("select p from ConversationParticipant p where p.conversation.id = :conversationId order by p.joinedAt asc")
    List<ConversationParticipant> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("select p from ConversationParticipant p where p.conversation.id = :conversationId and p.leftAt is null")
    List<ConversationParticipant> findActiveByConversationId(@Param("conversationId") Long conversationId);
}
