package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface YearEndReportsRepository extends JpaRepository<YearEndReport, Long> {

    Optional<YearEndReport> findByUserIdAndYear(Long userId, Integer year);

    List<YearEndReport> findByUserId(Long userId);
}
