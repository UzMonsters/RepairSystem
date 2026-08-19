package com.example.darks.repair_auto.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import com.example.darks.repair_auto.notification.domain.NotificationFailureCategory;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RequestLocationSource;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TechnicianAssignmentNotificationServiceTest {

    private RepairRequestRepository requestRepository;
    private RepairAttachmentRepository attachmentRepository;
    private ObjectStorageService objectStorageService;
    private TelegramBotClient botClient;
    private NotificationTemplateService templateService;
    private TechnicianAssignmentNotificationService service;

    private RepairRequest testRequest;
    private RepairCategory testCategory;
    private Customer testCustomer;
    private NotificationRecipientResolver.ResolvedRecipient technicianRecipient;
    private ClaimedNotification claimedNotification;

    @BeforeEach
    void setUp() {
        requestRepository = mock(RepairRequestRepository.class);
        attachmentRepository = mock(RepairAttachmentRepository.class);
        objectStorageService = mock(ObjectStorageService.class);
        botClient = mock(TelegramBotClient.class);
        templateService = mock(NotificationTemplateService.class);

        service = new TechnicianAssignmentNotificationService(
                requestRepository,
                attachmentRepository,
                objectStorageService,
                botClient,
                templateService);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        testCategory = new RepairCategory(
                "AC", "Кондиционер", "Konditsioner",
                "ac", "кондиционер", "konditsioner",
                "AC repairs", "Ремонт кондиционеров", "Konditsioner ta'mirlash",
                true, now);
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        testCustomer = new Customer("John Customer", "+998901234567", null, now);
        ReflectionTestUtils.setField(testCustomer, "id", 10L);

        testRequest = RepairRequest.mobile(
                "REQ-2026-000100",
                testCustomer,
                testCategory,
                "AC is not cooling properly",
                "Chilanzar 9, Tashkent",
                new BigDecimal("41.2800000"),
                new BigDecimal("69.2000000"),
                RequestLocationSource.DEVICE_GPS,
                RepairRequestPriority.HIGH,
                null,
                "REF-123",
                now);
        ReflectionTestUtils.setField(testRequest, "id", 100L);

        technicianRecipient = new NotificationRecipientResolver.ResolvedRecipient(
                777L,
                LanguageCode.EN);

        claimedNotification = new ClaimedNotification(
                1L,
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.TECHNICIAN,
                999L,
                "notification.technician.assigned",
                "{\"requestId\":\"100\"}",
                1,
                "EN",
                "New assignment",
                "A new repair request REQ-2026-000100 was assigned to you.",
                100L);

        when(requestRepository.findWithRelationsById(100L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.findById(100L)).thenReturn(Optional.of(testRequest));
    }

    @Test
    void givenSinglePhotoAndLocation_whenDeliverAssignment_thenSentInCorrectOrder() {
        List<String> eventSequence = new ArrayList<>();

        RepairAttachment attachment = createAttachment(201L, "problem.jpg", "req-100/photo1.jpg");
        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(List.of(attachment));

        when(objectStorageService.download("req-100/photo1.jpg"))
                .thenReturn(new StoredObjectDownload("image/jpeg", 12, new ByteArrayInputStream("fake-bytes".getBytes())));

        doAnswer(invocation -> {
            String text = invocation.getArgument(1);
            String markup = invocation.getArgument(2);
            if (markup == null) {
                eventSequence.add("SUMMARY: " + text);
            } else {
                eventSequence.add("DECISION: " + text + " | MARKUP: " + markup);
            }
            return null;
        }).when(botClient).sendMessage(anyLong(), anyString(), any());

        doAnswer(invocation -> {
            String filename = invocation.getArgument(1);
            eventSequence.add("PHOTO: " + filename);
            return null;
        }).when(botClient).sendPhoto(anyLong(), anyString(), any(), any());

        doAnswer(invocation -> {
            double lat = invocation.getArgument(1);
            double lon = invocation.getArgument(2);
            eventSequence.add("LOCATION: " + lat + "," + lon);
            return null;
        }).when(botClient).sendLocation(anyLong(), anyDouble(), anyDouble());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        assertThat(eventSequence).hasSize(4);
        assertThat(eventSequence.get(0)).startsWith("SUMMARY: 🔧 New repair request");
        assertThat(eventSequence.get(0)).contains("Request: REQ-2026-000100");
        assertThat(eventSequence.get(0)).contains("Category: AC");
        assertThat(eventSequence.get(0)).contains("Problem:\nAC is not cooling properly");
        assertThat(eventSequence.get(0)).contains("Address:\nChilanzar 9, Tashkent");
        assertThat(eventSequence.get(1)).isEqualTo("PHOTO: problem.jpg");
        assertThat(eventSequence.get(2)).isEqualTo("LOCATION: 41.28,69.2");
        assertThat(eventSequence.get(3)).startsWith("DECISION: Can you accept this repair?");
        assertThat(eventSequence.get(3)).contains("taccept:100");
        assertThat(eventSequence.get(3)).contains("treject:100");
    }

    @Test
    void givenMultiplePhotos_whenDeliverAssignment_thenSendMediaGroupCalled() {
        RepairAttachment att1 = createAttachment(201L, "photo1.jpg", "storage-1");
        RepairAttachment att2 = createAttachment(202L, "photo2.jpg", "storage-2");
        RepairAttachment att3 = createAttachment(203L, "photo3.jpg", "storage-3");

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(List.of(att1, att2, att3));

        when(objectStorageService.download(anyString()))
                .thenAnswer(invocation -> new StoredObjectDownload(
                        "image/jpeg", 10, new ByteArrayInputStream("photo-bytes".getBytes())));

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        verify(botClient, never()).sendPhoto(anyLong(), anyString(), any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TelegramMediaPhoto>> albumCaptor = ArgumentCaptor.forClass(List.class);
        verify(botClient).sendMediaGroup(eq(777L), albumCaptor.capture());
        assertThat(albumCaptor.getValue()).hasSize(3);
    }

    @Test
    void givenNoPhotosAndNoLocation_whenDeliverAssignment_thenOnlySummaryAndDecisionSent() {
        ReflectionTestUtils.setField(testRequest, "locationLatitude", null);
        ReflectionTestUtils.setField(testRequest, "locationLongitude", null);
        ReflectionTestUtils.setField(testRequest, "locationAddress", null);

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(Collections.emptyList());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        verify(botClient, never()).sendPhoto(anyLong(), anyString(), any(), any());
        verify(botClient, never()).sendMediaGroup(anyLong(), any());
        verify(botClient, never()).sendLocation(anyLong(), anyDouble(), anyDouble());

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> markupCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), markupCaptor.capture());

        assertThat(textCaptor.getAllValues().get(0)).doesNotContain("Address:");
        assertThat(markupCaptor.getAllValues().get(0)).isNull();

        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Can you accept this repair?");
        assertThat(markupCaptor.getAllValues().get(1)).contains("taccept:100");
    }

    @Test
    void givenAddressOnly_whenDeliverAssignment_thenAddressInSummaryAndNoSendLocation() {
        ReflectionTestUtils.setField(testRequest, "locationLatitude", null);
        ReflectionTestUtils.setField(testRequest, "locationLongitude", null);
        ReflectionTestUtils.setField(testRequest, "locationAddress", "Mirzo Ulugbek, 15");

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(Collections.emptyList());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        verify(botClient, never()).sendLocation(anyLong(), anyDouble(), anyDouble());

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> markupCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), markupCaptor.capture());

        assertThat(textCaptor.getAllValues().get(0)).contains("Address:\nMirzo Ulugbek, 15");
        assertThat(markupCaptor.getAllValues().get(0)).isNull();

        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Can you accept this repair?");
        assertThat(markupCaptor.getAllValues().get(1)).contains("taccept:100");
    }

    @Test
    void givenCoordinatesOnly_whenDeliverAssignment_thenNoAddressInSummaryAndSendLocationCalled() {
        ReflectionTestUtils.setField(testRequest, "locationLatitude", new BigDecimal("41.3110000"));
        ReflectionTestUtils.setField(testRequest, "locationLongitude", new BigDecimal("69.2400000"));
        ReflectionTestUtils.setField(testRequest, "locationAddress", null);

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(Collections.emptyList());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        verify(botClient).sendLocation(777L, 41.311, 69.24);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> markupCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), markupCaptor.capture());

        assertThat(textCaptor.getAllValues().get(0)).doesNotContain("Address:");
        assertThat(markupCaptor.getAllValues().get(0)).isNull();

        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Can you accept this repair?");
        assertThat(markupCaptor.getAllValues().get(1)).contains("taccept:100");
    }

    @Test
    void givenPhotoDownloadFailure_whenDeliverAssignment_thenContinuesToLocationAndDecision() {
        RepairAttachment attachment = createAttachment(201L, "broken.jpg", "broken-key");
        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(List.of(attachment));

        when(objectStorageService.download("broken-key"))
                .thenThrow(new RuntimeException("S3 connection timeout"));

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        verify(botClient).sendLocation(eq(777L), anyDouble(), anyDouble());

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> markupCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), markupCaptor.capture());
        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Can you accept this repair?");
    }

    @Test
    void givenLocationSendFailure_whenDeliverAssignment_thenContinuesToDecision() {
        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(Collections.emptyList());

        doThrow(new TelegramApiException("Failed to send location"))
                .when(botClient).sendLocation(anyLong(), anyDouble(), anyDouble());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> markupCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), markupCaptor.capture());
        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Can you accept this repair?");
    }

    @Test
    void givenSummarySendTransientFailure_whenDeliverAssignment_thenReturnsTransientFailure() {
        doThrow(new TelegramApiException("Connection timeout"))
                .when(botClient).sendMessage(eq(777L), anyString(), eq(null));

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.TRANSIENT_FAILURE);
        assertThat(result.failureCategory()).isEqualTo(NotificationFailureCategory.TELEGRAM_TRANSIENT_FAILURE);
        verify(botClient, never()).sendLocation(anyLong(), anyDouble(), anyDouble());
    }

    @Test
    void givenDecisionMessagePermanentFailure_whenDeliverAssignment_thenReturnsPermanentFailure() {
        doThrow(new TelegramApiException("Forbidden: bot was blocked by the user"))
                .when(botClient).sendMessage(eq(777L), eq("Can you accept this repair?"), anyString());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, technicianRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.PERMANENT_FAILURE);
        assertThat(result.failureCategory()).isEqualTo(NotificationFailureCategory.TELEGRAM_PERMANENT_FAILURE);
    }

    @Test
    void givenRussianLanguage_whenDeliverAssignment_thenSummaryAndDecisionAreInRussian() {
        NotificationRecipientResolver.ResolvedRecipient ruRecipient = new NotificationRecipientResolver.ResolvedRecipient(
                777L,
                LanguageCode.RU);

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(Collections.emptyList());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, ruRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyboardCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), keyboardCaptor.capture());

        assertThat(textCaptor.getAllValues().get(0)).contains("🔧 Новая заявка на ремонт");
        assertThat(textCaptor.getAllValues().get(0)).contains("Заявка: REQ-2026-000100");
        assertThat(textCaptor.getAllValues().get(0)).contains("Категория: Кондиционер");
        assertThat(textCaptor.getAllValues().get(0)).contains("Проблема:\nAC is not cooling properly");
        assertThat(textCaptor.getAllValues().get(0)).contains("Адрес:\nChilanzar 9, Tashkent");

        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Вы можете принять этот ремонт?");
        assertThat(keyboardCaptor.getAllValues().get(1)).contains("✅ Принять");
        assertThat(keyboardCaptor.getAllValues().get(1)).contains("❌ Отклонить");
    }

    @Test
    void givenUzbekLanguage_whenDeliverAssignment_thenSummaryAndDecisionAreInUzbek() {
        NotificationRecipientResolver.ResolvedRecipient uzRecipient = new NotificationRecipientResolver.ResolvedRecipient(
                777L,
                LanguageCode.UZ);

        when(attachmentRepository.findByRepairRequestIdAndStatusAndAttachmentTypeInOrderByUploadedAtAsc(
                eq(100L), eq(AttachmentStatus.AVAILABLE), any()))
                .thenReturn(Collections.emptyList());

        NotificationDeliveryResult result = service.deliverAssignment(claimedNotification, uzRecipient);

        assertThat(result.outcome()).isEqualTo(NotificationAttemptOutcome.DELIVERED);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyboardCaptor = ArgumentCaptor.forClass(String.class);
        verify(botClient, org.mockito.Mockito.times(2)).sendMessage(eq(777L), textCaptor.capture(), keyboardCaptor.capture());

        assertThat(textCaptor.getAllValues().get(0)).contains("🔧 Yangi ta'mirlash arizasi");
        assertThat(textCaptor.getAllValues().get(0)).contains("Ariza: REQ-2026-000100");
        assertThat(textCaptor.getAllValues().get(0)).contains("Kategoriya: Konditsioner");
        assertThat(textCaptor.getAllValues().get(0)).contains("Muammo:\nAC is not cooling properly");
        assertThat(textCaptor.getAllValues().get(0)).contains("Manzil:\nChilanzar 9, Tashkent");

        assertThat(textCaptor.getAllValues().get(1)).isEqualTo("Ushbu ta'mirlashni qabul qilasizmi?");
        assertThat(keyboardCaptor.getAllValues().get(1)).contains("✅ Qabul qilish");
        assertThat(keyboardCaptor.getAllValues().get(1)).contains("❌ Rad etish");
    }

    private RepairAttachment createAttachment(Long id, String filename, String storageKey) {
        RepairAttachment attachment = RepairAttachment.customerUpload(
                testRequest,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                storageKey,
                filename,
                testCustomer,
                OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(attachment, "id", id);
        ReflectionTestUtils.setField(attachment, "status", AttachmentStatus.AVAILABLE);
        return attachment;
    }
}
