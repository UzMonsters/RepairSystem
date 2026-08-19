package com.example.darks.repair_auto.notification.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationTemplateService;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.notification.inbox.api.dto.UnreadNotificationCountResponse;
import com.example.darks.repair_auto.notification.inbox.api.dto.UserNotificationResponse;
import com.example.darks.repair_auto.notification.inbox.domain.UserNotification;
import com.example.darks.repair_auto.notification.inbox.infrastructure.UserNotificationRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class UserNotificationServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private UserNotificationRepository userNotificationRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private NotificationTemplateService templateService;
    private RequestLocaleResolver requestLocaleResolver;
    private Clock clock;
    private UserNotificationService service;

    private Customer customer;
    private Technician technician;
    private RepairRequest repairRequest;

    @BeforeEach
    void setUp() {
        userNotificationRepository = mock(UserNotificationRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        templateService = mock(NotificationTemplateService.class);
        requestLocaleResolver = mock(RequestLocaleResolver.class);
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        service = new UserNotificationService(
                userNotificationRepository,
                customerRepository,
                technicianRepository,
                templateService,
                requestLocaleResolver,
                clock);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 42L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);

        repairRequest = mock(RepairRequest.class);
        when(repairRequest.getId()).thenReturn(101L);
        when(repairRequest.getRequestNumber()).thenReturn("REQ-2026-000042");
    }

    @Test
    void givenCustomerEvent_whenRecordFromEvent_thenExecutesAtomicInsertAndReturnsCreated() {
        NotificationEventFactory.NotificationEvent event = new NotificationEventFactory.NotificationEvent(
                "req:101:status:COMPLETED:customer:42",
                NotificationType.REPAIR_COMPLETED,
                NotificationRecipientType.CUSTOMER,
                42L,
                repairRequest,
                "notification.repair.completed",
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}");

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(userNotificationRepository.insertForCustomerOnConflictDoNothing(
                eq("req:101:status:COMPLETED:customer:42"),
                eq("REPAIR_COMPLETED"),
                eq(42L),
                eq(101L),
                eq("REQ-2026-000042"),
                eq("REPAIR_REQUEST_DETAILS"),
                eq(101L),
                eq("{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}"),
                eq(NOW))).thenReturn(1);

        UserNotificationService.RecordResult result = service.recordFromEvent(event);

        assertThat(result).isEqualTo(UserNotificationService.RecordResult.CREATED);
        verify(userNotificationRepository).insertForCustomerOnConflictDoNothing(
                "req:101:status:COMPLETED:customer:42",
                "REPAIR_COMPLETED",
                42L,
                101L,
                "REQ-2026-000042",
                "REPAIR_REQUEST_DETAILS",
                101L,
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}",
                NOW);
    }

    @Test
    void givenDuplicateCustomerEvent_whenRecordFromEvent_thenReturnsAlreadyExists() {
        NotificationEventFactory.NotificationEvent event = new NotificationEventFactory.NotificationEvent(
                "req:101:status:COMPLETED:customer:42",
                NotificationType.REPAIR_COMPLETED,
                NotificationRecipientType.CUSTOMER,
                42L,
                repairRequest,
                "notification.repair.completed",
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}");

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(userNotificationRepository.insertForCustomerOnConflictDoNothing(
                any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        UserNotificationService.RecordResult result = service.recordFromEvent(event);

        assertThat(result).isEqualTo(UserNotificationService.RecordResult.ALREADY_EXISTS);
    }

    @Test
    void givenTechnicianEvent_whenRecordFromEvent_thenExecutesAtomicInsertAndReturnsCreated() {
        NotificationEventFactory.NotificationEvent event = new NotificationEventFactory.NotificationEvent(
                "req:101:assigned:technician:17",
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.TECHNICIAN,
                17L,
                repairRequest,
                "notification.technician.assigned",
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}");

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));
        when(userNotificationRepository.insertForTechnicianOnConflictDoNothing(
                eq("req:101:assigned:technician:17"),
                eq("TECHNICIAN_ASSIGNED"),
                eq(17L),
                eq(101L),
                eq("REQ-2026-000042"),
                eq("TECHNICIAN_JOB_DETAILS"),
                eq(101L),
                eq("{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}"),
                eq(NOW))).thenReturn(1);

        UserNotificationService.RecordResult result = service.recordFromEvent(event);

        assertThat(result).isEqualTo(UserNotificationService.RecordResult.CREATED);
        verify(userNotificationRepository).insertForTechnicianOnConflictDoNothing(
                "req:101:assigned:technician:17",
                "TECHNICIAN_ASSIGNED",
                17L,
                101L,
                "REQ-2026-000042",
                "TECHNICIAN_JOB_DETAILS",
                101L,
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}",
                NOW);
    }

    @Test
    void givenDuplicateTechnicianEvent_whenRecordFromEvent_thenReturnsAlreadyExists() {
        NotificationEventFactory.NotificationEvent event = new NotificationEventFactory.NotificationEvent(
                "req:101:assigned:technician:17",
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.TECHNICIAN,
                17L,
                repairRequest,
                "notification.technician.assigned",
                "{\"requestId\":\"101\",\"requestNumber\":\"REQ-2026-000042\"}");

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));
        when(userNotificationRepository.insertForTechnicianOnConflictDoNothing(
                any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        UserNotificationService.RecordResult result = service.recordFromEvent(event);

        assertThat(result).isEqualTo(UserNotificationService.RecordResult.ALREADY_EXISTS);
    }

    @Test
    void givenStaffEvent_whenRecordFromEvent_thenSkipsCreation() {
        NotificationEventFactory.NotificationEvent event = new NotificationEventFactory.NotificationEvent(
                "req:101:created:staff:1",
                NotificationType.REQUEST_CREATED,
                NotificationRecipientType.STAFF,
                1L,
                repairRequest,
                "notification.request.created",
                "{}");

        UserNotificationService.RecordResult result = service.recordFromEvent(event);

        assertThat(result).isEqualTo(UserNotificationService.RecordResult.SKIPPED);
        verify(userNotificationRepository, never()).insertForCustomerOnConflictDoNothing(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(userNotificationRepository, never()).insertForTechnicianOnConflictDoNothing(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenCustomerActorAndNoFilter_whenListForMobile_thenQueriesAllNotifications() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        Pageable pageable = PageRequest.of(0, 20);

        UserNotification notif = UserNotification.forCustomer(
                "key-1",
                NotificationType.REPAIR_COMPLETED,
                customer,
                repairRequest,
                "REQ-2026-000042",
                "REPAIR_REQUEST_DETAILS",
                101L,
                "{}",
                NOW);
        ReflectionTestUtils.setField(notif, "id", 501L);

        when(userNotificationRepository.findByCustomerId(42L, pageable))
                .thenReturn(new PageImpl<>(List.of(notif), pageable, 1));
        when(requestLocaleResolver.resolveLanguage()).thenReturn(SupportedLanguage.UZ);
        when(templateService.render(eq(NotificationType.REPAIR_COMPLETED), eq(NotificationRecipientType.CUSTOMER), any(), eq(1), eq(LanguageCode.UZ)))
                .thenReturn(new NotificationTemplateService.RenderedNotification(LanguageCode.UZ, "Ta'mirlash yakunlandi", "Muvaffaqiyatli yakunlandi"));

        Page<UserNotificationResponse> result = service.listForMobile(actor, pageable, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        UserNotificationResponse item = result.getContent().get(0);
        assertThat(item.id()).isEqualTo(501L);
        assertThat(item.title()).isEqualTo("Ta'mirlash yakunlandi");
        assertThat(item.body()).isEqualTo("Muvaffaqiyatli yakunlandi");
        assertThat(item.target()).isEqualTo("REPAIR_REQUEST_DETAILS");
        assertThat(item.targetId()).isEqualTo(101L);
        assertThat(item.read()).isFalse();
        verify(userNotificationRepository).findByCustomerId(42L, pageable);
    }

    @Test
    void givenCustomerActorAndUnreadTrue_whenListForMobile_thenQueriesUnreadOnly() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        Pageable pageable = PageRequest.of(0, 20);

        when(userNotificationRepository.findByCustomerIdAndReadAtIsNull(42L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<UserNotificationResponse> result = service.listForMobile(actor, pageable, true);

        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(userNotificationRepository).findByCustomerIdAndReadAtIsNull(42L, pageable);
    }

    @Test
    void givenCustomerActorAndUnreadFalse_whenListForMobile_thenQueriesReadOnly() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        Pageable pageable = PageRequest.of(0, 20);

        when(userNotificationRepository.findByCustomerIdAndReadAtIsNotNull(42L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<UserNotificationResponse> result = service.listForMobile(actor, pageable, false);

        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(userNotificationRepository).findByCustomerIdAndReadAtIsNotNull(42L, pageable);
    }

    @Test
    void givenCustomerActor_whenGetUnreadCount_thenReturnsCount() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        when(userNotificationRepository.countByCustomerIdAndReadAtIsNull(42L)).thenReturn(3L);

        UnreadNotificationCountResponse response = service.getUnreadCount(actor);

        assertThat(response.unreadCount()).isEqualTo(3L);
    }

    @Test
    void givenUnreadNotification_whenMarkAsRead_thenSetsReadAt() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        UserNotification notif = UserNotification.forCustomer(
                "key-1",
                NotificationType.REPAIR_COMPLETED,
                customer,
                repairRequest,
                "REQ-2026-000042",
                "REPAIR_REQUEST_DETAILS",
                101L,
                "{}",
                NOW);
        ReflectionTestUtils.setField(notif, "id", 501L);

        when(userNotificationRepository.findById(501L)).thenReturn(Optional.of(notif));

        service.markAsRead(actor, 501L);

        assertThat(notif.isRead()).isTrue();
        assertThat(notif.getReadAt()).isEqualTo(NOW);
        verify(userNotificationRepository).save(notif);
    }

    @Test
    void givenAlreadyReadNotification_whenMarkAsRead_thenIsIdempotentAndPreservesOriginalReadAt() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        OffsetDateTime originalReadAt = NOW.minusHours(2);
        UserNotification notif = UserNotification.forCustomer(
                "key-1",
                NotificationType.REPAIR_COMPLETED,
                customer,
                repairRequest,
                "REQ-2026-000042",
                "REPAIR_REQUEST_DETAILS",
                101L,
                "{}",
                NOW.minusHours(3));
        ReflectionTestUtils.setField(notif, "id", 501L);
        notif.markRead(originalReadAt);

        when(userNotificationRepository.findById(501L)).thenReturn(Optional.of(notif));

        service.markAsRead(actor, 501L);

        assertThat(notif.getReadAt()).isEqualTo(originalReadAt);
        verify(userNotificationRepository, never()).save(any());
    }

    @Test
    void givenNotificationBelongingToAnotherActor_whenMarkAsRead_thenThrowsNotFound() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);

        Customer otherCustomer = new Customer("Other", "+998909998877", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(otherCustomer, "id", 99L);

        UserNotification notif = UserNotification.forCustomer(
                "key-other",
                NotificationType.REPAIR_COMPLETED,
                otherCustomer,
                repairRequest,
                "REQ-2026-000042",
                "REPAIR_REQUEST_DETAILS",
                101L,
                "{}",
                NOW);
        ReflectionTestUtils.setField(notif, "id", 501L);

        when(userNotificationRepository.findById(501L)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.markAsRead(actor, 501L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void givenCustomerActor_whenMarkAllAsRead_thenExecutesRepositoryUpdate() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);

        service.markAllAsRead(actor);

        verify(userNotificationRepository).markAllAsReadForCustomer(42L, NOW);
    }

    @Test
    void givenTechnicianActor_whenMarkAllAsRead_thenExecutesRepositoryUpdate() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);

        service.markAllAsRead(actor);

        verify(userNotificationRepository).markAllAsReadForTechnician(17L, NOW);
    }
}
