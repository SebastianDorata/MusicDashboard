package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Full history. Still needed for analytics aggregation
    // but now fetches song+artists in one query instead of n+1
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByUserIdOrderByPlayedAtDesc(@Param("userId") Long userId);

    @Query("SELECT p.playbackId FROM PlaybackHistory p " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.playedAt DESC")
    List<Long> findRecentIdsByUserId(
            @Param("userId") Long userId, Pageable pageable);

    // Step 2: Fetch full records with JOIN FETCH using those IDs
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.playbackId IN :ids " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByIdsWithSongs(@Param("ids") List<Long> ids);

    // DB-side filtering for monthly reports
    // Replaces in-memory stream filtering across all songs
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.user.id = :userId " +
            "AND FUNCTION('YEAR', p.playedAt) = :year " +
            "AND FUNCTION('MONTH', p.playedAt) = :month " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month);

    // DB-side filtering for yearly reports and chart data
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.user.id = :userId " +
            "AND FUNCTION('YEAR', p.playedAt) = :year " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year);
}