package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Favourite;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.FavouriteRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Manages the current user's favourited songs.
 *
 * <p>Provides add, remove, toggle, and query operations. All methods
 * resolve the current user from {@link UserSessionService} and delegate
 * persistence to {@link FavouriteRepository}.
 * Throws {@link IllegalStateException} if no user is logged in, and
 * {@link IllegalArgumentException} for duplicate add or missing-song
 * remove attempts.</p>
 */
@Service
public class FavouriteService {

    @Autowired
    private FavouriteRepository favouriteRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    SongRepository songRepository;

    // Fetch the managed User entity for the current session
    private User getCurrentUser() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No user logged in.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for id: " + userId));
    }

    public void addFavourite(Song song) {
        if (song == null) throw new IllegalArgumentException("Song cannot be null");
        if (isFavourited(song)) throw new IllegalArgumentException("Song already in favorites");

        User user = getCurrentUser();
        Favourite favourite = new Favourite();
        favourite.setUser(user);
        favourite.setSongId(song);
        favourite.setFavouritedAt(LocalDateTime.now());
        favouriteRepository.save(favourite);
    }

    public void removeFavourite(Song song) {
        if (song == null) throw new IllegalArgumentException("Song cannot be null");

        User user = getCurrentUser();
        Optional<Favourite> favourite = favouriteRepository.findByUserAndSongId(user, song);
        if (favourite.isEmpty()) throw new IllegalArgumentException("Song not in favorites");
        favouriteRepository.delete(favourite.get());
    }

    public boolean isFavourited(Song song) {
        if (song == null) return false;
        User user = getCurrentUser();
        return favouriteRepository.existsByUserAndSongId(user, song);
    }

    public boolean toggleFavourite(Song song) {
        if (isFavourited(song)) {
            removeFavourite(song);
            return false;
        } else {
            addFavourite(song);
            return true;
        }
    }

    public List<Song> getUserFavourites() {
        User user = getCurrentUser();
        return favouriteRepository.findByUserWithSongs(user).stream()
                .map(Favourite::getSongId)
                .collect(Collectors.toList());
    }

    public List<Song> getUserFavouritesSortedByDate() {
        User user = getCurrentUser();
        return favouriteRepository.findByUserWithSongs(user).stream()
                .sorted((a, b) -> b.getFavouritedAt().compareTo(a.getFavouritedAt()))
                .map(Favourite::getSongId)
                .collect(Collectors.toList());
    }

    public int getFavouriteCount() {
        User user = getCurrentUser();
        return favouriteRepository.findByUser(user).size();
    }
}