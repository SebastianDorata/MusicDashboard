package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // Get all playlists belonging to a user
    List<Playlist> findByUserId(Long userId);
}
