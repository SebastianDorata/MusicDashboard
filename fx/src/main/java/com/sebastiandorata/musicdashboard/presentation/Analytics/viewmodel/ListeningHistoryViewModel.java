package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.HistoryRowData;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningHistoryService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Loads paginated listening history rows for the Analytics page.
 *
 * <p>Retrieves a window of {@link PlaybackHistory} records from
 * {@link ListeningHistoryService}, maps each to a {@link HistoryRowData} DTO,
 * and delivers results to the JavaFX thread via {@link DataLoadingService}.</p>
 */
@Service
public class ListeningHistoryViewModel {

    @Autowired private ListeningHistoryService listeningHistoryService;
    @Autowired private DataLoadingService dataLoadingService;

    public void loadHistoryWindow(int offset, int limit, Consumer<List<HistoryRowData>> onSuccess) {
        dataLoadingService.loadAsync(
                () -> buildHistoryWindow(offset, limit),
                onSuccess
        );
    }

    private List<HistoryRowData> buildHistoryWindow(int offset, int limit) {
        List<PlaybackHistory> window = listeningHistoryService.getListeningHistoryWindow(offset, limit);
        return window.stream()
                .map(h -> {
                    Song song = h.getSong();
                    return new HistoryRowData(
                            song != null ? song.getTitle() : "Unknown",
                            ArtistUtils.getPrimaryArtistName(song),
                            AppUtils.formatRelativeTime(h.getPlayedAt()),
                            h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0,
                            song
                    );
                })
                .toList();
    }

    public void loadHistoryCount(Consumer<Integer> onSuccess) {
        dataLoadingService.loadAsync(listeningHistoryService::getListeningHistoryCount, onSuccess);
    }
}
