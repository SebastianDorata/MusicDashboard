package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlaybackTrackingServices {
    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    /**
     * Record that a user played a song
     */
    public void trackPlayback(User user, Song song, int durationPlayed, boolean completed) {
        PlaybackHistory history = new PlaybackHistory();
        history.setUser(user);
        history.setSong(song);
        history.setPlayedAt(LocalDateTime.now());
        history.setDurationPlayedSeconds(durationPlayed);
        history.setCompleted(completed);
        history.setSource("local");

        playbackHistoryRepository.save(history);
    }

    /**
     * Get recent playbacks for a user
     */
    public List<PlaybackHistory> getRecentPlaybacks(Long userId, int limit) {
        // You'll need to add this method to PlaybackHistoryRepository
        return playbackHistoryRepository.findAll(); // Simplified for now
    }

    /**
     * Get total plays for a user
     */
    public int getTotalPlaysByUser(Long userId) {
        // You'll need to add this method to PlaybackHistoryRepository
        return 0; // Placeholder
    }
}
