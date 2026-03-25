package com.sebastiandorata.musicdashboard.controllerUtils;

import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.viewmodel.TopArtistsViewModel;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class TopArtistsController extends UIComponent {

    @Autowired
    private TopArtistsViewModel topArtistsViewModel;

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;


    private List<Label> artistTimeLabels = new ArrayList<>();

    public VBox createPanel() {
        VBox topArtist = new VBox(0);
        topArtist.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        topArtist.setPrefHeight(AppUtils.APP_HEIGHT * 0.50);
        topArtist.getStyleClass().add("graph-style");

        Label title = new Label("Top 5 Artists (All-Time)");
        title.getStyleClass().add("txt-white-md-bld");
        title.setPadding(new Insets(0, 0, 10, 0));

        VBox artistRows = new VBox(0);
        artistRows.setFillWidth(true);


        for (int i = 0; i < 5; i++) {
            HBox row = createArtistRow();
            artistRows.getChildren().add(row);
        }

        topArtist.getChildren().addAll(title, artistRows);


        refreshTopArtists(artistRows);

        // Listen to playback events and refresh on song changes
        musicPlayerService.playbackEventProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && ("next".equals(newVal) || "previous".equals(newVal))) {
                refreshTopArtists(artistRows);
            }
        });

        return topArtist;
    }

    private HBox createArtistRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));
        row.getStyleClass().add("artist-row");

        // Artist name (left side)
        Label artistName = new Label("—");
        artistName.getStyleClass().add("artist-name");
        artistName.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(artistName, Priority.ALWAYS);


        Label listeningTime = new Label("—");
        listeningTime.getStyleClass().add("artist-time");
        listeningTime.setMinWidth(70);


        artistTimeLabels.add(listeningTime);

        row.getChildren().addAll(artistName, listeningTime);
        row.setId("artistRow_" + artistTimeLabels.size());
        return row;
    }

    private void refreshTopArtists(VBox artistRows) {
        topArtistsViewModel.loadTopArtistsData(topArtistsData -> {

            List<javafx.scene.Node> rows = artistRows.getChildren();


            for (int i = 0; i < Math.min(rows.size(), topArtistsData.size()); i++) {
                if (rows.get(i) instanceof HBox row) {
                    TopArtistsViewModel.TopArtistRowData data = topArtistsData.get(i);


                    if (row.getChildren().size() >= 2) {
                        Label nameLabel = (Label) row.getChildren().get(0);
                        Label timeLabel = (Label) row.getChildren().get(1);

                        nameLabel.setText(data.artistName);
                        timeLabel.setText(data.listeningTime);
                    }
                }
            }
        });
    }
}