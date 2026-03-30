package com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.ListeningHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class ListeningHistoryViewModel {

    @Autowired
    private ListeningHistoryService listeningHistoryService;

    @Autowired
    private DataLoadingService dataLoadingService;

    public static class HistoryRowData {
        public String songTitle;
        public String artistName;
        public String playedAt;
        public int durationSeconds;

        public HistoryRowData(String songTitle, String artistName, String playedAt, int durationSeconds) {
            this.songTitle = songTitle;
            this.artistName = artistName;
            this.playedAt = playedAt;
            this.durationSeconds = durationSeconds;
        }
    }

    /**
     * Loads a window of listening history with pagination.
     * Time Complexity: O(offset + limit)
     */
    public void loadHistoryWindow(int offset, int limit, Consumer<List<HistoryRowData>> onSuccess) {
        dataLoadingService.loadAsync(
                () -> buildHistoryWindow(offset, limit),
                onSuccess
        );
    }

    private List<HistoryRowData> buildHistoryWindow(int offset, int limit) {
        List<PlaybackHistory> window = listeningHistoryService.getListeningHistoryWindow(offset, limit);
        return window.stream()
                .map(h -> new HistoryRowData(
                        h.getSong() != null ? h.getSong().getTitle() : "Unknown",
                        h.getSong() != null && !h.getSong().getArtists().isEmpty()
                                ? h.getSong().getArtists().get(0).getName()
                                : "Unknown",
                        h.getPlayedAt().toString(),
                        h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0
                ))
                .toList();
    }

    /**
     * Gets total count of listening history.
     * Time Complexity: O(n)
     */
    public void loadHistoryCount(Consumer<Integer> onSuccess) {
        dataLoadingService.loadAsync(listeningHistoryService::getListeningHistoryCount, onSuccess);
    }
}