package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.presentation.Analytics.YearWrappedViewController;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import com.sebastiandorata.musicdashboard.repository.YearEndReportRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.PlaybackAggregator;
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
 * <p>Aggregation logic (top song, top artist, top album, top genre, valid
 * counts and minutes) is delegated to {@link PlaybackAggregator} rather
 * than being duplicated here and in the weekly/monthly services.</p>
 *
 * <p>Also provides read-only queries for top songs, top artists, and monthly
 * listening breakdowns consumed by {@link YearWrappedViewController} and
 * the dashboard graph.</p>
 *
 * <p>Time Complexity: O(n) where n = playback history entries for the year</p>
 */
@Service
public class YearEndReportService {

    @Autowired private YearEndReportRepository yearEndReportRepository;
    @Autowired private PlaybackHistoryRepository playbackHistoryRepository;
    @Autowired private UserSessionService userSessionService;
    @Autowired private UserRepository userRepository;

    /**
     * Returns an existing year-end report for the current user and year, or
     * generates a new one if none exists.
     *
     * <p>Uses SERIALIZABLE isolation + DataIntegrityViolationException catch
     * to prevent duplicate report creation under concurrent access (TOCTOU).</p>
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public YearEndReport getOrGenerateYearReport(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No user logged in");

        Optional<YearEndReport> existing = yearEndReportRepository.findByUserIdAndYear(userId, year);
        if (existing.isPresent()) return existing.get();

        try {
            return generateYearReport(userId, year);
        } catch (DataIntegrityViolationException e) {
            return yearEndReportRepository
                    .findByUserIdAndYear(userId, year)
                    .orElseThrow(() -> new RuntimeException(
                            "Year-end report missing immediately after duplicate key conflict " +
                                    "for userId=" + userId + ", year=" + year +
                                    ". This should never happen.", e));
        }
    }

    /**
     * Builds and persists a new YearEndReport for the given user and year.
     *
     * <p>Participates in the outer SERIALIZABLE transaction from
     * {@link #getOrGenerateYearReport}. No additional {@code @Transactional}
     * annotation is needed here.</p>
     */
    private YearEndReport generateYearReport(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: id=" + userId));

        // TODO: replace with a repo method that queries by year directly
        List<PlaybackHistory> yearHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.toList());

        YearEndReport report = new YearEndReport();
        report.setUser(user);
        report.setYear(year);
        report.setGeneratedAt(LocalDateTime.now());

        if (yearHistory.isEmpty()) {
            report.setTotalSongsPlayed(0);
            report.setTotalListeningTimeMinutes(0);
            return yearEndReportRepository.save(report);
        }

        // All four aggregations now delegate to PlaybackAggregator
        report.setTotalSongsPlayed(PlaybackAggregator.countValidPlays(yearHistory));
        report.setTotalListeningTimeMinutes(PlaybackAggregator.sumValidMinutes(yearHistory));
        report.setTopSong(PlaybackAggregator.findTopSong(yearHistory));
        report.setTopArtist(PlaybackAggregator.findTopArtist(yearHistory));
        report.setTopAlbum(PlaybackAggregator.findTopAlbumSong(yearHistory));
        report.setTopGenre(PlaybackAggregator.findTopGenre(yearHistory));

        return yearEndReportRepository.save(report);
    }

    /**
     * Returns the top N most-played songs for the current user in the given year.
     * Read-only — no report is generated or persisted here.
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
     * Read-only — no report is generated or persisted here.
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
     * Returns a sparse map of YearMonth → total listening minutes for chart visualization.
     * Months with no activity are omitted. Only valid plays are included.
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
     * sorted descending (most recent first).
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
