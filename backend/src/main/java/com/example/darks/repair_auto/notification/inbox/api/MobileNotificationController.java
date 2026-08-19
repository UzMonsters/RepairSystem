package com.example.darks.repair_auto.notification.inbox.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.notification.inbox.api.dto.UnreadNotificationCountResponse;
import com.example.darks.repair_auto.notification.inbox.api.dto.UserNotificationResponse;
import com.example.darks.repair_auto.notification.inbox.application.UserNotificationService;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/me/notifications")
@Tag(name = "Mobile In-App Notifications", description = "In-app notification inbox and read state management for Customer and Technician mobile apps")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
public class MobileNotificationController {

    private final UserNotificationService service;

    public MobileNotificationController(UserNotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "List Mobile in-app notifications",
            description = "Returns a paginated list of in-app notifications for the authenticated Customer or Technician, ordered newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public PageResponse<UserNotificationResponse> list(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @Parameter(description = "Filter only unread notifications if true")
            @RequestParam(value = "unread", required = false) Boolean unread,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.listForMobile(actor, pageable, unread));
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Get unread notification count",
            description = "Returns the total number of unread notifications for the authenticated Customer or Technician.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread notification count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied or account inactive")
    })
    public UnreadNotificationCountResponse getUnreadCount(
            @AuthenticationPrincipal AuthenticatedMobileActor actor) {
        return service.getUnreadCount(actor);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "Mark single notification as read",
            description = "Marks a specific notification as read for the authenticated Customer or Technician. Idempotent.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Notification not found or not owned by the current actor")
    })
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal AuthenticatedMobileActor actor,
            @PathVariable("notificationId") Long notificationId) {
        service.markAsRead(actor, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all unread notifications as read for the authenticated Customer or Technician.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All notifications marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedMobileActor actor) {
        service.markAllAsRead(actor);
        return ResponseEntity.noContent().build();
    }
}
