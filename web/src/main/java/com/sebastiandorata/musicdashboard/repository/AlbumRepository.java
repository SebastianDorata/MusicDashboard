package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Album} entities.
 *
 * <p>Extends {@link org.springframework.data.jpa.repository.JpaRepository}
 * and provides a derived finder to look up albums by exact title match,
 * used during song import to avoid creating duplicate album records.</p>
 */
@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    /**
     * Finds an album by its exact title. Used during import to reuse an existing album record.
     *
     * @param title the album title to match exactly
     * @return the matching album, or an empty {@code Optional} if none exists
     */
    Optional<Album> findByTitle(String title);

    /**
     * Fetches all albums with their artists, songs, song artists and song genres in one query.
     * Used by the album grid view.
     *
     * @return every album, with those associations loaded; an empty list if there are no albums
     */
    @Query("SELECT DISTINCT a FROM Album a " +
            "LEFT JOIN FETCH a.artists " +
            "LEFT JOIN FETCH a.songs s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres")
    List<Album> findAllWithArtists();

    /**
     * Fetches one album with its artists, songs, song artists and song genres in one query.
     * Used when drilling into an album detail view.
     *
     * @param id the album ID
     * @return the album with those associations loaded, or an empty {@code Optional} if no album has this ID
     */
    @Query("SELECT DISTINCT a FROM Album a " +
            "LEFT JOIN FETCH a.artists " +
            "LEFT JOIN FETCH a.songs s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres " +
            "WHERE a.albumId = :id")
    Optional<Album> findByIdWithSongsAndArtists(@Param("id") Long id);
}