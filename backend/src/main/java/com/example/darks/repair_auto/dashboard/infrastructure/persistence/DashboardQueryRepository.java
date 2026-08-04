package com.example.darks.repair_auto.dashboard.infrastructure.persistence;

import com.example.darks.repair_auto.dashboard.application.DashboardTimeRange;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public OverviewCounts overview(DashboardTimeRange today) {
        return jdbcTemplate.queryForObject("""
                select
                    (select count(*) from repair_requests) as total_requests,
                    (select count(*) from repair_requests
                        where created_at >= ? and created_at < ?) as new_today,
                    (select count(*) from repair_requests
                        where status in ('NEW', 'ASSIGNED', 'SCHEDULED', 'IN_PROGRESS', 'WAITING_FOR_PARTS')) as open_requests,
                    (select count(*) from repair_requests where status = 'IN_PROGRESS') as in_progress,
                    (select count(*) from repair_requests where status = 'WAITING_FOR_PARTS') as waiting_for_parts,
                    (select count(*) from repair_executions
                        where completed_at >= ? and completed_at < ?) as completed_today,
                    (select count(*) from repair_requests where status = 'COMPLETED') as completed_total,
                    (select count(*) from repair_requests where status = 'CANCELLED') as cancelled_total,
                    (select count(*) from technicians where active = true) as active_technicians,
                    (select count(distinct a.technician_id)
                        from repair_assignments a
                        join technicians t on t.id = a.technician_id
                        where t.active = true and a.status = 'ACCEPTED') as technicians_with_active_work,
                    (select count(*) from repair_assignments where status = 'PENDING') as pending_assignments,
                    (select count(*) from repair_reviews) as total_reviews,
                    (select sum(rating) from repair_reviews) as rating_sum
                """, this::overviewCounts,
                today.fromInclusive(),
                today.toExclusive(),
                today.fromInclusive(),
                today.toExclusive());
    }

    public Map<LocalDate, Long> createdByDay(DashboardTimeRange range, ZoneId businessZone) {
        return dailyCounts("""
                select cast(created_at at time zone ? as date) as bucket_date, count(*) as count
                from repair_requests
                where created_at >= ? and created_at < ?
                group by bucket_date
                """, range, businessZone);
    }

    public Map<LocalDate, Long> completedByDay(DashboardTimeRange range, ZoneId businessZone) {
        return dailyCounts("""
                select cast(completed_at at time zone ? as date) as bucket_date, count(*) as count
                from repair_executions
                where completed_at >= ? and completed_at < ?
                group by bucket_date
                """, range, businessZone);
    }

    public Map<LocalDate, Long> cancelledByDay(DashboardTimeRange range, ZoneId businessZone) {
        return dailyCounts("""
                select cast(cancelled_at at time zone ? as date) as bucket_date, count(*) as count
                from repair_executions
                where cancelled_at >= ? and cancelled_at < ?
                group by bucket_date
                """, range, businessZone);
    }

    public Map<RepairRequestStatus, Long> requestStatusCounts() {
        List<StatusCount> rows = jdbcTemplate.query("""
                select status, count(*) as count
                from repair_requests
                group by status
                """, (rs, rowNum) -> new StatusCount(
                RepairRequestStatus.valueOf(rs.getString("status")),
                rs.getLong("count")));
        Map<RepairRequestStatus, Long> counts = new EnumMap<>(RepairRequestStatus.class);
        for (StatusCount row : rows) {
            counts.put(row.status(), row.count());
        }
        return counts;
    }

    public List<CategoryCount> requestCategoryCounts(DashboardTimeRange range) {
        return jdbcTemplate.query("""
                select c.id as category_id,
                       c.name_en,
                       c.name_ru,
                       c.name_uz,
                       count(*) as count
                from repair_requests r
                join repair_categories c on c.id = r.category_id
                where r.created_at >= ? and r.created_at < ?
                group by c.id, c.name_en, c.name_ru, c.name_uz
                order by count(*) desc, c.id asc
                """, this::categoryCount, range.fromInclusive(), range.toExclusive());
    }

    public TechnicianCounts technicians() {
        return jdbcTemplate.queryForObject("""
                select
                    (select count(*) from technicians where active = true) as active_technicians,
                    (select count(*) from technicians where active = false) as inactive_technicians,
                    (select count(distinct a.technician_id)
                        from repair_assignments a
                        join technicians t on t.id = a.technician_id
                        where t.active = true and a.status in ('PENDING', 'ACCEPTED')) as technicians_with_active_work,
                    (select count(*) from repair_assignments where status = 'PENDING') as pending_assignments,
                    (select count(*) from repair_assignments where status = 'ACCEPTED') as accepted_assignments,
                    (select count(distinct r.id)
                        from repair_requests r
                        join repair_assignments a on a.repair_request_id = r.id
                        join technicians t on t.id = a.technician_id
                        where t.active = true
                          and a.status = 'ACCEPTED'
                          and r.status = 'IN_PROGRESS') as in_progress_requests,
                    (select count(distinct r.id)
                        from repair_requests r
                        join repair_assignments a on a.repair_request_id = r.id
                        join technicians t on t.id = a.technician_id
                        where t.active = true
                          and a.status = 'ACCEPTED'
                          and r.status = 'WAITING_FOR_PARTS') as waiting_for_parts_requests,
                    (select coalesce(sum(maximum_concurrent_requests), 0)
                        from technicians
                        where active = true) as total_capacity,
                    (select count(*)
                        from repair_assignments a
                        join technicians t on t.id = a.technician_id
                        where t.active = true and a.status in ('PENDING', 'ACCEPTED')) as active_assignments
                """, this::technicianCounts);
    }

    public ReviewCounts reviews() {
        return jdbcTemplate.queryForObject("""
                select
                    count(*) as total_reviews,
                    sum(rating) as rating_sum,
                    count(*) filter (where comment is not null) as reviews_with_comment,
                    count(*) filter (where rating = 1) as rating1,
                    count(*) filter (where rating = 2) as rating2,
                    count(*) filter (where rating = 3) as rating3,
                    count(*) filter (where rating = 4) as rating4,
                    count(*) filter (where rating = 5) as rating5
                from repair_reviews
                """, this::reviewCounts);
    }

    public List<String> explainCategoryDistribution(DashboardTimeRange range) {
        return jdbcTemplate.queryForList("""
                explain
                select c.id, count(*)
                from repair_requests r
                join repair_categories c on c.id = r.category_id
                where r.created_at >= ? and r.created_at < ?
                group by c.id
                order by count(*) desc, c.id asc
                """, String.class, range.fromInclusive(), range.toExclusive());
    }

    private Map<LocalDate, Long> dailyCounts(String sql, DashboardTimeRange range, ZoneId businessZone) {
        List<DailyCount> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DailyCount(rs.getObject("bucket_date", LocalDate.class), rs.getLong("count")),
                businessZone.getId(),
                range.fromInclusive(),
                range.toExclusive());
        Map<LocalDate, Long> counts = new java.util.HashMap<>();
        for (DailyCount row : rows) {
            counts.put(row.date(), row.count());
        }
        return counts;
    }

    private OverviewCounts overviewCounts(ResultSet rs, int rowNum) throws SQLException {
        return new OverviewCounts(
                rs.getLong("total_requests"),
                rs.getLong("new_today"),
                rs.getLong("open_requests"),
                rs.getLong("in_progress"),
                rs.getLong("waiting_for_parts"),
                rs.getLong("completed_today"),
                rs.getLong("completed_total"),
                rs.getLong("cancelled_total"),
                rs.getLong("active_technicians"),
                rs.getLong("technicians_with_active_work"),
                rs.getLong("pending_assignments"),
                rs.getLong("total_reviews"),
                nullableBigDecimal(rs, "rating_sum"));
    }

    private CategoryCount categoryCount(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryCount(
                rs.getLong("category_id"),
                rs.getString("name_en"),
                rs.getString("name_ru"),
                rs.getString("name_uz"),
                rs.getLong("count"));
    }

    private TechnicianCounts technicianCounts(ResultSet rs, int rowNum) throws SQLException {
        return new TechnicianCounts(
                rs.getLong("active_technicians"),
                rs.getLong("inactive_technicians"),
                rs.getLong("technicians_with_active_work"),
                rs.getLong("pending_assignments"),
                rs.getLong("accepted_assignments"),
                rs.getLong("in_progress_requests"),
                rs.getLong("waiting_for_parts_requests"),
                rs.getLong("total_capacity"),
                rs.getLong("active_assignments"));
    }

    private ReviewCounts reviewCounts(ResultSet rs, int rowNum) throws SQLException {
        return new ReviewCounts(
                rs.getLong("total_reviews"),
                nullableBigDecimal(rs, "rating_sum"),
                rs.getLong("reviews_with_comment"),
                rs.getLong("rating1"),
                rs.getLong("rating2"),
                rs.getLong("rating3"),
                rs.getLong("rating4"),
                rs.getLong("rating5"));
    }

    private BigDecimal nullableBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? null : value;
    }

    public record OverviewCounts(
            long totalRequests,
            long newToday,
            long openRequests,
            long inProgress,
            long waitingForParts,
            long completedToday,
            long completedTotal,
            long cancelledTotal,
            long activeTechnicians,
            long techniciansWithActiveWork,
            long pendingAssignments,
            long totalReviews,
            BigDecimal ratingSum) {
    }

    public record CategoryCount(
            Long categoryId,
            String nameEn,
            String nameRu,
            String nameUz,
            long count) {
    }

    public record TechnicianCounts(
            long activeTechnicians,
            long inactiveTechnicians,
            long techniciansWithActiveWork,
            long pendingAssignments,
            long acceptedAssignments,
            long inProgressRequests,
            long waitingForPartsRequests,
            long totalCapacity,
            long activeAssignments) {
    }

    public record ReviewCounts(
            long totalReviews,
            BigDecimal ratingSum,
            long reviewsWithComment,
            long rating1,
            long rating2,
            long rating3,
            long rating4,
            long rating5) {
    }

    private record DailyCount(LocalDate date, long count) {
    }

    private record StatusCount(RepairRequestStatus status, long count) {
    }
}
