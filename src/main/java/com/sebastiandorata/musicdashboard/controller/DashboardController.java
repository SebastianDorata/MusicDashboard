package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.Utils.Utils;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingServices;
import com.sebastiandorata.musicdashboard.service.SongService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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




    private Button myLibrary = new Button();
    private Button importFiles = new Button();
    private Button myPlaysits = new Button();
    private Button accountSettings = new Button();
    private TextField searchBar = new TextField();
    private Label accountUsername = new Label();
    private Button accountPlanStatus = new Button("Basic Plan");

    // Now Playing area
    private Label nowPlaying = new Label();
    private Label SongCount;
    private ListView<Song> songListView;

    private ListView<Artist> artistsListView;
    private ListView<Album> albumListView;
    private ListView<PlaybackHistory> playbackHistoryListView;









        //This is the public method that is called only to switch the scenes
        public void show(){
            Scene scene = createScene();
            MainController.switchViews(scene);
        }



            private Scene createScene(){
                BorderPane root = new BorderPane();
                //Add the Created VBoxs here after they are created (Getters)
                root.setCenter(centerMenu());
                root.setBottom(createPlayerControls());
                return new Scene(root, Utils.APP_WIDTH, Utils.APP_HEIGHT);
            }




        //private VBox leftMenu(){}
        // My account
        // My library
        // My playlist
        //Import files
        // My reports
        //Settings




            private VBox centerMenu() {
                VBox center = new VBox(20);
                center.setPadding(new Insets(20));
                HBox nowPlaying = createNowPlayingBar();
                VBox songList = createSongListSection();
                center.getChildren().addAll(nowPlaying, songList);
                return center;
            }



                    private HBox createNowPlayingBar() {
                        HBox nowPlaying = new HBox(20);
                        nowPlaying.setPadding(new Insets(15));
                        nowPlaying.setAlignment(Pos.CENTER_LEFT);
                        nowPlaying.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 10;");

                        ImageView albumArt = new ImageView(); //https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/image/ImageView.html
                        albumArt.setFitWidth(80);
                        albumArt.setFitHeight(80);
                        albumArt.setPreserveRatio(true);

                        Label songTitle = new Label("No song playing");
                        songTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

                                    //https://openjfx.io/javadoc/25/javafx.base/javafx/beans/property/package-summary.html
                                    //https://openjfx.io/javadoc/25/javafx.base/javafx/beans/Observable.html
                        // Listen to changes in current song
                        musicPlayerService.currentSongProperty().addListener( //currentSongProperty() is called to get the observable property
                                (observable, oldSong, newSong) -> {
                                    if (newSong != null) {
                                        songTitle.setText(newSong.getTitle());

                                        // Update album art
                                        if (newSong.getAlbum() != null) {
                                            String artPath = newSong.getAlbum().getAlbumArtPath();
                                            if (artPath != null) {
                                                Image image = new Image("file:" + artPath);
                                                albumArt.setImage(image);
                                            }
                                        }
                                    }
                                }
                        );

                        nowPlaying.getChildren().addAll(albumArt, songTitle);
                        return nowPlaying;
                    }

                    private VBox createSongListSection() {
                        VBox songSection = new VBox(15);

                        Label heading = new Label("Your Library");
                        heading.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

                        songListView = new ListView<>();
                        songListView.setPrefHeight(400);

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
                        return songSection;
                    }




                    private HBox createPlayerControls() {
                        HBox controls = new HBox(15);
                        controls.setPadding(new Insets(15));
                        controls.setAlignment(Pos.CENTER);
                        controls.setStyle("-fx-background-color: #1e1e1e;");

                        Button prevBtn = new Button("⏮");
                        Button playPauseBtn = new Button("▶");
                        Button nextBtn = new Button("⏭");

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






            //Info box 1/5
            //Playback duration today
            //Total time and average listening session.

            //Info box 2/5
            //Top artist today.


            //Info box 3/5
            //Top album today.

            //Info box 4/5
            //Top artist this week.


            //Info box 5/5
            //Top album this week.

            //Bottom HBox of all-time stats
            //Total playback hours for the current year. Jan 1st to present.
            // Change in playback hours of current year compared against previous year, at the current date.
            // Change in playback hours of current year compared against the year with the highest playback hours, at the current date.




        //private VBox rightMenu(){}




    //Data needed on the home page left to right.

    //left VBox N/A



    //Right  top VBox of top 5 artists of all time
            //left column artist name.
            // Right column total time listend to.

    //Right bottom VBox
            //List of songs played from most recent

}
//private void navigateToImportPage() {
//        importController.show();
//    }