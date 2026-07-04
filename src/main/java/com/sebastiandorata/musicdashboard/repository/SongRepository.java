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
 * <p><b>{@code findFirstByFilePath} / {@code findFirstByContentFingerprint}</b>
 * deliberately use "findFirst" rather than the derived-unique-result
 * "findBy" style. "findBy" throws {@code NonUniqueResultException} the
 * instant two rows share a value — which is exactly the crash this
 * refactor fixes. "findFirst" degrades gracefully (picks the
 * lowest-id row) even against a database that still has leftover
 * duplicates from before this fix, while the new unique constraint on
 * {@code file_path} (see the entity) prevents any new ones from being created.
 */
@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findFirstByFilePath(String filePath);

    Optional<Song> findFirstByContentFingerprint(String contentFingerprint);

    // Fetches all songs with artists and genres in one query
    // Used by the library view to avoid N+1 on thousands of songs
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