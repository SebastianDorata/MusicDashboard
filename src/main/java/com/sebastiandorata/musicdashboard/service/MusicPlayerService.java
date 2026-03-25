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

/**
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n) where n = queue size
 */
@Service
public class MusicPlayerService {

    @Autowired
    PlaybackTrackingService playbackTrackingService;

    @Getter
    private MediaPlayer mediaPlayer;

    @Getter
    private Song currentSong;


    // Controllers listen to this to update UI when song changes
    private final ObjectProperty<Song> currentSongProperty = new SimpleObjectProperty<>();


    private final ReadOnlyObjectWrapper<Duration> currentTime = new ReadOnlyObjectWrapper<>(Duration.ZERO);


    private final ObjectProperty<String> playbackEventProperty = new SimpleObjectProperty<>();


    private List<Song> queue = new ArrayList<>();
    private int queuePos = -1;

    private Timeline playbackTrackingTimer;
    private boolean hasBeenTracked = false;
    private boolean isNaturalSongEnd = false;
    private boolean songEnded = false;

    //TODO: When user-defined queues are implemented, add toggle for shuffle on/off


    public void setQueue(List<Song> songs) {
        queue = new ArrayList<>(songs);
        Collections.shuffle(queue);
        queuePos = -1;
    }


    public void playSong(Song song) {
        try {
            String filePath = song.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                System.err.println("Error: Song has no file path");
                return;
            }

            File audioFile = new File(song.getFilePath());
            if (!audioFile.exists()) {
                System.err.println("Error: File not found: " + song.getFilePath());
                return;
            }

            // Cancel previous song's tracking timer to prevent orphaned timers
            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.stop();
            }


            hasBeenTracked = false;
            isNaturalSongEnd = false;
            songEnded = false;

            // Resource management: MediaPlayer holds onto system audio resources, must be explicitly released
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            // Sync queue position to current song
            int idx = queue.indexOf(song);
            if (idx >= 0) queuePos = idx;

            // Update current song and notify all listeners
            this.currentSong = song;
            currentSongProperty.set(song);

            // Create MediaPlayer for this song
            Media media = new Media(audioFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // Wire up current playback time listener for progress bar updates
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) ->
                    Platform.runLater(() -> currentTime.set(newTime)));

            // Set up end-of-media handler. Stop the tracking timer BEFORE calling playNext(). Prevents the timer from firing after the song ends naturally
            mediaPlayer.setOnEndOfMedia(() -> {
                // Stop the timer immediately to prevent double-recording
                if (playbackTrackingTimer != null) {
                    playbackTrackingTimer.stop();
                }
                songEnded = true;
                isNaturalSongEnd = true;
                playNext();
            });


            mediaPlayer.setOnError(() ->
                    System.err.println("MediaPlayer error: " + mediaPlayer.getError().getMessage()));


            startPlaybackTrackingTimer(song);


            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Error playing song: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void startPlaybackTrackingTimer(Song song) {
        playbackTrackingTimer = new Timeline(new KeyFrame(
                Duration.seconds(20),
                event -> {

                    if (!hasBeenTracked && !songEnded) {
                        playbackTrackingService.recordPlay(song);
                        hasBeenTracked = true;
                    }
                }
        ));
        playbackTrackingTimer.setCycleCount(1);
        playbackTrackingTimer.play();
    }


    public void playNext() {
        if (queue.isEmpty()) return;

        if (!isNaturalSongEnd && currentSong != null && mediaPlayer != null) {
            int secondsPlayed = (int) mediaPlayer.getCurrentTime().toSeconds();
            if (secondsPlayed >= 20) {

                playbackTrackingService.recordPlayWithDuration(currentSong, secondsPlayed);
                hasBeenTracked = true;
            }
        }

        isNaturalSongEnd = false;


        queuePos = (queuePos + 1) % queue.size();
        playSong(queue.get(queuePos));


        playbackEventProperty.set("next");
    }

    public void playPrevious() {
        if (queue.isEmpty()) return;


        if (!isNaturalSongEnd && currentSong != null && mediaPlayer != null) {
            int secondsPlayed = (int) mediaPlayer.getCurrentTime().toSeconds();
            if (secondsPlayed >= 20) {

                playbackTrackingService.recordPlayWithDuration(currentSong, secondsPlayed);
                hasBeenTracked = true;
            }
        }


        isNaturalSongEnd = false;


        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 3.0) {
            mediaPlayer.seek(Duration.ZERO);
            return;
        }


        queuePos = (queuePos - 1 + queue.size()) % queue.size();
        playSong(queue.get(queuePos));


        playbackEventProperty.set("previous");
    }


    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();


            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.pause();
            }
        }
    }


    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();


            if (playbackTrackingTimer != null) {
                playbackTrackingTimer.play();
            }
        }
    }


    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.millis(seconds * 1000.0));
        }
    }


    public ObjectProperty<Song> currentSongProperty() {
        return currentSongProperty;
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime.getReadOnlyProperty();
    }

    public ObjectProperty<String> playbackEventProperty() {
        return playbackEventProperty;
    }

}