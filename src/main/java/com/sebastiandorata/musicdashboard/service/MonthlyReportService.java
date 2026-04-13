package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.repository.*;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Generates and persists monthly listening reports for the current user.
 *
 * <p>Filters the full playback history to the requested year and month,
 * then computes total songs played, total listening minutes, and the top
 * song, artist, album, and genre. Uses {@code SERIALIZABLE} transaction
 * isolation with a {@link org.springframework.dao.DataIntegrityViolationException}
 * catch to prevent duplicate reports under concurrent access (TOCTOU).</p>
 *
 * <p>Time Complexity: O(n) where n = playback history entries for the month</p>
 */

@Service
public class MonthlyReportService {

    @Autowired
    private MonthlyReportRepository monthlyReportRepository;

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns an existing monthly report for the current user, year, and month,
     * or generates a new one if none exists.
     *
     * <p>Uses SERIALIZABLE isolation to prevent race conditions during generation.</p>
     *
     * <p>Time Complexity: O(n) where n = playback history for the month.</p>
     * <p>Space Complexity: O(n) for filtering.</p>
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyReport getOrGenerateMonthlyReport(int year, int month) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in");
        }

        Optional<MonthlyReport> existing = monthlyReportRepository.findByUserIdAndYearAndMonth(userId, year, month);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return generateMonthlyReport(userId, year, month);
        } catch (DataIntegrityViolationException e) {
            return monthlyReportRepository
                    .findByUserIdAndYearAndMonth(userId, year, month)
                    .orElseThrow(() -> new RuntimeException(
                            "Monthly report missing after duplicate key conflict for userId=" + userId +
                                    ", year=" + year + ", month=" + month, e
                    ));
        }
    }

    private MonthlyReport generateMonthlyReport(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: id=" + userId));

        List<PlaybackHistory> monthHistory = playbackHistoryRepository
                .findByUserIdAndYearAndMonth(userId, year, month);

        MonthlyReport report = new MonthlyReport();
        report.setUser(user);
        report.setYear(year);
        report.setMonth(month);
        report.setGeneratedAt(LocalDateTime.now());

        if (monthHistory.isEmpty()) {
            report.setTotalSongsPlayed(0);
            report.setTotalListeningTimeMinutes(0);
            return monthlyReportRepository.save(report);
        }

        long validSongCount = monthHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .count();
        report.setTotalSongsPlayed((int) validSongCount);

        int totalSeconds = monthHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum();
        report.setTotalListeningTimeMinutes(totalSeconds / 60);

        Map<Song, Long> songFrequency = monthHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!songFrequency.isEmpty()) {
            report.setTopSong(songFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        Map<Artist, Long> artistFrequency = monthHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        if (!artistFrequency.isEmpty()) {
            report.setTopArtist(artistFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        Map<Song, Long> albumSongFrequency = monthHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null)
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!albumSongFrequency.isEmpty()) {
            report.setTopAlbum(albumSongFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        Map<Genre, Long> genreFrequency = monthHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
        if (!genreFrequency.isEmpty()) {
            report.setTopGenre(genreFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        return monthlyReportRepository.save(report);
    }

    /**
     * Returns list of available months for a given year.
     * Time Complexity: O(n) where n = playback history
     */
    public List<Integer> getAvailableMonths(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        return playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .map(h -> h.getPlayedAt().getMonthValue())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
}