package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopAlbumsViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopSongsViewModel;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pagination service for analytics listings (songs, artists, albums).
 * Wraps results in DoublyLinkedList for efficient forward/backward traversal.
 *
 * <p><b><u>Windowed methods:</u></b> getTopSongsWindow and similar are used by
 * the ViewModels for the small preview cards on the analytics page.</p>
 *
 * <p><b><u>Full-list methods:</u></b> getAllTopSongs, getAllTopAlbums, and
 * getAllTopArtists are used by GenericModalLoader and AnalyticsCacheService
 * to load once and page entirely in memory.</p>
 *
 * <p>Time Complexity: O(n log k) where n = plays, k = distinct entities.</p>
 * <p>Space Complexity: O(n) for the full list, O(k) for a windowed result.</p>
 */
@Service
public class ListeningPaginationService {

    @Autowired private PlaybackHistoryRepository playbackHistoryRepository;
    @Autowired private UserSessionService userSessionService;
    @Autowired private ArtistRepository artistRepository;

    /**
     * Returns all top songs sorted by total listening time, descending.
     * Intended for use with AnalyticsCacheService to keep page in memory.
     *
     * <p>Time Complexity: O(n log n).</p>
     * <p>Space Complexity: O(n).</p>
     */
    public DoublyLinkedList<Song> getAllTopSongs() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.toList());

        List<Song> sorted = allPlays.stream()
                .collect(Collectors.groupingBy(
                        PlaybackHistory::getSong,
                        Collectors.summingInt(PlaybackHistory::getDurationPlayedSeconds)
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DoublyLinkedList<Song> result = new DoublyLinkedList<>();
        sorted.forEach(result::add);
        return result;
    }

    /**
     * Returns all top albums sorted by total listening time, descending.
     * Intended for use with AnalyticsCacheService.
     *
     * <p>Time Complexity: O(n log n).</p>
     * <p>Space Complexity: O(n).</p>
     */
    public DoublyLinkedList<Album> getAllTopAlbums() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .filter(h -> h.getSong().getAlbum() != null)
                .collect(Collectors.toList());

        List<Album> sorted = allPlays.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getSong().getAlbum(),
                        Collectors.summingInt(PlaybackHistory::getDurationPlayedSeconds)
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DoublyLinkedList<Album> result = new DoublyLinkedList<>();
        sorted.forEach(result::add);
        return result;
    }


    /**
     * Returns the full top songs ranking for use by {@link TopSongsViewModel}.
     * Delegates entirely to {@link #getAllTopSongs()}; the caller is responsible
     * for slicing via {@link DoublyLinkedList#getWindow(int, int)}.
     *
     * <p>For paginated modal use, prefer {@link #getAllTopSongs()} combined with
     * {@link AnalyticsCacheService} to avoid rebuilding the full ranking on
     * every page turn.</p>
     *
     * <p>Time Complexity: O(n log n), full ranking rebuilt on each call.</p>
     * <p>Space Complexity: O(n).</p>
     *
     * @param offset unused, slicing is performed by the caller
     * @param limit unused, slicing is performed by the caller
     * @return a fully ranked {@link DoublyLinkedList} of {@link Song} entities
     */
    public DoublyLinkedList<Song> getTopSongsWindow(int offset, int limit) {
        return getAllTopSongs(); // list is built; caller uses getWindow()
    }

    /**
     * Returns the full top albums ranking for use by {@link TopAlbumsViewModel}.
     * Delegates entirely to {@link #getAllTopAlbums()}; the caller is responsible
     * for slicing via {@link DoublyLinkedList#getWindow(int, int)}.
     *
     * <p>Time Complexity: O(n log n) Full ranking rebuilt on each call.
     * <p>Space Complexity: O(n)
     *
     * @param offset unused. Slicing is performed by the caller
     * @param limit  unused. Slicing is performed by the caller
     * @return a fully ranked {@link DoublyLinkedList} of  {@link Album} entities
     */
    public DoublyLinkedList<Album> getTopAlbumsWindow(int offset, int limit) {
        return getAllTopAlbums();
    }

    /**
     * <p>Returns all top songs sorted by total listening time, descending.
     * Intended for use with AnalyticsCacheService to keep page in memory.
     *
     * <p>Time Complexity: O(n log n)
     * <p>Space Complexity: O(n)
     */
    public DoublyLinkedList<Artist> getAllTopArtists() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return new DoublyLinkedList<>();

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.toList());

        List<Artist> sorted = allPlays.stream()
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(
                        a -> a,
                        Collectors.summingInt(a ->
                                allPlays.stream()
                                        .filter(h -> h.getSong().getArtists().contains(a))
                                        .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                                        .sum()
                        )
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DoublyLinkedList<Artist> result = new DoublyLinkedList<>();
        sorted.forEach(result::add);
        return result;
    }


    /**
     * Returns the number of distinct songs that have at least one valid play
     * in the current user's playback history.
     *
     * <p>Time Complexity: O(n) where n = total playback history rows.
     * <p>Space Complexity: O(k) where k = distinct songs.
     *
     * @return count of distinct played songs, or {@code 0} if no user is logged in
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
     * Returns the number of distinct albums that have at least one valid play
     * in the current user's playback history. Songs with no album are excluded.
     *
     * <p>Time Complexity: O(n) where n = total playback history rows.
     * <p>Space Complexity: O(k) where k = distinct albums.
     *
     * @return count of distinct played albums, or {@code 0} if no user is logged in
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
     * Returns the full top artists ranking for use by analytics preview cards.
     * Delegates entirely to {@link #getAllTopArtists()}; the caller is responsible
     * for slicing via {@link DoublyLinkedList#getWindow(int, int)}.
     *
     * <p>Time Complexity: O(n log n) Full ranking rebuilt on each call.
     * <p>Space Complexity: O(n)
     *
     * @param offset unused. Slicing is performed by the caller
     * @param limit  unused. Slicing is performed by the caller
     * @return a fully ranked {@link DoublyLinkedList} of  {@link Album} entities
     */
    public DoublyLinkedList<Artist> getTopArtistsWindow(int offset, int limit) {
        return getAllTopArtists();
    }

    /**
     * Returns all artists in the library ranked by total listening time descending.
     * Artists with no valid plays are included with a total of zero seconds,
     * appearing at the bottom of the list.
     *
     * <p>Fetches the complete artist list from the repository and joins it in memory
     * against the playback history totals map. The extra repository query cost is
     * absorbed by {@link AnalyticsCacheService} on subsequent loads within the
     * same session.
     *
     * <p>Time Complexity: O(n + a) where n = valid playback history rows, a = total artists in the library.
     * <p>Space Complexity: O(a) for the result list.
     *
     * @return a {@link DoublyLinkedList} of all {@link Artist} entities ranked by total listening time, padded with zero-time entries for unplayed artists
     */
    public DoublyLinkedList<Artist> getAllArtistsRankedByTime() {
        Long userId = userSessionService.getCurrentUserId();

        // Build listening time map from playback history
        Map<Long, Integer> artistSecondsMap = new HashMap<>();
        if (userId != null) {
            playbackHistoryRepository
                    .findByUserIdOrderByPlayedAtDesc(userId)
                    .stream()
                    .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds())
                            && h.getSong() != null
                            && h.getSong().getArtists() != null)
                    .forEach(h -> {
                        for (Artist artist : h.getSong().getArtists()) {
                            artistSecondsMap.merge(
                                    artist.getArtistId(),
                                    h.getDurationPlayedSeconds(),
                                    Integer::sum
                            );
                        }
                    });
        }

        // Fetch all artists and rank by listening time, unplayed artists get 0
        List<Artist> sorted = artistRepository.findAll().stream()
                .sorted((a, b) -> {
                    int timeA = artistSecondsMap.getOrDefault(a.getArtistId(), 0);
                    int timeB = artistSecondsMap.getOrDefault(b.getArtistId(), 0);
                    return Integer.compare(timeB, timeA);
                })
                .collect(Collectors.toList());

        DoublyLinkedList<Artist> result = new DoublyLinkedList<>();
        sorted.forEach(result::add);
        return result;
    }
}