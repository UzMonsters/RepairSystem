package com.example.darks.repair_auto.notification.api;

import com.example.darks.repair_auto.notification.api.dto.NotificationDeliveryResponse;
import com.example.darks.repair_auto.notification.api.dto.NotificationRetryRequest;
import com.example.darks.repair_auto.notification.api.dto.NotificationSummaryResponse;
import com.example.darks.repair_auto.notification.application.NotificationAdminService;
import com.example.darks.repair_auto.notification.application.NotificationQuery;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationStatus;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class NotificationAdminController {

    private final NotificationAdminService notificationAdminService;
    private final EffectiveLanguageResolver effectiveLanguageResolver;

    public NotificationAdminController(
            NotificationAdminService notificationAdminService,
            EffectiveLanguageResolver effectiveLanguageResolver) {
        this.notificationAdminService = notificationAdminService;
        this.effectiveLanguageResolver = effectiveLanguageResolver;
    }

    @GetMapping("/api/v1/notifications")
    @Operation(summary = "List notifications")
    public PageResponse<NotificationSummaryResponse> list(
            @RequestParam(name = "deliveryStatus", required = false) NotificationStatus status,
            @RequestParam(name = "type", required = false) NotificationType notificationType,
            @RequestParam(required = false) NotificationRecipientType recipientType,
            @RequestParam(required = false) Long repairRequestId,
            @RequestParam(required = false) OffsetDateTime createdFrom,
            @RequestParam(required = false) OffsetDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return notificationAdminService.list(
                new NotificationQuery(
                        status,
                        notificationType,
                        recipientType,
                        repairRequestId,
                        createdFrom,
                        createdTo),
                NotificationPageRequest.toPageable(page, size, sort),
                effectiveLanguageResolver.resolveEffectiveLanguage());
    }

    @GetMapping("/api/v1/admin/notification-deliveries")
    @Operation(summary = "List notification deliveries")
    public PageResponse<NotificationDeliveryResponse> deliveries(
            @RequestParam(name = "deliveryStatus", required = false) NotificationStatus status,
            @RequestParam(name = "type", required = false) NotificationType notificationType,
            @RequestParam(required = false) NotificationRecipientType recipientType,
            @RequestParam(required = false) Long repairRequestId,
            @RequestParam(required = false) OffsetDateTime createdFrom,
            @RequestParam(required = false) OffsetDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return notificationAdminService.listDeliveries(
                new NotificationQuery(
                        status,
                        notificationType,
                        recipientType,
                        repairRequestId,
                        createdFrom,
                        createdTo),
                NotificationPageRequest.toPageable(page, size, sort));
    }

    @GetMapping({"/api/v1/notifications/{deliveryId}", "/api/v1/admin/notification-deliveries/{deliveryId}"})
    @Operation(summary = "Get notification delivery details")
    public NotificationDeliveryResponse getDelivery(@PathVariable Long deliveryId) {
        return notificationAdminService.getDelivery(deliveryId);
    }

    @PostMapping("/api/v1/admin/notification-deliveries/{deliveryId}/retry")
    @Operation(summary = "Retry an eligible failed Telegram notification")
    public NotificationDeliveryResponse retry(
            @PathVariable Long deliveryId,
            @Valid @RequestBody NotificationRetryRequest request) {
        return notificationAdminService.retry(deliveryId, request.reason());
    }
}
