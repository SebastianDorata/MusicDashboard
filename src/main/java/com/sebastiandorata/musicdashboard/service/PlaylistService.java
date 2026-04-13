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

    public List<Playlist> getCurrentUserPlaylists() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in — cannot retrieve playlists.");
        }
        return playlistRepository.findByUserId(userId);
    }

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

    public boolean isSongInPlaylist(Long playlistId, Song song) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));
        return playlist.getSongs() != null && playlist.getSongs().contains(song);
    }

    public int getPlaylistSongCount(Long playlistId) {
        Playlist playlist = getPlaylistById(playlistId)
                .orElseThrow(() -> new IllegalStateException("Playlist not found"));
        return playlist.getSongs() != null ? playlist.getSongs().size() : 0;
    }
}