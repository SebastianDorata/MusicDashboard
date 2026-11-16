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

    /**
     * Adds a song to the current user's favourites, timestamped with the current time.
     *
     * @param song the song to favourite
     * @throws IllegalArgumentException if {@code song} is null or is already a favourite
     * @throws IllegalStateException    if no user is logged in
     */
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

    /**
     * Removes a song from the current user's favourites.
     *
     * @param song the song to unfavourite
     * @throws IllegalArgumentException if {@code song} is null or is not a favourite
     * @throws IllegalStateException    if no user is logged in
     */
    public void removeFavourite(Song song) {
        if (song == null) throw new IllegalArgumentException("Song cannot be null");

        User user = getCurrentUser();
        Optional<Favourite> favourite = favouriteRepository.findByUserAndSongId(user, song);
        if (favourite.isEmpty()) throw new IllegalArgumentException("Song not in favorites");
        favouriteRepository.delete(favourite.get());
    }

    /**
     * Checks whether a song is in the current user's favourites.
     *
     * @param song the song to check
     * @return {@code true} if the song is favourited by the current user;
     *         {@code false} if it is not, or if {@code song} is null
     * @throws IllegalStateException if no user is logged in
     */
    public boolean isFavourited(Song song) {
        if (song == null) return false;
        User user = getCurrentUser();
        return favouriteRepository.existsByUserAndSongId(user, song);
    }

    /**
     * Flips the favourite state of a song for the current user.
     *
     * @param song the song to toggle
     * @return {@code true} if the song is now a favourite, {@code false} if it was removed
     * @throws IllegalArgumentException if {@code song} is null
     * @throws IllegalStateException    if no user is logged in
     */
    public boolean toggleFavourite(Song song) {
        if (isFavourited(song)) {
            removeFavourite(song);
            return false;
        } else {
            addFavourite(song);
            return true;
        }
    }

    /**
     * Returns the current user's favourite songs in repository order.
     *
     * <p>Time Complexity: O(n) where n = number of favourites.</p>
     *
     * @return the favourited songs; empty if there are none
     * @throws IllegalStateException if no user is logged in
     */
    public List<Song> getUserFavourites() {
        User user = getCurrentUser();
        return favouriteRepository.findByUserWithSongs(user).stream()
                .map(Favourite::getSongId)
                .collect(Collectors.toList());
    }

    /**
     * Returns the current user's favourite songs, most recently favourited first.
     *
     * <p>Time Complexity: O(n log n) where n = number of favourites.</p>
     *
     * @return the favourited songs sorted by {@code favouritedAt}, newest first
     * @throws IllegalStateException if no user is logged in
     */
    public List<Song> getUserFavouritesSortedByDate() {
        User user = getCurrentUser();
        return favouriteRepository.findByUserWithSongs(user).stream()
                .sorted((a, b) -> b.getFavouritedAt().compareTo(a.getFavouritedAt()))
                .map(Favourite::getSongId)
                .collect(Collectors.toList());
    }

    /**
     * Returns how many songs the current user has favourited.
     *
     * <p>Time Complexity: O(n) where n = number of favourites.</p>
     *
     * @return the number of favourites
     * @throws IllegalStateException if no user is logged in
     */
    public int getFavouriteCount() {
        User user = getCurrentUser();
        return favouriteRepository.findByUser(user).size();
    }
}