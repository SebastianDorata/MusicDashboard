package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(1)
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
    private Long currentPlaybackId = null;

    /**
     * Time Complexity: O(1). Two database operations
     * Space Complexity: O(1)
     */
    public void recordPlay(Song song) {
        Long userId = userSessionService.getCurrentUserId();

        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot record playback.");
        }

        // Re-fetch the User within the current Hibernate session so it's a managed entity, not a detached one from a previous session.
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id: " + userId));


        PlaybackHistory history = new PlaybackHistory();
        history.setUser(currentUser);
        history.setSong(song);
        history.setPlayedAt(LocalDateTime.now());
        history.setCompleted(false);
        PlaybackHistory saved = playbackHistoryRepository.save(history);
        currentPlaybackId = saved.getPlaybackId();
        /** Bug fix. Originally recordPlay() saves to the DB but throws away the returned entity,
         * so currentPlaybackId stays null forever. Then when updateCurrentPlayDuration() is called,
         * it hits the null check and immediately returns, doing nothing.
         * The solution now captures the saved entity's ID
         */

        song.setListenCount((song.getListenCount() != null ? song.getListenCount() : 0) + 1);
        songRepository.save(song);
    }


    /**
     * Time Complexity: O(n) where n = playback history size
     * Space Complexity: O(n)
     */
    public List<PlaybackHistory> getRecentlyPlayed(Long userId) {
        return playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }

    public void updateCurrentPlayDuration(Integer durationPlayedSeconds) {
        if (currentPlaybackId == null) return;

        playbackHistoryRepository.findById(currentPlaybackId).ifPresent(history -> {
            history.setDurationPlayedSeconds(durationPlayedSeconds);
            playbackHistoryRepository.save(history);
        });

        currentPlaybackId = null;
    }

}