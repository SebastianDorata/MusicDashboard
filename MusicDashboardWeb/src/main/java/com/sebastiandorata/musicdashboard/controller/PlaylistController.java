package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serves the Playlist page and the JSON endpoints used by it and by the library page.
 *
 * <p>The desktop controller kept the selected playlist and the display mode in fields. On the web
 * that state lives in the URL instead: {@code /playlist} lists the current user's playlists,
 * {@code /playlist?id=<playlistId>} shows one playlist selected and {@code &mode=list|grid}
 * carries the display-mode toggle (default {@code grid}, as the desktop reset it on every visit).</p>
 *
 * <p>Every playlist id coming from the browser is checked against the current user's playlists
 * before it is read or changed; a foreign or unknown id answers 404.</p>
 *
 * <p>SRP: request mapping, ownership checks and mapping of entities to view records; all
 * persistence is delegated to {@link PlaylistService}.</p>
 * <p>Time Complexity: O(n) per request, where n = playlists (or songs of the selected playlist) rendered.</p>
 * <p>Space Complexity: O(n).</p>
 */
@Controller
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private SongRepository songRepository;

    /**
     * One playlist as shown in the playlist browser (card or row) and in the selected header.
     *
     * @param id          the playlist id
     * @param name        the playlist name
     * @param description the optional description, may be {@code null}
     * @param songCount   how many songs the playlist holds
     */
    public record PlaylistView(Long id, String name, String description, int songCount) {
        /**
         * Returns the desktop's count label: {@code "1 song"}, {@code "3 songs"}.
         *
         * @return the count with its singular or plural noun
         */
        public String countLabel() {
            return songCount + " song" + (songCount == 1 ? "" : "s");
        }
    }

    /**
     * One song row or card inside the selected playlist.
     *
     * @param id       the song id
     * @param title    the title, or {@code "Unknown Title"} when missing
     * @param artist   the primary artist name, or {@code "Unknown Artist"}
     * @param albumId  the album id when the album has cover art, otherwise {@code null}
     * @param duration the formatted duration ({@code m:ss} or {@code --:--})
     */
    public record SongView(Long id, String title, String artist, Long albumId, String duration) {
    }

    /**
     * Body of the create and rename requests.
     *
     * @param name        the playlist name
     * @param description the optional description
     */
    public record PlaylistForm(String name, String description) {
    }

    /**
     * Summary returned by the playlist JSON endpoints.
     *
     * @param id        the playlist id
     * @param name      the playlist name
     * @param songCount how many songs the playlist holds
     */
    public record PlaylistSummary(Long id, String name, int songCount) {
    }

    /**
     * Renders the Playlist page.
     *
     * <p>Model attributes: {@code mode} ({@code "grid"} or {@code "list"}), {@code playlists}
     * (the current user's {@link PlaylistView}s), {@code selected} (the selected playlist or
     * {@code null}) and {@code songs} (its {@link SongView}s, empty when nothing is selected).</p>
     *
     * @param id    optional id of the playlist to select
     * @param mode  optional display mode, anything other than {@code list} means {@code grid}
     * @param model the Thymeleaf model
     * @return the {@code playlist} template name
     * @throws ResponseStatusException 404 when {@code id} is not one of the current user's playlists
     */
    @GetMapping("/playlist")
    public String playlist(@RequestParam(required = false) Long id,
                           @RequestParam(required = false) String mode,
                           Model model) {
        List<Playlist> owned = playlistService.getCurrentUserPlaylists();
        List<PlaylistView> views = owned.stream().map(PlaylistController::toView).toList();

        PlaylistView selected = null;
        List<SongView> songs = List.of();
        if (id != null) {
            Playlist playlist = owned.stream()
                    .filter(p -> id.equals(p.getPlaylistId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
            selected = toView(playlist);
            songs = playlistService.getPlaylistSongs(id).stream().map(PlaylistController::toSongView).toList();
        }

        model.addAttribute("mode", "list".equals(mode) ? "list" : "grid");
        model.addAttribute("playlists", views);
        model.addAttribute("selected", selected);
        model.addAttribute("songs", songs);
        return "playlist";
    }

    /**
     * Lists the current user's playlists.
     *
     * @return one {@link PlaylistSummary} per playlist
     */
    @GetMapping("/api/playlists")
    @ResponseBody
    public List<PlaylistSummary> list() {
        return playlistService.getCurrentUserPlaylists().stream()
                .map(PlaylistController::toSummary)
                .toList();
    }

    /**
     * Creates a playlist for the current user.
     *
     * @param form the name and optional description
     * @return the created playlist's summary
     * @throws ResponseStatusException 400 with the desktop message when the name is empty
     */
    @PostMapping("/api/playlists")
    @ResponseBody
    public PlaylistSummary create(@RequestBody PlaylistForm form) {
        requireName(form);
        try {
            return toSummary(playlistService.createPlaylist(form.name(), form.description()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Renames a playlist and updates its description.
     *
     * @param playlistId the playlist id
     * @param form       the new name and description
     * @return the updated playlist's summary
     * @throws ResponseStatusException 404 when the playlist is not the user's, 400 when the name is empty
     */
    @PutMapping("/api/playlists/{playlistId}")
    @ResponseBody
    public PlaylistSummary update(@PathVariable Long playlistId, @RequestBody PlaylistForm form) {
        Playlist playlist = ownedPlaylist(playlistId);
        requireName(form);
        return toSummary(playlistService.updatePlaylist(
                playlist.getPlaylistId(), form.name(), form.description() == null ? "" : form.description(), null));
    }

    /**
     * Deletes a playlist; its songs stay in the library.
     *
     * @param playlistId the playlist id
     * @throws ResponseStatusException 404 when the playlist is not the user's
     */
    @DeleteMapping("/api/playlists/{playlistId}")
    @ResponseBody
    public void delete(@PathVariable Long playlistId) {
        playlistService.deletePlaylist(ownedPlaylist(playlistId).getPlaylistId());
    }

    /**
     * Adds a song to a playlist.
     *
     * @param playlistId the playlist id
     * @param songId     the song id
     * @return {@code {"added": true}}, or {@code {"added": false}} when the song was already in it
     * @throws ResponseStatusException 404 when the playlist is not the user's or the song is unknown
     */
    @PostMapping("/api/playlists/{playlistId}/songs/{songId}")
    @ResponseBody
    public Map<String, Boolean> addSong(@PathVariable Long playlistId, @PathVariable Long songId) {
        Playlist playlist = ownedPlaylist(playlistId);
        Song song = song(songId);
        try {
            playlistService.addSongToPlaylist(playlist.getPlaylistId(), song);
            return Map.of("added", true);
        } catch (IllegalArgumentException e) {
            return Map.of("added", false);
        }
    }

    /**
     * Removes a song from a playlist.
     *
     * @param playlistId the playlist id
     * @param songId     the song id
     * @throws ResponseStatusException 404 when the playlist is not the user's or the song is unknown,
     *                                 400 when the song is not in the playlist
     */
    @DeleteMapping("/api/playlists/{playlistId}/songs/{songId}")
    @ResponseBody
    public void removeSong(@PathVariable Long playlistId, @PathVariable Long songId) {
        Playlist playlist = ownedPlaylist(playlistId);
        Song song = song(songId);
        try {
            playlistService.removeSongFromPlaylist(playlist.getPlaylistId(), song);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Finds a playlist among the current user's own playlists.
     *
     * @param playlistId the id sent by the browser
     * @return the owned playlist
     * @throws ResponseStatusException 404 when it does not exist or belongs to someone else
     */
    private Playlist ownedPlaylist(Long playlistId) {
        Optional<Playlist> found = playlistService.getCurrentUserPlaylists().stream()
                .filter(p -> p.getPlaylistId().equals(playlistId))
                .findFirst();
        return found.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
    }

    /**
     * Loads a song in the current request.
     *
     * @param songId the song id
     * @return the song
     * @throws ResponseStatusException 404 when no such song exists
     */
    private Song song(Long songId) {
        return songRepository.findById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found"));
    }

    /**
     * Rejects a missing or blank playlist name with the desktop's validation message.
     *
     * @param form the request body
     * @throws ResponseStatusException 400 when the name is blank
     */
    private static void requireName(PlaylistForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Playlist name cannot be empty");
        }
    }

    /**
     * Maps a playlist entity to its page view record.
     *
     * @param p the playlist
     * @return the view record
     */
    private static PlaylistView toView(Playlist p) {
        int n = p.getSongs() != null ? p.getSongs().size() : 0;
        return new PlaylistView(p.getPlaylistId(), p.getName(), p.getDescription(), n);
    }

    /**
     * Maps a playlist entity to its JSON summary.
     *
     * @param p the playlist
     * @return the summary record
     */
    private static PlaylistSummary toSummary(Playlist p) {
        return new PlaylistSummary(p.getPlaylistId(), p.getName(), p.getSongs() != null ? p.getSongs().size() : 0);
    }

    /**
     * Maps a song entity to its page view record.
     *
     * @param s the song
     * @return the view record
     */
    private static SongView toSongView(Song s) {
        Long albumId = (s.getAlbum() != null && s.getAlbum().getAlbumArtPath() != null)
                ? s.getAlbum().getAlbumId() : null;
        return new SongView(s.getSongID(),
                s.getTitle() != null ? s.getTitle() : "Unknown Title",
                ArtistUtils.getPrimaryArtistName(s),
                albumId,
                AppUtils.formatDuration(s.getDuration()));
    }
}
