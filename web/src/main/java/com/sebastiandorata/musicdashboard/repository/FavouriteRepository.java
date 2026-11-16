package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Favourite;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Favourite} entities.
 *
 * <p>Provides methods to retrieve all favourites for a user, check whether
 * a specific song is already favourited, and fetch the exact
 * {@link Favourite} record for toggling or deletion.</p>
 */
@Repository
public interface FavouriteRepository extends JpaRepository<Favourite, Long> {

    /**
     * Finds all favourites for a given user.
     *
     * @param user the user whose favourites to return
     * @return the user's favourite records; an empty list if there are none
     */
    List<Favourite> findByUser(User user);

    /**
     * Checks whether a user has already favourited a song.
     *
     * @param user the user to check
     * @param song the song to check
     * @return {@code true} if a favourite record exists for this user and song
     */
    boolean existsByUserAndSongId(User user, Song song);

    /**
     * Finds the specific favourite record for a user and song, used for toggling or deletion.
     *
     * @param user the user who favourited the song
     * @param song the favourited song
     * @return the matching favourite, or an empty {@code Optional} if none exists
     */
    Optional<Favourite> findByUserAndSongId(User user, Song song);

    /**
     * Fetches all favourites for a user, with each song's artists and genres loaded in one query.
     *
     * @param user the user whose favourites to return
     * @return the user's favourite records with songs, artists and genres loaded; an empty list if there are none
     */
    @Query("SELECT f FROM Favourite f " +
            "LEFT JOIN FETCH f.songId s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE f.user = :user")
    List<Favourite> findByUserWithSongs(@Param("user") User user);

    /**
     * Deletes every favourite that references the given song, directly in the database.
     *
     * @param songId the ID of the song whose favourites should be removed
     */
    @Modifying
    @Query("DELETE FROM Favourite f WHERE f.songId.songID = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}