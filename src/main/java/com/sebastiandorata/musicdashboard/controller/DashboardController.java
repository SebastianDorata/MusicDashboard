package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.Utils.SongCell;
import com.sebastiandorata.musicdashboard.Utils.Utils;
import com.sebastiandorata.musicdashboard.entity.*;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingServices;
import com.sebastiandorata.musicdashboard.service.SongService;
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
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DashboardController {

    @Autowired
    private MusicPlayerService musicPlayerService;
    @Setter
    @Getter
    @Autowired
    private SongService songService;
    @Setter
    @Getter
    @Autowired
    private PlaybackTrackingServices playbackTrackingServices;
    @Autowired
    private MyLibraryController myLibraryController;
    @Autowired
    private PlaylistController playlistController;
    @Autowired
    private ImportController importController;
    @Autowired
    private AnalyticsController analyticsController;


    private TextField searchBar = new TextField();
    private Label accountUsername = new Label();
    private Button accountPlanStatus = new Button("Basic Plan");

    // Now Playing area

    private Label SongCount;
    private ListView<Song> songListView;

    private ListView<Artist> artistsListView;
    private ListView<Album> albumListView;
    private ListView<PlaybackHistory> playbackHistoryListView;


    //This is the public method that is called only to switch the scenes
    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }
        MainController.switchViews(scene);
    }


    private Scene createScene() {
        BorderPane root = new BorderPane();

        VBox left = leftMenu();
        VBox center = centerMenu();
        VBox right = rightMenu();

        root.setLeft(left);
        root.setCenter(center);
        root.setRight(right);


        return new Scene(root, Utils.APP_WIDTH, Utils.APP_HEIGHT);
    }


    private VBox leftMenu() {
        VBox left = new VBox(20);
        left.setPadding(new Insets(20));
        left.setPrefWidth(Utils.APP_WIDTH * 0.25);
        left.getStyleClass().addAll("green-background");

        HBox myLibraryPage = myLibrary();
        HBox myPlaylistPage = myPlaylist();
        HBox importFilesPage = importFiles();
        HBox myReportsPage = myReports();
        left.getChildren().addAll(myLibraryPage, myPlaylistPage, importFilesPage, myReportsPage);
        return left;
    }

    private HBox myLibrary() {
        HBox myLibraryPage = new HBox();
        Button myLibraryBtn = createLeftButton("My Library");
        myLibraryBtn.setOnAction(e -> myLibraryController.show());
        myLibraryPage.getChildren().add(myLibraryBtn);
        return myLibraryPage;
    }

    private HBox myPlaylist() {
        HBox myPlaylistPage = new HBox();
        Button myPlaylistBtn = createLeftButton("My Playlist");
        myPlaylistBtn.setOnAction(e -> playlistController.show());
        myPlaylistPage.getChildren().add(myPlaylistBtn);
        return myPlaylistPage;
    }

    private HBox importFiles() {
        HBox importFilesPage = new HBox();
        Button importFilesBtn = createLeftButton("Import Files");
        importFilesBtn.setOnAction(e -> importController.show());
        importFilesPage.getChildren().add(importFilesBtn);
        return importFilesPage;
    }

    private HBox myReports() {
        HBox myReportsPage = new HBox();
        Button ReportsBtn = createLeftButton("My Reports");
        ReportsBtn.setOnAction(e -> analyticsController.show());
        myReportsPage.getChildren().add(ReportsBtn);
        return myReportsPage;
    }

    private Button createLeftButton(String text) {
        Button LeftBtn = new Button(text);
        LeftBtn.getStyleClass().addAll("Left-Btn");
        return LeftBtn;
    }


    private VBox centerMenu() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(20));
        center.setPrefWidth(Utils.APP_WIDTH * 0.5);


        center.getChildren().addAll(createNowPlayingBar(), createStatCards(), createGraphSection());
        return center;
    }

    private StackPane createNowPlayingBar() {
        StackPane nowPlaying = new StackPane();
        nowPlaying.setPrefWidth(Utils.APP_WIDTH * 0.50);
        nowPlaying.setPrefHeight(Utils.APP_HEIGHT * 0.25);
        nowPlaying.getStyleClass().add("now-playing-bar");


        ImageView albumArt = new ImageView(); //https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/image/ImageView.html
        albumArt.setPreserveRatio(false);
        albumArt.fitWidthProperty().bind(nowPlaying.widthProperty());
        albumArt.fitHeightProperty().bind(nowPlaying.heightProperty());



        HBox overlay = new HBox(20);
        overlay.setPadding(new Insets(20));
        overlay.setAlignment(Pos.CENTER_LEFT);
        overlay.getStyleClass().add("now-playing-overlay");

        Label songTitle = new Label("No song playing");
        songTitle.getStyleClass().add("now-playing-title");

        overlay.getChildren().add(songTitle);

        // Time labels and slider
        HBox timeRow = new HBox(10);
        Label currentTime = new Label("0:00");
        Label remainingTime = new Label("-0:00");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        timeRow.getChildren().addAll(currentTime, spacer, remainingTime);

        Slider progressSlider = new Slider();
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setPrefWidth(Double.MAX_VALUE);

        VBox.setVgrow(progressSlider, Priority.ALWAYS);




        //https://openjfx.io/javadoc/25/javafx.base/javafx/beans/property/package-summary.html
        //https://openjfx.io/javadoc/25/javafx.base/javafx/beans/Observable.html
        // Listen to changes in current song
        musicPlayerService.currentSongProperty().addListener( //currentSongProperty() is called to get the observable property
                (observable, oldSong, newSong) -> {
                    if (newSong != null) {
                        songTitle.setText(newSong.getTitle());

                        // Update album art
                        if (newSong.getAlbum() != null && newSong.getAlbum().getAlbumArtPath() != null) {
                            Image image = new Image("file:" + newSong.getAlbum().getAlbumArtPath(), true);
                            albumArt.setImage(image);
                        } else {
                            albumArt.setImage(null); // fallback if no album art
                        }

                        // Reset slider
                        progressSlider.setValue(0);
                        progressSlider.setMax(newSong.getDuration()); // set max to song duration
                        currentTime.setText("0:00");
                        remainingTime.setText("-" + musicPlayerService.formatTime(newSong.getDuration()));
                    } else {
                        // No song playing
                        songTitle.setText("No song playing");
                        albumArt.setImage(null);
                        progressSlider.setValue(0);
                        progressSlider.setMax(100);
                        currentTime.setText("0:00");
                        remainingTime.setText("-0:00");
                    }
                });
        // Update slider and times while playing
        musicPlayerService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            Song song = musicPlayerService.getCurrentSong();
            if (song != null && newTime != null) {
                double total = song.getDuration();
                double played = newTime.toSeconds();

                // Clamp values to avoid slider going out of bounds
                played = Math.min(played, total);

                progressSlider.setMax(total);
                progressSlider.setValue(played);

                currentTime.setText(musicPlayerService.formatTime(played));
                remainingTime.setText("-" + musicPlayerService.formatTime(total - played));
            }
        });
        progressSlider.setOnMouseReleased(e -> {
            Song song = musicPlayerService.getCurrentSong();
            if (song != null) {
                musicPlayerService.seek(progressSlider.getValue());
            }
        });
        nowPlaying.getChildren().addAll(albumArt, overlay,songTitle,timeRow, progressSlider, createPlayerControls(), createSongListSection());
        return nowPlaying;
    }


    private HBox createPlayerControls() {
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(15));
        controls.setAlignment(Pos.CENTER);
        controls.getStyleClass().add("player-controls");


        Button prevBtn = new Button("⏮");
        prevBtn.getStyleClass().add("btn-blue");
        Button playPauseBtn = new Button("▶");
        playPauseBtn.getStyleClass().add("btn-blue");
        Button nextBtn = new Button("⏭");
        nextBtn.getStyleClass().add("btn-blue");


        // Toggle play/pause
        playPauseBtn.setOnAction(e -> {
            if (playPauseBtn.getText().equals("▶")) {
                musicPlayerService.play();
                playPauseBtn.setText("⏸");
            } else {
                musicPlayerService.pause();
                playPauseBtn.setText("▶");
            }
        });

        controls.getChildren().addAll(prevBtn, playPauseBtn, nextBtn);
        return controls;
    }


    private VBox createSongListSection() {
        VBox songSection = new VBox(15);

        Label heading = new Label("Your Library");
        heading.getStyleClass().add("section-title");

        songListView = new ListView<>();

        //https://openjfx.io/javadoc/25/javafx.controls/javafx/scene/control/Cell.html
        songListView.setCellFactory(list -> new SongCell());

        // Load songs from database
        List<Song> songs = songService.getAllSongs();
        songListView.getItems().addAll(songs);

        // Listen for song selection
        songListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldSong, newSong) -> {
                    if (newSong != null) {
                        musicPlayerService.playSong(newSong);  // Play when clicked
                    }
                }
        );

        songSection.getChildren().addAll(heading, songListView);
        VBox.setVgrow(songListView, Priority.ALWAYS);
        return songSection;
    }


    private HBox createStatCards() {
        HBox cards = new HBox(20);


        cards.getChildren().addAll(
                statCard("Playback Today"),
                statCard("Top Artist Today"),
                statCard("Top Album Today"),
                statCard("Top Artist Week"),
                statCard("Top Album Week")
        );

        return cards;
    }

    private VBox statCard(String title) {
        VBox card = new VBox(10);
        card.setPrefWidth(Utils.APP_WIDTH * 0.50);
        card.setPrefHeight(Utils.APP_HEIGHT * 0.25);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("dashboard-card");


        //Label playbackToday = new Label("Playback Today");
        //playbackToday.getStyleClass().add("section-title");

        //Label number = new Label("4 Hours");
        //number.getStyleClass().add("stat-number");

        //card.getChildren().addAll(playbackToday, number);


        return card;
    }


    private VBox createGraphSection() {
        VBox graphBox = new VBox(10);
        graphBox.setPrefWidth(Utils.APP_WIDTH * 0.50);
        graphBox.setPrefHeight(Utils.APP_HEIGHT * 0.50);

        Label title = new Label("All Time Stats");

        graphBox.getChildren().add(title);

        graphBox.setStyle("-fx-background-color:#0d2342; -fx-background-radius:15; -fx-padding:20;");

        return graphBox;
    }


    private VBox rightMenu() {

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

        topArtist.getChildren().add(title);

        for (int i = 1; i <= 5; i++) {
            HBox row = new HBox(10);

            Label name = new Label("Top Artist " + i);
            Label time = new Label("00:00");

            row.getChildren().addAll(name, time);
            topArtist.getChildren().add(row);
        }

        return topArtist;
    }

    private VBox createRecentlyPlayed() {

        VBox recentlyPlayed = new VBox();
        recentlyPlayed.setPrefWidth(Utils.APP_WIDTH * 0.25);
        recentlyPlayed.setPrefHeight(Utils.APP_HEIGHT * 0.50);
        recentlyPlayed.getStyleClass().add("recently-played");

        Label title = new Label("Recently Played");
        recentlyPlayed.getChildren().add(title);


        return recentlyPlayed;
    }

}

