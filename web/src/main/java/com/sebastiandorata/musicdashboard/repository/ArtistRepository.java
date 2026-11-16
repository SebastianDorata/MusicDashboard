package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Artist} entities.
 *
 * <p>Provides a derived finder to look up artists by exact name match,
 * used during song import to reuse existing artist records rather than
 * creating duplicates.</p>
 */
@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    /**
     * Finds an artist by its exact name. Used during import to reuse an existing artist record.
     *
     * @param name the artist name to match exactly
     * @return the matching artist, or an empty {@code Optional} if none exists
     */
    Optional<Artist> findByName(String name);

    /**
     * Fetches all artists with their songs, song genres and albums in one query.
     *
     * @return every artist, with those associations loaded; an empty list if there are no artists
     */
    @Query("SELECT DISTINCT a FROM Artist a " +
            "LEFT JOIN FETCH a.songs s " +
            "LEFT JOIN FETCH s.genres " +
            "LEFT JOIN FETCH a.albums")
    List<Artist> findAllWithSongs();


    /**
     * Fetches one artist with their albums in one query.
     *
     * @param id the artist ID
     * @return the artist with albums loaded, or an empty {@code Optional} if no artist has this ID
     */
    @Query("SELECT a FROM Artist a " +
            "LEFT JOIN FETCH a.albums " +
            "WHERE a.artistId = :id")
    Optional<Artist> findByIdWithAlbums(@Param("id") Long id);
}