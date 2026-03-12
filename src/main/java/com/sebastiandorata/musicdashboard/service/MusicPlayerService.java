package com.sebastiandorata.musicdashboard.service;


import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Service
public class MusicPlayerService {
    @Autowired
    PlaybackTrackingService playbackTrackingService;


    @Getter
    private MediaPlayer mediaPlayer;//https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/package-summary.html
    @Getter
    private Song currentSong;

    //Using the Observer pattern to notify all the controllers that use it
    private final ObjectProperty<Song> currentSongProperty = new SimpleObjectProperty<>();

    // Observable property for current playback time
    private final ReadOnlyObjectWrapper<Duration> currentTime = new ReadOnlyObjectWrapper<>(Duration.ZERO);

    private List<Song> queue = new ArrayList<>();
    private int queuePos = -1;


    private Timeline playbackTrackingTimer;
    private boolean hasBeenTracked = false;  // Prevent double-tracking


      //Sets the playback queue and shuffles it. Call this before playSong() when the user starts playing from a new context.

      //TODO: When user-defined queues are implemented, controllers will call setQueue(songs) with the relevant list before calling playSong(). Shuffle should become a toggle (on/off) rather than always-on.

    public void setQueue(List<Song> songs) {
        queue = new ArrayList<>(songs);
        Collections.shuffle(queue);
        queuePos = -1;
    }

    public void playSong(Song song) {
        try {
            String filePath = song.getFilePath();// Gets the file path from song entity
            if (filePath == null || filePath.isEmpty()) {
                System.err.println("Error: Song has no file path");
                return;
            }
            File audioFile = new File(song.getFilePath()); //Creates the file object
            if (!audioFile.exists()) {
                System.err.println("Error: File not found: " + song.getFilePath());
                return;
            }

            // Cancel previous tracking timer if exists
            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.stop();
            }

            // Reset tracking flag for new song
            hasBeenTracked = false;

            //Resource Management Pattern
            //Stop using and release the resource.
            if (mediaPlayer != null) { //object pooling
                mediaPlayer.stop();
                mediaPlayer.dispose(); //This prevents a memory leak
            }
            // Sync queue position to the song being played
            int idx = queue.indexOf(song);
            if (idx >= 0) queuePos = idx;



            this.currentSong = song;
            currentSongProperty.set(song);  // Notify all controllers who are listeners



            //https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/Media.html#%3Cinit%3E(java.lang.String)
            Media media = new Media(audioFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            // Wire up currentTime.  Platform.runLater ensures JavaFX thread safety
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) ->
                    Platform.runLater(() -> currentTime.set(newTime)));
            // Auto advance when song finishes
            mediaPlayer.setOnEndOfMedia(this::playNext);
            mediaPlayer.setOnError(() ->
                    System.err.println("MediaPlayer error: " + mediaPlayer.getError().getMessage()));

            // Start the 20-second timer before playing
            startPlaybackTrackingTimer(song);

            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Error playing song: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void startPlaybackTrackingTimer(Song song) {
        playbackTrackingTimer = new Timeline(new KeyFrame(
                Duration.seconds(20),  // ← Wait 20 seconds
                event -> {
                    if (!hasBeenTracked) {
                        //System.out.println(" 20 seconds elapsed - tracking play for: " + song.getTitle());
                        playbackTrackingService.recordPlay(song);
                        hasBeenTracked = true;
                    }
                }
        ));
        playbackTrackingTimer.setCycleCount(1);  // Only run once
        playbackTrackingTimer.play();
    }

    public void playNext() {
        if (queue.isEmpty()) return;
        queuePos = (queuePos + 1) % queue.size();
        playSong(queue.get(queuePos));
    }
    public void playPrevious() {
        if (queue.isEmpty()) return;

        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 3.0) {
            mediaPlayer.seek(Duration.ZERO);
            return;
        }

        queuePos = (queuePos - 1 + queue.size()) % queue.size();
        playSong(queue.get(queuePos));
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();

            // Pause the tracking timer when user pauses playback
            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.pause();
            }
        }
    }

    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();

            // Resume the tracking timer when user resumes playback
            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.play();
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();

            // Cancel tracking if stopped before 20 seconds
            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.stop();
            }
        }
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            // Use millis-based duration for finer timescale on macOS AVFoundation
            // Avoids CMTimeMakeWithSeconds timescale=1 rounding warning
            mediaPlayer.seek(Duration.millis(seconds * 1000.0));
        }
    }



    public ObjectProperty<Song> currentSongProperty() {
        return currentSongProperty;
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }


    public String formatTime(double seconds) {
        int totalSeconds = (int) seconds;
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

}