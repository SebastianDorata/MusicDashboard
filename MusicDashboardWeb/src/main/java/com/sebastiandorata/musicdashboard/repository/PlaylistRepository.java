package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Playlist} entities.
 *
 * <p>Provides retrieval of all playlists belonging to a specific user,
 * used by {@link PlaylistService}
 * to populate the Playlist page.</p>
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // Get all playlists belonging to a user
    List<Playlist> findByUserId(Long userId);
}
