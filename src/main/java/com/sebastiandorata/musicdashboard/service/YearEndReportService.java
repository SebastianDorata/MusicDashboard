package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

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


    public YearEndReport getOrGenerateYearReport(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in");
        }

        // Check if report already exists
        Optional<YearEndReport> existing = yearEndReportRepository.findByUserIdAndYear(userId, year);
        if (existing.isPresent()) {
            return existing.get();
        }
        return generateYearReport(userId, year);
    }


    private YearEndReport generateYearReport(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Get all playback history for the user in the specified year
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

        // Calculate total songs played
        report.setTotalSongsPlayed(yearHistory.size());

        // Calculate total listening time (in minutes)
        int totalSeconds = yearHistory.stream()
                .mapToInt(h -> h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0)
                .sum();
        report.setTotalListeningTimeMinutes(totalSeconds / 60);

        // Find top song (most played)
        Map<Song, Long> songFrequency = yearHistory.stream()
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()));
        if (!songFrequency.isEmpty()) {
            report.setTopSong(songFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        // Find top artist
        Map<Artist, Long> artistFrequency = yearHistory.stream()
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        if (!artistFrequency.isEmpty()) {
            report.setTopArtist(artistFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null));
        }

        // Find top album
        Map<Album, Long> albumFrequency = yearHistory.stream()
                .map(h -> h.getSong().getAlbum())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        if (!albumFrequency.isEmpty()) {
            Album topAlbum = albumFrequency.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            // Note: YearEndReport.topAlbum is Song type in the entity, this might need adjustment
        }

        // Find top genre
        Map<Genre, Long> genreFrequency = yearHistory.stream()
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


    public List<Map.Entry<Song, Long>> getTopSongsForYear(int year, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        List<PlaybackHistory> yearHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.toList());

        return yearHistory.stream()
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    public List<Map.Entry<Artist, Long>> getTopArtistsForYear(int year, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        List<PlaybackHistory> yearHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.toList());

        return yearHistory.stream()
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    public Map<YearMonth, Integer> getMonthlyListeningTime(int year) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new HashMap<>();

        return playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> h.getPlayedAt().getYear() == year)
                .collect(Collectors.groupingBy(
                        h -> YearMonth.from(h.getPlayedAt()),
                        Collectors.summingInt(h -> h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() / 60 : 0)
                ));
    }


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