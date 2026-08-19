package com.example.darks.repair_auto.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationChannelResolverTest {

    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private NotificationChannelResolver resolver;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        resolver = new NotificationChannelResolver(customerRepository, technicianRepository);
    }

    @Test
    void givenCustomerWithTelegram_whenResolve_thenReturnsTelegramAndPush() {
        Customer customer = new Customer("Ali", "+998901234567", LanguageCode.UZ, OffsetDateTime.now());
        customer.linkTelegram(1001L, 123456789L, LanguageCode.UZ, OffsetDateTime.now());

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        Set<NotificationChannel> channels = resolver.resolve(
                NotificationType.REPAIR_COMPLETED,
                NotificationRecipientType.CUSTOMER,
                42L);

        assertThat(channels).containsExactlyInAnyOrder(NotificationChannel.TELEGRAM, NotificationChannel.PUSH);
    }

    @Test
    void givenCustomerWithoutTelegram_whenResolve_thenReturnsPushOnly() {
        Customer customer = new Customer("Ali", "+998901234567", LanguageCode.UZ, OffsetDateTime.now());

        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));

        Set<NotificationChannel> channels = resolver.resolve(
                NotificationType.REPAIR_COMPLETED,
                NotificationRecipientType.CUSTOMER,
                42L);

        assertThat(channels).containsExactly(NotificationChannel.PUSH);
    }

    @Test
    void givenTechnicianWithTelegram_whenResolve_thenReturnsTelegramAndPush() {
        Technician technician = new Technician("Aziz", "+998901112233", "Cooling", "Notes", 5, LanguageCode.RU, true, OffsetDateTime.now());
        technician.linkTelegram(2001L, 987654321L, OffsetDateTime.now());

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        Set<NotificationChannel> channels = resolver.resolve(
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.TECHNICIAN,
                17L);

        assertThat(channels).containsExactlyInAnyOrder(NotificationChannel.TELEGRAM, NotificationChannel.PUSH);
    }

    @Test
    void givenTechnicianWithoutTelegram_whenResolve_thenReturnsPushOnly() {
        Technician technician = new Technician("Aziz", "+998901112233", "Cooling", "Notes", 5, LanguageCode.RU, true, OffsetDateTime.now());

        when(technicianRepository.findById(17L)).thenReturn(Optional.of(technician));

        Set<NotificationChannel> channels = resolver.resolve(
                NotificationType.TECHNICIAN_ASSIGNED,
                NotificationRecipientType.TECHNICIAN,
                17L);

        assertThat(channels).containsExactly(NotificationChannel.PUSH);
    }

    @Test
    void givenStaffRecipient_whenResolve_thenReturnsPushOnly() {
        Set<NotificationChannel> channels = resolver.resolve(
                NotificationType.REQUEST_CREATED,
                NotificationRecipientType.STAFF,
                1L);

        assertThat(channels).containsExactly(NotificationChannel.PUSH);
    }
}
