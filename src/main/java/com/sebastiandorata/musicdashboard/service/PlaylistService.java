package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.PlaylistRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
     * Time Complexity: O(1)
     * Space Complexity: O(1)
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
        playlist.setIsPublic(false);  // Default to private

        return playlistRepository.save(playlist);
    }

    /**
     * Time Complexity: O(n) where n = number of user's playlists
     * Space Complexity: O(n)
     */
    public List<Playlist> getCurrentUserPlaylists() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot retrieve playlists.");
        }

        return playlistRepository.findByUserId(userId);
    }

    /**
     * Time Complexity: O(1)  database lookup by ID
     * Space Complexity: O(1)
     */
    public Optional<Playlist> getPlaylistById(Long playlistId) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot retrieve playlist.");
        }

        Optional<Playlist> playlist = playlistRepository.findById(playlistId);

        // Verify user owns this playlist
        if (playlist.isPresent() && !playlist.get().getUser().getId().equals(userId)) {
            throw new IllegalStateException("User does not own this playlist");
        }

        return playlist;
    }

    /**
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public Playlist updatePlaylist(Long playlistId, String name, String description, Boolean isPublic) {
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
     * Time Complexity: O(1) amortized list add operation
     * Space Complexity: O(1)  single song object
     */
    public void addSongToPlaylist(Long playlistId, Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }

        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        // Check if song already in playlist
        if (playlist.getSongs() != null && playlist.getSongs().contains(song)) {
            throw new IllegalArgumentException("Song already in playlist");
        }

        // Initialize songs list if null
        if (playlist.getSongs() == null) {
            playlist.setSongs(new java.util.ArrayList<>());
        }

        playlist.getSongs().add(song);
        playlistRepository.save(playlist);
    }

    /**
     * Time Complexity: O(n) where n = songs in playlist (list.remove())
     * Space Complexity: O(1)
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
     * Time Complexity: O(1) cascade delete to be handled by database
     * Space Complexity: O(1)
     */
    public void deletePlaylist(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        playlistRepository.delete(playlist);
    }

    /**
     * Time Complexity: O(1)  memory access
     * Space Complexity: O(n) where n = songs in playlist
     */
    public List<Song> getPlaylistSongs(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        return playlist.getSongs() != null ? playlist.getSongs() : new java.util.ArrayList<>();
    }

    /**
     * Time Complexity: O(n) where n = songs in playlist (list.contains)
     * Space Complexity: O(1)
     */
    public boolean isSongInPlaylist(Long playlistId, Song song) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        return playlist.getSongs() != null && playlist.getSongs().contains(song);
    }

    /**
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public int getPlaylistSongCount(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));

        return playlist.getSongs() != null ? playlist.getSongs().size() : 0;
    }
}