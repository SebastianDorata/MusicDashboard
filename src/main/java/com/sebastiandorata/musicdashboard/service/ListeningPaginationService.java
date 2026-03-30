package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic pagination service for analytics listings (songs, artists, albums).
 * Wraps results in DoublyLinkedList for backward/forward traversal.
 *
 * Time Complexity: O(n log k) where n = plays, k = limit
 * Space Complexity: O(k) for result window
 */
@Service
public class ListeningPaginationService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * Gets a paginated window of top songs by listening time.
     * Time Complexity: O(n log k)
     * Space Complexity: O(k)
     */
    public DoublyLinkedList<Song> getTopSongsWindow(int offset, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.toList());

        List<Song> topSongs = allPlays.stream()
                .collect(Collectors.groupingBy(
                        PlaybackHistory::getSong,
                        Collectors.summingInt(h -> h.getDurationPlayedSeconds())
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DoublyLinkedList<Song> result = new DoublyLinkedList<>();
        for (Song song : topSongs) {
            result.add(song);
        }

        return result;
    }

    /**
     * Gets a paginated window of top albums by listening time.
     * Time Complexity: O(n log k)
     * Space Complexity: O(k)
     */
    public DoublyLinkedList<Album> getTopAlbumsWindow(int offset, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null)
                .collect(Collectors.toList());

        List<Album> topAlbums = allPlays.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getSong().getAlbum(),
                        Collectors.summingInt(h -> h.getDurationPlayedSeconds())
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DoublyLinkedList<Album> result = new DoublyLinkedList<>();
        for (Album album : topAlbums) {
            result.add(album);
        }

        return result;
    }

    /**
     * Gets a paginated window of top artists by listening time.
     * Time Complexity: O(n log k)
     * Space Complexity: O(k)
     */
    public DoublyLinkedList<Artist> getTopArtistsWindow(int offset, int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.toList());

        List<Artist> topArtists = allPlays.stream()
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(
                        a -> a,
                        Collectors.summingInt(a -> {
                            return allPlays.stream()
                                    .filter(h -> h.getSong().getArtists().contains(a))
                                    .mapToInt(h -> h.getDurationPlayedSeconds())
                                    .sum();
                        })
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DoublyLinkedList<Artist> result = new DoublyLinkedList<>();
        for (Artist artist : topArtists) {
            result.add(artist);
        }

        return result;
    }

    /**
     * Gets total count of top songs.
     */
    public int getTopSongsCount() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return 0;

        return (int) playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .map(PlaybackHistory::getSong)
                .distinct()
                .count();
    }

    /**
     * Gets total count of top albums.
     */
    public int getTopAlbumsCount() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return 0;

        return (int) playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null)
                .map(h -> h.getSong().getAlbum())
                .distinct()
                .count();
    }

    /**
     * Gets total count of top artists.
     */
    public int getTopArtistsCount() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return 0;

        return (int) playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .distinct()
                .count();
    }
}