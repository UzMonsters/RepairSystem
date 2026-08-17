package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecipientResolver {

    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;

    public NotificationRecipientResolver(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
    }

    public Optional<ResolvedRecipient> resolve(ClaimedNotification notification) {
        if (notification.recipientType() == NotificationRecipientType.CUSTOMER) {
            return customerRepository.findById(notification.recipientId())
                    .filter(customer -> customer.isActive() && customer.getTelegramChatId() != null)
                    .map(customer -> new ResolvedRecipient(
                            customer.getTelegramChatId(),
                            customer.getPreferredLanguage()));
        }
        return technicianRepository.findById(notification.recipientId())
                .filter(technician -> technician.isActive() && technician.getTelegramChatId() != null)
                .map(technician -> new ResolvedRecipient(
                        technician.getTelegramChatId(),
                        technician.getPreferredLanguage()));
    }

    public Optional<LanguageCode> resolveLanguage(NotificationRecipientType recipientType, Long recipientId) {
        if (recipientType == NotificationRecipientType.CUSTOMER) {
            return customerRepository.findById(recipientId).map(customer -> languageOrDefault(customer.getPreferredLanguage()));
        }
        return technicianRepository.findById(recipientId).map(technician -> languageOrDefault(technician.getPreferredLanguage()));
    }

    private LanguageCode languageOrDefault(LanguageCode language) {
        return language == null ? LanguageCode.UZ : language;
    }

    public record ResolvedRecipient(Long chatId, LanguageCode language) {
    }
}
