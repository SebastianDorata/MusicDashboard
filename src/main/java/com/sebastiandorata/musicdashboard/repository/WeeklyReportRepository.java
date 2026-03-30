package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    Optional<WeeklyReport> findByUserIdAndYearAndWeekOfYear(Long userId, Integer year, Integer weekOfYear);
    List<WeeklyReport> findByUserIdAndYear(Long userId, Integer year);
}