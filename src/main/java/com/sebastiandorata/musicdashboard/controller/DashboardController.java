package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.Utils.AlbumArtComponents;
import com.sebastiandorata.musicdashboard.Utils.CardFactory;
import com.sebastiandorata.musicdashboard.Utils.IconFactory;
import com.sebastiandorata.musicdashboard.Utils.Utils;
import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.SongService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.sebastiandorata.musicdashboard.Utils.IconFactory.createIcon;


@Component
public class DashboardController {

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;

    @Setter
    @Getter
    @Lazy
    @Autowired
    private SongService songService;

    @Setter
    @Getter
    @Lazy
    @Autowired
    private PlaybackTrackingService playbackTrackingService;
    @Autowired
    private UserSessionService userSessionService;


    @Autowired
    private PlaylistController playlistController;

    @Autowired
    private ImportController importController;

    @Autowired
    private AnalyticsController analyticsController;

    private Button playPauseBtn;
    private AlbumArtComponents albumArtComponents;
    private HBox nowPlaying;


    // Register with MainController after Spring builds this bean,
    // so MainController can navigate back here without circular wiring.
    @PostConstruct
    public void register() {
        MainController.registerDashboard(this);
    }




    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/dashboard.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }

        MainController.switchViews(scene);
    }


    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.setLeft(leftMenu());
        root.setCenter(centerMenu());
        root.setRight(createRightMenu());
        return new Scene(root, Utils.APP_WIDTH, Utils.APP_HEIGHT);
    }

    private VBox leftMenu() {
        VBox left = new VBox(20);
        left.setPadding(new Insets(20));
        left.setPrefWidth(Utils.APP_WIDTH * 0.25);
        left.getStyleClass().add("green-background");

        Button libraryBtn  = createLeftButton("My Library");
        Button playlistBtn = createLeftButton("My Playlist");
        Button importBtn   = createLeftButton("Import Files");
        Button reportsBtn  = createLeftButton("My Reports");

        libraryBtn.setOnAction(e  -> MainController.navigateTo("library"));
        playlistBtn.setOnAction(e -> playlistController.show());
        importBtn.setOnAction(e   -> MainController.navigateTo("import"));
        reportsBtn.setOnAction(e  -> analyticsController.show());

        left.getChildren().addAll(
                wrap(libraryBtn), wrap(playlistBtn), wrap(importBtn), wrap(reportsBtn)
        );
        return left;
    }
    private HBox wrap(Button btn) {
        return new HBox(btn);
    }
    private Button createLeftButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("Left-Btn");
        return btn;
    }


    private VBox centerMenu() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(20));
        center.setPrefWidth(Utils.APP_WIDTH * 0.5);
        center.getChildren().addAll(createNowPlayingBar(), createStatCards(), createGraphSection());
        return center;
    }
    // ── NOW PLAYING BAR ──────────────────────────────────────────────
    private HBox createNowPlayingBar() {
        nowPlaying = new HBox(0); // was: HBox nowPlaying = new HBox(0);
        nowPlaying.getStyleClass().add("now-playing-bar");
        nowPlaying.setAlignment(Pos.CENTER_LEFT);

        // LEFT: Album art container
        StackPane artContainer = createAlbumArtContainer();

        // RIGHT: Info + Controls
        VBox infoSection = createNowPlayingInfoSection();
        HBox.setHgrow(infoSection, Priority.ALWAYS);

        nowPlaying.getChildren().addAll(artContainer, infoSection);
        return nowPlaying;
    }
    private StackPane createAlbumArtContainer() {
        ImageView albumArt = new ImageView(); //https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/image/ImageView.html
            albumArt.setFitWidth(Utils.APP_HEIGHT * 0.25);
            albumArt.setFitHeight(Utils.APP_HEIGHT * 0.25);
            albumArt.setPreserveRatio(false); // fill the square fully
            albumArt.getStyleClass().add("now-playing-art");

        Label artPlaceholder = new Label("♪");
            artPlaceholder.getStyleClass().add("txt-white-lg");

        // Placeholder shown when no art is available
        StackPane artContainer = new StackPane();
            artContainer.setMinWidth(Utils.APP_HEIGHT * 0.25);
            artContainer.setMinHeight(Utils.APP_HEIGHT * 0.25);
            artContainer.setMaxWidth(Utils.APP_HEIGHT * 0.25);
            artContainer.setMaxHeight(Utils.APP_HEIGHT * 0.25);
            artContainer.getStyleClass().add("now-playing-art-placeholder");
            artContainer.getChildren().addAll(artPlaceholder, albumArt);

            albumArtComponents = new AlbumArtComponents(albumArt, artPlaceholder);

        // Update album art when song changes
        //https://openjfx.io/javadoc/25/javafx.base/javafx/beans/property/package-summary.html
        //https://openjfx.io/javadoc/25/javafx.base/javafx/beans/Observable.html
        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null && newSong.getAlbum() != null && newSong.getAlbum().getAlbumArtPath() != null) {
                try {
                    albumArt.setImage(new Image("file:" + newSong.getAlbum().getAlbumArtPath(), true));
                    artPlaceholder.setVisible(false);
                } catch (Exception e) {
                    artPlaceholder.setVisible(true);
                }
            } else {
                albumArt.setImage(null);
                artPlaceholder.setVisible(true);
            }
        });

        return artContainer;
    }
    private VBox createNowPlayingInfoSection() {
        VBox infoSection = new VBox(0);
            infoSection.getStyleClass().add("now-playing-info");

        // Song title
        Label songTitle = new Label("No song playing");
            songTitle.getStyleClass().addAll("txt-white-md-bld", "empty-msg");

        // Artist name
        Label artistName = new Label("—");
            artistName.getStyleClass().add("now-playing-artist");
            // TODO: Make artistName clickable to navigate to that artist's discography in MyLibraryController once the artist detail view is implemented.

        // Spacer to push controls to bottom
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        // Progress slider
        Slider progressSlider = new Slider(0, 100, 0);
            progressSlider.getStyleClass().add("now-playing-slider");

        // Time labels
        Label currentTime = new Label("0:00");
        Label remainingTime = new Label("-0:00");
            currentTime.getStyleClass().add("now-playing-time");
            remainingTime.getStyleClass().add("now-playing-time");

        Region timeSpacer = new Region();
        HBox.setHgrow(timeSpacer, Priority.ALWAYS);
        HBox timeRow = new HBox(currentTime, timeSpacer, remainingTime);
            timeRow.getStyleClass().add("time-row");

        // Player controls
        HBox controls = createPlayerControls();

        // Small gap
        Region controlSpacer = new Region();
        controlSpacer.setMinHeight(8);

        infoSection.getChildren().addAll(songTitle, artistName, topSpacer, progressSlider, timeRow, controlSpacer, controls);
        // Fixes the issue where artwork doesn't show when switching from LibraryController to DashboardController without playing next/prev
        Song alreadyPlaying = musicPlayerService.getCurrentSong();
        if (alreadyPlaying != null) {
            songTitle.setText(alreadyPlaying.getTitle());

            String artistText = alreadyPlaying.getArtists() != null && !alreadyPlaying.getArtists().isEmpty()
                    ? alreadyPlaying.getArtists().get(0).getName()
                    : "Unknown Artist";
            artistName.setText(artistText);

            // Use the AlbumArtComponents to update artwork
            if (alreadyPlaying.getAlbum() != null && alreadyPlaying.getAlbum().getAlbumArtPath() != null) {
                try {
                    albumArtComponents.albumArt.setImage(new Image("file:" + alreadyPlaying.getAlbum().getAlbumArtPath(), true));
                    albumArtComponents.artPlaceholder.setVisible(false);
                } catch (Exception e) {
                    albumArtComponents.artPlaceholder.setVisible(true);
                }
            }

            progressSlider.setMax(alreadyPlaying.getDuration());
            remainingTime.setText("-" + musicPlayerService.formatTime(alreadyPlaying.getDuration()));
            if (playPauseBtn != null)
                playPauseBtn.setGraphic(IconFactory.createIcon("pause", 24));
                playPauseBtn.setUserData("pause");
        }

        // Listen to changes in current song
        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                nowPlaying.getStyleClass().add("is-playing");
                songTitle.setText(newSong.getTitle());

                    playPauseBtn.setGraphic(IconFactory.createIcon("pause", 24));
                    playPauseBtn.setUserData("pause");
                // Artist name
                String artistText = newSong.getArtists() != null && !newSong.getArtists().isEmpty()
                        ? newSong.getArtists().get(0).getName()
                        : "Unknown Artist";
                artistName.setText(artistText);

                progressSlider.setMax(newSong.getDuration());
                remainingTime.setText("-" + musicPlayerService.formatTime(newSong.getDuration()));
                if (playPauseBtn != null)
                    playPauseBtn.setGraphic(IconFactory.createIcon("pause", 24));
                    playPauseBtn.setUserData("pause");
            } else {
                nowPlaying.getStyleClass().remove("is-playing");
                songTitle.setText("No song playing");
                artistName.setText("—");
                currentTime.setText("0:00");
                remainingTime.setText("-0:00");
                progressSlider.setValue(0);
                progressSlider.setMax(100);
                if (playPauseBtn != null)
                    playPauseBtn.setGraphic(IconFactory.createIcon("play", 24));
                    playPauseBtn.setUserData("play");
            }
        });

        musicPlayerService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isPressed()) {
                progressSlider.setValue(newTime.toSeconds());
                currentTime.setText(musicPlayerService.formatTime(newTime.toSeconds()));
            }
        });

        progressSlider.setOnMouseReleased(e -> {
            musicPlayerService.seek(Math.round(progressSlider.getValue()));
        });

        return infoSection;
    }
    // ── PLAYER CONTROLS ──────────────────────────────────────────────
    private HBox createPlayerControls() {
        HBox controls = new HBox(12);
            controls.getStyleClass().add("cntr-spc-sm");
        Button prevBtn = new Button();
            prevBtn.setGraphic(createIcon("prev", 20));
        playPauseBtn = new Button();
            playPauseBtn.setGraphic(createIcon("play", 24));
            playPauseBtn.setUserData("play");
        Button nextBtn = new Button();
            nextBtn.setGraphic(createIcon("next", 20));

        prevBtn.getStyleClass().addAll("player-btn", "player-btn-secondary", "cursor");
        playPauseBtn.getStyleClass().addAll("player-btn", "player-btn-primary", "cursor");
        nextBtn.getStyleClass().addAll("player-btn", "player-btn-secondary", "cursor");

        // Prev — restarts current song if > 3s in, otherwise plays previous
        prevBtn.setOnAction(e -> musicPlayerService.playPrevious());

        // Play/Pause toggle
        playPauseBtn.setOnAction(e -> {
            String currentState = (String) playPauseBtn.getUserData();

            if ("play".equals(currentState)) {
                musicPlayerService.play();
                nowPlaying.getStyleClass().add("is-playing");
                playPauseBtn.setGraphic(IconFactory.createIcon("pause", 24));
                playPauseBtn.setUserData("pause");
            } else {
                musicPlayerService.pause();
                nowPlaying.getStyleClass().remove("is-playing");
                playPauseBtn.setGraphic(IconFactory.createIcon("play", 24));
                playPauseBtn.setUserData("play");
            }
        });

        // Next — advances to next song in shuffled queue
        nextBtn.setOnAction(e -> musicPlayerService.playNext());


        controls.getChildren().addAll(prevBtn, playPauseBtn, nextBtn);
        return controls;
    }
    // ── STAT CARDS ───────────────────────────────────────────────────
    private HBox createStatCards() {
        HBox cards = new HBox(20);
        cards.getChildren().addAll(
                CardFactory.createStatCard("Playback Today", "—"),
                CardFactory.createStatCard("Top Artist Today", "—"),
                CardFactory.createStatCard("Top Album Today", "—"),
                CardFactory.createStatCard("Top Artist Week", "—"),
                CardFactory.createStatCard("Top Album Week", "—")
        );
        return cards;
    }
    // ── GRAPH SECTION ────────────────────────────────────────────────
    private VBox createGraphSection() {
        VBox graphBox = new VBox(10);
            graphBox.setPrefWidth(Utils.APP_WIDTH * 0.50);
            graphBox.setPrefHeight(Utils.APP_HEIGHT * 0.50);
            graphBox.getStyleClass().add("dk-blue-background");

        Label title = new Label("All Time Stats");
            title.getStyleClass().add("section-title");
            graphBox.getChildren().add(title);

        // TODO: Wire up analytics visualization here
        return graphBox;
    }

    private VBox createRightMenu() {
        VBox right = new VBox(25);
        right.setPadding(new Insets(20));
        right.setPrefWidth(Utils.APP_WIDTH * 0.25);
        right.getChildren().addAll(createTopArtists(), createRecentlyPlayed());
        return right;
    }
    private VBox createTopArtists() {
        VBox topArtist = new VBox(15);
        topArtist.setPrefWidth(Utils.APP_WIDTH * 0.25);
        topArtist.setPrefHeight(Utils.APP_HEIGHT * 0.50);
        topArtist.getStyleClass().add("graph-style");

        Label title = new Label("Top 5 Artists");
        title.getStyleClass().add("txt-white-md-bld");
        topArtist.getChildren().add(title);

        for (int i = 1; i <= 5; i++) {
            HBox row = new HBox(10);
            Label name = new Label("Top Artist " + i);
            Label time = new Label("00:00");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(name, spacer, time);
            topArtist.getChildren().add(row);
        }

        // TODO: Wire up analytics service to populate with real data
        return topArtist;
    }
    private VBox createRecentlyPlayed() {
        VBox recentlyPlayed = new VBox(0);
            recentlyPlayed.setPrefWidth(Utils.APP_WIDTH * 0.25);
            recentlyPlayed.setPrefHeight(Utils.APP_HEIGHT * 0.50);
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
                    timeLbl.setText(Utils.formatRelativeTime(item.getPlayedAt()));
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
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> refreshRecentlyPlayedList(historyList));
                }).start();
            }
        });

        // Double-click to replay
        historyList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                PlaybackHistory selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getSong() != null) {
                    musicPlayerService.playSong(selected.getSong());

                    // Load all songs into queue so next/prev work
                    List<Song> allSongs = songService.getAllSongs();
                    musicPlayerService.setQueue(allSongs);
                    musicPlayerService.playSong(selected.getSong());

                }

            }
        });




        recentlyPlayed.getChildren().addAll(title, historyList);
        return recentlyPlayed;
    }
    /**
     * Refreshes the recently played list from the database.
     * TODO: Once authentication session context is accessible, replace hardcoded userId
     *       with authenticationService.getCurrentUser().getId()
     */
    private void refreshRecentlyPlayedList(ListView<PlaybackHistory> historyList) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId != null) {
            List<PlaybackHistory> history = playbackTrackingService.getRecentlyPlayed(userId);
            historyList.getItems().setAll(history);
        }
    }
}