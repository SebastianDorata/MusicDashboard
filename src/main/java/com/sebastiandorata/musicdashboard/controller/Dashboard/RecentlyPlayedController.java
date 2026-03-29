package com.sebastiandorata.musicdashboard.controller.Dashboard;


import com.sebastiandorata.musicdashboard.controllerUtils.UIComponent;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.SongService;
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
    private SongService songService;


    public VBox createPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("recently-played-panel");
        panel.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        panel.setMaxHeight(Double.MAX_VALUE);//  allow unlimited growth
        VBox.setVgrow(panel, Priority.ALWAYS);// stretch to fill parent


        Label title = new Label("Recently Played");
        title.getStyleClass().addAll("txt-white-md-bld", "padding-btm");

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
        songLbl.getStyleClass().addAll("play-history-song", "txt-white-md");
        HBox.setHgrow(songLbl, Priority.ALWAYS);

        Label timeLabel = new Label(AppUtils.formatRelativeTime(item.getPlayedAt()));
        timeLabel.getStyleClass().add("txt-white-sm-bld");

        row.getChildren().addAll(songLbl, timeLabel);

        // Double-click replays the song in the context of the full library queue
        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && item.getSong() != null) {
                List<Song> allSongs = songService.getAllSongs();
                musicPlayerService.setQueue(allSongs);
                musicPlayerService.playSong(item.getSong());
            }
        });

        return row;
    }
}