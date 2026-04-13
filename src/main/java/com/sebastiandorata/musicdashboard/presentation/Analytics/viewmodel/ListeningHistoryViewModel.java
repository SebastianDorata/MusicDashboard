package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.HistoryRowData;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningHistoryService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
/**
 * Loads paginated listening history rows for the Analytics page.
 *
 * <p>Retrieves a window of {@link PlaybackHistory}
 * records from {@link ListeningHistoryService},
 * maps each record to a {@link HistoryRowData} DTO,
 * and delivers results to the JavaFX thread via
 * {@link DataLoadingService}.</p>
 */
@Service
public class ListeningHistoryViewModel {

    @Autowired
    private ListeningHistoryService listeningHistoryService;

    @Autowired
    private DataLoadingService dataLoadingService;

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



    /**
     * Maps each {@link PlaybackHistory} row to a display DTO.
     *
     * <p>{@code playedAt} is formatted with {@link AppUtils#formatRelativeTime}
     * ("Just now", "3m ago", "2h ago", "Yesterday", "3d ago") instead of the raw
     * {@code LocalDateTime.toString()} which produces unreadable ISO strings.
     *
     * <p>No timezone conversion is needed here because
     * {@code AppUtils.formatRelativeTime} computes elapsed time as
     * {@code playedAt} using {@code LocalDateTime.now()}. Both values
     * come from the same system clock, so the relative difference is always
     * correct regardless of what zone the server runs in.
     *
     * Time Complexity: O(limit)
     * Space Complexity: O(limit)
     */
    private List<HistoryRowData> buildHistoryWindow(int offset, int limit) {
        List<PlaybackHistory> window = listeningHistoryService.getListeningHistoryWindow(offset, limit);
        return window.stream()
                .map(h -> {
                    Song song = h.getSong();
                    return new HistoryRowData(
                            song != null ? song.getTitle() : "Unknown",
                            song != null && !song.getArtists().isEmpty()
                                    ? song.getArtists().stream().findFirst()
    .map(Artist::getName).orElse("Unknown Artist")
                                    : "Unknown",
                            AppUtils.formatRelativeTime(h.getPlayedAt()),
                            h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0,
                            song
                    );
                })
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