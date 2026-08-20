package com.example.darks.repair_auto.chat.application;

import com.example.darks.repair_auto.chat.api.dto.ChatMapper;
import com.example.darks.repair_auto.chat.api.dto.ChatMessageResponse;
import com.example.darks.repair_auto.chat.api.dto.ConversationDetailResponse;
import com.example.darks.repair_auto.chat.api.dto.ConversationSummaryResponse;
import com.example.darks.repair_auto.chat.api.dto.ParticipantSummaryResponse;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.domain.ChatMessage;
import com.example.darks.repair_auto.chat.domain.ChatMessageType;
import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.chat.domain.ConversationParticipant;
import com.example.darks.repair_auto.chat.domain.ConversationType;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ChatMessageRepository;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ConversationParticipantRepository;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ConversationRepository;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.realtime.delivery.RealtimeEventPublisher;
import com.example.darks.repair_auto.realtime.event.RealtimeEvent;
import com.example.darks.repair_auto.realtime.event.RealtimeEventType;
import com.example.darks.repair_auto.realtime.event.application.ChatMessageCreatedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.ChatMessageReadDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.ParticipantRecipient;
import com.example.darks.repair_auto.realtime.event.dto.ChatReadPayload;
import com.example.darks.repair_auto.realtime.event.dto.ChatTypingPayload;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.access.application.RepairResourceAccessPolicy;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final int MAX_CLIENT_MESSAGE_ID_LENGTH = 64;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final RepairRequestRepository repairRequestRepository;
    private final RepairAssignmentRepository assignmentRepository;
    private final RepairAttachmentRepository attachmentRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final RepairResourceAccessPolicy repairResourceAccessPolicy;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final TypingThrottleService typingThrottleService;
    private final Clock clock;

    @Autowired
    public ChatService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            ChatMessageRepository messageRepository,
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository assignmentRepository,
            RepairAttachmentRepository attachmentRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            RepairResourceAccessPolicy repairResourceAccessPolicy,
            ApplicationEventPublisher applicationEventPublisher,
            RealtimeEventPublisher realtimeEventPublisher,
            TypingThrottleService typingThrottleService) {
        this(
                conversationRepository,
                participantRepository,
                messageRepository,
                repairRequestRepository,
                assignmentRepository,
                attachmentRepository,
                customerRepository,
                technicianRepository,
                userRepository,
                repairResourceAccessPolicy,
                applicationEventPublisher,
                realtimeEventPublisher,
                typingThrottleService,
                Clock.systemUTC());
    }

    public ChatService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            ChatMessageRepository messageRepository,
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository assignmentRepository,
            RepairAttachmentRepository attachmentRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            RepairResourceAccessPolicy repairResourceAccessPolicy,
            ApplicationEventPublisher applicationEventPublisher,
            RealtimeEventPublisher realtimeEventPublisher,
            TypingThrottleService typingThrottleService,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.assignmentRepository = assignmentRepository;
        this.attachmentRepository = attachmentRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.repairResourceAccessPolicy = repairResourceAccessPolicy;
        this.applicationEventPublisher = applicationEventPublisher;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.typingThrottleService = typingThrottleService;
        this.clock = clock;
    }

    @Transactional
    public Conversation getOrCreateCustomerTechnicianConversation(Long requestId) {
        RepairRequest request = repairRequestRepository.findWithRelationsById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Repair request was not found."));

        Optional<Conversation> existing = conversationRepository
                .findByRepairRequestIdAndConversationType(requestId, ConversationType.CUSTOMER_TECHNICIAN);

        OffsetDateTime now = now();
        RepairAssignment activeAssignment = assignmentRepository
                .findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElse(null);

        if (existing.isPresent()) {
            Conversation conv = existing.get();
            syncCustomerTechnicianParticipants(conv, request, activeAssignment, now);
            return conv;
        }

        Conversation conv = new Conversation(request, ConversationType.CUSTOMER_TECHNICIAN, now);
        conv = conversationRepository.saveAndFlush(conv);

        // Add customer participant
        ConversationParticipant customerParticipant = new ConversationParticipant(
                conv,
                ActorType.CUSTOMER,
                request.getCustomer().getId(),
                "CUSTOMER",
                now);
        participantRepository.saveAndFlush(customerParticipant);

        // Add technician participant if assigned
        if (activeAssignment != null && activeAssignment.getTechnician() != null) {
            ConversationParticipant techParticipant = new ConversationParticipant(
                    conv,
                    ActorType.TECHNICIAN,
                    activeAssignment.getTechnician().getId(),
                    "TECHNICIAN",
                    now);
            participantRepository.saveAndFlush(techParticipant);
        }

        return conv;
    }

    @Transactional
    public Conversation getOrCreateCustomerTechnicianConversationForMobileActor(
            Long requestId,
            ActorType actorType,
            Long actorId) {
        if (actorType == ActorType.CUSTOMER) {
            repairResourceAccessPolicy.requireCustomerCanReadRequest(actorId, requestId);
        } else if (actorType == ActorType.TECHNICIAN) {
            repairResourceAccessPolicy.requireTechnicianCurrentAssignment(actorId, requestId);
        } else {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return getOrCreateCustomerTechnicianConversation(requestId);
    }

    @Transactional
    public Conversation getOrCreateTechnicianManagerConversation(Long requestId, Long staffUserId) {
        RepairRequest request = repairRequestRepository.findWithRelationsById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Repair request was not found."));

        Optional<Conversation> existing = conversationRepository
                .findByRepairRequestIdAndConversationType(requestId, ConversationType.TECHNICIAN_MANAGER);

        OffsetDateTime now = now();
        RepairAssignment activeAssignment = assignmentRepository
                .findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElse(null);

        if (existing.isPresent()) {
            Conversation conv = existing.get();
            syncTechnicianManagerParticipants(conv, request, activeAssignment, staffUserId, now);
            return conv;
        }

        Conversation conv = new Conversation(request, ConversationType.TECHNICIAN_MANAGER, now);
        conv = conversationRepository.saveAndFlush(conv);

        if (activeAssignment != null && activeAssignment.getTechnician() != null) {
            ConversationParticipant techParticipant = new ConversationParticipant(
                    conv,
                    ActorType.TECHNICIAN,
                    activeAssignment.getTechnician().getId(),
                    "TECHNICIAN",
                    now);
            participantRepository.saveAndFlush(techParticipant);
        }

        if (staffUserId != null) {
            ConversationParticipant staffParticipant = new ConversationParticipant(
                    conv,
                    ActorType.STAFF,
                    staffUserId,
                    "MANAGER",
                    now);
            participantRepository.saveAndFlush(staffParticipant);
        }

        return conv;
    }

    @Transactional
    public ChatMessageResponse sendMessage(
            SendMessageRequest request,
            ActorType senderType,
            Long senderId) {
        validateSendMessageRequest(request);

        OffsetDateTime now = now();
        Conversation conversation = conversationRepository.findByIdForUpdate(request.conversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation was not found."));

        if (!conversation.isActive()) {
            throw new BusinessRuleException(
                    "CONVERSATION_CLOSED",
                    "Conversation is closed.",
                    409);
        }

        // Lifecycle rule: terminal requests are read-only
        if (conversation.getRepairRequest() != null) {
            RepairRequest repairRequest = conversation.getRepairRequest();
            if (repairRequest.getStatus() == RepairRequestStatus.COMPLETED
                    || repairRequest.getStatus() == RepairRequestStatus.CANCELLED) {
                throw new BusinessRuleException(
                        "CONVERSATION_READ_ONLY",
                        "Conversation is read-only because the repair request is completed or cancelled.",
                        409);
            }
        }

        // Verify active participant
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndActorTypeAndActorIdForUpdate(conversation.getId(), senderType, senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        if (!participant.isActive()) {
            throw new BusinessRuleException(
                    "PARTICIPANT_INACTIVE",
                    "You are no longer an active participant in this conversation.",
                    403);
        }

        // Idempotency check
        Optional<ChatMessage> existing = messageRepository.findByIdempotencyKey(
                conversation.getId(),
                senderType,
                senderId,
                request.clientMessageId().trim());
        if (existing.isPresent()) {
            return ChatMapper.toResponse(existing.get());
        }

        // Validate attachment if applicable
        Long attachmentId = request.attachmentId();
        if (request.type() == ChatMessageType.IMAGE || request.type() == ChatMessageType.FILE) {
            if (attachmentId == null) {
                throw new BusinessRuleException(
                        "ATTACHMENT_REQUIRED",
                        "Attachment ID is required for image and file messages.",
                        400);
            }
            validateAttachmentForChat(attachmentId, conversation, senderType, senderId);
        }

        // Validate replyToMessage if present
        Long replyToId = request.replyToMessageId();
        if (replyToId != null) {
            ChatMessage replyTo = messageRepository.findById(replyToId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "REPLY_TO_MESSAGE_NOT_FOUND",
                            "Replied message was not found.",
                            404));
            if (!replyTo.getConversation().getId().equals(conversation.getId())) {
                throw new BusinessRuleException(
                        "INVALID_REPLY_TO_MESSAGE",
                        "Replied message does not belong to this conversation.",
                        400);
            }
        }

        ChatMessage message = new ChatMessage(
                conversation,
                senderType,
                senderId,
                request.clientMessageId().trim(),
                request.type(),
                request.text() != null ? request.text().trim() : null,
                attachmentId,
                replyToId,
                now);

        ChatMessage savedMessage = messageRepository.saveAndFlush(message);
        conversation.touch(now);
        conversationRepository.saveAndFlush(conversation);

        // Advance sender's last read message id
        participant.advanceReadState(savedMessage.getId(), now);
        participantRepository.saveAndFlush(participant);

        List<ConversationParticipant> activeParticipants = participantRepository
                .findActiveByConversationId(conversation.getId());
        List<ParticipantRecipient> recipients = activeParticipants.stream()
                .filter(p -> !(p.getActorType() == senderType && p.getActorId().equals(senderId)))
                .map(p -> new ParticipantRecipient(p.getActorType(), p.getActorId()))
                .toList();

        // Publish domain event
        applicationEventPublisher.publishEvent(new ChatMessageCreatedDomainEvent(
                conversation.getId(),
                savedMessage.getId(),
                senderType,
                senderId,
                recipients,
                ChatMapper.toPayload(savedMessage)));

        return ChatMapper.toResponse(savedMessage);
    }

    @Transactional
    public void markAsRead(
            Long conversationId,
            Long messageId,
            ActorType readerType,
            Long readerId) {
        if (conversationId == null || messageId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        OffsetDateTime now = now();
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndActorTypeAndActorIdForUpdate(conversationId, readerType, readerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));

        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message was not found."));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new BusinessRuleException(
                    "INVALID_MESSAGE_CONVERSATION",
                    "Message does not belong to this conversation.",
                    400);
        }
        if (!canParticipantAccessMessage(participant, message)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        boolean advanced = participant.advanceReadState(messageId, now);
        if (advanced) {
            participantRepository.saveAndFlush(participant);

            List<ConversationParticipant> activeParticipants = participantRepository
                    .findActiveByConversationId(conversationId);
            List<ParticipantRecipient> recipients = activeParticipants.stream()
                    .filter(p -> !(p.getActorType() == readerType && p.getActorId().equals(readerId)))
                    .map(p -> new ParticipantRecipient(p.getActorType(), p.getActorId()))
                    .toList();

            ChatReadPayload payload = new ChatReadPayload(
                    conversationId,
                    messageId,
                    readerType.name(),
                    readerId,
                    now.toInstant());

            applicationEventPublisher.publishEvent(new ChatMessageReadDomainEvent(
                    conversationId,
                    messageId,
                    readerType,
                    readerId,
                    recipients,
                    payload));
        }
    }

    public void handleTyping(
            Long conversationId,
            boolean typing,
            ActorType actorType,
            Long actorId) {
        if (conversationId == null) {
            return;
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndActorTypeAndActorId(conversationId, actorType, actorId)
                .orElse(null);

        if (participant == null || !participant.isActive()) {
            return;
        }

        if (!typingThrottleService.canSendTyping(conversationId, actorType, actorId)) {
            return;
        }

        List<ConversationParticipant> activeParticipants = participantRepository
                .findActiveByConversationId(conversationId);

        RealtimeEventType eventType = typing
                ? RealtimeEventType.CHAT_TYPING_STARTED
                : RealtimeEventType.CHAT_TYPING_STOPPED;

        ChatTypingPayload payload = new ChatTypingPayload(
                conversationId,
                actorType.name(),
                actorId,
                typing);
        RealtimeEvent<ChatTypingPayload> rtEvent = RealtimeEvent.of(eventType, payload);

        for (ConversationParticipant p : activeParticipants) {
            if (p.getActorType() == actorType && p.getActorId().equals(actorId)) {
                continue;
            }
            if (p.getActorType() == ActorType.STAFF) {
                realtimeEventPublisher.publishToUser(ActorType.STAFF, p.getActorId(), rtEvent);
            } else {
                realtimeEventPublisher.publishToUser(p.getActorType(), p.getActorId(), rtEvent);
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> listConversationsForActor(
            ActorType actorType,
            Long actorId,
            Pageable pageable) {
        Page<Conversation> page = conversationRepository.findActiveByParticipant(actorType, actorId, pageable);

        List<ConversationSummaryResponse> summaries = page.getContent().stream().map(conv -> {
            ConversationParticipant currentParticipant = participantRepository
                    .findByConversationIdAndActorTypeAndActorId(conv.getId(), actorType, actorId)
                    .orElse(null);

            Long lastReadId = currentParticipant != null ? currentParticipant.getLastReadMessageId() : null;
            long unread = lastReadId == null
                    ? messageRepository.countTotalMessages(conv.getId())
                    : messageRepository.countUnreadMessages(conv.getId(), lastReadId);

            ChatMessage lastMessage = messageRepository.findLatestMessage(conv.getId()).orElse(null);
            List<ParticipantSummaryResponse> participants = loadParticipantSummaries(conv.getId());

            return ChatMapper.toSummary(conv, unread, lastMessage, participants);
        }).toList();

        return PageResponse.from(page, summaries);
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetails(
            Long conversationId,
            ActorType actorType,
            Long actorId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation was not found."));

        if (actorType != ActorType.STAFF) {
            ConversationParticipant participant = participantRepository
                    .findByConversationIdAndActorTypeAndActorId(conversationId, actorType, actorId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        }

        ConversationParticipant currentParticipant = participantRepository
                .findByConversationIdAndActorTypeAndActorId(conversationId, actorType, actorId)
                .orElse(null);

        OffsetDateTime accessibleUntil = accessibleUntil(currentParticipant);
        Long lastReadId = currentParticipant != null ? currentParticipant.getLastReadMessageId() : null;
        long unread = unreadCount(conv.getId(), lastReadId, accessibleUntil);

        List<ParticipantSummaryResponse> participants = loadParticipantSummaries(conv.getId());
        return ChatMapper.toDetails(conv, unread, participants);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMessageHistory(
            Long conversationId,
            Long beforeId,
            Pageable pageable,
            ActorType actorType,
            Long actorId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation was not found."));

        ConversationParticipant participant = null;
        if (actorType != ActorType.STAFF) {
            participant = participantRepository
                    .findByConversationIdAndActorTypeAndActorId(conversationId, actorType, actorId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
        }

        OffsetDateTime accessibleUntil = accessibleUntil(participant);
        Page<ChatMessage> page = messageHistoryPage(conversationId, beforeId, accessibleUntil, pageable);

        return PageResponse.from(page.map(ChatMapper::toResponse));
    }

    @Transactional
    public void handleTechnicianReassigned(Long requestId, Long oldTechnicianId, Long newTechnicianId) {
        OffsetDateTime now = now();
        List<Conversation> conversations = conversationRepository.findByRepairRequestId(requestId);
        for (Conversation conv : conversations) {
            if (oldTechnicianId != null) {
                participantRepository
                        .findByConversationIdAndActorTypeAndActorIdForUpdate(conv.getId(), ActorType.TECHNICIAN, oldTechnicianId)
                        .ifPresent(p -> {
                            p.leave(now);
                            participantRepository.saveAndFlush(p);
                        });
            }
            if (newTechnicianId != null) {
                Optional<ConversationParticipant> existingNew = participantRepository
                        .findByConversationIdAndActorTypeAndActorIdForUpdate(conv.getId(), ActorType.TECHNICIAN, newTechnicianId);
                if (existingNew.isPresent()) {
                    ConversationParticipant p = existingNew.get();
                    p.rejoin(now);
                    participantRepository.saveAndFlush(p);
                } else {
                    ConversationParticipant newParticipant = new ConversationParticipant(
                            conv,
                            ActorType.TECHNICIAN,
                            newTechnicianId,
                            "TECHNICIAN",
                            now);
                    participantRepository.saveAndFlush(newParticipant);
                }
            }
        }
    }

    @Transactional
    public void handleTechnicianUnassigned(Long requestId, Long technicianId) {
        if (technicianId == null) {
            return;
        }
        OffsetDateTime now = now();
        List<Conversation> conversations = conversationRepository.findByRepairRequestId(requestId);
        for (Conversation conv : conversations) {
            participantRepository
                    .findByConversationIdAndActorTypeAndActorIdForUpdate(conv.getId(), ActorType.TECHNICIAN, technicianId)
                    .ifPresent(p -> {
                        p.leave(now);
                        participantRepository.saveAndFlush(p);
                    });
        }
    }

    private void syncCustomerTechnicianParticipants(
            Conversation conv,
            RepairRequest request,
            RepairAssignment activeAssignment,
            OffsetDateTime now) {
        if (activeAssignment != null && activeAssignment.getTechnician() != null) {
            Long techId = activeAssignment.getTechnician().getId();
            Optional<ConversationParticipant> techPart = participantRepository
                    .findByConversationIdAndActorTypeAndActorIdForUpdate(conv.getId(), ActorType.TECHNICIAN, techId);
            if (techPart.isEmpty()) {
                ConversationParticipant newTech = new ConversationParticipant(
                        conv,
                        ActorType.TECHNICIAN,
                        techId,
                        "TECHNICIAN",
                        now);
                participantRepository.saveAndFlush(newTech);
            } else if (!techPart.get().isActive()) {
                techPart.get().rejoin(now);
                participantRepository.saveAndFlush(techPart.get());
            }
        }
    }

    private void syncTechnicianManagerParticipants(
            Conversation conv,
            RepairRequest request,
            RepairAssignment activeAssignment,
            Long staffUserId,
            OffsetDateTime now) {
        if (activeAssignment != null && activeAssignment.getTechnician() != null) {
            Long techId = activeAssignment.getTechnician().getId();
            Optional<ConversationParticipant> techPart = participantRepository
                    .findByConversationIdAndActorTypeAndActorIdForUpdate(conv.getId(), ActorType.TECHNICIAN, techId);
            if (techPart.isEmpty()) {
                ConversationParticipant newTech = new ConversationParticipant(
                        conv,
                        ActorType.TECHNICIAN,
                        techId,
                        "TECHNICIAN",
                        now);
                participantRepository.saveAndFlush(newTech);
            } else if (!techPart.get().isActive()) {
                techPart.get().rejoin(now);
                participantRepository.saveAndFlush(techPart.get());
            }
        }

        if (staffUserId != null) {
            Optional<ConversationParticipant> staffPart = participantRepository
                    .findByConversationIdAndActorTypeAndActorIdForUpdate(conv.getId(), ActorType.STAFF, staffUserId);
            if (staffPart.isEmpty()) {
                ConversationParticipant newStaff = new ConversationParticipant(
                        conv,
                        ActorType.STAFF,
                        staffUserId,
                        "MANAGER",
                        now);
                participantRepository.saveAndFlush(newStaff);
            }
        }
    }

    private List<ParticipantSummaryResponse> loadParticipantSummaries(Long conversationId) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        List<ParticipantSummaryResponse> results = new ArrayList<>();
        for (ConversationParticipant p : participants) {
            String name = resolveDisplayName(p.getActorType(), p.getActorId());
            results.add(ChatMapper.toParticipantSummary(p, name));
        }
        return results;
    }

    private String resolveDisplayName(ActorType actorType, Long actorId) {
        try {
            return switch (actorType) {
                case CUSTOMER -> customerRepository.findById(actorId)
                        .map(Customer::getFullName)
                        .orElse("Customer " + actorId);
                case TECHNICIAN -> technicianRepository.findById(actorId)
                        .map(Technician::getFullName)
                        .orElse("Technician " + actorId);
                case STAFF -> userRepository.findById(actorId)
                        .map(User::getFullName)
                        .orElse("Staff " + actorId);
            };
        } catch (Exception ex) {
            return actorType.name() + " " + actorId;
        }
    }

    private long unreadCount(Long conversationId, Long lastReadId, OffsetDateTime accessibleUntil) {
        if (accessibleUntil == null) {
            return lastReadId == null
                    ? messageRepository.countTotalMessages(conversationId)
                    : messageRepository.countUnreadMessages(conversationId, lastReadId);
        }
        return lastReadId == null
                ? messageRepository.countTotalMessagesCreatedAtOnOrBefore(conversationId, accessibleUntil)
                : messageRepository.countUnreadMessagesCreatedAtOnOrBefore(conversationId, lastReadId, accessibleUntil);
    }

    private Page<ChatMessage> messageHistoryPage(
            Long conversationId,
            Long beforeId,
            OffsetDateTime accessibleUntil,
            Pageable pageable) {
        if (accessibleUntil == null) {
            return beforeId == null
                    ? messageRepository.findByConversationIdOrderByIdDesc(conversationId, pageable)
                    : messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, beforeId, pageable);
        }
        return beforeId == null
                ? messageRepository.findByConversationIdAndCreatedAtLessThanEqualOrderByIdDesc(
                        conversationId,
                        accessibleUntil,
                        pageable)
                : messageRepository.findByConversationIdAndIdLessThanAndCreatedAtLessThanEqualOrderByIdDesc(
                        conversationId,
                        beforeId,
                        accessibleUntil,
                        pageable);
    }

    private OffsetDateTime accessibleUntil(ConversationParticipant participant) {
        if (participant == null || participant.isActive()) {
            return null;
        }
        return participant.getLeftAt();
    }

    private boolean canParticipantAccessMessage(ConversationParticipant participant, ChatMessage message) {
        OffsetDateTime accessibleUntil = accessibleUntil(participant);
        return accessibleUntil == null || !message.getCreatedAt().isAfter(accessibleUntil);
    }

    private void validateSendMessageRequest(SendMessageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (request.conversationId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (request.clientMessageId() == null || request.clientMessageId().isBlank()) {
            throw new BusinessRuleException(
                    "CLIENT_MESSAGE_ID_REQUIRED",
                    "clientMessageId is required.",
                    400);
        }
        if (request.clientMessageId().trim().length() > MAX_CLIENT_MESSAGE_ID_LENGTH) {
            throw new BusinessRuleException(
                    "CLIENT_MESSAGE_ID_TOO_LONG",
                    "clientMessageId must be at most 64 characters.",
                    400);
        }
        if (request.type() == null) {
            throw new BusinessRuleException(
                    "MESSAGE_TYPE_REQUIRED",
                    "Message type is required.",
                    400);
        }
        if (request.type() == ChatMessageType.TEXT) {
            if (request.text() == null || request.text().isBlank()) {
                throw new BusinessRuleException(
                        "MESSAGE_TEXT_BLANK",
                        "Message text cannot be blank.",
                        400);
            }
            if (request.text().trim().length() > MAX_TEXT_LENGTH) {
                throw new BusinessRuleException(
                        "MESSAGE_TEXT_TOO_LONG",
                        "Message text must be at most 4000 characters.",
                        400);
            }
        }
    }

    private void validateAttachmentForChat(
            Long attachmentId,
            Conversation conversation,
            ActorType senderType,
            Long senderId) {
        RepairAttachment attachment = attachmentRepository.findByIdAndStatus(attachmentId, AttachmentStatus.AVAILABLE)
                .orElseThrow(() -> new BusinessRuleException(
                        "ATTACHMENT_NOT_AVAILABLE",
                        "Attachment is not available.",
                        404));

        if (conversation.getRepairRequest() != null) {
            Long reqId = conversation.getRepairRequest().getId();
            if (!attachment.getRepairRequest().getId().equals(reqId)) {
                throw new BusinessRuleException(
                        "ATTACHMENT_FORBIDDEN",
                        "Attachment does not belong to this repair request.",
                        403);
            }
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
