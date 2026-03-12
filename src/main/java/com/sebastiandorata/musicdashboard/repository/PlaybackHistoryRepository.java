package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Long> {
    // Returns all history for a user, most recent first
    // No deduplication. Same song repeated = multiple rows
    List<PlaybackHistory> findByUserIdOrderByPlayedAtDesc(Long userId);

    List<PlaybackHistory> findTop20ByUserIdOrderByPlayedAtDesc(Long userId);
}