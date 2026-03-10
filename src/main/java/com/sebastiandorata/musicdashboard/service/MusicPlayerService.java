package com.sebastiandorata.musicdashboard.service;

import ch.qos.logback.classic.LoggerContext;
import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.io.File;
// Only one media player exists and each controller will use the same service provided here.
@Service
public class MusicPlayerService {

    @Getter
    private MediaPlayer mediaPlayer;//https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/package-summary.html
    @Getter
    private Song currentSong;

    //Using the Observer pattern to notify all the controllers that use it
    private final ObjectProperty<Song> currentSongProperty = new SimpleObjectProperty<>();

    // Observable property for current playback time
    private final ReadOnlyObjectWrapper<Duration> currentTime = new ReadOnlyObjectWrapper<>(Duration.ZERO);


    public void playSong(Song song) {
        try {
                String filePath = song.getFilePath();// Gets the file path from song entity
                    if (filePath == null || filePath.isEmpty()) {
                        System.err.println("Error: Song has no file path");
                        return;
                    }
                    this.currentSong = song;
                    currentSongProperty.set(song);  // Notify all controllers who are listeners

                File audioFile = new File(song.getFilePath()); //Creates the file object
                    if (!audioFile.exists()) {
                        System.err.println("Error: File not found: " + song.getFilePath());
                        return;
                    }

        //https://openjfx.io/javadoc/25/javafx.media/javafx/scene/media/Media.html#%3Cinit%3E(java.lang.String)
        Media media = new Media(audioFile.toURI().toString()); //This converts the file to a URI

        //Resource Management Pattern
            //Stop using and release the resource.
        if (mediaPlayer != null) { //object pooling
            mediaPlayer.stop();
            mediaPlayer.dispose(); //This prevents a memory leak
        }
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnError(() -> {
            System.err.println("MediaPlayer error: " + mediaPlayer.getError().getMessage());
        });

        mediaPlayer.play();

    } catch (Exception e) {
        System.err.println("Error playing song: " + e.getMessage());
        e.printStackTrace();
    }
}

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }
    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public void seek(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(seconds));
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