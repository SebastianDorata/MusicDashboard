package com.sebastiandorata.musicdashboard.controllerUtils;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.AlbumArtView;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.IconFactory;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Component
public class PlaybackPanelController extends UIComponent {

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;

    private Button playPauseBtn;
    private AlbumArtView albumArtView;
    private HBox nowPlaying;


    public HBox createPanel() {
        nowPlaying = new HBox(0);
        nowPlaying.getStyleClass().add("now-playing-bar");
        nowPlaying.setAlignment(Pos.CENTER_LEFT);

        StackPane artContainer = createAlbumArtContainer();


        VBox infoSection = createNowPlayingInfoSection();
        HBox.setHgrow(infoSection, Priority.ALWAYS);

        nowPlaying.getChildren().addAll(artContainer, infoSection);
        return nowPlaying;
    }

    private StackPane createAlbumArtContainer() {
        ImageView albumArt = new ImageView();
        albumArt.setFitWidth(AppUtils.APP_HEIGHT * 0.25);
        albumArt.setFitHeight(AppUtils.APP_HEIGHT * 0.25);
        albumArt.setPreserveRatio(false);
        albumArt.getStyleClass().add("now-playing-art");

        Label artPlaceholder = new Label("♪");
        artPlaceholder.getStyleClass().add("txt-white-lg");

        StackPane artContainer = new StackPane();
        artContainer.setMinWidth(AppUtils.APP_HEIGHT * 0.25);
        artContainer.setMinHeight(AppUtils.APP_HEIGHT * 0.25);
        artContainer.setMaxWidth(AppUtils.APP_HEIGHT * 0.25);
        artContainer.setMaxHeight(AppUtils.APP_HEIGHT * 0.25);
        artContainer.getStyleClass().add("now-playing-art-placeholder");
        artContainer.getChildren().addAll(artPlaceholder, albumArt);


        double containerSize = AppUtils.APP_HEIGHT * 0.25;
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(containerSize, containerSize);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        artContainer.setClip(clip);

        albumArtView = new AlbumArtView(albumArt, artPlaceholder);

        // Update album art when song changes
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

        Label songTitle = new Label("No song playing");
        songTitle.getStyleClass().addAll("txt-white-md-bld", "empty-msg");

        Label artistName = new Label("—");
        artistName.getStyleClass().add("now-playing-artist");

        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        Slider progressSlider = new Slider(0, 100, 0);
        progressSlider.getStyleClass().add("now-playing-slider");

        Label currentTime = new Label("0:00");
        Label remainingTime = new Label("-0:00");
        currentTime.getStyleClass().add("now-playing-time");
        remainingTime.getStyleClass().add("now-playing-time");

        Region timeSpacer = new Region();
        HBox.setHgrow(timeSpacer, Priority.ALWAYS);
        HBox timeRow = new HBox(currentTime, timeSpacer, remainingTime);
        timeRow.getStyleClass().add("time-row");

        HBox controls = createPlayerControls();

        Region controlSpacer = new Region();
        controlSpacer.setMinHeight(8);

        infoSection.getChildren().addAll(songTitle, artistName, topSpacer, progressSlider, timeRow, controlSpacer, controls);

        // Initialize with currently playing song
        Song alreadyPlaying = musicPlayerService.getCurrentSong();
        if (alreadyPlaying != null) {
            updateSongDisplay(songTitle, artistName, alreadyPlaying, progressSlider, remainingTime);
            playPauseBtn.setGraphic(IconFactory.createIcon("pause", 24));
            playPauseBtn.setUserData("pause");
        }

        // Listen to song changes
        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                nowPlaying.getStyleClass().add("is-playing");
                updateSongDisplay(songTitle, artistName, newSong, progressSlider, remainingTime);
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
                playPauseBtn.setGraphic(IconFactory.createIcon("play", 24));
                playPauseBtn.setUserData("play");
            }
        });

        // Update progress bar
        musicPlayerService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isPressed()) {
                progressSlider.setValue(newTime.toSeconds());
                currentTime.setText(AppUtils.formatTime(newTime.toSeconds()));
            }
        });

        progressSlider.setOnMouseReleased(e -> {
            musicPlayerService.seek(Math.round(progressSlider.getValue()));
        });

        return infoSection;
    }

    private void updateSongDisplay(Label songTitle, Label artistName, Song song, Slider progressSlider, Label remainingTime) {
        songTitle.setText(song.getTitle());
        String artistText = song.getArtists() != null && !song.getArtists().isEmpty()
                ? song.getArtists().get(0).getName()
                : "Unknown Artist";
        artistName.setText(artistText);
        progressSlider.setMax(song.getDuration());
        remainingTime.setText("-" + AppUtils.formatTime(song.getDuration()));

        if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
            try {
                albumArtView.albumArt.setImage(new Image("file:" + song.getAlbum().getAlbumArtPath(), true));
                albumArtView.artPlaceholder.setVisible(false);
            } catch (Exception e) {
                albumArtView.artPlaceholder.setVisible(true);
            }
        }
    }

    private HBox createPlayerControls() {
        HBox controls = new HBox(12);
        controls.getStyleClass().add("cntr-spc-sm");

        Button prevBtn = new Button();
        prevBtn.setGraphic(IconFactory.createIcon("prev", 20));

        playPauseBtn = new Button();
        playPauseBtn.setGraphic(IconFactory.createIcon("play", 24));
        playPauseBtn.setUserData("play");

        Button nextBtn = new Button();
        nextBtn.setGraphic(IconFactory.createIcon("next", 20));

        prevBtn.getStyleClass().addAll("player-btn", "player-btn-secondary", "cursor");
        playPauseBtn.getStyleClass().addAll("player-btn", "player-btn-primary", "cursor");
        nextBtn.getStyleClass().addAll("player-btn", "player-btn-secondary", "cursor");

        prevBtn.setOnAction(e -> musicPlayerService.playPrevious());

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

        nextBtn.setOnAction(e -> musicPlayerService.playNext());

        controls.getChildren().addAll(prevBtn, playPauseBtn, nextBtn);
        return controls;
    }
}