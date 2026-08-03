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

@Service
public class DashboardService {

    private static final int MIN_CATEGORY_LIMIT = 1;
    private static final int DEFAULT_CATEGORY_LIMIT = 10;
    private static final int MAX_CATEGORY_LIMIT = 20;
    private static final int SCALE = 2;

    private final DashboardQueryRepository repository;
    private final DashboardTimeService timeService;
    private final Clock clock;

    public DashboardService(DashboardQueryRepository repository, DashboardTimeService timeService, Clock clock) {
        this.repository = repository;
        this.timeService = timeService;
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
        for (RepairRequestStatus status : RepairRequestStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            items.add(new RequestStatusDistributionItemResponse(
                    status,
                    label(status),
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
        List<RequestCategoryDistributionItemResponse> items = rows.stream()
                .limit(limit)
                .map(row -> new RequestCategoryDistributionItemResponse(
                        row.categoryId(),
                        row.nameEn(),
                        row.nameRu(),
                        row.nameUz(),
                        row.count(),
                        percentage(row.count(), total)))
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

    private DashboardStatusLabelResponse label(RepairRequestStatus status) {
        return switch (status) {
            case NEW -> new DashboardStatusLabelResponse("New", "Новая", "Yangi");
            case ASSIGNED -> new DashboardStatusLabelResponse("Assigned", "Назначена", "Biriktirilgan");
            case SCHEDULED -> new DashboardStatusLabelResponse("Scheduled", "Запланирована", "Rejalashtirilgan");
            case IN_PROGRESS -> new DashboardStatusLabelResponse("In progress", "В работе", "Jarayonda");
            case WAITING_FOR_PARTS -> new DashboardStatusLabelResponse(
                    "Waiting for parts",
                    "Ожидает запчасти",
                    "Ehtiyot qismlar kutilmoqda");
            case COMPLETED -> new DashboardStatusLabelResponse("Completed", "Завершена", "Yakunlangan");
            case CANCELLED -> new DashboardStatusLabelResponse("Cancelled", "Отменена", "Bekor qilingan");
        };
    }
}
