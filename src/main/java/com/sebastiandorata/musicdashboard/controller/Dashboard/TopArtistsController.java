package com.sebastiandorata.musicdashboard.controller.Dashboard;

import com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel.ArtistsViewModel;
import com.sebastiandorata.musicdashboard.controllerUtils.UIComponent;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@Component
public class TopArtistsController extends UIComponent {

    @Autowired
    private ArtistsViewModel topArtistsViewModel;

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;


    /**
     * @param onArtistClicked callback invoked with the clicked Artist entity.
     *                        Decouples this panel from any specific navigation target (DIP).
     */
    public VBox createPanel(Consumer<Artist> onArtistClicked) {
        VBox topArtist = new VBox(0);
        topArtist.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        topArtist.setPrefHeight(AppUtils.APP_HEIGHT * 0.38);
        topArtist.getStyleClass().add("top-artists-panel");

        Label title = new Label("Top 5 Artists All-Time");
        title.getStyleClass().add("txt-white-ttl-bld");
        title.setPadding(new Insets(0, 0, 10, 0));

        VBox artistRows = new VBox(0);
        artistRows.setFillWidth(true);

        topArtist.getChildren().addAll(title, artistRows);

        refreshTopArtists(artistRows, onArtistClicked);

        // Listen to playback from currentSongProperty instead so that all events (double-clicking from Recently Played) are captured and refresh on song changes
        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                new Thread(() -> {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> refreshTopArtists(artistRows, onArtistClicked));
                }).start();
            }
        });

        return topArtist;
    }

    /**
     * Clears and rebuilds all artist rows from fresh data.
     * Rebuilding rather than patching removes the implicit index coupling
     * that existed with the previous parallel-label-list approach.
     */
    private void refreshTopArtists(VBox artistRows, Consumer<Artist> onArtistClicked) {
        topArtistsViewModel.loadTopArtistsData(data -> {
            artistRows.getChildren().clear();
            for (ArtistsViewModel.TopArtistRowData rowData : data) {
                artistRows.getChildren().add(buildArtistRow(rowData, onArtistClicked));
            }
        });
    }

    private HBox buildArtistRow(ArtistsViewModel.TopArtistRowData data, Consumer<Artist> onArtistClicked) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("artist-row");

        Label nameLabel = new Label(data.artistName);
        nameLabel.getStyleClass().add("artist-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // Only real artists (non-placeholder) are clickable
        if (data.artist != null) {
            nameLabel.getStyleClass().add("artist-name-clickable");
            nameLabel.setOnMouseClicked(e -> onArtistClicked.accept(data.artist));
        }

        Label timeLabel = new Label(data.listeningTime);
        timeLabel.getStyleClass().add("artist-time");
        timeLabel.setMinWidth(70);

        row.getChildren().addAll(nameLabel, timeLabel);
        return row;
    }
}