package com.sebastiandorata.musicdashboard.utils;

import com.sebastiandorata.musicdashboard.entity.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that aggregates {@link PlaybackHistory} records into
 * report-ready values.
 *
 * <p>Centralizes the aggregation logic that was previously copy-pasted
 * verbatim across three report services:
 * {@code WeeklyReportService}, {@code MonthlyReportService}, and
 * {@code YearEndReportService}. Each service contained identical
 * stream pipelines for computing valid song counts, total minutes,
 * top song, top artist, top album, and top genre.</p>
 *
 * <p>Every method accepts a pre-filtered {@code List<PlaybackHistory>}
 * so the callers remain in control of which time window they pass in.
 * Filtering to valid plays ({@link PlaybackConstants#isValidPlay}) is
 * applied internally so callers never need to repeat that guard either.</p>
 *
 * <p>All methods are static. This class is not instantiable.</p>
 *
 * <p>Time Complexity: O(n) per method, where n = history.size().<br>
 * Space Complexity: O(k) per method, where k = distinct entities grouped.</p>
 */
public final class PlaybackAggregator {

    private PlaybackAggregator() {}

    /**
     * Counts the number of valid plays in the history list.
     *
     * @param history the playback records to count; must not be {@code null}
     * @return count of records that pass {@link PlaybackConstants#isValidPlay}
     */
    public static int countValidPlays(List<PlaybackHistory> history) {
        return (int) history.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .count();
    }

    /**
     * Sums the duration of all valid plays and converts to whole minutes.
     *
     * <p>Integer division is intentional — truncate, not round — consistent
     * with the original behaviour in all three report services.
     *
     * @param history the playback records to sum; must not be {@code null}
     * @return total valid listening time in minutes
     */
    public static int sumValidMinutes(List<PlaybackHistory> history) {
        int totalSeconds = history.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum();
        return totalSeconds / 60;
    }

    /**
     * Returns the most frequently played {@link Song} among valid plays,
     * or {@code null} if no valid plays exist.
     *
     * @param history the playback records to analyse; must not be {@code null}
     * @return the top {@link Song} by play count, or {@code null}
     */
    public static Song findTopSong(List<PlaybackHistory> history) {
        return history.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns the most frequently credited {@link Artist} among valid plays.
     *
     * <p>Multi-artist songs contribute a count to each of their artists,
     * consistent with the original behaviour in all three report services.
     *
     * @param history the playback records to analyse; must not be {@code null}
     * @return the top {@link Artist} by credited play count, or {@code null}
     */
    public static Artist findTopArtist(List<PlaybackHistory> history) {
        return history.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns the {@link Song} whose album was played most among valid plays.
     *
     * <p>Only songs that belong to an album are considered. The return type
     * is {@link Song} (not {@link Album}) because the report entities store
     * {@code topAlbum} as a {@link Song} reference — the most-played track
     * from the top album — matching the original behaviour in all three services.
     *
     * @param history the playback records to analyse; must not be {@code null}
     * @return the most-played {@link Song} that has a non-null album,
     *         or {@code null} if no qualifying plays exist
     */
    public static Song findTopAlbumSong(List<PlaybackHistory> history) {
        return history.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null)
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns the most frequently encountered {@link Genre} among valid plays.
     *
     * <p>Multi-genre songs contribute a count to each of their genres,
     * consistent with the original behaviour in all three report services.
     *
     * @param history the playback records to analyse; must not be {@code null}
     * @return the top {@link Genre} by credited play count, or {@code null}
     */
    public static Genre findTopGenre(List<PlaybackHistory> history) {
        return history.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
