package com.example.darks.repair_auto.customer.api;

import com.example.darks.repair_auto.customer.api.dto.CustomerActivationRequest;
import com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest;
import com.example.darks.repair_auto.customer.api.dto.CustomerDetailResponse;
import com.example.darks.repair_auto.customer.api.dto.CustomerSummaryResponse;
import com.example.darks.repair_auto.customer.api.dto.CustomerUpdateRequest;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.CustomerRegistrationSource;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "List customers", description = "Requires ADMIN or MANAGER. Page size is limited to 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customers returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page, size, or sort"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required")
    })
    public PageResponse<CustomerSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) LanguageCode language,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) CustomerRegistrationSource registrationSource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdTo,
            @Parameter(description = "Zero-based page index. Default: 0.")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size from 1 to 100. Default: 20.")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sort fields: id, fullName, phone, preferredLanguage, registrationSource, active, createdAt, updatedAt.")
            @RequestParam(required = false) List<String> sort) {
        return customerService.list(
                search,
                phone,
                language,
                active,
                registrationSource,
                createdFrom,
                createdTo,
                CustomerPageRequest.toPageable(page, size, sort));
    }

    @GetMapping("/{id}")
    public CustomerDetailResponse get(@PathVariable Long id) {
        return customerService.get(id);
    }

    @PostMapping
    public CustomerDetailResponse create(@Valid @RequestBody CustomerCreateRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    public CustomerDetailResponse update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(id, request);
    }

    @PatchMapping("/{id}/activation")
    public CustomerDetailResponse changeActivation(
            @PathVariable Long id,
            @Valid @RequestBody CustomerActivationRequest request) {
        return customerService.changeActivation(id, request.active(), request.reason());
    }

    @GetMapping("/{id}/avatar")
    @Operation(summary = "Get customer avatar image stream", description = "Streams customer avatar. Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer avatar returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "Customer or avatar not found")
    })
    public ResponseEntity<InputStreamResource> getAvatar(@PathVariable Long id) {
        AttachmentDownload download = customerService.downloadAvatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.fileName())
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new InputStreamResource(download.inputStream()));
    }

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace customer avatar", description = "Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or unsupported image format"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "503", description = "Storage operation failed")
    })
    public AvatarResponse uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return customerService.uploadAvatar(id, file);
    }

    @DeleteMapping("/{id}/avatar")
    @Operation(summary = "Remove customer avatar", description = "Requires ADMIN or MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<Void> deleteAvatar(@PathVariable Long id) {
        customerService.deleteAvatar(id);
        return ResponseEntity.noContent().build();
    }
}
