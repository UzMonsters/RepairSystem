package com.example.darks.repair_auto.dashboard.application;

import com.example.darks.repair_auto.dashboard.api.dto.DashboardOverviewResponse;
import com.example.darks.repair_auto.dashboard.api.dto.DashboardStatusLabelResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestCategoryDistributionItemResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestCategoryDistributionResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestCategoryOtherResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestStatusDistributionItemResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestStatusDistributionResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestTrendBucketResponse;
import com.example.darks.repair_auto.dashboard.api.dto.RequestTrendResponse;
import com.example.darks.repair_auto.dashboard.api.dto.ReviewDashboardResponse;
import com.example.darks.repair_auto.dashboard.api.dto.ReviewRatingDistributionResponse;
import com.example.darks.repair_auto.dashboard.api.dto.TechnicianDashboardResponse;
import com.example.darks.repair_auto.dashboard.domain.DashboardPeriod;
import com.example.darks.repair_auto.dashboard.infrastructure.persistence.DashboardQueryRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.settings.domain.Language;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DashboardService {

    private static final int MIN_CATEGORY_LIMIT = 1;
    private static final int DEFAULT_CATEGORY_LIMIT = 10;
    private static final int MAX_CATEGORY_LIMIT = 20;
    private static final int SCALE = 2;

    private final DashboardQueryRepository repository;
    private final DashboardTimeService timeService;
    private final EffectiveLanguageResolver effectiveLanguageResolver;
    private final LocalizedValueResolver localizedValueResolver;
    private final Clock clock;

    @Autowired
    public DashboardService(
            DashboardQueryRepository repository,
            DashboardTimeService timeService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver) {
        this(repository, timeService, effectiveLanguageResolver, localizedValueResolver, Clock.systemUTC());
    }

    public DashboardService(
            DashboardQueryRepository repository,
            DashboardTimeService timeService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            Clock clock) {
        this.repository = repository;
        this.timeService = timeService;
        this.effectiveLanguageResolver = effectiveLanguageResolver;
        this.localizedValueResolver = localizedValueResolver;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewResponse overview() {
        OffsetDateTime generatedAt = generatedAt();
        DashboardTimeRange today = timeService.todayRange(generatedAt);
        DashboardQueryRepository.OverviewCounts counts = repository.overview(today);
        return new DashboardOverviewResponse(
                generatedAt,
                today.fromDate(),
                counts.totalRequests(),
                counts.newToday(),
                counts.openRequests(),
                counts.inProgress(),
                counts.waitingForParts(),
                counts.completedToday(),
                counts.completedTotal(),
                counts.cancelledTotal(),
                counts.activeTechnicians(),
                counts.techniciansWithActiveWork(),
                counts.pendingAssignments(),
                average(counts.ratingSum(), counts.totalReviews()),
                counts.totalReviews());
    }

    @Transactional(readOnly = true)
    public RequestTrendResponse requestTrends(String periodValue) {
        DashboardPeriod period = DashboardPeriod.parse(periodValue);
        DashboardTimeRange range = timeService.periodRange(period, generatedAt());
        Map<LocalDate, Long> created = repository.createdByDay(range, timeService.businessZone());
        Map<LocalDate, Long> completed = repository.completedByDay(range, timeService.businessZone());
        Map<LocalDate, Long> cancelled = repository.cancelledByDay(range, timeService.businessZone());
        List<RequestTrendBucketResponse> buckets = new ArrayList<>();
        for (int index = 0; index < period.days(); index++) {
            LocalDate date = range.fromDate().plusDays(index);
            buckets.add(new RequestTrendBucketResponse(
                    date,
                    created.getOrDefault(date, 0L),
                    completed.getOrDefault(date, 0L),
                    cancelled.getOrDefault(date, 0L)));
        }
        return new RequestTrendResponse(period, range.fromDate(), range.toDate(), buckets);
    }

    @Transactional(readOnly = true)
    public RequestStatusDistributionResponse requestsByStatus() {
        Map<RepairRequestStatus, Long> counts = repository.requestStatusCounts();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        List<RequestStatusDistributionItemResponse> items = new ArrayList<>();
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        for (RepairRequestStatus status : RepairRequestStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            items.add(new RequestStatusDistributionItemResponse(
                    status,
                    label(status, lang),
                    count,
                    percentage(count, total)));
        }
        return new RequestStatusDistributionResponse(total, items);
    }

    @Transactional(readOnly = true)
    public RequestCategoryDistributionResponse requestsByCategory(String periodValue, Integer limitValue) {
        DashboardPeriod period = DashboardPeriod.parse(periodValue);
        int limit = categoryLimit(limitValue);
        DashboardTimeRange range = timeService.periodRange(period, generatedAt());
        List<DashboardQueryRepository.CategoryCount> rows = repository.requestCategoryCounts(range);
        long total = rows.stream().mapToLong(DashboardQueryRepository.CategoryCount::count).sum();
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        List<RequestCategoryDistributionItemResponse> items = rows.stream()
                .limit(limit)
                .map(row -> {
                    String resolvedName = localizedValueResolver.resolve(lang, row.nameUz(), row.nameRu(), row.nameEn());
                    return new RequestCategoryDistributionItemResponse(
                            row.categoryId(),
                            resolvedName,
                            row.nameEn(),
                            row.nameRu(),
                            row.nameUz(),
                            row.count(),
                            percentage(row.count(), total));
                })
                .toList();
        long visible = items.stream().mapToLong(RequestCategoryDistributionItemResponse::count).sum();
        long otherCount = Math.max(0, total - visible);
        return new RequestCategoryDistributionResponse(
                period,
                total,
                items,
                new RequestCategoryOtherResponse(otherCount, percentage(otherCount, total)));
    }

    @Transactional(readOnly = true)
    public TechnicianDashboardResponse technicians() {
        DashboardQueryRepository.TechnicianCounts counts = repository.technicians();
        long withoutActiveWork = Math.max(0, counts.activeTechnicians() - counts.techniciansWithActiveWork());
        long availableCapacity = Math.max(0, counts.totalCapacity() - counts.activeAssignments());
        return new TechnicianDashboardResponse(
                counts.activeTechnicians(),
                counts.inactiveTechnicians(),
                counts.techniciansWithActiveWork(),
                withoutActiveWork,
                counts.pendingAssignments(),
                counts.acceptedAssignments(),
                counts.inProgressRequests(),
                counts.waitingForPartsRequests(),
                availableCapacity,
                counts.totalCapacity());
    }

    @Transactional(readOnly = true)
    public ReviewDashboardResponse reviews() {
        DashboardQueryRepository.ReviewCounts counts = repository.reviews();
        return new ReviewDashboardResponse(
                counts.totalReviews(),
                average(counts.ratingSum(), counts.totalReviews()),
                counts.reviewsWithComment(),
                new ReviewRatingDistributionResponse(
                        counts.rating1(),
                        counts.rating2(),
                        counts.rating3(),
                        counts.rating4(),
                        counts.rating5()));
    }

    private int categoryLimit(Integer value) {
        int limit = value == null ? DEFAULT_CATEGORY_LIMIT : value;
        if (limit < MIN_CATEGORY_LIMIT || limit > MAX_CATEGORY_LIMIT) {
            throw new BusinessRuleException(
                    "DASHBOARD_CATEGORY_LIMIT_INVALID",
                    "Dashboard category limit must be between 1 and 20.",
                    400);
        }
        return limit;
    }

    private OffsetDateTime generatedAt() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private BigDecimal average(BigDecimal sum, long count) {
        if (sum == null || count == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_UP);
    }

    private DashboardStatusLabelResponse label(RepairRequestStatus status, Language language) {
        String en;
        String ru;
        String uz;
        switch (status) {
            case NEW -> { en = "New"; ru = "Новая"; uz = "Yangi"; }
            case ASSIGNED -> { en = "Assigned"; ru = "Назначена"; uz = "Biriktirilgan"; }
            case SCHEDULED -> { en = "Scheduled"; ru = "Запланирована"; uz = "Rejalashtirilgan"; }
            case IN_PROGRESS -> { en = "In progress"; ru = "В работе"; uz = "Jarayonda"; }
            case WAITING_FOR_PARTS -> { en = "Waiting for parts"; ru = "Ожидает запчасти"; uz = "Ehtiyot qismlar kutilmoqda"; }
            case COMPLETED -> { en = "Completed"; ru = "Завершена"; uz = "Yakunlangan"; }
            case CANCELLED -> { en = "Cancelled"; ru = "Отменена"; uz = "Bekor qilingan"; }
            default -> { en = status.name(); ru = status.name(); uz = status.name(); }
        }
        String resolvedLabel = localizedValueResolver.resolve(language, uz, ru, en);
        return new DashboardStatusLabelResponse(resolvedLabel, en, ru, uz);
    }
}
