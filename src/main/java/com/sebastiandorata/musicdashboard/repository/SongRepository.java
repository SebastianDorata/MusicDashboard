package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Song} entities.
 *
 * <p>Provides a derived finder to look up a song by its absolute file
 * path, used during import to skip files that have already been
 * ingested.</p>
 */
@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findByFilePath(String filePath);

    // Fetches all songs with artists and genres in one query
    // Used by the library view to avoid N+1 on 4700 songs
    @Query("SELECT DISTINCT s FROM Song s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres")
    List<Song> findAllWithArtistsAndGenres();

    // Used by playback. Fetches one song with everything needed
    // to display now-playing info and start tracking
    @Query("SELECT s FROM Song s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres " +
            "LEFT JOIN FETCH s.album " +
            "WHERE s.songID = :id")
    Optional<Song> findByIdWithDetails(@Param("id") Long id);
}