package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Time Complexity Analysis:
 * getTodayListeningTimeMinutes: O(n) where n = plays today
 * getTodayAverageSessionSeconds: O(n) with filter
 * getTodayTopSong/Album: O(n log n) for grouping/sorting
 * getWeeklyTopSong/Album: O(n log n) for grouping/sorting
 * Space Complexity: O(n) for all methods due to intermediate collections
 */
@Service
public class DailyListeningStatsService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    private static final ZoneId TORONTO_ZONE = ZoneId.of("America/Toronto");

    /**
     * Time Complexity: O(n) where n = plays today
     * Space Complexity: O(n) for filtering
     */
    public Integer getTodayListeningTimeMinutes() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return 0;

        List<PlaybackHistory> todayPlays = getTodayPlaybacks(userId);

        // Sum all durationPlayedSeconds, convert to minutes
        int totalSeconds = todayPlays.stream()
                .mapToInt(h -> h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0)
                .sum();

        return totalSeconds / 60;
    }

    /**
     * Time Complexity: O(n) where n = plays today
     * Space Complexity: O(n) for filtering
     */
    public Integer getTodayAverageSessionSeconds() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return 0;

        List<PlaybackHistory> todayPlays = getTodayPlaybacks(userId);

        if (todayPlays.isEmpty()) return 0;

        /**
         * Only counts sessions with a recorded duration (> 0).
         * Threshold kept consistent with {@link ArtistListeningTimeService#getTopArtistsAllTime(int)}
         * and {@link ArtistListeningTimeService#getArtistTotalSeconds(Artist)}
         */
        int totalSeconds = todayPlays.stream()
                .filter(h -> h.getDurationPlayedSeconds() != null && h.getDurationPlayedSeconds() > 0)
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum();

        long sessionCount = todayPlays.stream()
                .filter(h -> h.getDurationPlayedSeconds() != null && h.getDurationPlayedSeconds() > 0)
                .count();

        if (sessionCount == 0) return 0;
        return (int) (totalSeconds / sessionCount);
    }

    /**
     * Time Complexity: O(n log n) for sorting via max()
     * Space Complexity: O(n) for grouping map
     */
    public Song getTodayTopSong() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return null;

        List<PlaybackHistory> todayPlays = getTodayPlaybacks(userId);

        return todayPlays.stream()
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Time Complexity: O(n log n) for sorting via max()
     * Space Complexity: O(n) for grouping map
     */
    public Album getTodayTopAlbum() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return null;

        List<PlaybackHistory> todayPlays = getTodayPlaybacks(userId);

        return todayPlays.stream()
                .map(h -> h.getSong().getAlbum())
                .filter(album -> album != null)  // Skip songs without albums
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Time Complexity: O(n log n) for sorting
     * Space Complexity: O(n)
     */
    public Artist getTodayTopArtist() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return null;

        List<PlaybackHistory> todayPlays = getTodayPlaybacks(userId);

        return todayPlays.stream()
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Time Complexity: O(n log n) for sorting via max()
     * Space Complexity: O(n) for grouping map
     */
    public Song getWeeklyTopSong() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return null;

        List<PlaybackHistory> weeklyPlays = getWeeklyPlaybacks(userId);

        return weeklyPlays.stream()
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Time Complexity: O(n log n) for sorting via max()
     * Space Complexity: O(n) for grouping map
     */
    public Album getWeeklyTopAlbum() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return null;

        List<PlaybackHistory> weeklyPlays = getWeeklyPlaybacks(userId);

        return weeklyPlays.stream()
                .map(h -> h.getSong().getAlbum())
                .filter(album -> album != null)  // Skip songs without albums
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Time Complexity: O(n log n) for sorting via max()
     * Space Complexity: O(n) for grouping map
     */
    public Artist getWeeklyTopArtist() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return null;

        List<PlaybackHistory> weeklyPlays = getWeeklyPlaybacks(userId);

        return weeklyPlays.stream()
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Time Complexity: O(n) for filtering
     * Space Complexity: O(m) where m = filtered results
     */
    private List<PlaybackHistory> getTodayPlaybacks(Long userId) {
        LocalDate torontoToday = LocalDate.now(TORONTO_ZONE);
        return getPlaybacksByDate(userId, torontoToday, torontoToday);
    }

    /**
     * Time Complexity: O(n) for filtering
     * Space Complexity: O(m) where m = filtered results
     */
    private List<PlaybackHistory> getWeeklyPlaybacks(Long userId) {
        LocalDate torontoToday = LocalDate.now(TORONTO_ZONE);

        // Get Sunday of current week Sunday = 1, Saturday = 7 in US locale
        TemporalField weekFields = WeekFields.of(Locale.US).dayOfWeek();
        LocalDate sundayStart = torontoToday.minusDays(torontoToday.get(weekFields) - 1);
        LocalDate saturdayEnd = sundayStart.plusDays(6);

        return getPlaybacksByDate(userId, sundayStart, saturdayEnd);
    }

    /**
     * Time Complexity: O(n) for filtering
     * Space Complexity: O(m) where m = filtered results
     */
    private List<PlaybackHistory> getPlaybacksByDate(Long userId, LocalDate startDate, LocalDate endDate) {
        // Get all plays for user
        List<PlaybackHistory> allPlays = playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);

        LocalDateTime startDateTime = startDate.atStartOfDay(TORONTO_ZONE).toLocalDateTime();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59).atZone(TORONTO_ZONE).toLocalDateTime();

        return allPlays.stream()
                .filter(h -> {
                    LocalDateTime torontoTime = h.getPlayedAt().atZone(ZoneId.of("UTC"))
                            .withZoneSameInstant(TORONTO_ZONE)
                            .toLocalDateTime();
                    return !torontoTime.isBefore(startDateTime) && !torontoTime.isAfter(endDateTime);
                })
                .collect(Collectors.toList());
    }
}