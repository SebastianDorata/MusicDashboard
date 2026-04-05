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

    Optional<YearEndReport> findByUserIdAndYear(Long userId, Integer year);
    //Optional<YearEndReport> findByUserIdAndYear(Long userId, int year);

    List<YearEndReport> findByUserId(Long userId);
}
