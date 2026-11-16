package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.entity.Song;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GenreFilterService provides filtering logic for songs and albums by genre.
 *
 * Time Complexity: O(n). Single pass filter operation
 * Space Complexity: O(m) where m ≤ n (filtered result size)
 */
@Service
public class GenreFilterService {

    /**
     * Filters songs by a selected genre.
     *
     * @param songs The list of songs to filter
     * @param genre The genre to filter by (null = no filter, returns all songs)
     * @return Filtered list of songs that contain the genre
     */
    public List<Song> filterSongsByGenre(List<Song> songs, Genre genre) {
        if (genre == null) return songs;

        return songs.stream()
                .filter(s -> s.getGenres() != null && s.getGenres().contains(genre))
                .collect(Collectors.toList());
    }

    /**
     * Filters albums by a selected genre.
     * An album matches if any of its songs contains the genre.
     *
     * @param albums The list of albums to filter
     * @param genre The genre to filter by (null = no filter, returns all albums)
     * @return Filtered list of albums containing songs with the genre
     */
    public List<Album> filterAlbumsByGenre(List<Album> albums, Genre genre) {
        if (genre == null) return albums;

        return albums.stream()
                .filter(a -> a.getSongs() != null &&
                        a.getSongs().stream()
                                .anyMatch(s -> s.getGenres() != null && s.getGenres().contains(genre)))
                .collect(Collectors.toList());
    }
}