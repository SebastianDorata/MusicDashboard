package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.MonthlyReport;
import com.sebastiandorata.musicdashboard.service.MonthlyReportService;
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
    Optional<MonthlyReport> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
    List<MonthlyReport> findByUserIdAndYear(Long userId, Integer year);
}