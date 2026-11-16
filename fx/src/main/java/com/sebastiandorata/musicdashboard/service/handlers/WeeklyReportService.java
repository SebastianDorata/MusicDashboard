package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.repository.*;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.PlaybackAggregator;
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
 * <p>Aggregation logic (top song, top artist, top album, top genre, valid
 * counts and minutes) is delegated to {@link PlaybackAggregator} rather
 * than being duplicated here and in the monthly/yearly services.</p>
 *
 * <p>Time Complexity: O(n) where n = playback history entries for the week</p>
 */
@Service
public class WeeklyReportService {

    @Autowired private WeeklyReportRepository weeklyReportRepository;
    @Autowired private PlaybackHistoryRepository playbackHistoryRepository;
    @Autowired private UserSessionService userSessionService;
    @Autowired private UserRepository userRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WeeklyReport getOrGenerateWeeklyReport(int year, int weekOfYear) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No user logged in");

        Optional<WeeklyReport> existing =
                weeklyReportRepository.findByUserIdAndYearAndWeekOfYear(userId, year, weekOfYear);
        if (existing.isPresent()) return existing.get();

        try {
            return generateWeeklyReport(userId, year, weekOfYear);
        } catch (DataIntegrityViolationException e) {
            return weeklyReportRepository
                    .findByUserIdAndYearAndWeekOfYear(userId, year, weekOfYear)
                    .orElseThrow(() -> new RuntimeException(
                            "Weekly report missing after duplicate key conflict for userId=" + userId +
                                    ", year=" + year + ", week=" + weekOfYear, e));
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

        // All four aggregations now delegate to PlaybackAggregator
        report.setTotalSongsPlayed(PlaybackAggregator.countValidPlays(weekHistory));
        report.setTotalListeningTimeMinutes(PlaybackAggregator.sumValidMinutes(weekHistory));
        report.setTopSong(PlaybackAggregator.findTopSong(weekHistory));
        report.setTopArtist(PlaybackAggregator.findTopArtist(weekHistory));
        report.setTopAlbum(PlaybackAggregator.findTopAlbumSong(weekHistory));
        report.setTopGenre(PlaybackAggregator.findTopGenre(weekHistory));

        return weeklyReportRepository.save(report);
    }

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
