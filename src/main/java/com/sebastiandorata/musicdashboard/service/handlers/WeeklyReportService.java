package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.repository.*;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
/**
 * Generates and persists weekly listening reports for the current user.
 *
 * <p>Filters the full playback history to the requested ISO year and week,
 * then computes total songs played, total listening minutes, and the top
 * song, artist, album, and genre. Uses {@code SERIALIZABLE} transaction
 * isolation with a {@link org.springframework.dao.DataIntegrityViolationException}
 * catch to prevent duplicate reports under concurrent access.</p>
 *
 * <p>Time Complexity: O(n) where n = playback history entries for the week</p>
 */
@Service
public class WeeklyReportService {

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns an existing weekly report for the current user, year, and week,
     * or generates a new one if none exists.
     *
     * Uses SERIALIZABLE isolation to prevent race conditions during generation.
     *
     * Time Complexity: O(n) where n = playback history for the week
     * Space Complexity: O(n) for filtering
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WeeklyReport getOrGenerateWeeklyReport(int year, int weekOfYear) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in");
        }

        Optional<WeeklyReport> existing = weeklyReportRepository.findByUserIdAndYearAndWeekOfYear(userId, year, weekOfYear);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return generateWeeklyReport(userId, year, weekOfYear);
        } catch (DataIntegrityViolationException e) {
            return weeklyReportRepository
                    .findByUserIdAndYearAndWeekOfYear(userId, year, weekOfYear)
                    .orElseThrow(() -> new RuntimeException(
                            "Weekly report missing after duplicate key conflict for userId=" + userId +
                                    ", year=" + year + ", week=" + weekOfYear, e
                    ));
        }
    }

    private WeeklyReport generateWeeklyReport(Long userId, int year, int weekOfYear) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: id=" + userId));

        List<PlaybackHistory> weekHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .filter(h -> h.getPlayedAt().get(WeekFields.ISO.weekOfYear()) == weekOfYear)
                .collect(Collectors.toList());

        WeeklyReport report = new WeeklyReport();
        report.setUser(user);
        report.setYear(year);
        report.setWeekOfYear(weekOfYear);
        report.setGeneratedAt(LocalDateTime.now());

        if (weekHistory.isEmpty()) {
            report.setTotalSongsPlayed(0);
            report.setTotalListeningTimeMinutes(0);
            return weeklyReportRepository.save(report);
        }

        long validSongCount = weekHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .count();
        report.setTotalSongsPlayed((int) validSongCount);

        int totalSeconds = weekHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum();
        report.setTotalListeningTimeMinutes(totalSeconds / 60);

        Map<Song, Long> songFrequency = weekHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!songFrequency.isEmpty()) {
            report.setTopSong(songFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        Map<Artist, Long> artistFrequency = weekHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        if (!artistFrequency.isEmpty()) {
            report.setTopArtist(artistFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        Map<Song, Long> albumSongFrequency = weekHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null)
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!albumSongFrequency.isEmpty()) {
            report.setTopAlbum(albumSongFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        Map<Genre, Long> genreFrequency = weekHistory.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getGenres().stream())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()));
        if (!genreFrequency.isEmpty()) {
            report.setTopGenre(genreFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        return weeklyReportRepository.save(report);
    }

    /**
     * Returns list of available week numbers for a given year.
     * Time Complexity: O(n) where n = playback history
     */
    public List<Integer> getAvailableWeeks(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        return playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .map(h -> h.getPlayedAt().get(WeekFields.ISO.weekOfYear()))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
}