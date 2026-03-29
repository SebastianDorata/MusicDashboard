package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class ArtistListeningTimeService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;


    public static class ArtistTimeData implements Comparable<ArtistTimeData> {
        public Artist artist;
        public Integer totalSeconds;

        public ArtistTimeData(Artist artist, Integer totalSeconds) {
            this.artist = artist;
            this.totalSeconds = totalSeconds != null ? totalSeconds : 0;
        }


        @Override
        public int compareTo(ArtistTimeData other) {
            return other.totalSeconds.compareTo(this.totalSeconds);
        }

        public String formatTime() {
            int hours = totalSeconds / 3600;
            int minutes = (totalSeconds % 3600) / 60;

            if (hours > 0) {
                return hours + "h " + minutes + "m";
            } else {
                return minutes + "m";
            }
        }
    }

    /**
     * Time Complexity: O(n log k) where n = total plays, k = limit
     * Query all plays: O(n)
     * Group by artist: O(n)
     * Take top k: O(k)
     * Space Complexity: O(n) for grouping, O(k) for result
     */
    public List<ArtistTimeData> getTopArtistsAllTime(int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        // Get all plays for user
        List<PlaybackHistory> allPlays = playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);

        if (allPlays.isEmpty()) return Collections.emptyList();

        // Group by artist, sum their total seconds
        Map<Artist, Integer> artistTotals = new HashMap<>();

        for (PlaybackHistory play : allPlays) {
            if (play.getSong() == null || play.getSong().getArtists() == null) continue;


            int duration = (play.getDurationPlayedSeconds() != null ? play.getDurationPlayedSeconds() : 0);
            if (duration <= 0) continue;

            // Add duration to each artist on this song
            for (Artist artist : play.getSong().getArtists()) {
                artistTotals.put(
                        artist,
                        artistTotals.getOrDefault(artist, 0) + duration
                );
            }
        }


        return artistTotals.entrySet().stream()
                .map(e -> new ArtistTimeData(e.getKey(), e.getValue()))
                .sorted()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Integer getArtistTotalSeconds(Artist artist) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null || artist == null) return 0;

        List<PlaybackHistory> allPlays = playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);

        return allPlays.stream()
                .filter(h -> h.getSong() != null && h.getSong().getArtists() != null)
                .filter(h -> h.getSong().getArtists().contains(artist))
                .mapToInt(h -> (h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0))
                .filter(duration -> duration > 0)
                .sum();
    }
}