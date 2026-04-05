package com.sebastiandorata.musicdashboard.presentation.Dashboard;


import com.sebastiandorata.musicdashboard.presentation.UIComponent;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.SongImportService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds and refreshes the Recently Played panel on the Dashboard.
 *
 * <p>Loads the most recent playback records asynchronously from
 * {@link PlaybackTrackingService}
 * and renders up to {@code MAX_RECENT_ITEMS} rows. Registers a listener on
 * {@link MusicPlayerService#currentSongProperty()}
 * to re-fetch after each song change. Double-clicking a row replays
 * the song with the full library as the queue.</p>
 */
@Component
public class RecentlyPlayedController extends UIComponent {

    private static final int MAX_RECENT_ITEMS = 10;

    @Lazy
    @Autowired
    private PlaybackTrackingService playbackTrackingService;

    @Autowired
    private MusicPlayerService musicPlayerService;

    @Autowired
    private UserSessionService userSessionService;

    @Lazy
    @Autowired
    private SongImportService songImportService;


    public VBox createPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().addAll("recently-played-panel","panels");
        panel.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        panel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(panel, Priority.ALWAYS);


        Label title = new Label("Recently Played");
        title.getStyleClass().addAll("txt-white-bld-thirty","txt-centre-underline", "padding-btm");

        VBox historyList = new VBox(0);
        historyList.getStyleClass().add("trans-background");
        historyList.setFillWidth(true);
        VBox.setVgrow(historyList, Priority.ALWAYS);

        refreshRecentlyPlayedList(historyList);

        // Refresh after a brief delay so the new record is committed before re-querying
        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {
                    }
                    javafx.application.Platform.runLater(() -> refreshRecentlyPlayedList(historyList));
                }).start();
            }
        });

        panel.getChildren().addAll(title, historyList);
        return panel;
    }

    /**
     * Fetches the full history then streams the first MAX_RECENT_ITEMS.
     * TODO: A repository method returning only the top N rows directly, which would reduce the DB payload to O(MAX_RECENT_ITEMS) instead of O(n).
     */
    private void refreshRecentlyPlayedList(VBox container) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            loadDataAsync(
                    () -> playbackTrackingService.getRecentlyPlayed(userId),
                    history -> {
                        container.getChildren().clear();
                        history.stream()
                                .limit(MAX_RECENT_ITEMS)
                                .forEach(h -> container.getChildren().add(buildHistoryRow(h)));
                    }
            );
        }
    }

    private HBox buildHistoryRow(PlaybackHistory item) {
        HBox row = new HBox(0);
        row.getStyleClass().add("play-history-row");

        Label songLbl = new Label(item.getSong() != null ? item.getSong().getTitle() : "Unknown");
        songLbl.getStyleClass().add("play-history-song");
        HBox.setHgrow(songLbl, Priority.ALWAYS);

        Label timeLabel = new Label(AppUtils.formatRelativeTime(item.getPlayedAt()));
        timeLabel.getStyleClass().add("wt-smmd-bld");

        row.getChildren().addAll(songLbl, timeLabel);

        // Double-click replays the song in the context of the full library queue
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && item.getSong() != null) {
                List<Song> allSongs = songImportService.getAllSongs();
                musicPlayerService.setQueue(allSongs);
                musicPlayerService.playSong(item.getSong());
            }
        });

        return row;
    }
}