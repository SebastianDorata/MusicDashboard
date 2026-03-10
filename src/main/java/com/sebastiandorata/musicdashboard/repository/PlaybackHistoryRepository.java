package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Long> {
    // Add custom methods later if needed
}