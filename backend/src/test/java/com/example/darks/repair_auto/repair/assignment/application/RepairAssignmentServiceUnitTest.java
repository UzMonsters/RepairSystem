package com.example.darks.repair_auto.repair.assignment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationOutboxService;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.realtime.event.application.RequestAssignedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestAssignmentCreatedDomainEvent;
import com.example.darks.repair_auto.realtime.event.application.RequestUnassignedDomainEvent;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.application.RepairStatusHistoryService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.domain.RequestLocationSource;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class RepairAssignmentServiceUnitTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-29T10:00:00Z");

    private RepairAssignmentRepository assignmentRepository;
    private RepairRequestRepository requestRepository;
    private TechnicianRepository technicianRepository;
    private UserRepository userRepository;
    private RepairStatusHistoryService statusHistoryService;
    private NotificationEventFactory notificationEventFactory;
    private NotificationOutboxService notificationOutboxService;
    private EffectiveLanguageResolver effectiveLanguageResolver;
    private LocalizedValueResolver localizedValueResolver;
    private ApplicationEventPublisher eventPublisher;
    private Clock clock;

    private RepairAssignmentService service;

    private Customer customer;
    private Technician technicianA;
    private Technician technicianB;
    private RepairRequest request;
    private User adminUser;
    private User managerUser;
    private AuthenticatedUser adminActor;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(RepairAssignmentRepository.class);
        requestRepository = mock(RepairRequestRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        userRepository = mock(UserRepository.class);
        statusHistoryService = mock(RepairStatusHistoryService.class);
        notificationEventFactory = new NotificationEventFactory();
        notificationOutboxService = mock(NotificationOutboxService.class);
        effectiveLanguageResolver = mock(EffectiveLanguageResolver.class);
        localizedValueResolver = mock(LocalizedValueResolver.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        clock = Clock.fixed(Instant.parse("2026-08-29T10:00:00Z"), ZoneOffset.UTC);

        service = new RepairAssignmentService(
                assignmentRepository,
                requestRepository,
                technicianRepository,
                userRepository,
                statusHistoryService,
                notificationEventFactory,
                notificationOutboxService,
                effectiveLanguageResolver,
                localizedValueResolver,
                eventPublisher,
                null,
                clock);

        customer = new Customer("Rustam Karimov", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 101L);

        RepairCategory category = new RepairCategory(
                "Refrigerator", "Холодильник", "Sovutgich",
                "fridge", "холодильник", "sovutgich",
                "Fridge repair", "Ремонт холодильников", "Sovutgich ta'miri",
                true, NOW);
        ReflectionTestUtils.setField(category, "id", 1L);

        request = RepairRequest.mobile(
                "REQ-2026-000100",
                customer,
                category,
                "Not cooling",
                "Navoi 12, Tashkent",
                new BigDecimal("41.3000000"),
                new BigDecimal("69.2500000"),
                RequestLocationSource.DEVICE_GPS,
                RepairRequestPriority.HIGH,
                null,
                null,
                NOW);
        ReflectionTestUtils.setField(request, "id", 201L);

        technicianA = new Technician("Akmal Usta", "+998901112233", "Cooling", "Notes", 5, LanguageCode.UZ, true, NOW);
        ReflectionTestUtils.setField(technicianA, "id", 301L);

        technicianB = new Technician("Bekzod Usta", "+998904445566", "Cooling", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technicianB, "id", 302L);

        adminUser = new User("admin@test.com", "hash", "Admin User", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(adminUser, "id", 1L);

        managerUser = new User("manager@test.com", "hash", "Manager User", UserRole.MANAGER, true, NOW);
        ReflectionTestUtils.setField(managerUser, "id", 2L);

        adminActor = new AuthenticatedUser(adminUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findActiveStaff()).thenReturn(List.of(adminUser, managerUser));
        when(requestRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(request));
        when(requestRepository.findById(201L)).thenReturn(Optional.of(request));
        when(technicianRepository.findByIdForUpdate(301L)).thenReturn(Optional.of(technicianA));
        when(technicianRepository.findByIdForUpdate(302L)).thenReturn(Optional.of(technicianB));
        when(technicianRepository.findById(301L)).thenReturn(Optional.of(technicianA));
        when(technicianRepository.findById(302L)).thenReturn(Optional.of(technicianB));
        when(assignmentRepository.save(any())).thenAnswer(inv -> {
            RepairAssignment a = inv.getArgument(0);
            if (ReflectionTestUtils.getField(a, "id") == null) {
                ReflectionTestUtils.setField(a, "id", 501L);
            }
            return a;
        });
    }

    @Test
    void givenAssignmentCreation_thenOnlyTechnicianIsNotifiedAndCustomerGetsNothing() {
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any())).thenReturn(Optional.empty());

        service.assign(201L, new AssignmentRequest(301L, null), adminActor);

        // 1. Notification outbox should ONLY contain technician notification
        ArgumentCaptor<NotificationEventFactory.NotificationEvent> captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(1)).enqueue(captor.capture());

        NotificationEventFactory.NotificationEvent event = captor.getValue();
        assertThat(event.recipientType()).isEqualTo(NotificationRecipientType.TECHNICIAN);
        assertThat(event.recipientId()).isEqualTo(301L);
        assertThat(event.type()).isEqualTo(NotificationType.TECHNICIAN_ASSIGNED);

        // Customer was NOT enqueued in outbox
        verify(notificationOutboxService, never()).enqueue(argThat(e ->
                e != null && e.recipientType() == NotificationRecipientType.CUSTOMER));

        // 2. Realtime event: RequestAssignmentCreatedDomainEvent published
        verify(eventPublisher).publishEvent(any(RequestAssignmentCreatedDomainEvent.class));
        verify(eventPublisher, never()).publishEvent(any(RequestAssignedDomainEvent.class));
    }

    @Test
    void givenTechnicianAccepts_thenCustomerIsNotifiedOfAssignmentAndDomainEventPublished() {
        RepairAssignment assignment = new RepairAssignment(request, technicianA, null, adminUser, NOW);
        ReflectionTestUtils.setField(assignment, "id", 501L);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(assignment));

        service.acceptByTechnician(201L, 301L);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);

        // Customer notification enqueued on accept
        ArgumentCaptor<NotificationEventFactory.NotificationEvent> captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(1)).enqueue(captor.capture());

        NotificationEventFactory.NotificationEvent event = captor.getValue();
        assertThat(event.recipientType()).isEqualTo(NotificationRecipientType.CUSTOMER);
        assertThat(event.recipientId()).isEqualTo(101L);
        assertThat(event.type()).isEqualTo(NotificationType.TECHNICIAN_ASSIGNED);
        assertThat(event.payloadJson()).contains("Akmal Usta");

        // Realtime event: RequestAssignmentAcceptedDomainEvent published on acceptance
        verify(eventPublisher).publishEvent(any(com.example.darks.repair_auto.realtime.event.application.RequestAssignmentAcceptedDomainEvent.class));
    }

    @Test
    void givenTechnicianRejects_thenCustomerGetsNothingAndStaffUsersAreNotifiedWithReason() {
        RepairAssignment assignment = new RepairAssignment(request, technicianA, null, adminUser, NOW);
        ReflectionTestUtils.setField(assignment, "id", 501L);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(assignment));

        service.rejectByTechnician(201L, new AssignmentRejectionRequest("Traffic congestion and too far away"), 301L);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.REJECTED);
        assertThat(request.getStatus()).isEqualTo(RepairRequestStatus.NEW);

        // Customer receives NO notification
        verify(notificationOutboxService, never()).enqueue(argThat(e ->
                e != null && e.recipientType() == NotificationRecipientType.CUSTOMER));

        // Active Admin and Manager staff receive TECHNICIAN_REJECTED notification
        ArgumentCaptor<NotificationEventFactory.NotificationEvent> captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(2)).enqueue(captor.capture());

        List<NotificationEventFactory.NotificationEvent> events = captor.getAllValues();
        assertThat(events).allMatch(e -> e.recipientType() == NotificationRecipientType.STAFF);
        assertThat(events).allMatch(e -> e.type() == NotificationType.TECHNICIAN_REJECTED);
        assertThat(events).extracting(NotificationEventFactory.NotificationEvent::recipientId).containsExactly(1L, 2L);
        assertThat(events.get(0).payloadJson())
                .contains("REQ-2026-000100")
                .contains("Akmal Usta")
                .contains("Traffic congestion and too far away");

        // Domain event: RequestAssignmentRejectedDomainEvent published
        verify(eventPublisher).publishEvent(any(com.example.darks.repair_auto.realtime.event.application.RequestAssignmentRejectedDomainEvent.class));
    }

    @Test
    void givenReassignment_thenCurrentTechUnassigned_nextTechAssigned_customerNotNotifiedUntilAcceptance() {
        RepairAssignment currentAssignment = new RepairAssignment(request, technicianA, null, adminUser, NOW);
        ReflectionTestUtils.setField(currentAssignment, "id", 501L);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(currentAssignment));

        service.reassign(201L, new ReassignmentRequest(302L, null, "Reassigning to specialized master"), adminActor);

        assertThat(currentAssignment.getStatus()).isEqualTo(AssignmentStatus.REASSIGNED);

        // Technician A notified unassigned, Technician B notified assigned. Customer receives NOTHING yet.
        ArgumentCaptor<NotificationEventFactory.NotificationEvent> captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(2)).enqueue(captor.capture());

        List<NotificationEventFactory.NotificationEvent> events = captor.getAllValues();
        assertThat(events.get(0).recipientType()).isEqualTo(NotificationRecipientType.TECHNICIAN);
        assertThat(events.get(0).recipientId()).isEqualTo(301L);
        assertThat(events.get(0).type()).isEqualTo(NotificationType.TECHNICIAN_UNASSIGNED);

        assertThat(events.get(1).recipientType()).isEqualTo(NotificationRecipientType.TECHNICIAN);
        assertThat(events.get(1).recipientId()).isEqualTo(302L);
        assertThat(events.get(1).type()).isEqualTo(NotificationType.TECHNICIAN_ASSIGNED);

        // Ensure NO customer notification was enqueued
        verify(notificationOutboxService, never()).enqueue(argThat(e ->
                e != null && e.recipientType() == NotificationRecipientType.CUSTOMER));
    }

    @Test
    void givenAlreadyAcceptedAssignment_whenAcceptAgain_thenThrowsConflict() {
        RepairAssignment assignment = new RepairAssignment(request, technicianA, null, adminUser, NOW);
        ReflectionTestUtils.setField(assignment, "id", 501L);
        assignment.accept(NOW);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.acceptByTechnician(201L, 301L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Assignment has already been accepted.");

        verify(notificationOutboxService, never()).enqueue(any());
        verify(eventPublisher, never()).publishEvent(any(RequestAssignedDomainEvent.class));
    }

    @Test
    void givenFailedAcceptance_whenPreconditionFails_thenNoCustomerNotificationEscapes() {
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptByTechnician(201L, 301L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Active assignment was not found");

        verify(notificationOutboxService, never()).enqueue(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void givenFailedRejectionWithEmptyReason_thenValidationFailsAndNoStaffNotificationEscapes() {
        RepairAssignment assignment = new RepairAssignment(request, technicianA, null, adminUser, NOW);
        ReflectionTestUtils.setField(assignment, "id", 501L);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.rejectByTechnician(201L, new AssignmentRejectionRequest("   "), 301L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Reason must be between 1 and 500 characters.");

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.PENDING);
        verify(notificationOutboxService, never()).enqueue(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void givenFullReassignmentChronology_TechAAssigned_TechARejected_TechBAssigned_TechBAccepted_thenCustomerNotifiedOnceForTechBOnly() {
        // Step 1: Admin assigns Tech A -> Customer gets NOTHING, Tech A gets assignment
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any())).thenReturn(Optional.empty());
        service.assign(201L, new AssignmentRequest(301L, null), adminActor);

        ArgumentCaptor<NotificationEventFactory.NotificationEvent> step1Captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(1)).enqueue(step1Captor.capture());
        assertThat(step1Captor.getValue().recipientType()).isEqualTo(NotificationRecipientType.TECHNICIAN);
        assertThat(step1Captor.getValue().recipientId()).isEqualTo(301L);

        // Step 2: Tech A rejects -> Customer gets NOTHING, Staff notified with reason
        RepairAssignment assignmentA = new RepairAssignment(request, technicianA, null, adminUser, NOW);
        ReflectionTestUtils.setField(assignmentA, "id", 501L);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(assignmentA));

        service.rejectByTechnician(201L, new AssignmentRejectionRequest("Too busy"), 301L);
        assertThat(assignmentA.getStatus()).isEqualTo(AssignmentStatus.REJECTED);
        assertThat(request.getStatus()).isEqualTo(RepairRequestStatus.NEW);

        ArgumentCaptor<NotificationEventFactory.NotificationEvent> step2Captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(3)).enqueue(step2Captor.capture());
        List<NotificationEventFactory.NotificationEvent> step2Events = step2Captor.getAllValues().subList(1, 3);
        assertThat(step2Events).allMatch(e -> e.recipientType() == NotificationRecipientType.STAFF);
        assertThat(step2Events).allMatch(e -> e.type() == NotificationType.TECHNICIAN_REJECTED);

        // Step 3: Admin assigns Tech B -> Customer gets NOTHING, Tech B gets assignment
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any())).thenReturn(Optional.empty());
        service.assign(201L, new AssignmentRequest(302L, null), adminActor);

        ArgumentCaptor<NotificationEventFactory.NotificationEvent> step3Captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(4)).enqueue(step3Captor.capture());
        assertThat(step3Captor.getValue().recipientType()).isEqualTo(NotificationRecipientType.TECHNICIAN);
        assertThat(step3Captor.getValue().recipientId()).isEqualTo(302L);

        // Step 4: Tech B accepts -> Customer receives EXACTLY ONE notification referring to Tech B
        RepairAssignment assignmentB = new RepairAssignment(request, technicianB, null, adminUser, NOW);
        ReflectionTestUtils.setField(assignmentB, "id", 502L);
        when(assignmentRepository.findActiveByRequestIdForUpdate(eq(201L), any()))
                .thenReturn(Optional.of(assignmentB));

        service.acceptByTechnician(201L, 302L);
        assertThat(assignmentB.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);

        ArgumentCaptor<NotificationEventFactory.NotificationEvent> step4Captor =
                ArgumentCaptor.forClass(NotificationEventFactory.NotificationEvent.class);
        verify(notificationOutboxService, times(5)).enqueue(step4Captor.capture());

        NotificationEventFactory.NotificationEvent customerEvent = step4Captor.getValue();
        assertThat(customerEvent.recipientType()).isEqualTo(NotificationRecipientType.CUSTOMER);
        assertThat(customerEvent.recipientId()).isEqualTo(101L);
        assertThat(customerEvent.type()).isEqualTo(NotificationType.TECHNICIAN_ASSIGNED);
        assertThat(customerEvent.payloadJson())
                .contains("Bekzod Usta")
                .doesNotContain("Akmal Usta");

        // Verify across the entire sequence that ONLY ONE customer notification occurred
        List<NotificationEventFactory.NotificationEvent> allEvents = step4Captor.getAllValues();
        List<NotificationEventFactory.NotificationEvent> customerEvents = allEvents.stream()
                .filter(e -> e.recipientType() == NotificationRecipientType.CUSTOMER)
                .toList();
        assertThat(customerEvents).hasSize(1);
        assertThat(customerEvents.get(0).payloadJson()).contains("Bekzod Usta");
    }
}
