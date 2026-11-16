package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.service.Import.SongImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sebastiandorata.musicdashboard.repository.FavouriteRepository;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;

import java.util.*;
import java.util.stream.Collectors;
/**
 * Thin facade for song, album, and artist retrieval.
 *
 * <p>Delegates to the underlying repositories and {@link SongImportService}.
 * Also resolves the complete album list for an artist, falling back from
 * the artist's direct {@code albums} relationship to albums inferred
 * across the artist's songs when no direct relationship exists.</p>
 */
@Service
public class LibraryService {

    @Autowired private AlbumRepository albumRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private SongImportService songService;
    @Autowired private FavouriteRepository favouriteRepository;
    @Autowired private PlaybackHistoryRepository playbackHistoryRepository;

    // In-memory cache. Songs only change on import
    private List<Song>   songCache   = null;
    private List<Album>  albumCache  = null;
    private List<Artist> artistCache = null;

    /**
     * Returns every song in the library.
     *
     * <p>The result is cached in memory; the first call loads from
     * {@link SongImportService}, later calls return the cached list until
     * {@link #invalidateCache()} is called.</p>
     *
     * <p>Time Complexity: O(n) on a cache miss, O(1) on a cache hit.</p>
     *
     * @return all songs in the library
     */
    @Transactional(readOnly = true)
    public List<Song> getAllSongs() {
        if (songCache == null) {
            songCache = songService.getAllSongs();
        }
        return songCache;
    }

    /**
     * Returns every album in the library, with each album's artists loaded.
     *
     * <p>Cached in memory until {@link #invalidateCache()} is called.</p>
     *
     * <p>Time Complexity: O(n) on a cache miss, O(1) on a cache hit.</p>
     *
     * @return all albums in the library
     */
    @Transactional(readOnly = true)
    public List<Album> getAllAlbums() {
        if (albumCache == null) {
            albumCache = albumRepository.findAllWithArtists();
        }
        return albumCache;
    }

    /**
     * Returns every artist in the library, with each artist's songs loaded.
     *
     * <p>Cached in memory until {@link #invalidateCache()} is called.</p>
     *
     * <p>Time Complexity: O(n) on a cache miss, O(1) on a cache hit.</p>
     *
     * @return all artists in the library
     */
    @Transactional(readOnly = true)
    public List<Artist> getAllArtists() {
        if (artistCache == null) {
            artistCache = artistRepository.findAllWithSongs();
        }
        return artistCache;
    }

    /**
     * Clears the cached songs, albums and artists so the next call to
     * {@link #getAllSongs()}, {@link #getAllAlbums()} or {@link #getAllArtists()}
     * reloads from the database. Call this after any import or deletion.
     */
    public void invalidateCache() {
        songCache   = null;
        albumCache  = null;
        artistCache = null;
    }

    /**
     * Returns every album associated with an artist, without duplicates.
     *
     * <p>Combines the artist's direct {@code albums} relationship with the
     * albums of the artist's songs. The artist is re-loaded by ID first so
     * that lazy collections are read from the current database state.</p>
     *
     * <p>Time Complexity: O(a + s) where a = direct albums and s = songs by the artist.</p>
     *
     * @param artist the artist whose albums to resolve
     * @return the distinct albums, direct albums first, in encounter order
     */
    @Transactional(readOnly = true)
    public List<Album> resolveAlbumsForArtist(Artist artist) {
        // Re-attach via ID. Never trust the detached entity's lazy collections
        Artist managed = artistRepository.findByIdWithAlbums(artist.getArtistId())
                .orElse(artist);
    // Do not modify again, otherwise one random album per artist.
        Set<Album> direct = managed.getAlbums();

        // Pick up albums linked only through songs (existing fallback)
        Set<Album> viaSongs = songRepository.findByArtistIdWithAlbum(artist.getArtistId())
                .stream()
                .map(Song::getAlbum)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Album> all = new LinkedHashSet<>(direct);
        all.addAll(viaSongs);
        return new ArrayList<>(all);
    }


    /**
     * Loads one album with its songs and artists.
     *
     * @param albumId the ID of the album
     * @return the album with its songs and artists initialised
     * @throws IllegalStateException if no album exists with the given ID
     */
    @Transactional(readOnly = true)
    public Album getAlbumWithFullDetails(Long albumId) {
        return albumRepository.findByIdWithSongsAndArtists(albumId)
                .orElseThrow(() -> new IllegalStateException(
                        "Album not found: " + albumId));
    }


    /**
     * Permanently deletes a song and everything that references it: playback
     * history, favourites, playlist entries. Song's own artist/genre join rows
     * are cleaned up automatically by Hibernate since Song owns those tables.
     */
    @Transactional
    public void deleteSong(Long songId) {
        playbackHistoryRepository.deleteBySongId(songId);
        favouriteRepository.deleteBySongId(songId);
        songRepository.removeFromAllPlaylists(songId);
        songRepository.deleteById(songId);
        invalidateCache();
    }

    /**
     * Permanently deletes an album and every song it contains, cascading each
     * song through {@link #deleteSong(Long)} first so their playback history,
     * favourites, and playlist entries are cleaned up too.
     */
    @Transactional
    public void deleteAlbum(Long albumId) {
        Album album = albumRepository.findByIdWithSongsAndArtists(albumId)
                .orElseThrow(() -> new IllegalStateException("Album not found: " + albumId));

        List<Long> songIds = album.getSongs().stream()
                .map(Song::getSongID)
                .collect(Collectors.toList());

        for (Long songId : songIds) {
            deleteSong(songId);
        }

        albumRepository.deleteById(albumId);
        invalidateCache();
    }
}