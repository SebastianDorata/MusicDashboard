package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.repository.*;
import com.sebastiandorata.musicdashboard.utils.PlaybackAggregator;
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
 * <p>Aggregation logic (top song, top artist, top album, top genre, valid
 * counts and minutes) is delegated to {@link PlaybackAggregator} rather
 * than being duplicated here and in the weekly/yearly services.</p>
 *
 * <p>Time Complexity: O(n) where n = playback history entries for the month</p>
 */
@Service
public class MonthlyReportService {

    @Autowired private MonthlyReportRepository monthlyReportRepository;
    @Autowired private PlaybackHistoryRepository playbackHistoryRepository;
    @Autowired private UserSessionService userSessionService;
    @Autowired private UserRepository userRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MonthlyReport getOrGenerateMonthlyReport(int year, int month) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No user logged in");

        Optional<MonthlyReport> existing =
                monthlyReportRepository.findByUserIdAndYearAndMonth(userId, year, month);
        if (existing.isPresent()) return existing.get();

        try {
            return generateMonthlyReport(userId, year, month);
        } catch (DataIntegrityViolationException e) {
            return monthlyReportRepository
                    .findByUserIdAndYearAndMonth(userId, year, month)
                    .orElseThrow(() -> new RuntimeException(
                            "Monthly report missing after duplicate key conflict for userId=" + userId +
                                    ", year=" + year + ", month=" + month, e));
        }
    }

    private MonthlyReport generateMonthlyReport(Long userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: id=" + userId));

        List<PlaybackHistory> monthHistory =
                playbackHistoryRepository.findByUserIdAndYearAndMonth(userId, year, month);

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

        // All four aggregations now delegate to PlaybackAggregator
        report.setTotalSongsPlayed(PlaybackAggregator.countValidPlays(monthHistory));
        report.setTotalListeningTimeMinutes(PlaybackAggregator.sumValidMinutes(monthHistory));
        report.setTopSong(PlaybackAggregator.findTopSong(monthHistory));
        report.setTopArtist(PlaybackAggregator.findTopArtist(monthHistory));
        report.setTopAlbum(PlaybackAggregator.findTopAlbumSong(monthHistory));
        report.setTopGenre(PlaybackAggregator.findTopGenre(monthHistory));

        return monthlyReportRepository.save(report);
    }

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
