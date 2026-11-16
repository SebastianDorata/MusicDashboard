package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.PlaylistRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Time Complexity:
 * Create/delete: O(1)  single database operation
 * Add/remove song: O(1) list manipulation in memory
 * Get playlists: O(n)  where n = user's playlists
 * Space Complexity: O(n)  where n = playlist size (songs in playlist)
 */
@Service
public class PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a private playlist owned by the current user.
     *
     * @param name        the playlist name; must not be blank (surrounding whitespace is trimmed)
     * @param description an optional description, trimmed; may be null
     * @return the saved playlist
     * @throws IllegalArgumentException if {@code name} is null or blank
     * @throws IllegalStateException    if no user is logged in or the user cannot be found
     */
    public Playlist createPlaylist(String name, String description) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot create playlist.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Playlist name cannot be empty");
        }
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id: " + userId));

        Playlist playlist = new Playlist();
        playlist.setUser(currentUser);
        playlist.setName(name.trim());
        playlist.setDescription(description != null ? description.trim() : null);
        playlist.setCreatedAt(LocalDateTime.now());
        playlist.setIsPublic(false);
        return playlistRepository.save(playlist);
    }

    /**
     * Returns every playlist owned by the current user.
     *
     * <p>Time Complexity: O(n) where n = number of the user's playlists.</p>
     *
     * @return the user's playlists; empty if there are none
     * @throws IllegalStateException if no user is logged in
     */
    public List<Playlist> getCurrentUserPlaylists() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot retrieve playlists.");
        }
        return playlistRepository.findByUserId(userId);
    }

    /**
     * Finds a playlist by ID, provided the current user owns it.
     *
     * @param playlistId the ID of the playlist
     * @return the playlist, or an empty Optional if no playlist has that ID
     * @throws IllegalStateException if no user is logged in or the playlist belongs to another user
     */
    public Optional<Playlist> getPlaylistById(Long playlistId) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot retrieve playlist.");
        }
        Optional<Playlist> playlist = playlistRepository.findById(playlistId);
        if (playlist.isPresent() && !playlist.get().getUser().getId().equals(userId)) {
            throw new IllegalStateException("User does not own this playlist");
        }
        return playlist;
    }

    /**
     * Updates the fields of one of the current user's playlists.
     *
     * <p>A {@code null} argument leaves that field unchanged. A blank name is
     * ignored. A blank description clears the description.</p>
     *
     * @param playlistId the ID of the playlist to update
     * @param name       the new name, or null/blank to keep the current name
     * @param description the new description, or null to keep it; blank clears it
     * @param isPublic   the new visibility, or null to keep it
     * @return the saved playlist
     * @throws IllegalStateException if the playlist is not found or not owned by the current user
     */
    public Playlist updatePlaylist(Long playlistId, String name,
                                   String description, Boolean isPublic) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));
        if (name != null && !name.isBlank()) {
            playlist.setName(name.trim());
        }
        if (description != null) {
            playlist.setDescription(description.isBlank() ? null : description.trim());
        }
        if (isPublic != null) {
            playlist.setIsPublic(isPublic);
        }
        return playlistRepository.save(playlist);
    }

    /**
     * Adds a song to one of the current user's playlists.
     *
     * @param playlistId the ID of the playlist
     * @param song       the song to add
     * @throws IllegalArgumentException if {@code song} is null or already in the playlist
     * @throws IllegalStateException    if the playlist is not found or not owned by the current user
     */
    public void addSongToPlaylist(Long playlistId, Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        if (playlist.getSongs() != null && playlist.getSongs().contains(song)) {
            throw new IllegalArgumentException("Song already in playlist");
        }

        // Initialize as HashSet if null
        if (playlist.getSongs() == null) {
            playlist.setSongs(new HashSet<>());
        }

        playlist.getSongs().add(song);
        playlistRepository.save(playlist);
    }

    /**
     * Removes a song from one of the current user's playlists.
     *
     * @param playlistId the ID of the playlist
     * @param song       the song to remove
     * @throws IllegalArgumentException if {@code song} is null or not in the playlist
     * @throws IllegalStateException    if the playlist is not found or not owned by the current user
     */
    public void removeSongFromPlaylist(Long playlistId, Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        if (playlist.getSongs() == null || !playlist.getSongs().contains(song)) {
            throw new IllegalArgumentException("Song not in playlist");
        }
        playlist.getSongs().remove(song);
        playlistRepository.save(playlist);
    }

    /**
     * Permanently deletes one of the current user's playlists.
     *
     * @param playlistId the ID of the playlist to delete
     * @throws IllegalStateException if the playlist is not found or not owned by the current user
     */
    public void deletePlaylist(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));
        playlistRepository.delete(playlist);
    }

    /**
     * Returns playlist songs as a List for UI compatibility.
     * Converts from Set to List here so the rest of the UI
     * code that expects {@code List<Song>} does not need to change.
     */
    public List<Song> getPlaylistSongs(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        return playlist.getSongs() != null
                ? new ArrayList<>(playlist.getSongs())
                : new ArrayList<>();
    }

    /**
     * Checks whether a song is in one of the current user's playlists.
     *
     * @param playlistId the ID of the playlist
     * @param song       the song to look for
     * @return {@code true} if the playlist contains the song
     * @throws IllegalStateException if the playlist is not found or not owned by the current user
     */
    public boolean isSongInPlaylist(Long playlistId, Song song) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));
        return playlist.getSongs() != null && playlist.getSongs().contains(song);
    }

    /**
     * Returns how many songs a playlist contains.
     *
     * @param playlistId the ID of the playlist
     * @return the number of songs, or 0 if the playlist has none
     * @throws IllegalStateException if the playlist is not found or not owned by the current user
     */
    public int getPlaylistSongCount(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));
        return playlist.getSongs() != null ? playlist.getSongs().size() : 0;
    }
}