package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.WeeklyReport;
import com.sebastiandorata.musicdashboard.service.handlers.WeeklyReportService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WeeklyReport} entities.
 *
 * <p>Provides lookup by user, year, and ISO week number for the
 * get-or-generate pattern in
 * {@link WeeklyReportService}, and bulk retrieval of all weekly reports for a given user and year.</p>
 */
@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    /**
     * Finds one user's weekly report for a given year and ISO week number.
     *
     * @param userId     the ID of the owning user
     * @param year       the calendar year of the report
     * @param weekOfYear the ISO week number of the report
     * @return the matching report, or an empty {@code Optional} if none has been generated
     */
    Optional<WeeklyReport> findByUserIdAndYearAndWeekOfYear(Long userId, Integer year, Integer weekOfYear);
}