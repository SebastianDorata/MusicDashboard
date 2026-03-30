package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user listening history with pagination support.
 *
 * Time Complexity:
 *   - Load all history: O(n) where n = total plays
 *   - Get window: O(offset + limit)
 *
 * Space Complexity: O(n) for storing full history in DoublyLinkedList
 */
@Service
public class ListeningHistoryService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * Returns complete listening history as a DoublyLinkedList for pagination.
     * Only includes valid plays (>= 20 seconds).
     *
     * Time Complexity: O(n)
     * Space Complexity: O(k) where k = valid plays
     */
    public DoublyLinkedList<PlaybackHistory> getListeningHistory() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allHistory = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.toList());

        DoublyLinkedList<PlaybackHistory> history = new DoublyLinkedList<>();
        for (PlaybackHistory h : allHistory) {
            history.add(h);
        }

        return history;
    }

    /**
     * Gets a specific window from listening history.
     * Time Complexity: O(offset + limit)
     * Space Complexity: O(limit)
     */
    public List<PlaybackHistory> getListeningHistoryWindow(int offset, int limit) {
        DoublyLinkedList<PlaybackHistory> history = getListeningHistory();
        return history.getWindow(offset, limit);
    }

    /**
     * Gets total count of valid listening history entries.
     * Time Complexity: O(n)
     */
    public int getListeningHistoryCount() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return 0;

        return (int) playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .count();
    }
}