package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlaybackTrackingService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

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

        playbackHistoryRepository.save(history);
    }

    public List<PlaybackHistory> getRecentlyPlayed(Long userId) {
        return playbackHistoryRepository.findTop20ByUserIdOrderByPlayedAtDesc(userId);
    }
}