package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.Favourite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavouriteRepository extends JpaRepository<Favourite, Long> {

    // Find all favourites for a given user
    List<Favourite> findByUserId(Long userId);

    // Check if a user has already favourited a song
    boolean existsByUserIdAndSongSongID(Long userId, Long songId);
}