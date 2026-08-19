package com.example.darks.repair_auto.notification.application;

import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.notification.domain.NotificationChannel;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NotificationChannelResolver {

    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;

    public NotificationChannelResolver(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
    }

    public Set<NotificationChannel> resolve(
            NotificationType type,
            NotificationRecipientType recipientType,
            Long recipientId) {
        Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);

        if (recipientType == NotificationRecipientType.CUSTOMER) {
            channels.add(NotificationChannel.PUSH);
            if (recipientId != null) {
                customerRepository.findById(recipientId).ifPresent(customer -> {
                    if (customer.isActive() && customer.getTelegramChatId() != null) {
                        channels.add(NotificationChannel.TELEGRAM);
                    }
                });
            }
        } else if (recipientType == NotificationRecipientType.TECHNICIAN) {
            channels.add(NotificationChannel.PUSH);
            if (recipientId != null) {
                technicianRepository.findById(recipientId).ifPresent(technician -> {
                    if (technician.isActive() && technician.getTelegramChatId() != null) {
                        channels.add(NotificationChannel.TELEGRAM);
                    }
                });
            }
        } else if (recipientType == NotificationRecipientType.STAFF) {
            channels.add(NotificationChannel.PUSH);
        }

        if (channels.isEmpty()) {
            channels.add(NotificationChannel.TELEGRAM);
        }

        return channels;
    }
}
