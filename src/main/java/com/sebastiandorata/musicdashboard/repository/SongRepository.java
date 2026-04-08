package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
