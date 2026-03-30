package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    Optional<MonthlyReport> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
    List<MonthlyReport> findByUserIdAndYear(Long userId, Integer year);
}