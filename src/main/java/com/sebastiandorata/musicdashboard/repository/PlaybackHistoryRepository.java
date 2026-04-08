package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link PlaybackHistory} entities.
 *
 * <p>Provides a single query method returning all playback records for a
 * user ordered by {@code played_at} descending. All analytics services
 * call this method once and filter/aggregate in memory to minimise
 * round-trips to the database.</p>
 */
@Repository
public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, Long> {
    // Returns all history for a user, most recent first
    // No deduplication. Same song repeated = multiple rows


    List<PlaybackHistory> findByUserIdOrderByPlayedAtDesc(Long userId);
}