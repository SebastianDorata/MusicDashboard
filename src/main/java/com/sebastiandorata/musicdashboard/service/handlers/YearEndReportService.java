package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.presentation.Analytics.YearWrappedViewController;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import com.sebastiandorata.musicdashboard.repository.YearEndReportRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates and persists annual year-end listening reports for the current user.
 *
 * <p>Computes total listening time, total songs played, and the top song,
 * artist, album, and genre for the year.</p>
 * <p>Also provides read-only queries
 * for top songs, top artists, and monthly listening breakdowns consumed
 * by {@link YearWrappedViewController}
 * and the dashboard graph. </p>
 * <p>Uses {@code SERIALIZABLE} isolation to prevent
 * duplicate report creation under concurrent access.</p>
 *
 * <p>Time Complexity: O(n) where n = playback history entries for the year</p>
 */
@Service
public class YearEndReportService {

    @Autowired
    private YearEndReportRepository yearEndReportRepository;

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns an existing year-end report for the current user and year, or generates
     * a new one if none exists.
     *
     * - @Transactional with serializable isolation is used because without a transaction,
     *  two threads can both pass the "does this exist?" check
     *  at the same time, then both try to Insert, causing a check-then-act race condition. TOCTOU
     *  With a key violation on the (user_id, year) unique constraint.
     *
     * Isolation.SERIALIZABLE is the strictest level. It makes concurrent transactions
     * behave as if they ran one after the other (serially), preventing this race.
     * The second thread will block until the first commits, then re-read and find
     * the row already exists.
     *
     * Catch DataIntegrityViolationException is used because SERIALIZABLE isolation eliminates the race in almost all cases,
     * but as a final safety net we catch the constraint violation anyway. This handles any edge case
     * where two requests slip through simultaneously.
     *
     * On catching the exception, a simple re-query is made for the row that the "winning" thread just inserted.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public YearEndReport getOrGenerateYearReport(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in");
        }

        // First, try to find an existing report for this user/year combo.
        // With SERIALIZABLE isolation, if two threads reach here at the same time,
        // Postgre will force them to execute this check sequentially.
        Optional<YearEndReport> existing = yearEndReportRepository.findByUserIdAndYear(userId, year);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            // No existing report found, generate and save a new one.
            return generateYearReport(userId, year);

        } catch (DataIntegrityViolationException e) {
            // Rather than propagating the exception, a fetch is made
            // for the row that the winning thread just committed.
            // orElseThrow should never trigger here. The row must exist since it
            // just hit a duplicate key violation proving it was inserted.
            return yearEndReportRepository
                    .findByUserIdAndYear(userId, year)
                    .orElseThrow(() -> new RuntimeException(
                            "Year-end report missing immediately after duplicate key conflict " +
                                    "for userId=" + userId + ", year=" + year +
                                    ". This should never happen.", e
                    ));
        }
    }


    /**
     * Builds and persists a new YearEndReport for the given user and year.
     *
     * This method is private and only called from getOrGenerateYearReport, which
     * already holds a serializable transaction. No additional @Transactional
     * annotation is needed here as it participates in the outer transaction.
     *
     * Steps:
     *  1. Load the user entity (needed for the report's @ManyToOne relationship)
     *  2. Filter this year's playback history
     *  3. Compute all aggregate stats (counts, top song/artist/album/genre)
     *  4. Save and return the report
     */
    private YearEndReport generateYearReport(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: id=" + userId));

        // Pull all playback history for this user, then filter in-memory to the target year.
        // TODO: A repo method that queries by year directly, avoiding loading irrelevant rows into memory.
        List<PlaybackHistory> yearHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.toList());

        YearEndReport report = new YearEndReport();
        report.setUser(user);
        report.setYear(year);
        report.setGeneratedAt(LocalDateTime.now());

        // If the user has no listening history for this year, save a zeroed-out
        // report rather than returning null. This prevents repeated generation
        // attempts on future calls and keeps the DB consistent.
        if (yearHistory.isEmpty()) {
            report.setTotalSongsPlayed(0);
            report.setTotalListeningTimeMinutes(0);
            return yearEndReportRepository.save(report);
        }

        // Total songs played.
        // Only count "valid" plays as defined by
        // PlaybackConstants.isValidPlay()
        // (currently: duration >= 20 seconds). Skips and accidental taps are excluded.
        long validSongCount = yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .count();
        report.setTotalSongsPlayed((int) validSongCount);

        // Total listening time
        // Sum duration of all valid plays, then convert seconds to minutes.
        // Integer division is intentional here (truncate, not round).
        int totalSeconds = yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum();
        report.setTotalListeningTimeMinutes(totalSeconds / 60);

        // Top song
        // Group valid plays by Song entity, count occurrences, pick the highest.
        Map<Song, Long> songFrequency = yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!songFrequency.isEmpty()) {
            report.setTopSong(songFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        // Top artist
        // A song can have multiple artists (flatMap), so each contributing artist
        // gets credit for the play. Pick the artist with the highest total count.
        Map<Artist, Long> artistFrequency = yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        if (!artistFrequency.isEmpty()) {
            report.setTopArtist(artistFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        // Top album
        // Groups by Song (only songs with an album), setTopAlbum() receives a Song
        // The entity stores topAlbum as a Song reference (the most-played track from
        // the top album), not an Album entity directly. Group by Song, filter to
        // only songs that belong to an album, and pass the winning Song to setTopAlbum().
        Map<Song, Long> albumSongFrequency = yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null) // exclude singles with no album
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!albumSongFrequency.isEmpty()) {
            report.setTopAlbum(albumSongFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        // Top genre
        // Like artists, a song can belong to multiple genres, a flatMap gives each
        // genre credit, then it picks the one with the most accumulated plays.
        Map<Genre, Long> genreFrequency = yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
        if (!genreFrequency.isEmpty()) {
            report.setTopGenre(genreFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        return yearEndReportRepository.save(report);
    }


    /**
     * Returns the top N most-played songs for the current user in the given year.
     * Only valid plays (>= 20 seconds) are counted.
     *
     * Used by the dashboard to populate the "Top Songs" chart/list.
     * Read-only. No report is generated or persisted here.
     */
    public List<Map.Entry<Song, Long>> getTopSongsForYear(int year, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        List<PlaybackHistory> yearHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.toList());

        return yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    /**
     * Returns the top N most-listened-to artists for the current user in the given year.
     * Multi-artist songs contribute a play count to each of their artists.
     * Only valid plays (>= 20 seconds) are counted.
     *
     * Used by the dashboard to populate the "Top Artists" chart/list.
     * Read-only No report is generated or persisted here.
     */
    public List<Map.Entry<Artist, Long>> getTopArtistsForYear(int year, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        List<PlaybackHistory> yearHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.toList());

        return yearHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    /**
     * Returns a map of YearMonth → total listening minutes for chart visualization.
     * Months with no listening activity are omitted from the map (sparse).
     *
     * Time Complexity:  O(n) where n = number of playback history rows for this user
     * Space Complexity: O(12) = O(1) — at most 12 month buckets per year
     *
     * Only valid plays (>= 20 seconds) are included, consistent with all other
     * analytics in this service. Duration is truncated to minutes (integer division).
     */
    public Map<YearMonth, Integer> getMonthlyListeningTime(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new HashMap<>();

        return playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(
                        h -> YearMonth.from(h.getPlayedAt()),
                        Collectors.summingInt(h -> h.getDurationPlayedSeconds() / 60)
                ));
    }


    /**
     * Returns the distinct years for which the current user has any playback history,
     * sorted in descending order (most recent first).
     *
     * Used to populate the year selector in the dashboard UI.
     * Read-only. No report is generated or persisted here.
     */
    public List<Integer> getAvailableYears() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        return playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .map(h -> h.getPlayedAt().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
}