package com.example.darks.repair_auto.chat.infrastructure.persistence;

import com.example.darks.repair_auto.chat.domain.ChatMessage;
import com.example.darks.repair_auto.identity.domain.ActorType;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m where m.conversation.id = :conversationId and m.senderType = :senderType and m.senderId = :senderId and m.clientMessageId = :clientMessageId")
    Optional<ChatMessage> findByIdempotencyKey(
            @Param("conversationId") Long conversationId,
            @Param("senderType") ActorType senderType,
            @Param("senderId") Long senderId,
            @Param("clientMessageId") String clientMessageId);

    @Query("""
        select m from ChatMessage m
        join fetch m.conversation c
        left join fetch c.repairRequest
        where m.id = :messageId
    """)
    Optional<ChatMessage> findByIdWithConversationAndRepairRequest(@Param("messageId") Long messageId);

    @Query("select m from ChatMessage m where m.conversation.id = :conversationId order by m.id desc")
    Page<ChatMessage> findByConversationIdOrderByIdDesc(
            @Param("conversationId") Long conversationId,
            Pageable pageable);

    @Query("select m from ChatMessage m where m.conversation.id = :conversationId and m.id < :beforeId order by m.id desc")
    Page<ChatMessage> findByConversationIdAndIdLessThanOrderByIdDesc(
            @Param("conversationId") Long conversationId,
            @Param("beforeId") Long beforeId,
            Pageable pageable);

    @Query("""
        select m from ChatMessage m
        where m.conversation.id = :conversationId and m.createdAt <= :accessibleUntil
        order by m.id desc
    """)
    Page<ChatMessage> findByConversationIdAndCreatedAtLessThanEqualOrderByIdDesc(
            @Param("conversationId") Long conversationId,
            @Param("accessibleUntil") OffsetDateTime accessibleUntil,
            Pageable pageable);

    @Query("""
        select m from ChatMessage m
        where m.conversation.id = :conversationId
          and m.id < :beforeId
          and m.createdAt <= :accessibleUntil
        order by m.id desc
    """)
    Page<ChatMessage> findByConversationIdAndIdLessThanAndCreatedAtLessThanEqualOrderByIdDesc(
            @Param("conversationId") Long conversationId,
            @Param("beforeId") Long beforeId,
            @Param("accessibleUntil") OffsetDateTime accessibleUntil,
            Pageable pageable);

    @Query("select count(m) from ChatMessage m where m.conversation.id = :conversationId and m.id > :lastReadId")
    long countUnreadMessages(
            @Param("conversationId") Long conversationId,
            @Param("lastReadId") Long lastReadId);

    @Query("select count(m) from ChatMessage m where m.conversation.id = :conversationId")
    long countTotalMessages(@Param("conversationId") Long conversationId);

    @Query("""
        select count(m) from ChatMessage m
        where m.conversation.id = :conversationId and m.id > :lastReadId and m.createdAt <= :accessibleUntil
    """)
    long countUnreadMessagesCreatedAtOnOrBefore(
            @Param("conversationId") Long conversationId,
            @Param("lastReadId") Long lastReadId,
            @Param("accessibleUntil") OffsetDateTime accessibleUntil);

    @Query("""
        select count(m) from ChatMessage m
        where m.conversation.id = :conversationId and m.createdAt <= :accessibleUntil
    """)
    long countTotalMessagesCreatedAtOnOrBefore(
            @Param("conversationId") Long conversationId,
            @Param("accessibleUntil") OffsetDateTime accessibleUntil);

    @Query("select m from ChatMessage m where m.conversation.id = :conversationId and m.id = (select max(m2.id) from ChatMessage m2 where m2.conversation.id = :conversationId)")
    Optional<ChatMessage> findLatestMessage(@Param("conversationId") Long conversationId);
}
