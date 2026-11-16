package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.Import.SongImportService;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * JSON API through which the browser player talks to the per-session {@link MusicPlayerService}.
 *
 * <p>The browser plays audio itself; these endpoints keep the server-side queue, next/previous
 * decisions and play tracking in step. Every state-returning endpoint answers with a
 * {@link PlayerState}. A song whose file is missing yields a 404 whose reason is the desktop error
 * text so the UI can show it as a toast.</p>
 *
 * <p>SRP: translates HTTP requests to service calls and entities to JSON-safe records.</p>
 * <p>DIP: depends on the session-scoped service through its injected proxy.</p>
 *
 * <p>Time Complexity: O(1) per request except queue replacement, O(n).</p>
 * <p>Space Complexity: O(n) for a queue of n ids.</p>
 */
@RestController
@RequestMapping("/api/player")
public class PlayerController {

    @Autowired
    private MusicPlayerService musicPlayerService;

    @Autowired
    private SongImportService songImportService;

    /**
     * Request body of {@code POST /api/player/queue}.
     *
     * @param songIds ids of the songs to queue
     */
    public record QueueRequest(List<Long> songIds) {
    }

    /**
     * Request body of {@code POST /api/player/play}.
     *
     * @param songId   id of the song to play
     * @param position seconds the previous song had reached; may be {@code null}
     */
    public record PlayRequest(Long songId, Double position) {
    }

    /**
     * Request body of next and previous.
     *
     * @param position seconds the current song had reached; may be {@code null}
     */
    public record PositionRequest(Double position) {
    }

    /**
     * Request body of {@code POST /api/player/ended}.
     *
     * @param duration total length in seconds of the song that finished; may be {@code null}
     */
    public record EndedRequest(Double duration) {
    }

    /**
     * JSON view of a song for the player UI.
     *
     * @param id         song id
     * @param title      song title
     * @param artistName primary artist name or "Unknown Artist"
     * @param artistId   primary artist id, or {@code null}
     * @param albumId    album id, or {@code null}
     * @param albumTitle album title, or {@code null}
     * @param duration   length in seconds, or {@code null}
     * @param artUrl     cover image URL, or {@code null} when the album has no art
     * @param streamUrl  audio URL
     */
    public record SongView(Long id, String title, String artistName, Long artistId, Long albumId,
                           String albumTitle, Integer duration, String artUrl, String streamUrl) {
    }

    /**
     * Player state returned by the endpoints.
     *
     * @param song      current song or {@code null}
     * @param playing   whether playback is running
     * @param queueSize number of songs in the queue
     * @param restart   {@code true} when the browser should seek to 0 instead of loading a song
     */
    public record PlayerState(SongView song, boolean playing, int queueSize, boolean restart) {
    }

    /**
     * Returns the current player state of this session.
     *
     * @return the state; song is {@code null} when nothing is loaded
     */
    @GetMapping("/state")
    public PlayerState state() {
        return state(musicPlayerService.getCurrentSong(), false);
    }

    /**
     * Replaces the queue with a shuffled copy of the given songs (desktop {@code setQueue}).
     *
     * @param request the song ids
     * @return the state after the change
     */
    @PostMapping("/queue")
    public PlayerState queue(@RequestBody QueueRequest request) {
        musicPlayerService.setQueue(request.songIds());
        return state();
    }

    /**
     * Replaces the queue with a shuffled copy of the whole library.
     *
     * <p>The desktop Recently Played panel queued every song in the library before
     * playing the row that was double-clicked, so playback carries on through the
     * library afterwards. Building the queue on the server saves the browser from
     * having to know every song id.</p>
     *
     * <p>Time Complexity: O(n) for a library of n songs.</p>
     *
     * @return the state after the change
     */
    @PostMapping("/queue/library")
    public PlayerState queueLibrary() {
        List<Long> ids = songImportService.getAllSongs().stream().map(Song::getSongID).toList();
        musicPlayerService.setQueue(ids);
        return state();
    }

    /**
     * Plays a song (desktop {@code playSong}).
     *
     * @param request the song id and the position of the song being left
     * @return the state with the new song
     * @throws ResponseStatusException 404 when the song has no playable file
     */
    @PostMapping("/play")
    public PlayerState play(@RequestBody PlayRequest request) {
        if (request.songId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "songId is required");
        }
        Song song = playable(() -> musicPlayerService.playSong(request.songId(), request.position()));
        return state(song, false);
    }

    /**
     * Skips to the next queued song.
     *
     * @param request the position of the song being left; may be {@code null}
     * @return the state with the new song
     * @throws ResponseStatusException 404 when the next song has no playable file
     */
    @PostMapping("/next")
    public PlayerState next(@RequestBody(required = false) PositionRequest request) {
        Double position = request == null ? null : request.position();
        Song song = playable(() -> musicPlayerService.playNext(position));
        return state(song != null ? song : musicPlayerService.getCurrentSong(), false);
    }

    /**
     * Goes to the previous song, or reports that the current one should restart.
     *
     * @param request the position of the current song; may be {@code null}
     * @return the state; {@code restart} is {@code true} when the browser should seek to 0
     * @throws ResponseStatusException 404 when the previous song has no playable file
     */
    @PostMapping("/previous")
    public PlayerState previous(@RequestBody(required = false) PositionRequest request) {
        Double position = request == null ? null : request.position();
        MusicPlayerService.PreviousResult result = playable(() -> musicPlayerService.playPrevious(position));
        Song song = result.song() != null ? result.song() : musicPlayerService.getCurrentSong();
        return state(song, result.restart());
    }

    /**
     * Records that the current song has been played for 20 seconds.
     *
     * @return 204 No Content
     */
    @PostMapping("/tracked")
    public ResponseEntity<Void> tracked() {
        musicPlayerService.markTracked();
        return ResponseEntity.noContent().build();
    }

    /**
     * Handles the natural end of a song and returns the next one.
     *
     * @param request the finished song's duration
     * @return the state of the next song; song is {@code null} if the queue is empty
     * @throws ResponseStatusException 404 when the next song has no playable file
     */
    @PostMapping("/ended")
    public PlayerState ended(@RequestBody(required = false) EndedRequest request) {
        int duration = request != null && request.duration() != null ? (int) request.duration().doubleValue() : 0;
        Song song = playable(() -> musicPlayerService.songEnded(duration));
        return state(song, false);
    }

    /**
     * Remembers that playback is paused.
     *
     * @return 204 No Content
     */
    @PostMapping("/pause")
    public ResponseEntity<Void> pause() {
        musicPlayerService.pause();
        return ResponseEntity.noContent().build();
    }

    /**
     * Remembers that playback was resumed.
     *
     * @return 204 No Content
     */
    @PostMapping("/resume")
    public ResponseEntity<Void> resume() {
        musicPlayerService.play();
        return ResponseEntity.noContent().build();
    }

    /**
     * Runs a service call and converts the desktop playback errors to a 404.
     *
     * @param call the service call
     * @param <T>  its result type
     * @return the call's result
     * @throws ResponseStatusException 404 carrying the desktop error text
     */
    private <T> T playable(Supplier<T> call) {
        try {
            return call.get();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Builds the state record for a song and the session's flags.
     *
     * @param song    current song or {@code null}
     * @param restart restart flag
     * @return the state
     */
    private PlayerState state(Song song, boolean restart) {
        SongView view = song == null ? null : toView(song);
        return new PlayerState(view, view != null && musicPlayerService.isPlaying(),
                musicPlayerService.getQueueSize(), restart);
    }

    /**
     * Maps a song entity to its JSON view.
     *
     * @param song the song, artists and album loaded
     * @return the view
     */
    private SongView toView(Song song) {
        Artist artist = song.getArtists() == null ? null : song.getArtists().stream().findFirst().orElse(null);
        Album album = song.getAlbum();
        String art = album != null && album.getAlbumArtPath() != null && !album.getAlbumArtPath().isEmpty()
                ? "/media/art/album/" + album.getAlbumId() : null;
        return new SongView(song.getSongID(), song.getTitle(),
                ArtistUtils.getPrimaryArtistName(song),
                artist != null ? artist.getArtistId() : null,
                album != null ? album.getAlbumId() : null,
                album != null ? album.getTitle() : null,
                song.getDuration(), art, "/media/song/" + song.getSongID());
    }
}
