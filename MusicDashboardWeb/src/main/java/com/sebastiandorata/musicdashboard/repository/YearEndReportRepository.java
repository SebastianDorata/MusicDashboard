package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import com.sebastiandorata.musicdashboard.service.handlers.YearEndReportService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link YearEndReport} entities.
 *
 * <p>Provides lookup by user and year for the get-or-generate pattern
 * in {@link YearEndReportService},
 * and bulk retrieval of all year-end reports for a given user.</p>
 */
@Repository
public interface YearEndReportRepository extends JpaRepository<YearEndReport, Long> {

    /**
     * Finds one user's year-end report for a given year.
     *
     * @param userId the ID of the owning user
     * @param year   the calendar year of the report
     * @return the matching report, or an empty {@code Optional} if none has been generated
     */
    Optional<YearEndReport> findByUserIdAndYear(Long userId, Integer year);

    /**
     * Returns all of one user's year-end reports.
     *
     * @param userId the ID of the owning user
     * @return the user's year-end reports; an empty list if there are none
     */
    List<YearEndReport> findByUserId(Long userId);
}
