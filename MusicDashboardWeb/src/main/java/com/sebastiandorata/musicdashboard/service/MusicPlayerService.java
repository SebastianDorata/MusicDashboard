package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Server-side playback state for one logged-in browser session: queue, current song and tracking flags.
 *
 * <p>The browser plays the audio with an HTML5 {@code Audio} element; this service keeps the
 * decisions the desktop {@code MusicPlayerService} made around the {@code MediaPlayer}: the shuffled
 * queue, wrap-around next/previous, "previous restarts the song after 3 seconds", and the rules
 * that decide when a play is written to the listening history. Wherever the desktop read the
 * media player's current time or total duration, the value is now a method parameter reported by
 * the browser.</p>
 *
 * <p>The bean is {@link SessionScope session scoped}: every HTTP session gets its own instance and
 * singleton controllers receive a scoped proxy. The queue holds song ids only, never JPA
 * entities, because entities would be detached and stale between requests. Public methods are
 * {@code synchronized} because a browser can fire overlapping requests.</p>
 *
 * <p>SRP: owns queue navigation and tracking rules; audio bytes are served by the media controller.</p>
 * <p>DIP: depends on {@link SongRepository} and {@link PlaybackTrackingService} abstractions.</p>
 *
 * <p>Time Complexity: O(1) for all operations except {@link #setQueue(List)} and
 * {@link #playSong(Long, Double)} (O(n) queue scan).</p>
 * <p>Space Complexity: O(n) where n = queue size.</p>
 */
@Service
@SessionScope
public class MusicPlayerService {

    /** Seconds of playback after which a play counts as a valid listen. */
    private static final int TRACKING_SECONDS = 20;

    /** Seconds into a song after which "previous" restarts the song instead of going back. */
    private static final double RESTART_THRESHOLD_SECONDS = 3.0;

    @Autowired
    private PlaybackTrackingService playbackTrackingService;

    @Autowired
    private SongRepository songRepository;

    private List<Long> queue = new ArrayList<>();
    private int queuePos = -1;
    private Long currentSongId;
    private boolean playing = false;

    private boolean hasBeenTracked = false;
    private boolean isNaturalSongEnd = false;
    private boolean songEnded = false;

    /**
     * Replaces the queue with a shuffled copy of the given song ids and resets the position.
     *
     * <p>Time Complexity: O(n). Space Complexity: O(n).</p>
     *
     * @param songIds ids of the songs to queue
     */
    public synchronized void setQueue(List<Long> songIds) {
        queue = new ArrayList<>(songIds == null ? List.of() : songIds);
        Collections.shuffle(queue);
        queuePos = -1;
    }

    /**
     * Starts the given song: validates its file, applies the skip-tracking rule to the song being
     * left, syncs the queue position and resets the tracking flags.
     *
     * @param songId   id of the song to play
     * @param position seconds the previously playing song had reached, or {@code null} if unknown
     * @return the song now current, with artists and album loaded
     * @throws NoSuchElementException if the song does not exist, has no file path or its file is missing;
     *                                the message is the desktop error text
     */
    public synchronized Song playSong(Long songId, Double position) {
        Song song = songRepository.findByIdWithDetails(songId)
                .orElseThrow(() -> new NoSuchElementException("Error: Song not found"));
        validatePlayable(song);

        // Leaving a different song: complete its history row if the rule says so.
        if (currentSongId != null && !currentSongId.equals(songId)) {
            recordPlayIfNeeded(position);
        }
        startSong(song);
        return song;
    }

    /**
     * Advances to the next queued song, wrapping around at the end.
     *
     * <p>Does nothing when the queue is empty. The previous song is first passed through the
     * "record on skip if at least 20 seconds" rule.</p>
     *
     * @param position seconds the current song had reached, or {@code null}
     * @return the new current song, or {@code null} when the queue is empty
     * @throws NoSuchElementException if the next song cannot be played (see {@link #playSong(Long, Double)})
     */
    public synchronized Song playNext(Double position) {
        if (queue.isEmpty()) return null;

        recordPlayIfNeeded(position);

        isNaturalSongEnd = false;
        queuePos = (queuePos + 1) % queue.size();
        return playQueued();
    }

    /**
     * Goes to the previous queued song, or asks the browser to restart the current one.
     *
     * <p>If the current song is more than 3 seconds in, nothing changes server side and
     * {@link PreviousResult#restart()} is {@code true}; the browser seeks to 0. Otherwise the queue
     * moves back one position (wrapping) and that song starts.</p>
     *
     * @param position seconds the current song had reached, or {@code null}
     * @return the outcome; the song is {@code null} when the queue is empty
     * @throws NoSuchElementException if the previous song cannot be played
     */
    public synchronized PreviousResult playPrevious(Double position) {
        if (queue.isEmpty()) return new PreviousResult(null, false);

        // Complete the existing row rather than creating a new one.
        if (!isNaturalSongEnd && !hasBeenTracked && currentSongId != null
                && position != null && (int) position.doubleValue() >= TRACKING_SECONDS) {
            playbackTrackingService.updateCurrentPlayDuration((int) position.doubleValue(), false);
            hasBeenTracked = true;
        }

        isNaturalSongEnd = false;

        if (currentSongId != null && position != null && position > RESTART_THRESHOLD_SECONDS) {
            return new PreviousResult(getCurrentSong(), true);
        }

        queuePos = (queuePos - 1 + queue.size()) % queue.size();
        return new PreviousResult(playQueued(), false);
    }

    /**
     * Records that the browser has played the current song for 20 seconds (the desktop 20 second timer).
     *
     * <p>Creates the history row once per song; later calls for the same song do nothing.</p>
     */
    public synchronized void markTracked() {
        if (hasBeenTracked || songEnded || currentSongId == null) return;
        Song song = songRepository.findByIdWithDetails(currentSongId).orElse(null);
        if (song == null) return;
        playbackTrackingService.recordPlay(song);
        hasBeenTracked = true;
    }

    /**
     * Handles the natural end of the current song and moves on to the next one.
     *
     * <p>Completes the history row with the full duration and marks it completed, then behaves like
     * {@link #playNext(Double)}. With an empty queue playback stops.</p>
     *
     * @param durationSeconds total length of the finished song as reported by the browser
     * @return the next song, or {@code null} if the queue is empty
     * @throws NoSuchElementException if the next song cannot be played
     */
    public synchronized Song songEnded(int durationSeconds) {
        if (currentSongId != null) {
            playbackTrackingService.updateCurrentPlayDuration(durationSeconds, true);
            hasBeenTracked = true;
        }
        songEnded = true;
        isNaturalSongEnd = true;
        Song next = playNext(null);
        if (next == null) playing = false;
        return next;
    }

    /** Remembers that playback is paused so the next page load does not auto-resume. */
    public synchronized void pause() {
        playing = false;
    }

    /** Remembers that playback is running so the next page load resumes it. */
    public synchronized void play() {
        if (currentSongId != null) playing = true;
    }

    /**
     * Returns the current song freshly loaded from the database.
     *
     * @return the current song, or {@code null} if none is set or it no longer exists
     */
    public synchronized Song getCurrentSong() {
        if (currentSongId == null) return null;
        return songRepository.findByIdWithDetails(currentSongId).orElse(null);
    }

    /**
     * Tells whether playback is currently running.
     *
     * @return {@code true} while a song is playing
     */
    public synchronized boolean isPlaying() {
        return playing;
    }

    /**
     * Returns the number of songs in the queue.
     *
     * @return queue size
     */
    public synchronized int getQueueSize() {
        return queue.size();
    }

    /**
     * Result of {@link #playPrevious(Double)}.
     *
     * @param song    the song that is current afterwards, or {@code null} when the queue is empty
     * @param restart {@code true} when the browser should seek to 0 instead of loading another song
     */
    public record PreviousResult(Song song, boolean restart) {
    }

    /**
     * Plays the song at the current queue position.
     *
     * @return the started song
     */
    private Song playQueued() {
        Long id = queue.get(queuePos);
        Song song = songRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NoSuchElementException("Error: Song not found"));
        validatePlayable(song);
        startSong(song);
        return song;
    }

    /**
     * Checks the song has a file path and the file exists.
     *
     * @param song song to check
     * @throws NoSuchElementException with the desktop error text when it cannot be played
     */
    private void validatePlayable(Song song) {
        String filePath = song.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new NoSuchElementException("Error: Song has no file path");
        }
        if (!new File(filePath).exists()) {
            throw new NoSuchElementException("Error: File not found: " + filePath);
        }
    }

    /**
     * Makes the song current, syncs the queue position and resets the tracking flags.
     *
     * @param song the validated song
     */
    private void startSong(Song song) {
        hasBeenTracked = false;
        isNaturalSongEnd = false;
        songEnded = false;

        int idx = queue.indexOf(song.getSongID());
        if (idx >= 0) queuePos = idx;

        currentSongId = song.getSongID();
        playing = true;
    }

    /**
     * Completes the history row of the song being left when it was not yet tracked and had played 20+ seconds.
     *
     * <p>If the 20 second mark already created the row, the duration is not updated (desktop behaviour).</p>
     *
     * @param position seconds reached by the song being left, or {@code null}
     */
    private void recordPlayIfNeeded(Double position) {
        if (!hasBeenTracked && currentSongId != null && !songEnded && position != null) {
            int secondsPlayed = (int) position.doubleValue();
            if (secondsPlayed >= TRACKING_SECONDS) {
                playbackTrackingService.updateCurrentPlayDuration(secondsPlayed, false);
                hasBeenTracked = true;
            }
        }
    }
}
