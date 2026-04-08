package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.TopArtistsController;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes total listening time per artist from the current user's playback history.
 *
 * <p>Fetches all playback records in a single query, skips invalid plays
 * (under {@link PlaybackConstants#MINIMUM_PLAY_SECONDS}
 * seconds), and aggregates duration across all songs each artist appears on.
 * Used by {@link TopArtistsController}
 * and the Analytics Top Artists view.</p>
 *
 * <p>Time Complexity: O(n log k) where n = total valid plays, k = limit<br>
 * Space Complexity: O(n) for the grouping map, O(k) for the result</p>
 */
@Service
public class ArtistListeningTimeService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * Pairs an {@link com.sebastiandorata.musicdashboard.entity.Artist} with
     * their total accumulated listening time in seconds.
     *
     * <p>Implements {@link Comparable} for descending sort by total seconds.
     * The {@link #formatTime()} method formats the duration as "Xh Ym" or
     * "Ym" for display in the Top Artists panels.</p>
     */
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
     * Filter &amp; validate: O(n)
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

            // Use centralized validation constant to check if play is valid
            if (!PlaybackConstants.isValidPlay(play.getDurationPlayedSeconds())) {
                continue;  // Skip null durations and plays < 20 seconds
            }

            int duration = play.getDurationPlayedSeconds();

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
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum();
    }
}