package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}