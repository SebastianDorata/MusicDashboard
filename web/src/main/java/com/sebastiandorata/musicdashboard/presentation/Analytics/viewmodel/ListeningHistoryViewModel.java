package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.HistoryRowData;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.handlers.ListeningHistoryService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds paginated listening history rows for the Analytics page.
 *
 * <p>Retrieves a window of {@link PlaybackHistory} records from
 * {@link ListeningHistoryService}, maps each to a {@link HistoryRowData} DTO,
 * and returns the rows directly to the caller.</p>
 *
 * <p>SRP: this class only shapes history data for display; querying stays in
 * {@link ListeningHistoryService}.</p>
 */
@Service
public class ListeningHistoryViewModel {

    @Autowired private ListeningHistoryService listeningHistoryService;

    /**
     * Returns one window of listening history rows, synchronously.
     *
     * <p>Time Complexity: O(offset + limit) in the history service.</p>
     * <p>Space Complexity: O(limit) for the returned rows.</p>
     *
     * @param offset zero-based index of the first row to return
     * @param limit  maximum number of rows to return
     * @return the mapped rows for the requested window, never {@code null}
     */
    public List<HistoryRowData> getHistoryWindow(int offset, int limit) {
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

    /**
     * Returns the total number of listening history records for the current user.
     *
     * <p>Time Complexity: O(n) where n = playback history size (delegated to the service).</p>
     * <p>Space Complexity: O(1).</p>
     *
     * @return the total history record count
     */
    public int getHistoryCount() {
        return listeningHistoryService.getListeningHistoryCount();
    }
}
