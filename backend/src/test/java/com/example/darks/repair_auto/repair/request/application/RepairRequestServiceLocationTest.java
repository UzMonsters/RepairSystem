package com.example.darks.repair_auto.repair.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationOutboxService;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.application.RepairStatusHistoryService;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RequestLocationRequest;
import com.example.darks.repair_auto.repair.request.domain.RequestLocationSource;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestNumberGenerator;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepairRequestServiceLocationTest {

    private RepairRequestService service;

    @BeforeEach
    void setUp() {
        service = new RepairRequestService(
                mock(RepairRequestRepository.class),
                mock(CustomerRepository.class),
                mock(RepairCategoryRepository.class),
                mock(UserRepository.class),
                mock(RepairAssignmentRepository.class),
                mock(RepairExecutionRepository.class),
                mock(RepairStatusHistoryService.class),
                mock(RepairRequestNumberGenerator.class),
                mock(PhoneNumberNormalizer.class),
                mock(NotificationEventFactory.class),
                mock(NotificationOutboxService.class),
                mock(EffectiveLanguageResolver.class),
                mock(LocalizedValueResolver.class),
                Clock.systemUTC());
    }

    @Test
    void givenNullLocation_whenValidateLocation_thenReturnsNullFields() {
        var location = service.validateLocation(null, null);

        assertThat(location.address()).isNull();
        assertThat(location.latitude()).isNull();
        assertThat(location.longitude()).isNull();
        assertThat(location.source()).isNull();
    }

    @Test
    void givenEmptyLocation_whenValidateLocation_thenReturnsNullFields() {
        var location = service.validateLocation(new RequestLocationRequest(null, null, "   ", null), null);

        assertThat(location.address()).isNull();
        assertThat(location.latitude()).isNull();
        assertThat(location.longitude()).isNull();
        assertThat(location.source()).isNull();
    }

    @Test
    void givenCoordinatesOnlyWithoutSource_whenValidateLocation_thenInfersDeviceGpsSource() {
        var location = service.validateLocation(
                new RequestLocationRequest(new BigDecimal("41.3110810"), new BigDecimal("69.2405620"), null, null),
                null);

        assertThat(location.address()).isNull();
        assertThat(location.latitude()).isEqualTo(new BigDecimal("41.3110810"));
        assertThat(location.longitude()).isEqualTo(new BigDecimal("69.2405620"));
        assertThat(location.source()).isEqualTo(RequestLocationSource.DEVICE_GPS);
    }

    @Test
    void givenAddressOnlyWithoutSource_whenValidateLocation_thenInfersManualSource() {
        var location = service.validateLocation(
                new RequestLocationRequest(null, null, "Tashkent, Uzbekistan", null),
                null);

        assertThat(location.address()).isEqualTo("Tashkent, Uzbekistan");
        assertThat(location.latitude()).isNull();
        assertThat(location.longitude()).isNull();
        assertThat(location.source()).isEqualTo(RequestLocationSource.MANUAL);
    }

    @Test
    void givenCoordinatesAndAddressWithMapPinSource_whenValidateLocation_thenPreservesMapPinSource() {
        var location = service.validateLocation(
                new RequestLocationRequest(
                        new BigDecimal("41.3110810"),
                        new BigDecimal("69.2405620"),
                        "Tashkent, Uzbekistan",
                        RequestLocationSource.MAP_PIN),
                null);

        assertThat(location.address()).isEqualTo("Tashkent, Uzbekistan");
        assertThat(location.latitude()).isEqualTo(new BigDecimal("41.3110810"));
        assertThat(location.longitude()).isEqualTo(new BigDecimal("69.2405620"));
        assertThat(location.source()).isEqualTo(RequestLocationSource.MAP_PIN);
    }

    @Test
    void givenTelegramEnforcedSource_whenValidateLocation_thenSetsTelegramSource() {
        var location = service.validateLocation(
                new RequestLocationRequest(
                        new BigDecimal("41.3110810"),
                        new BigDecimal("69.2405620"),
                        null,
                        null),
                RequestLocationSource.TELEGRAM);

        assertThat(location.source()).isEqualTo(RequestLocationSource.TELEGRAM);
    }

    @Test
    void givenClientSendsTelegramSourceViaRest_whenValidateLocation_thenThrowsInvalidSource() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(
                                new BigDecimal("41.3110810"),
                                new BigDecimal("69.2405620"),
                                null,
                                RequestLocationSource.TELEGRAM),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_SOURCE_INVALID);
    }

    @Test
    void givenLatitudeAbove90_whenValidateLocation_thenThrowsLatitudeInvalid() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(new BigDecimal("90.0000001"), new BigDecimal("69.0"), null, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_LATITUDE_INVALID);
    }

    @Test
    void givenLatitudeBelowMinus90_whenValidateLocation_thenThrowsLatitudeInvalid() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(new BigDecimal("-90.0000001"), new BigDecimal("69.0"), null, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_LATITUDE_INVALID);
    }

    @Test
    void givenLongitudeAbove180_whenValidateLocation_thenThrowsLongitudeInvalid() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(new BigDecimal("41.0"), new BigDecimal("180.0000001"), null, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_LONGITUDE_INVALID);
    }

    @Test
    void givenLongitudeBelowMinus180_whenValidateLocation_thenThrowsLongitudeInvalid() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(new BigDecimal("41.0"), new BigDecimal("-180.0000001"), null, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_LONGITUDE_INVALID);
    }

    @Test
    void givenLatitudeWithoutLongitude_whenValidateLocation_thenThrowsCoordinatesIncomplete() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(new BigDecimal("41.0"), null, null, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_COORDINATES_INCOMPLETE);
    }

    @Test
    void givenLongitudeWithoutLatitude_whenValidateLocation_thenThrowsCoordinatesIncomplete() {
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(null, new BigDecimal("69.0"), null, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_COORDINATES_INCOMPLETE);
    }

    @Test
    void givenAddressExceeding500Chars_whenValidateLocation_thenThrowsAddressTooLong() {
        String longAddress = "A".repeat(501);
        BusinessException exception = catchThrowableOfType(
                () -> service.validateLocation(
                        new RequestLocationRequest(null, null, longAddress, null),
                        null),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REQUEST_LOCATION_ADDRESS_TOO_LONG);
    }
}
