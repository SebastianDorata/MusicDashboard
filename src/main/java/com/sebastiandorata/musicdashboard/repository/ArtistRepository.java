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
    Optional<Artist> findByName(String name);

    @Query("SELECT DISTINCT a FROM Artist a " +
            "LEFT JOIN FETCH a.songs s " +
            "LEFT JOIN FETCH s.genres " +
            "LEFT JOIN FETCH a.albums")
    List<Artist> findAllWithSongs();


    @Query("SELECT a FROM Artist a " +
            "LEFT JOIN FETCH a.albums " +
            "WHERE a.artistId = :id")
    Optional<Artist> findByIdWithAlbums(@Param("id") Long id);
}