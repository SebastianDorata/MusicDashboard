package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.MonthlyReport;
import com.sebastiandorata.musicdashboard.service.handlers.MonthlyReportService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MonthlyReport} entities.
 *
 * <p>Provides lookup by user, year, and month for the get-or-generate
 * pattern in {@link MonthlyReportService},
 * and bulk retrieval of all monthly reports for a given user and year.</p>
 */
@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    /**
     * Finds one user's monthly report for a given year and month.
     *
     * @param userId the ID of the owning user
     * @param year   the calendar year of the report
     * @param month  the month number, 1 to 12
     * @return the matching report, or an empty {@code Optional} if none has been generated
     */
    Optional<MonthlyReport> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);

    /**
     * Returns all of one user's monthly reports for a given year.
     *
     * @param userId the ID of the owning user
     * @param year   the calendar year of the reports
     * @return the matching monthly reports; an empty list if there are none
     */
    List<MonthlyReport> findByUserIdAndYear(Long userId, Integer year);
}