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

    Optional<Album> findByTitle(String title);

    // Fetches all albums with their artists in one query
    // Used by the album grid view
    @Query("SELECT DISTINCT a FROM Album a " +
            "LEFT JOIN FETCH a.artists " +
            "LEFT JOIN FETCH a.songs s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres")
    List<Album> findAllWithArtists();

    // Fetches one album with songs and artists
    // Used when drilling into an album detail view
    @Query("SELECT DISTINCT a FROM Album a " +
            "LEFT JOIN FETCH a.songs s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH a.artists " +
            "WHERE a.albumId = :id")
    Optional<Album> findByIdWithSongsAndArtists(@Param("id") Long id);
}