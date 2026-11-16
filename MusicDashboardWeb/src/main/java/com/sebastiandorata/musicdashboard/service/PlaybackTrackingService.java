package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import org.springframework.data.domain.PageRequest;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records listening history rows and later completes them with the played duration.
 *
 * <p>A play is recorded once, when the browser reports 20 seconds of actual playback
 * ({@link #recordPlay(Song)}). The id of the freshly saved {@link PlaybackHistory} row is
 * remembered so that a later skip or natural song end can fill in the real duration through
 * {@link #updateCurrentPlayDuration(Integer, boolean)}.</p>
 *
 * <p>The web app is a singleton shared by many users, so the "current playback row" is
 * kept per user id in a {@link ConcurrentHashMap} instead of a single field. One user
 * skipping a song can therefore never complete another user's row.</p>
 *
 * <p>SRP: owns persistence of playback history rows and nothing about queues or audio.</p>
 *
 * <p>Time Complexity: O(1) for all operations except {@link #getRecentlyPlayed(Long)}.</p>
 * <p>Space Complexity: O(u) where u = number of users with an open playback row.</p>
 */
@Service
public class PlaybackTrackingService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

    /** Open playback row id per user id; an entry exists between recordPlay and the duration update. */
    private final Map<Long, Long> currentPlaybackIds = new ConcurrentHashMap<>();

    /**
     * Creates the playback history row for the current user and increments the song's listen count.
     *
     * <p>The id of the saved row is stored for the current user so that
     * {@link #updateCurrentPlayDuration(Integer, boolean)} can find it later.</p>
     *
     * <p>Time Complexity: O(1). Three database operations.</p>
     * <p>Space Complexity: O(1)</p>
     *
     * @param song the song that has been listened to for at least 20 seconds
     * @throws IllegalStateException if no user is logged in or the user no longer exists
     */
    public void recordPlay(Song song) {
        Long userId = userSessionService.getCurrentUserId();

        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot record playback.");
        }

        // Re-fetch the User so it is a managed entity in this request's persistence context.
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id: " + userId));

        PlaybackHistory history = new PlaybackHistory();
        history.setUser(currentUser);
        history.setSong(song);
        history.setPlayedAt(LocalDateTime.now());
        history.setCompleted(false);
        PlaybackHistory saved = playbackHistoryRepository.save(history);

        // The saved id must be remembered or updateCurrentPlayDuration() would have nothing to update.
        currentPlaybackIds.put(userId, saved.getPlaybackId());

        song.setListenCount((song.getListenCount() != null ? song.getListenCount() : 0) + 1);
        songRepository.save(song);
    }

    /**
     * Returns the most recent playback history rows of a user, songs fetched.
     *
     * <p>Time Complexity: O(n) where n = number of rows returned (at most 50).</p>
     * <p>Space Complexity: O(n)</p>
     *
     * @param userId the user whose history is wanted
     * @return up to 50 history rows, newest first; empty when there are none
     */
    public List<PlaybackHistory> getRecentlyPlayed(Long userId) {
        List<Long> ids = playbackHistoryRepository.findRecentIdsByUserId(
                userId, PageRequest.of(0, 50));
        if (ids.isEmpty()) return List.of();
        return playbackHistoryRepository.findByIdsWithSongs(ids);
    }

    /**
     * Completes the current user's open playback row with the seconds actually played.
     *
     * <p>Does nothing when the user has no open row (the song never reached 20 seconds).
     * The open row is forgotten afterwards so it can be completed only once.</p>
     *
     * @param durationPlayedSeconds seconds of the song that were played
     * @param completed {@code true} when the song played to its natural end
     */
    public void updateCurrentPlayDuration(Integer durationPlayedSeconds, boolean completed) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return;

        Long playbackId = currentPlaybackIds.remove(userId);
        if (playbackId == null) return;

        playbackHistoryRepository.findById(playbackId).ifPresent(history -> {
            history.setDurationPlayedSeconds(durationPlayedSeconds);
            history.setCompleted(completed);
            playbackHistoryRepository.save(history);
        });
    }

}
