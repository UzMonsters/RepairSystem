package com.example.darks.repair_auto.notification.push.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class FirebasePushDeliveryGatewayTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private FirebaseMessagingClient messagingClient;
    private FirebasePushDeliveryGateway gateway;

    private User staffAdmin;
    private Customer customer;
    private Technician technician;

    @BeforeEach
    void setUp() {
        messagingClient = mock(FirebaseMessagingClient.class);
        gateway = new FirebasePushDeliveryGateway(messagingClient);

        staffAdmin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(staffAdmin, "id", 1L);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 42L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);
    }

    @Test
    void givenStaffWebEndpoint_whenBuildMessage_thenPopulatesWebpushConfigAndData() {
        PushEndpoint endpoint = PushEndpoint.forStaff(
                staffAdmin,
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "fcm-staff-web-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 10L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "New Repair Request",
                "REQ-2026-000042 created",
                "STAFF_NEW_REQUEST",
                101L,
                42L,
                "REQ-2026-000042",
                "/requests/42",
                Map.of("priority", "HIGH"));

        Message message = gateway.buildMessage(command);

        assertThat(message).isNotNull();
        assertThat(ReflectionTestUtils.getField(message, "token")).isEqualTo("fcm-staff-web-123");
    }

    @Test
    void givenCustomerAndroidEndpoint_whenBuildMessage_thenPopulatesAndroidConfigAndData() {
        PushEndpoint endpoint = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 20L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Usta tayinlandi",
                "Aziz Karimov ta'mirlash uchun tayinlandi",
                "CUSTOMER_TECHNICIAN_ASSIGNED",
                102L,
                42L,
                "REQ-2026-000042",
                "/requests/42",
                null);

        Message message = gateway.buildMessage(command);

        assertThat(message).isNotNull();
    }

    @Test
    void givenTechnicianIosEndpoint_whenBuildMessage_thenPopulatesApnsConfigAndData() {
        PushEndpoint endpoint = PushEndpoint.forTechnician(
                technician,
                PushClientType.TECHNICIAN_MOBILE,
                PushPlatform.IOS,
                PushFirebaseApp.TECHNICIAN_IOS,
                "fid-tech-ios-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 30L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Новая заявка",
                "Вам назначена заявка REQ-2026-000042",
                "TECHNICIAN_JOB_ASSIGNED",
                103L,
                42L,
                "REQ-2026-000042",
                "/jobs/42",
                null);

        Message message = gateway.buildMessage(command);

        assertThat(message).isNotNull();
    }

    @Test
    void givenValidCommand_whenSendSucceeds_thenReturnsSuccessResult() throws Exception {
        PushEndpoint endpoint = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 20L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Title",
                "Body",
                "TEST_TYPE",
                1L,
                null,
                null,
                null,
                null);

        when(messagingClient.send(any(Message.class))).thenReturn("projects/repairauto/messages/msg-123");

        PushDeliveryResult result = gateway.deliver(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.status()).isEqualTo(PushDeliveryStatus.SUCCESS);
        assertThat(result.firebaseMessageId()).isEqualTo("projects/repairauto/messages/msg-123");
        assertThat(result.shouldDisableEndpoint()).isFalse();
    }

    @Test
    void givenUnregisteredFid_whenSendFails_thenClassifiesAsPermanentFailure() throws Exception {
        PushEndpoint endpoint = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 20L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Title",
                "Body",
                "TEST_TYPE",
                1L,
                null,
                null,
                null,
                null);

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(exception.getMessage()).thenReturn("Requested entity was not found.");
        when(messagingClient.send(any(Message.class))).thenThrow(exception);

        PushDeliveryResult result = gateway.deliver(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo(PushDeliveryStatus.PERMANENT_FAILURE);
        assertThat(result.errorCode()).isEqualTo("UNREGISTERED");
        assertThat(result.shouldDisableEndpoint()).isTrue();
    }

    @Test
    void givenInvalidArgument_whenSendFails_thenClassifiesAsInvalidPayloadAndDoesNotDisableEndpoint() throws Exception {
        PushEndpoint endpoint = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 20L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Title",
                "Body",
                "TEST_TYPE",
                1L,
                null,
                null,
                null,
                null);

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(exception.getMessage()).thenReturn("Malformed payload data field.");
        when(messagingClient.send(any(Message.class))).thenThrow(exception);

        PushDeliveryResult result = gateway.deliver(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo(PushDeliveryStatus.INVALID_PAYLOAD);
        assertThat(result.errorCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(result.shouldDisableEndpoint()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MessagingErrorCode.class, names = {"QUOTA_EXCEEDED", "UNAVAILABLE", "INTERNAL"})
    void givenRetryableError_whenSendFails_thenClassifiesAsRetryableFailure(MessagingErrorCode code) throws Exception {
        PushEndpoint endpoint = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 20L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Title",
                "Body",
                "TEST_TYPE",
                1L,
                null,
                null,
                null,
                null);

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        when(exception.getMessage()).thenReturn("Service temporarily unavailable.");
        when(messagingClient.send(any(Message.class))).thenThrow(exception);

        PushDeliveryResult result = gateway.deliver(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo(PushDeliveryStatus.RETRYABLE_FAILURE);
        assertThat(result.errorCode()).isEqualTo(code.name());
        assertThat(result.shouldDisableEndpoint()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MessagingErrorCode.class, names = {"SENDER_ID_MISMATCH", "THIRD_PARTY_AUTH_ERROR"})
    void givenConfigurationError_whenSendFails_thenClassifiesAsConfigurationFailure(MessagingErrorCode code) throws Exception {
        PushEndpoint endpoint = PushEndpoint.forCustomer(
                customer,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "fid-cust-android-123",
                "1.0.0",
                NOW);
        ReflectionTestUtils.setField(endpoint, "id", 20L);

        PushDeliveryCommand command = new PushDeliveryCommand(
                endpoint,
                "Title",
                "Body",
                "TEST_TYPE",
                1L,
                null,
                null,
                null,
                null);

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        when(exception.getMessage()).thenReturn("Sender ID mismatch.");
        when(messagingClient.send(any(Message.class))).thenThrow(exception);

        PushDeliveryResult result = gateway.deliver(command);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo(PushDeliveryStatus.CONFIGURATION_FAILURE);
        assertThat(result.errorCode()).isEqualTo(code.name());
        assertThat(result.shouldDisableEndpoint()).isFalse();
    }

    @Test
    void givenNullCommandFields_whenConstructCommand_thenThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> new PushDeliveryCommand(null, "Title", "Body", "TYPE", 1L, null, null, null, null))
                .isInstanceOf(NullPointerException.class);

        PushEndpoint endpoint = PushEndpoint.forStaff(
                staffAdmin,
                PushClientType.ADMIN_WEB,
                PushPlatform.WEB,
                PushFirebaseApp.ADMIN_WEB,
                "fid-1",
                null,
                NOW);

        assertThatThrownBy(() -> new PushDeliveryCommand(endpoint, " ", "Body", "TYPE", 1L, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PushDeliveryCommand(endpoint, "Title", "", "TYPE", 1L, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PushDeliveryCommand(endpoint, "Title", "Body", "", 1L, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
