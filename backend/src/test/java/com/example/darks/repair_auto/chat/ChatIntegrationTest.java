package com.example.darks.repair_auto.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.chat.api.dto.ChatMessageResponse;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.chat.domain.ChatMessageType;
import com.example.darks.repair_auto.chat.domain.Conversation;
import com.example.darks.repair_auto.chat.domain.ConversationType;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ChatMessageRepository;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ConversationParticipantRepository;
import com.example.darks.repair_auto.chat.infrastructure.persistence.ConversationRepository;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private RepairAssignmentRepository repairAssignmentRepository;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private com.example.darks.repair_auto.identity.application.MobileSessionService mobileSessionService;

    private User admin;
    private Customer customer;
    private Technician technician;
    private RepairCategory category;
    private RepairRequest repairRequest;
    private RepairAssignment assignment;

    private String issueCustomerToken(Customer c) {
        com.example.darks.repair_auto.identity.domain.MobileSession session = mobileSessionService.createForCustomer(
                c,
                com.example.darks.repair_auto.identity.domain.MobileAuthProvider.PHONE,
                null,
                "127.0.0.1",
                "ChatIntegrationTest");
        return jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                c.getId(),
                c.getAuthVersion(),
                session.getId(),
                com.example.darks.repair_auto.notification.push.domain.PushClientType.CUSTOMER_MOBILE,
                c.getPhone());
    }

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);

        admin = userRepository.findByEmail("chat_admin@test.com").orElseGet(() ->
                userRepository.save(new User(
                        "Chat Admin",
                        "chat_admin@test.com",
                        passwordService.hash("Password123!"),
                        UserRole.ADMIN,
                        true,
                        now)));

        customer = customerRepository.findByPhone("+998901112233").orElseGet(() ->
                customerRepository.save(new Customer(
                        "Chat Customer",
                        "+998901112233",
                        LanguageCode.UZ,
                        now)));

        technician = technicianRepository.findByPhone("+998904445566").orElseGet(() ->
                technicianRepository.save(new Technician(
                        "Chat Technician",
                        "+998904445566",
                        null,
                        null,
                        null,
                        LanguageCode.UZ,
                        true,
                        now)));

        category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new RepairCategory("Engine Repair", "Motor", "Dvigatel", "engine repair", "motor", "dvigatel", null, null, null, true, now)));

        repairRequest = repairRequestRepository.save(RepairRequest.mobile(
                "REQ-CHAT-001",
                customer,
                category,
                "Need brake diagnosis and pads replacement",
                "Tashkent, Chilonzor",
                null,
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                "REF-CHAT-001",
                now));

        assignment = repairAssignmentRepository.save(new RepairAssignment(
                repairRequest,
                technician,
                now.plusHours(2),
                admin,
                now));
        assignment.accept(now);
        repairAssignmentRepository.saveAndFlush(assignment);
    }

    @Test
    void getOrCreateCustomerTechnicianConversation_createsConversationWithParticipants() {
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        assertThat(conv).isNotNull();
        assertThat(conv.getConversationType()).isEqualTo(ConversationType.CUSTOMER_TECHNICIAN);
        assertThat(conv.getRepairRequest().getId()).isEqualTo(repairRequest.getId());

        var participants = participantRepository.findByConversationId(conv.getId());
        assertThat(participants).hasSize(2);
        assertThat(participants).anyMatch(p -> p.getActorType() == ActorType.CUSTOMER && p.getActorId().equals(customer.getId()));
        assertThat(participants).anyMatch(p -> p.getActorType() == ActorType.TECHNICIAN && p.getActorId().equals(technician.getId()));
    }

    @Test
    void sendMessage_textMessage_persistsAndAdvancesReadState() {
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        SendMessageRequest req = new SendMessageRequest(
                conv.getId(),
                "msg-uuid-001",
                ChatMessageType.TEXT,
                "Hello, when will you arrive?",
                null,
                null);

        ChatMessageResponse res = chatService.sendMessage(req, ActorType.CUSTOMER, customer.getId());

        assertThat(res).isNotNull();
        assertThat(res.id()).isNotNull();
        assertThat(res.text()).isEqualTo("Hello, when will you arrive?");
        assertThat(res.senderType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(res.senderId()).isEqualTo(customer.getId());

        // Participant read state should be advanced
        var participant = participantRepository
                .findByConversationIdAndActorTypeAndActorId(conv.getId(), ActorType.CUSTOMER, customer.getId())
                .orElseThrow();
        assertThat(participant.getLastReadMessageId()).isEqualTo(res.id());
    }

    @Test
    void sendMessage_idempotency_returnsExistingMessageWithoutDuplication() {
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        SendMessageRequest req = new SendMessageRequest(
                conv.getId(),
                "msg-idempotent-001",
                ChatMessageType.TEXT,
                "Idempotent text",
                null,
                null);

        ChatMessageResponse first = chatService.sendMessage(req, ActorType.CUSTOMER, customer.getId());
        ChatMessageResponse second = chatService.sendMessage(req, ActorType.CUSTOMER, customer.getId());

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(messageRepository.countTotalMessages(conv.getId())).isEqualTo(1);
    }

    @Test
    void getOrCreateMobileConversation_withUnownedRequest_doesNotCreateConversation() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);
        Customer otherCustomer = customerRepository.findByPhone("+998901110000").orElseGet(() ->
                customerRepository.save(new Customer(
                        "Other Chat Customer",
                        "+998901110000",
                        LanguageCode.UZ,
                        now)));
        RepairRequest otherRequest = repairRequestRepository.saveAndFlush(RepairRequest.mobile(
                "REQ-CHAT-OTHER",
                otherCustomer,
                category,
                "Need unrelated vehicle diagnosis for access test",
                "Tashkent, Yunusabad",
                null,
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                "REF-CHAT-OTHER",
                now));

        String token = issueCustomerToken(customer);

        mockMvc.perform(post("/api/v1/mobile/me/conversations/requests/" + otherRequest.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        assertThat(conversationRepository.findByRepairRequestId(otherRequest.getId())).isEmpty();
    }

    @Test
    void getMessageHistory_forReassignedTechnician_excludesMessagesAfterLeave() throws Exception {
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());
        ChatMessageResponse beforeLeave = chatService.sendMessage(new SendMessageRequest(
                conv.getId(),
                "msg-before-reassign",
                ChatMessageType.TEXT,
                "Visible before reassignment",
                null,
                null), ActorType.CUSTOMER, customer.getId());

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);
        Technician nextTechnician = technicianRepository.findByPhone("+998905557777").orElseGet(() ->
                technicianRepository.save(new Technician(
                        "Next Chat Technician",
                        "+998905557777",
                        null,
                        null,
                        null,
                        LanguageCode.UZ,
                        true,
                        now)));

        chatService.handleTechnicianReassigned(repairRequest.getId(), technician.getId(), nextTechnician.getId());
        Thread.sleep(10);

        ChatMessageResponse afterLeave = chatService.sendMessage(new SendMessageRequest(
                conv.getId(),
                "msg-after-reassign",
                ChatMessageType.TEXT,
                "Hidden after reassignment",
                null,
                null), ActorType.CUSTOMER, customer.getId());

        var history = chatService.getMessageHistory(
                conv.getId(),
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")),
                ActorType.TECHNICIAN,
                technician.getId());

        assertThat(history.content()).extracting(ChatMessageResponse::id)
                .contains(beforeLeave.id())
                .doesNotContain(afterLeave.id());
    }

    @Test
    void sendMessage_withAvailableAttachment_attachesSuccessfully() {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);
        RepairAttachment attachment = new RepairAttachment(
                repairRequest,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "storage-chat-01",
                "car.jpg",
                admin,
                now);
        attachment.markAvailable("image/jpeg", 1024L, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", now);
        attachment = attachmentRepository.saveAndFlush(attachment);

        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        SendMessageRequest req = new SendMessageRequest(
                conv.getId(),
                "msg-uuid-attach-01",
                ChatMessageType.IMAGE,
                null,
                attachment.getId(),
                null);

        ChatMessageResponse res = chatService.sendMessage(req, ActorType.CUSTOMER, customer.getId());
        assertThat(res.attachmentId()).isEqualTo(attachment.getId());
        assertThat(res.messageType()).isEqualTo(ChatMessageType.IMAGE);
    }

    @Test
    void sendMessage_withPendingAttachment_failsValidation() {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);
        RepairAttachment attachment = new RepairAttachment(
                repairRequest,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "storage-chat-02",
                "car.jpg",
                admin,
                now);
        attachment = attachmentRepository.saveAndFlush(attachment);

        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        SendMessageRequest req = new SendMessageRequest(
                conv.getId(),
                "msg-uuid-attach-02",
                ChatMessageType.IMAGE,
                null,
                attachment.getId(),
                null);

        assertThatThrownBy(() -> chatService.sendMessage(req, ActorType.CUSTOMER, customer.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Attachment is not available");
    }

    @Test
    void sendMessage_whenRequestCompleted_isReadOnly() {
        repairRequest.markCompleted(OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC));
        repairRequestRepository.saveAndFlush(repairRequest);

        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        SendMessageRequest req = new SendMessageRequest(
                conv.getId(),
                "msg-uuid-readonly",
                ChatMessageType.TEXT,
                "Trying to write in completed request",
                null,
                null);

        assertThatThrownBy(() -> chatService.sendMessage(req, ActorType.CUSTOMER, customer.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void markAsRead_advancesReadPosition() {
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        SendMessageRequest req = new SendMessageRequest(
                conv.getId(),
                "msg-uuid-read-test",
                ChatMessageType.TEXT,
                "Technician message",
                null,
                null);

        ChatMessageResponse res = chatService.sendMessage(req, ActorType.TECHNICIAN, technician.getId());

        chatService.markAsRead(conv.getId(), res.id(), ActorType.CUSTOMER, customer.getId());

        var custPart = participantRepository
                .findByConversationIdAndActorTypeAndActorId(conv.getId(), ActorType.CUSTOMER, customer.getId())
                .orElseThrow();
        assertThat(custPart.getLastReadMessageId()).isEqualTo(res.id());
    }

    @Test
    void restEndpoint_mobileSendMessageAndFetchHistory() throws Exception {
        String token = issueCustomerToken(customer);

        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        String requestJson = """
            {
                "clientMessageId": "mobile-rest-msg-01",
                "type": "TEXT",
                "text": "Sent via Mobile REST API"
            }
        """;

        mockMvc.perform(post("/api/v1/mobile/me/conversations/" + conv.getId() + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Sent via Mobile REST API"))
                .andExpect(jsonPath("$.senderType").value("CUSTOMER"));

        mockMvc.perform(get("/api/v1/mobile/me/conversations/" + conv.getId() + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].text").value("Sent via Mobile REST API"));
    }

    @Test
    void technicianReassignment_historyAndCutoffBehavior() {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);
        Technician technicianB = technicianRepository.findByPhone("+998909998877").orElseGet(() ->
                technicianRepository.save(new Technician(
                        "Chat Technician B",
                        "+998909998877",
                        null,
                        null,
                        null,
                        LanguageCode.UZ,
                        true,
                        now)));

        // 1. Initial assignment: Customer <-> Technician (technician A)
        Conversation conv = chatService.getOrCreateCustomerTechnicianConversation(repairRequest.getId());

        // 2. Customer sends message M1
        SendMessageRequest m1Req = new SendMessageRequest(
                conv.getId(),
                "msg-m1",
                ChatMessageType.TEXT,
                "Message M1 for Technician A",
                null,
                null);
        ChatMessageResponse m1 = chatService.sendMessage(m1Req, ActorType.CUSTOMER, customer.getId());

        // Technician A can read M1
        var techAHistoryBefore = chatService.getMessageHistory(conv.getId(), null, PageRequest.of(0, 50), ActorType.TECHNICIAN, technician.getId());
        assertThat(techAHistoryBefore.content()).extracting(ChatMessageResponse::id).contains(m1.id());

        // 3. Technician A is removed (unassigned / reassigned to B)
        chatService.handleTechnicianReassigned(repairRequest.getId(), technician.getId(), technicianB.getId());

        // 4. Customer sends message M2
        SendMessageRequest m2Req = new SendMessageRequest(
                conv.getId(),
                "msg-m2",
                ChatMessageType.TEXT,
                "Message M2 after Tech A removed",
                null,
                null);
        ChatMessageResponse m2 = chatService.sendMessage(m2Req, ActorType.CUSTOMER, customer.getId());

        // Technician A history cannot see M2 (only M1)
        var techAHistoryAfter = chatService.getMessageHistory(conv.getId(), null, PageRequest.of(0, 50), ActorType.TECHNICIAN, technician.getId());
        assertThat(techAHistoryAfter.content()).extracting(ChatMessageResponse::id)
                .contains(m1.id())
                .doesNotContain(m2.id());

        // Technician A cannot send new messages
        SendMessageRequest techASendReq = new SendMessageRequest(
                conv.getId(),
                "msg-tech-a-rejected",
                ChatMessageType.TEXT,
                "I should not be able to send this",
                null,
                null);
        assertThatThrownBy(() -> chatService.sendMessage(techASendReq, ActorType.TECHNICIAN, technician.getId()))
                .isInstanceOf(BusinessException.class);

        // 5. Newly assigned Technician B can read BOTH M1 and M2
        var techBHistory = chatService.getMessageHistory(conv.getId(), null, PageRequest.of(0, 50), ActorType.TECHNICIAN, technicianB.getId());
        assertThat(techBHistory.content()).extracting(ChatMessageResponse::id).contains(m1.id(), m2.id());

        // 6. Customer sends message M3
        SendMessageRequest m3Req = new SendMessageRequest(
                conv.getId(),
                "msg-m3",
                ChatMessageType.TEXT,
                "Message M3 for Tech B",
                null,
                null);
        ChatMessageResponse m3 = chatService.sendMessage(m3Req, ActorType.CUSTOMER, customer.getId());

        // Technician B receives/reads M1, M2, M3
        var techBHistoryWithM3 = chatService.getMessageHistory(conv.getId(), null, PageRequest.of(0, 50), ActorType.TECHNICIAN, technicianB.getId());
        assertThat(techBHistoryWithM3.content()).extracting(ChatMessageResponse::id).contains(m1.id(), m2.id(), m3.id());

        // Technician A still cannot read M2 or M3
        var techAFinalHistory = chatService.getMessageHistory(conv.getId(), null, PageRequest.of(0, 50), ActorType.TECHNICIAN, technician.getId());
        assertThat(techAFinalHistory.content()).extracting(ChatMessageResponse::id)
                .contains(m1.id())
                .doesNotContain(m2.id(), m3.id());
    }
}
