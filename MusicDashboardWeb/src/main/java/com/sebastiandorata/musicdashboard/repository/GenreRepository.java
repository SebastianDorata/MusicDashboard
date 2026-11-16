package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Genre} entities.
 *
 * <p>Provides a derived finder to look up genres by exact name match,
 * used during song import to reuse existing genre records.</p>
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    /**
     * Finds a genre by its exact name. Used during import to reuse an existing genre record.
     *
     * @param name the genre name to match exactly
     * @return the matching genre, or an empty {@code Optional} if none exists
     */
    Optional<Genre> findByName(String name);
}
