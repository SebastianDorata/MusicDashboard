package com.sebastiandorata.musicdashboard.utils;

import com.sebastiandorata.musicdashboard.controllerUtils.UIComponent;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;

import com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel.StatCardsViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CardFactory extends UIComponent {

    @Autowired
    private MusicPlayerService musicPlayerService;
    @Autowired
    private StatCardsViewModel statCardsViewModel;


    private Label playbackValueLabel;
    private Label playbackUnitLabel;
    private Label avgSessionValueLabel;
    private Label avgSessionUnitLabel;
    private Label todaySongLabel;
    private Label todayAlbumLabel;
    private Label weeklySongLabel;
    private Label weeklyAlbumLabel;


    public HBox createStatCards() {
        HBox cards = new HBox(15);
        cards.getStyleClass().add("stat-cards-row");

        VBox card1 = createCombinedPlaybackAvgCard();

        todaySongLabel   = new Label("—");
        todayAlbumLabel  = new Label("—");
        weeklySongLabel  = new Label("—");
        weeklyAlbumLabel = new Label("—");

        VBox card2 = createNamedStatCard("Top Song Today", todaySongLabel);
        VBox card3 = createNamedStatCard("Top Album Today", todayAlbumLabel);
        VBox card4 = createNamedStatCard("Top Song This Week", weeklySongLabel);
        VBox card5 = createNamedStatCard("Top Album This Week", weeklyAlbumLabel);


        for (VBox card : new VBox[]{card1, card2, card3, card4, card5}) {
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
        }

        cards.getChildren().addAll(card1, card2, card3, card4, card5);

        refreshStatCards();
        musicPlayerService.currentSongProperty()
                .addListener((obs, oldSong, newSong) -> refreshStatCards());

        return cards;
    }


    private VBox createCombinedPlaybackAvgCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("dashboard-stat-card");

        Label playbackTitle = new Label("Playback Duration Today");
        playbackTitle.getStyleClass().addAll("wt-smmd-bld", "dashboard-stat-title");

        playbackValueLabel = new Label("0");
        playbackValueLabel.getStyleClass().add("dashboard-stat-value");

        playbackUnitLabel = new Label("Minutes");
        playbackUnitLabel.getStyleClass().add("dashboard-stat-unit");

        VBox playbackSection = new VBox(4);
        playbackSection.getChildren().addAll(playbackTitle, playbackValueLabel, playbackUnitLabel);


        Region separator = new Region();
        separator.setPrefHeight(10);

        Label avgTitle = new Label("Avg Listening Period");
        avgTitle.getStyleClass().addAll("wt-smmd-bld", "dashboard-stat-title");

        HBox avgValueRow = new HBox(6);
        avgValueRow.setAlignment(Pos.CENTER_LEFT);

        Label avgPrefix = new Label("Avg");
        avgPrefix.getStyleClass().add("dashboard-stat-prefix");

        avgSessionValueLabel = new Label("0");
        avgSessionValueLabel.getStyleClass().add("dashboard-stat-value");

        avgSessionUnitLabel = new Label("min/session");
        avgSessionUnitLabel.getStyleClass().add("dashboard-stat-unit");

        avgValueRow.getChildren().addAll(avgPrefix, avgSessionValueLabel);

        VBox avgSection = new VBox(4);
        avgSection.getChildren().addAll(avgTitle, avgValueRow, avgSessionUnitLabel);

        card.getChildren().addAll(playbackSection, separator, avgSection);
        return card;
    }
    public static VBox createSongCard(Song song, MusicPlayerService musicPlayerService) {
        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setPrefHeight(220);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("dashboard-stat-card");
        card.setCursor(javafx.scene.Cursor.HAND);


        ImageView albumArt = new ImageView();
        albumArt.setFitWidth(130);
        albumArt.setFitHeight(130);
        albumArt.setPreserveRatio(true);

        if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
            try {
                Image image = new Image("file:" + song.getAlbum().getAlbumArtPath(), true);
                albumArt.setImage(image);
            } catch (Exception e) {
                albumArt.setImage(null);
            }
        }


        Label title = new Label(song.getTitle());
        title.getStyleClass().addAll("txt-white-sm-bld");
        title.setWrapText(true);
        title.setMaxWidth(130);


        String artistName = "Unknown Artist";
        if (song.getArtists() != null && !song.getArtists().isEmpty()) {
            artistName = song.getArtists().get(0).getName();
        }
        Label artist = new Label(artistName);
        artist.getStyleClass().addAll("txt-grey-sm");
        artist.setWrapText(true);
        artist.setMaxWidth(130);

        card.getChildren().addAll(albumArt, title, artist);


        card.setOnMouseClicked(e -> musicPlayerService.playSong(song));

        return card;
    }
    public static VBox createAlbumCard(Album album, MusicPlayerService musicPlayerService) {
        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setPrefHeight(220);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("dashboard-card");
        card.setCursor(javafx.scene.Cursor.HAND);


        ImageView albumArt = new ImageView();
        albumArt.setFitWidth(130);
        albumArt.setFitHeight(130);
        albumArt.setPreserveRatio(true);

        if (album.getAlbumArtPath() != null) {
            try {
                Image image = new Image("file:" + album.getAlbumArtPath(), true);
                albumArt.setImage(image);
            } catch (Exception e) {
                albumArt.setImage(null);
            }
        }


        Label title = new Label(album.getTitle());
        title.getStyleClass().addAll("txt-white-sm-bld");
        title.setWrapText(true);
        title.setMaxWidth(130);


        String year = album.getReleaseYear() != null ? String.valueOf(album.getReleaseYear()) : "Unknown";
        Label yearLabel = new Label(year);
        yearLabel.getStyleClass().addAll("txt-grey-sm");

        card.getChildren().addAll(albumArt, title, yearLabel);

        card.setOnMouseClicked(e -> {
            if (album.getSongs() != null && !album.getSongs().isEmpty()) {
                musicPlayerService.playSong(album.getSongs().get(0));
            }
        });

        return card;
    }








    public static VBox createListPanel(String panelTitle, VBox listContent) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("analytics-list-panel");
        panel.getStyleClass().add("analytics-dark-bg");

        Label title = new Label(panelTitle);
        title.getStyleClass().add("analytics-section-title");

        ScrollPane scrollPane = new ScrollPane(listContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("trans-background");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(title, scrollPane);
        return panel;
    }
    private VBox createNamedStatCard(String titleText, Label valueLabel) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-stat-card");


        Label title = new Label(titleText);
        title.getStyleClass().addAll("wt-smmd-bld", "dashboard-stat-title");

        valueLabel.getStyleClass().add("dashboard-stat-value");
        valueLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(title, spacer, valueLabel);
        return card;
    }



    private void refreshStatCards() {
        statCardsViewModel.loadStatCardsData(statsData -> {

            playbackValueLabel.setText(statsData.playbackDurationValue);
            playbackUnitLabel.setText(statsData.playbackDurationUnit);
            avgSessionValueLabel.setText(statsData.averageSessionValue);
            avgSessionUnitLabel.setText(statsData.averageSessionUnit);

            todaySongLabel.setText(statsData.todayTopSongName != null ? statsData.todayTopSongName : "—");

            todayAlbumLabel.setText(statsData.todayTopAlbumName != null ? statsData.todayTopAlbumName : "—");

            weeklySongLabel.setText(statsData.weeklyTopSongName != null ? statsData.weeklyTopSongName : "—");

            weeklyAlbumLabel.setText(statsData.weeklyTopAlbumName != null ? statsData.weeklyTopAlbumName : "—");
        });
    }
}