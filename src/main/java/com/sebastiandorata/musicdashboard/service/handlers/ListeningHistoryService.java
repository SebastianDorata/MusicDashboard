package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user listening history with pagination support.
 *
 * <p><b><u>Time Complexity:</u></b></p>
 * <ul>
 *   <li>Load all history: O(n) where n = total plays.</li>
 *   <li>Get window: O(offset + limit).</li>
 * </ul>
 *
 * <p>Space Complexity: O(n) for storing full history in DoublyLinkedList.</p>
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
     * <p>Time Complexity: O(n).</p>
     * <p>Space Complexity: O(k) where k = valid plays.</p>
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
     * <p>Time Complexity: O(offset + limit).</p>
     * <p>Space Complexity: O(limit).</p>
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