package com.example.darks.repair_auto.telegram.technician.api;

import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.telegram.technician.api.dto.TechnicianTelegramLinkResponse;
import com.example.darks.repair_auto.telegram.technician.application.TechnicianTelegramLinkService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/technicians/{technicianId}/telegram-link")
public class TechnicianTelegramLinkController {

    private final TechnicianTelegramLinkService linkService;

    public TechnicianTelegramLinkController(TechnicianTelegramLinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping
    public TechnicianTelegramLinkResponse create(
            @PathVariable Long technicianId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return linkService.create(technicianId, user);
    }

    @DeleteMapping
    public void unlink(@PathVariable Long technicianId) {
        linkService.unlink(technicianId);
    }
}
