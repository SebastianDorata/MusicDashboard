package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Favourite;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // Find all favourites for a given user
    List<Favourite> findByUser(User user);

    // Check if a user has already favourited a song
    boolean existsByUserAndSongId(User user, Song song);

    // Find a specific favourite record
    Optional<Favourite> findByUserAndSongId(User user, Song song);

    @Query("SELECT f FROM Favourite f " +
            "LEFT JOIN FETCH f.songId s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE f.user = :user")
    List<Favourite> findByUserWithSongs(@Param("user") User user);
}