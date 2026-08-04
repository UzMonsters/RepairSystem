package com.example.darks.repair_auto.catalog.category.application;

import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryDetailResponse;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryMapper;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryReorderRequest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategorySummaryResponse;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryUpdateRequest;
import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepairCategoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepairCategoryService.class);

    private final RepairCategoryRepository repairCategoryRepository;
    private final CategoryNameNormalizer categoryNameNormalizer;

    public RepairCategoryService(
            RepairCategoryRepository repairCategoryRepository,
            CategoryNameNormalizer categoryNameNormalizer) {
        this.repairCategoryRepository = repairCategoryRepository;
        this.categoryNameNormalizer = categoryNameNormalizer;
    }

    @Transactional(readOnly = true)
    public PageResponse<CategorySummaryResponse> list(String search, Boolean active, Pageable pageable) {
        return PageResponse.from(repairCategoryRepository.findAll(filters(blankToNull(search), active), pageable)
                .map(CategoryMapper::summary));
    }

    @Transactional(readOnly = true)
    public CategoryDetailResponse get(Long id) {
        return CategoryMapper.details(find(id));
    }

    @Transactional
    public CategoryDetailResponse create(CategoryCreateRequest request) {
        validateDisplayOrder(request.displayOrder());
        RepairCategory category = new RepairCategory(
                request.nameEn().trim(),
                request.nameRu().trim(),
                request.nameUz().trim(),
                categoryNameNormalizer.normalize(request.nameEn()),
                categoryNameNormalizer.normalize(request.nameRu()),
                categoryNameNormalizer.normalize(request.nameUz()),
                blankToNull(request.descriptionEn()),
                blankToNull(request.descriptionRu()),
                blankToNull(request.descriptionUz()),
                request.displayOrder(),
                request.active(),
                now());
        try {
            RepairCategory saved = repairCategoryRepository.saveAndFlush(category);
            LOGGER.info("Category event operation=category_created result=success categoryId={}", saved.getId());
            return CategoryMapper.details(saved);
        } catch (DataIntegrityViolationException exception) {
            throw categoryConflict(exception);
        }
    }

    @Transactional
    public CategoryDetailResponse update(Long id, CategoryUpdateRequest request) {
        validateDisplayOrder(request.displayOrder());
        RepairCategory category = repairCategoryRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        category.update(
                request.nameEn().trim(),
                request.nameRu().trim(),
                request.nameUz().trim(),
                categoryNameNormalizer.normalize(request.nameEn()),
                categoryNameNormalizer.normalize(request.nameRu()),
                categoryNameNormalizer.normalize(request.nameUz()),
                blankToNull(request.descriptionEn()),
                blankToNull(request.descriptionRu()),
                blankToNull(request.descriptionUz()),
                request.displayOrder(),
                now());
        try {
            return CategoryMapper.details(repairCategoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw categoryConflict(exception);
        }
    }

    @Transactional
    public CategoryDetailResponse changeActivation(Long id, boolean active, String reason) {
        RepairCategory category = repairCategoryRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        category.setActive(active, now());
        LOGGER.info(
                "Category event operation=category_activation_changed result=success categoryId={} active={} reason={}",
                id,
                active,
                reason == null ? "" : reason.trim());
        return CategoryMapper.details(category);
    }

    @Transactional
    public void reorder(CategoryReorderRequest request) {
        var ids = new HashSet<Long>();
        var orders = new HashSet<Integer>();
        for (CategoryReorderRequest.Item item : request.items()) {
            if (!ids.add(item.categoryId())) {
                throw invalidOrder("Duplicate category IDs are not allowed.");
            }
            if (!orders.add(item.displayOrder())) {
                throw invalidOrder("Duplicate display orders are not allowed.");
            }
            validateDisplayOrder(item.displayOrder());
        }
        var categories = repairCategoryRepository.findAllByIdInForUpdate(ids);
        if (categories.size() != ids.size()) {
            throw invalidOrder("Every category in a reorder request must exist.");
        }
        Map<Long, RepairCategory> byId = categories.stream()
                .collect(Collectors.toMap(RepairCategory::getId, Function.identity()));
        OffsetDateTime now = now();
        for (CategoryReorderRequest.Item item : request.items()) {
            byId.get(item.categoryId()).setDisplayOrder(item.displayOrder(), now);
        }
        repairCategoryRepository.saveAllAndFlush(categories);
    }

    private RepairCategory find(Long id) {
        return repairCategoryRepository.findById(id).orElseThrow(this::notFound);
    }

    private void validateDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw invalidOrder("Display order must be non-negative.");
        }
    }

    private BusinessRuleException invalidOrder(String message) {
        return new BusinessRuleException("INVALID_CATEGORY_ORDER", message, 400);
    }

    private BusinessRuleException notFound() {
        return new BusinessRuleException("CATEGORY_NOT_FOUND", "Repair category was not found.", 404);
    }

    private BusinessRuleException categoryConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("name_en")) {
            return new BusinessRuleException(
                    "CATEGORY_NAME_EN_ALREADY_EXISTS",
                    "English category name already exists.",
                    409);
        }
        if (message != null && message.contains("name_ru")) {
            return new BusinessRuleException(
                    "CATEGORY_NAME_RU_ALREADY_EXISTS",
                    "Russian category name already exists.",
                    409);
        }
        return new BusinessRuleException(
                "CATEGORY_NAME_UZ_ALREADY_EXISTS",
                "Uzbek category name already exists.",
                409);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Specification<RepairCategory> filters(String search, Boolean active) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("nameEn")), pattern),
                        builder.like(builder.lower(root.get("nameRu")), pattern),
                        builder.like(builder.lower(root.get("nameUz")), pattern)));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            return predicate;
        };
    }
}
