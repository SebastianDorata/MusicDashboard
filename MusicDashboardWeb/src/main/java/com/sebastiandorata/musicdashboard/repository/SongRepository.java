package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Finds the lowest-ID song with the given file path. Uses "findFirst" so that a database
     * with leftover duplicate paths does not throw.
     *
     * @param filePath the file path to match exactly
     * @return the matching song, or an empty {@code Optional} if none exists
     */
    Optional<Song> findFirstByFilePath(String filePath);

    /**
     * Finds the lowest-ID song with the given content fingerprint. Uses "findFirst" so that a
     * database with leftover duplicates does not throw.
     *
     * @param contentFingerprint the content fingerprint to match exactly
     * @return the matching song, or an empty {@code Optional} if none exists
     */
    Optional<Song> findFirstByContentFingerprint(String contentFingerprint);

    /**
     * Fetches all songs with their artists and genres in one query. Used by the library view
     * to avoid N+1 queries on large libraries.
     *
     * @return every song, with artists and genres loaded; an empty list if there are no songs
     */
    @Query("SELECT DISTINCT s FROM Song s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres")
    List<Song> findAllWithArtistsAndGenres();

    /**
     * Fetches one song with its artists, genres and album in one query. Used by playback to
     * display now-playing info and start tracking.
     *
     * @param id the song ID
     * @return the song with those associations loaded, or an empty {@code Optional} if no song has this ID
     */
    @Query("SELECT s FROM Song s " +
            "LEFT JOIN FETCH s.artists " +
            "LEFT JOIN FETCH s.genres " +
            "LEFT JOIN FETCH s.album " +
            "WHERE s.songID = :id")
    Optional<Song> findByIdWithDetails(@Param("id") Long id);

    /**
     * Fetches all songs credited to an artist, with each song's album eagerly loaded. Used by
     * {@code LibraryService.resolveAlbumsForArtist} to find albums that are linked only through
     * the artist's songs.
     *
     * @param artistId the ID of the artist
     * @return the distinct songs credited to the artist; an empty list if there are none
     */
    @Query("SELECT DISTINCT s FROM Song s " +
            "JOIN s.artists a " +
            "LEFT JOIN FETCH s.album " +
            "WHERE a.artistId = :artistId")
    List<Song> findByArtistIdWithAlbum(@Param("artistId") Long artistId);

    /**
     * Removes a song from every playlist, directly in the database.
     *
     * @param songId the ID of the song to remove from all playlists
     */
    @Modifying
    @Query(value = "DELETE FROM playlist_songs WHERE song_id = :songId", nativeQuery = true)
    void removeFromAllPlaylists(@Param("songId") Long songId);
}