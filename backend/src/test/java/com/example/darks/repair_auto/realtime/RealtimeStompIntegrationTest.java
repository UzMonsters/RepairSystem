package com.example.darks.repair_auto.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.chat.api.dto.SendMessageRequest;
import com.example.darks.repair_auto.chat.application.ChatService;
import com.example.darks.repair_auto.chat.domain.ChatMessageType;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ScheduleRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.context.annotation.Import(RealtimeStompIntegrationTest.StorageTestConfiguration.class)
class RealtimeStompIntegrationTest extends PostgreSqlIntegrationTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class StorageTestConfiguration {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService objectStorageService() {
            return new com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService() {
                private final java.util.Map<String, byte[]> objects = new java.util.concurrent.ConcurrentHashMap<>();

                @Override
                public com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObject upload(
                        com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload command) {
                    try {
                        objects.put(command.storageKey(), command.inputStream().readAllBytes());
                        return new com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObject(
                                command.storageKey(), command.contentType(), command.sizeBytes());
                    } catch (java.io.IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                }

                @Override
                public com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload download(String storageKey) {
                    byte[] bytes = objects.get(storageKey);
                    if (bytes == null) {
                        throw new IllegalStateException("Object not found.");
                    }
                    return new com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload(
                            "image/jpeg", bytes.length, new java.io.ByteArrayInputStream(bytes));
                }

                @Override
                public java.net.URI createDownloadUrl(String storageKey, String downloadFileName, java.time.Duration ttl) {
                    return java.net.URI.create("https://storage.test/1");
                }

                @Override
                public void delete(String storageKey) {
                    objects.remove(storageKey);
                }

                @Override
                public boolean exists(String storageKey) {
                    return objects.containsKey(storageKey);
                }
            };
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private RepairRequestService repairRequestService;

    @Autowired
    private RepairAssignmentService repairAssignmentService;

    @Autowired
    private RepairExecutionService repairExecutionService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User staffUser;
    private Customer customerA;
    private Customer customerB;
    private Technician technicianA;
    private Technician technicianB;
    private RepairCategory testCategory;

    private String staffToken;
    private String customerAToken;
    private String customerBToken;
    private String technicianAToken;
    private String technicianBToken;

    private WebSocketStompClient stompClient;
    private StompSession staffSession;
    private StompSession customerASession;
    private StompSession customerBSession;
    private StompSession technicianASession;
    private StompSession technicianBSession;

    private final BlockingQueue<JsonNode> staffEvents = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> customerAEvents = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> customerBEvents = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> technicianAEvents = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> technicianBEvents = new LinkedBlockingQueue<>();

    private final BlockingQueue<JsonNode> staffChat = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> customerAChat = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> customerBChat = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonNode> technicianAChat = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        staffUser = userRepository.save(new User(
                "Staff Member",
                "staff_" + UUID.randomUUID() + "@test.com",
                "hash",
                UserRole.ADMIN,
                true,
                now));

        customerA = customerRepository.save(new Customer(
                "Customer A",
                "+99890" + (System.nanoTime() % 9000000 + 1000000),
                LanguageCode.UZ,
                now));

        customerB = customerRepository.save(new Customer(
                "Customer B",
                "+99891" + (System.nanoTime() % 9000000 + 1000000),
                LanguageCode.RU,
                now));

        technicianA = technicianRepository.save(new Technician(
                "Tech A",
                "+99893" + (System.nanoTime() % 9000000 + 1000000),
                "HVAC",
                "Notes",
                5,
                LanguageCode.UZ,
                true,
                now));

        technicianB = technicianRepository.save(new Technician(
                "Tech B",
                "+99894" + (System.nanoTime() % 9000000 + 1000000),
                "Brakes",
                "Notes",
                5,
                LanguageCode.RU,
                true,
                now));

        testCategory = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new RepairCategory(
                        "Cat " + UUID.randomUUID(),
                        "Категория " + UUID.randomUUID(),
                        "Kategoriya " + UUID.randomUUID(),
                        "cat-" + UUID.randomUUID(),
                        "категория-" + UUID.randomUUID(),
                        "kategoriya-" + UUID.randomUUID(),
                        "Desc",
                        "Описание",
                        "Tavsif",
                        true,
                        now)));

        staffToken = jwtTokenService.issue(staffUser);
        customerAToken = jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                customerA.getId(),
                customerA.getAuthVersion(),
                UUID.randomUUID(),
                PushClientType.CUSTOMER_MOBILE,
                customerA.getPhone());
        customerBToken = jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                customerB.getId(),
                customerB.getAuthVersion(),
                UUID.randomUUID(),
                PushClientType.CUSTOMER_MOBILE,
                customerB.getPhone());
        technicianAToken = jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                technicianA.getId(),
                technicianA.getAuthVersion(),
                UUID.randomUUID(),
                PushClientType.TECHNICIAN_MOBILE,
                technicianA.getPhone());
        technicianBToken = jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                technicianB.getId(),
                technicianB.getAuthVersion(),
                UUID.randomUUID(),
                PushClientType.TECHNICIAN_MOBILE,
                technicianB.getPhone());

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new CompositeMessageConverter(List.of(
                new org.springframework.messaging.converter.MappingJackson2MessageConverter(),
                new StringMessageConverter(),
                new ByteArrayMessageConverter()
        )));

        staffSession = connectAndSubscribe(staffToken, staffEvents, staffChat);
        customerASession = connectAndSubscribe(customerAToken, customerAEvents, customerAChat);
        customerBSession = connectAndSubscribe(customerBToken, customerBEvents, customerBChat);
        technicianASession = connectAndSubscribe(technicianAToken, technicianAEvents, technicianAChat);
        technicianBSession = connectAndSubscribe(technicianBToken, technicianBEvents, null);

        // Give connections and subscriptions a moment to register in session registry
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() {
        disconnectSafely(staffSession);
        disconnectSafely(customerASession);
        disconnectSafely(customerBSession);
        disconnectSafely(technicianASession);
        disconnectSafely(technicianBSession);
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    private StompSession connectAndSubscribe(
            String token,
            BlockingQueue<JsonNode> eventsQueue,
            BlockingQueue<JsonNode> chatQueue) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        System.err.println("STOMP error: " + exception.getMessage());
                        exception.printStackTrace();
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        System.err.println("STOMP transport error: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                }).get(5, TimeUnit.SECONDS);

        if (eventsQueue != null) {
            session.subscribe("/user/queue/events", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return JsonNode.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof JsonNode node) {
                        eventsQueue.offer(node);
                    } else if (payload != null) {
                        try {
                            eventsQueue.offer(objectMapper.readTree(String.valueOf(payload)));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }

        if (chatQueue != null) {
            session.subscribe("/user/queue/chat", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return JsonNode.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload instanceof JsonNode node) {
                        chatQueue.offer(node);
                    } else if (payload != null) {
                        try {
                            chatQueue.offer(objectMapper.readTree(String.valueOf(payload)));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }

        return session;
    }

    private void disconnectSafely(StompSession session) {
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception ignored) {}
        }
    }

    @Autowired
    private com.example.darks.repair_auto.realtime.session.RealtimeSessionRegistry sessionRegistry;

    @Test
    void requestLifecycle_routingAndIsolation_verifiedAcrossAllActors() throws Exception {
        AuthenticatedUser staffActor = new AuthenticatedUser(staffUser);

        System.out.println("=== DEBUG RealtimeStompIntegrationTest ===");
        System.out.println("staffSession.isConnected=" + staffSession.isConnected() + " id=" + staffSession.getSessionId());
        System.out.println("staffSessionIds in registry=" + sessionRegistry.findStaffSessionIds());
        System.out.println("customerASessionIds in registry=" + sessionRegistry.findSessionIdsForActor(ActorType.CUSTOMER, customerA.getId()));

        // 1. Customer A creates a request -> Staff & Customer A receive REQUEST_CREATED, Customer B receives NOTHING
        var createResponse = repairRequestService.create(new RepairRequestCreateRequest(
                customerA.getId(),
                testCategory.getId(),
                "Car engine noise",
                "Amir Temur 1",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null), staffActor);

        Long requestId = createResponse.id();

        JsonNode staffReqCreated = pollEvent(staffEvents, "REQUEST_CREATED");
        assertThat(staffReqCreated).isNotNull();
        assertThat(staffReqCreated.get("payload").get("requestId").asLong()).isEqualTo(requestId);

        JsonNode custAReqCreated = pollEvent(customerAEvents, "REQUEST_CREATED");
        assertThat(custAReqCreated).isNotNull();
        assertThat(custAReqCreated.get("payload").get("requestId").asLong()).isEqualTo(requestId);

        // Customer B must receive NOTHING
        assertThat(customerBEvents.poll(500, TimeUnit.MILLISECONDS)).isNull();

        // 2. Staff assigns Tech A -> Staff & Tech A receive REQUEST_ASSIGNMENT_CREATED. Customer A & Tech B receive NOTHING!
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianA.getId(), null), staffActor);

        JsonNode staffAssignCreated = pollEvent(staffEvents, "REQUEST_ASSIGNMENT_CREATED");
        assertThat(staffAssignCreated).isNotNull();
        assertThat(staffAssignCreated.get("payload").get("technicianId").asLong()).isEqualTo(technicianA.getId());

        JsonNode techAAssignCreated = pollEvent(technicianAEvents, "REQUEST_ASSIGNMENT_CREATED");
        assertThat(techAAssignCreated).isNotNull();
        assertThat(techAAssignCreated.get("payload").get("technicianId").asLong()).isEqualTo(technicianA.getId());

        // Customer A & Tech B must NOT receive assignment creation
        assertThat(pollEvent(customerAEvents, "REQUEST_ASSIGNMENT_CREATED", 500)).isNull();
        assertThat(pollEvent(technicianBEvents, "REQUEST_ASSIGNMENT_CREATED", 500)).isNull();

        // 3. Tech A accepts assignment -> Staff, Tech A, and Customer A receive REQUEST_ASSIGNMENT_ACCEPTED
        repairAssignmentService.acceptByTechnician(requestId, technicianA.getId());

        JsonNode staffAccepted = pollEvent(staffEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        assertThat(staffAccepted).isNotNull();

        JsonNode custAAccepted = pollEvent(customerAEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        assertThat(custAAccepted).isNotNull();
        assertThat(custAAccepted.get("payload").get("technicianId").asLong()).isEqualTo(technicianA.getId());

        JsonNode techAAccepted = pollEvent(technicianAEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        assertThat(techAAccepted).isNotNull();

        // Customer B & Tech B receive NOTHING
        assertThat(customerBEvents.poll(500, TimeUnit.MILLISECONDS)).isNull();
        assertThat(technicianBEvents.poll(500, TimeUnit.MILLISECONDS)).isNull();

        // 4. Schedule visit changed -> Staff, Tech A, Customer A receive REQUEST_SCHEDULE_CHANGED
        OffsetDateTime scheduledAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        repairAssignmentService.schedule(requestId, new ScheduleRequest(scheduledAt, false), staffActor);

        JsonNode staffSched = pollEvent(staffEvents, "REQUEST_SCHEDULE_CHANGED");
        assertThat(staffSched).isNotNull();
        assertThat(staffSched.get("payload").get("scheduleAction").asText()).isEqualTo("SCHEDULED");

        JsonNode custASched = pollEvent(customerAEvents, "REQUEST_SCHEDULE_CHANGED");
        assertThat(custASched).isNotNull();

        JsonNode techASched = pollEvent(technicianAEvents, "REQUEST_SCHEDULE_CHANGED");
        assertThat(techASched).isNotNull();

        // 5. Attachment upload finalized -> Staff, Tech A, Customer A receive REQUEST_ATTACHMENTS_CHANGED
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        attachmentService.upload(requestId, AttachmentType.GENERAL_DOCUMENT, file, staffActor);

        JsonNode staffAtt = pollEvent(staffEvents, "REQUEST_ATTACHMENTS_CHANGED");
        assertThat(staffAtt).isNotNull();
        assertThat(staffAtt.get("payload").get("changeType").asText()).isEqualTo("UPLOADED");

        JsonNode custAAtt = pollEvent(customerAEvents, "REQUEST_ATTACHMENTS_CHANGED");
        assertThat(custAAtt).isNotNull();

        JsonNode techAAtt = pollEvent(technicianAEvents, "REQUEST_ATTACHMENTS_CHANGED");
        assertThat(techAAtt).isNotNull();

        // 6. Staff reassigns from Tech A to Tech B -> Staff, old Tech A, new Tech B, Customer A receive REQUEST_REASSIGNED
        repairAssignmentService.reassign(
                requestId,
                new ReassignmentRequest(technicianB.getId(), null, "Master reallocated"),
                staffActor);

        JsonNode staffReassign = pollEvent(staffEvents, "REQUEST_REASSIGNED");
        assertThat(staffReassign).isNotNull();
        assertThat(staffReassign.get("payload").get("technicianId").asLong()).isEqualTo(technicianB.getId());
        assertThat(staffReassign.get("payload").get("previousTechnicianId").asLong()).isEqualTo(technicianA.getId());

        JsonNode oldTechReassign = pollEvent(technicianAEvents, "REQUEST_REASSIGNED");
        assertThat(oldTechReassign).isNotNull();

        JsonNode newTechReassign = pollEvent(technicianBEvents, "REQUEST_REASSIGNED");
        assertThat(newTechReassign).isNotNull();

        JsonNode custAReassign = pollEvent(customerAEvents, "REQUEST_REASSIGNED");
        assertThat(custAReassign).isNotNull();

        // 7. Tech B accepts -> Staff, Tech B, Customer A receive REQUEST_ASSIGNMENT_ACCEPTED
        repairAssignmentService.acceptByTechnician(requestId, technicianB.getId());

        JsonNode staffAccept = pollEvent(staffEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        assertThat(staffAccept).isNotNull();

        JsonNode custAAccept = pollEvent(customerAEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        assertThat(custAAccept).isNotNull();

        JsonNode techBAccept = pollEvent(technicianBEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        assertThat(techBAccept).isNotNull();

        // Tech A receives NOTHING
        assertThat(pollEvent(technicianAEvents, "REQUEST_ASSIGNMENT_ACCEPTED", 500)).isNull();

        // 8. Tech B starts work -> Staff, Tech B, Customer A receive REQUEST_STATUS_CHANGED (IN_PROGRESS)
        repairExecutionService.startByTechnician(requestId, technicianB.getId());

        JsonNode staffStatus = pollEvent(staffEvents, "REQUEST_STATUS_CHANGED");
        assertThat(staffStatus).isNotNull();
        assertThat(staffStatus.get("payload").get("status").asText()).isEqualTo("IN_PROGRESS");

        JsonNode custAStatus = pollEvent(customerAEvents, "REQUEST_STATUS_CHANGED");
        assertThat(custAStatus).isNotNull();

        JsonNode techBStatus = pollEvent(technicianBEvents, "REQUEST_STATUS_CHANGED");
        assertThat(techBStatus).isNotNull();

        // 9. Tech B updates diagnosis -> Staff, Tech B, Customer A receive REQUEST_DIAGNOSIS_UPDATED
        repairExecutionService.updateDiagnosisByTechnician(requestId, new DiagnosisRequest("Faulty alternator"), technicianB.getId());

        JsonNode staffDiag = pollEvent(staffEvents, "REQUEST_DIAGNOSIS_UPDATED");
        assertThat(staffDiag).isNotNull();

        JsonNode custADiag = pollEvent(customerAEvents, "REQUEST_DIAGNOSIS_UPDATED");
        assertThat(custADiag).isNotNull();

        JsonNode techBDiag = pollEvent(technicianBEvents, "REQUEST_DIAGNOSIS_UPDATED");
        assertThat(techBDiag).isNotNull();

        // Tech A receives NOTHING
        assertThat(pollEvent(technicianAEvents, "REQUEST_DIAGNOSIS_UPDATED", 500)).isNull();

        // 10. Soft delete request -> Staff, Customer A, Tech B receive REQUEST_DELETED
        repairRequestService.softDelete(requestId, staffActor);

        JsonNode staffDelete = pollEvent(staffEvents, "REQUEST_DELETED");
        assertThat(staffDelete).isNotNull();

        JsonNode custADelete = pollEvent(customerAEvents, "REQUEST_DELETED");
        assertThat(custADelete).isNotNull();

        JsonNode techBDelete = pollEvent(technicianBEvents, "REQUEST_DELETED");
        assertThat(techBDelete).isNotNull();
    }

    @Test
    void chatMessage_routingAndIsolation_deliveredExclusivelyToChatQueue() throws Exception {
        AuthenticatedUser staffActor = new AuthenticatedUser(staffUser);
        var createResponse = repairRequestService.create(new RepairRequestCreateRequest(
                customerA.getId(),
                testCategory.getId(),
                "Chat request",
                "Amir Temur 1",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null), staffActor);

        Long requestId = createResponse.id();
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianA.getId(), null), staffActor);
        repairAssignmentService.acceptByTechnician(requestId, technicianA.getId());

        // Drain any domain events from assignment
        pollEvent(customerAEvents, "REQUEST_ASSIGNMENT_ACCEPTED");
        pollEvent(technicianAEvents, "REQUEST_ASSIGNMENT_ACCEPTED");

        var conversation = chatService.getOrCreateCustomerTechnicianConversation(requestId);

        // Send a message from Customer A
        chatService.sendMessage(new SendMessageRequest(
                conversation.getId(),
                "client-msg-123",
                ChatMessageType.TEXT,
                "Hello master!",
                null,
                null), ActorType.CUSTOMER, customerA.getId());

        // 1. Customer A receives CHAT_MESSAGE_CREATED on /user/queue/chat
        JsonNode custMsg = customerAChat.poll(3, TimeUnit.SECONDS);
        assertThat(custMsg).isNotNull();
        assertThat(custMsg.get("type").asText()).isEqualTo("CHAT_MESSAGE_CREATED");
        assertThat(custMsg.get("payload").get("text").asText()).isEqualTo("Hello master!");

        // 2. Tech A receives CHAT_MESSAGE_CREATED on /user/queue/chat
        JsonNode techMsg = technicianAChat.poll(3, TimeUnit.SECONDS);
        assertThat(techMsg).isNotNull();
        assertThat(techMsg.get("type").asText()).isEqualTo("CHAT_MESSAGE_CREATED");
        assertThat(techMsg.get("payload").get("text").asText()).isEqualTo("Hello master!");

        // 3. Customer B receives NOTHING on chat
        assertThat(customerBChat.poll(500, TimeUnit.MILLISECONDS)).isNull();

        // 4. /user/queue/events receives NO CHAT_MESSAGE_CREATED events (strict separation)
        assertThat(pollEvent(customerAEvents, "CHAT_MESSAGE_CREATED", 500)).isNull();
        assertThat(pollEvent(technicianAEvents, "CHAT_MESSAGE_CREATED", 500)).isNull();
    }

    @Test
    void transactionRollback_doesNotPublishRealtimeEvents() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        AuthenticatedUser staffActor = new AuthenticatedUser(staffUser);

        try {
            tx.execute(status -> {
                repairRequestService.create(new RepairRequestCreateRequest(
                        customerA.getId(),
                        testCategory.getId(),
                        "Rollback test request",
                        "Street 1",
                        null,
                        null,
                        RepairRequestPriority.NORMAL,
                        null,
                        null), staffActor);

                // Force a rollback
                throw new RuntimeException("Forced rollback in test");
            });
        } catch (RuntimeException ignored) {}

        // Neither staff nor customer receives any event because transaction rolled back
        assertThat(staffEvents.poll(1, TimeUnit.SECONDS)).isNull();
        assertThat(customerAEvents.poll(1, TimeUnit.SECONDS)).isNull();
    }

    private JsonNode pollEvent(BlockingQueue<JsonNode> queue, String expectedType) throws InterruptedException {
        return pollEvent(queue, expectedType, 5000);
    }

    private JsonNode pollEvent(BlockingQueue<JsonNode> queue, String expectedType, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) break;
            JsonNode event = queue.poll(remaining, TimeUnit.MILLISECONDS);
            if (event != null && event.has("type") && expectedType.equals(event.get("type").asText())) {
                return event;
            }
        }
        return null;
    }
}
