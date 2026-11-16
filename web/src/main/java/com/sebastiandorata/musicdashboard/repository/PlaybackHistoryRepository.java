package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Returns all playback records for a user, newest first, with each song and its artists
     * loaded in one query. Analytics services call this once and aggregate in memory.
     *
     * @param userId the ID of the user
     * @return the user's playback records ordered by {@code played_at} descending; an empty list if there are none
     */
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByUserIdOrderByPlayedAtDesc(@Param("userId") Long userId);

    /**
     * Returns the IDs of a user's most recent playback records, newest first.
     *
     * @param userId   the ID of the user
     * @param pageable the page to return, which sets the number of IDs
     * @return the playback record IDs in the requested page; an empty list if there are none
     */
    @Query("SELECT p.playbackId FROM PlaybackHistory p " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.playedAt DESC")
    List<Long> findRecentIdsByUserId(
            @Param("userId") Long userId, Pageable pageable);

    /**
     * Fetches the playback records with the given IDs, with each song and its artists loaded.
     *
     * @param ids the playback record IDs to fetch
     * @return the matching records, newest first; an empty list if none match
     */
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.playbackId IN :ids " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByIdsWithSongs(@Param("ids") List<Long> ids);

    /**
     * Returns one user's playback records for a given month, newest first, with each song and
     * its artists loaded. Filtering happens in the database for monthly reports.
     *
     * @param userId the ID of the user
     * @param year   the calendar year
     * @param month  the month number, 1 to 12
     * @return the matching records; an empty list if there are none
     */
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.user.id = :userId " +
            "AND EXTRACT(YEAR FROM p.playedAt) = :year " +
            "AND EXTRACT(MONTH FROM p.playedAt) = :month " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * Returns one user's playback records for a given year, newest first, with each song and
     * its artists loaded. Filtering happens in the database for yearly reports and chart data.
     *
     * @param userId the ID of the user
     * @param year   the calendar year
     * @return the matching records; an empty list if there are none
     */
    @Query("SELECT p FROM PlaybackHistory p " +
            "LEFT JOIN FETCH p.song s " +
            "LEFT JOIN FETCH s.artists " +
            "WHERE p.user.id = :userId " +
            "AND EXTRACT(YEAR FROM p.playedAt) = :year " +
            "ORDER BY p.playedAt DESC")
    List<PlaybackHistory> findByUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year);

    /**
     * Deletes every playback record for the given song, directly in the database.
     *
     * @param songId the ID of the song whose playback records should be removed
     */
    @Modifying
    @Query("DELETE FROM PlaybackHistory p WHERE p.song.songID = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}