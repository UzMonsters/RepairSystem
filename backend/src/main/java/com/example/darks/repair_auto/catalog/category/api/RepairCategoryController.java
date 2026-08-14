package com.example.darks.repair_auto.catalog.category.api;

import com.example.darks.repair_auto.catalog.category.api.dto.CategoryActivationRequest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryDetailResponse;
import com.example.darks.repair_auto.catalog.category.api.dto.CategorySummaryResponse;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryUpdateRequest;
import com.example.darks.repair_auto.catalog.category.application.RepairCategoryService;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@SecurityRequirement(name = "bearerAuth")
public class RepairCategoryController {

    private final RepairCategoryService repairCategoryService;

    public RepairCategoryController(RepairCategoryService repairCategoryService) {
        this.repairCategoryService = repairCategoryService;
    }

    @GetMapping
    @Operation(summary = "List repair categories", description = "Requires ADMIN or MANAGER. Defaults to id ascending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page, size, or sort"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or MANAGER role required")
    })
    public PageResponse<CategorySummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Zero-based page index. Default: 0.")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size from 1 to 100. Default: 20.")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sort fields: id, nameEn, nameRu, nameUz, active, createdAt, updatedAt.")
            @RequestParam(required = false) List<String> sort) {
        return repairCategoryService.list(search, active, CategoryPageRequest.toPageable(page, size, sort));
    }

    @GetMapping("/{id}")
    public CategoryDetailResponse get(@PathVariable Long id) {
        return repairCategoryService.get(id);
    }

    @PostMapping
    public CategoryDetailResponse create(@Valid @RequestBody CategoryCreateRequest request) {
        return repairCategoryService.create(request);
    }

    @PutMapping("/{id}")
    public CategoryDetailResponse update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return repairCategoryService.update(id, request);
    }

    @PatchMapping("/{id}/activation")
    public CategoryDetailResponse changeActivation(
            @PathVariable Long id,
            @Valid @RequestBody CategoryActivationRequest request) {
        return repairCategoryService.changeActivation(id, request.active(), request.reason());
    }
}
