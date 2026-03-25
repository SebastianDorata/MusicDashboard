package com.sebastiandorata.musicdashboard.controllerUtils;


import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.SongService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class RecentlyPlayedController extends UIComponent {

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
        VBox recentlyPlayed = new VBox(0);
        recentlyPlayed.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        recentlyPlayed.setPrefHeight(AppUtils.APP_HEIGHT * 0.50);
        recentlyPlayed.getStyleClass().add("dk-blue-background");

        Label title = new Label("Recently Played");
        title.getStyleClass().addAll("txt-white-md-bld", "recently-played-title");

        ListView<PlaybackHistory> historyList = new ListView<>();
        historyList.getStyleClass().add("trans-background");
        VBox.setVgrow(historyList, Priority.ALWAYS);

        historyList.setCellFactory(list -> new ListCell<PlaybackHistory>() {
            private final HBox row = new HBox(10);
            private final Label songLbl = new Label();
            private final Label timeLbl = new Label();

            {
                songLbl.getStyleClass().addAll("play-history-song", "txt-black-sm");
                HBox.setHgrow(songLbl, Priority.ALWAYS);
                timeLbl.getStyleClass().add("play-history-time");
                row.getStyleClass().add("play-history-row");
                row.getChildren().addAll(songLbl, timeLbl);
            }

            @Override
            protected void updateItem(PlaybackHistory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getSong() == null) {
                    setGraphic(null);
                } else {
                    songLbl.setText(item.getSong().getTitle());
                    timeLbl.setText(AppUtils.formatRelativeTime(item.getPlayedAt()));
                    setGraphic(row);
                }
            }
        });

        // Load recently played songs
        refreshRecentlyPlayedList(historyList);

        // Refresh when new song plays
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

        // Double-click to replay
        historyList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                PlaybackHistory selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getSong() != null) {
                    List<Song> allSongs = songService.getAllSongs();
                    musicPlayerService.setQueue(allSongs);
                    musicPlayerService.playSong(selected.getSong());
                }
            }
        });

        recentlyPlayed.getChildren().addAll(title, historyList);
        return recentlyPlayed;
    }

    private void refreshRecentlyPlayedList(ListView<PlaybackHistory> historyList) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            loadDataAsync(
                    () -> playbackTrackingService.getRecentlyPlayed(userId),
                    history -> historyList.getItems().setAll(history)
            );
        }
    }
}
