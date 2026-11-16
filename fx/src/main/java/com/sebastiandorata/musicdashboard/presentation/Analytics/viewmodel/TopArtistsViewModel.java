package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.TopArtistRowData;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.service.handlers.AnalyticsCacheService;
import com.sebastiandorata.musicdashboard.service.handlers.ArtistListeningTimeService;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.GenericModalLoader;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ViewModel for artist-related analytics views.
 *
 * <p>Serves two distinct consumers with separate methods:
 * <ul>
 *   <li>The dashboard top-5 panel via {@link #loadTopArtistsData(Consumer, int)},
 *       which returns only artists with valid play history, ranked by listening
 *       time and padded to exactly 5 rows.</li>
 *   <li>The analytics "View All" modal via {@link #buildAllArtistsRankedData()},
 *       which returns every artist in the library ranked by listening time,
 *       including unplayed artists with a time of zero.</li>
 * </ul>
 *
 * <p>Both methods perform a single database fetch of playback history and
 * derive all required values in memory, avoiding N+1 query patterns.
 */
@Service
public class TopArtistsViewModel {

    @Autowired
    private ArtistListeningTimeService artistListeningTimeService;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private DataLoadingService dataLoadingService;

    // ── Dashboard path ─────────────────────────────────────────────

    /**
     * Loads a ranked list of top artists asynchronously for display in a preview
     * or panel, delivering results to {@code onSuccess} on the JavaFX thread.
     *
     * <p>Delegates to {@link #buildTopArtistsData(int)} on a background thread
     * via {@link DataLoadingService}. The {@code limit} parameter controls how
     * many rows are returned, allowing the dashboard panel and analytics preview
     * to share this method with different display sizes.
     *
     * @param onSuccess callback invoked on the JavaFX thread with the completed
     *                  list of exactly {@code limit} {@link TopArtistRowData} rows
     * @param limit     the number of rows to return; rows are padded with
     *                  placeholders if fewer than {@code limit} artists have
     *                  valid play history
     */
    public void loadTopArtistsData(Consumer<List<TopArtistRowData>> onSuccess, int limit) {
        dataLoadingService.loadAsync(() -> buildTopArtistsData(limit), onSuccess);
    }

    /**
     * Builds and returns a ranked list of top artist rows synchronously, ranked
     * by total listening time descending, bounded to {@code limit} rows.
     *
     * <p>Optimization: Replaces the original implementation that made one
     * {@link ArtistListeningTimeService#getArtistTotalSeconds} call per artist,
     * each triggering a full table scan — resulting in up to N+1 sequential
     * database round trips per refresh. This method performs a single database
     * fetch and accumulates total seconds per artist in memory using a
     * {@link HashMap} in one pass before sorting and slicing to {@code limit}.
     * The formatted time string is derived from the in-memory total via
     * {@link ArtistListeningTimeService.ArtistTimeData#formatTime()},
     * requiring no additional queries.
     *
     * <p>If fewer than {@code limit} artists exist in the playback history, the
     * result is padded with placeholder entries (null artist, "—" for name and
     * time) so the calling panel always renders a full list of the expected size.
     *
     * <p>Time Complexity: O(n) where n = total valid playback history rows.
     * One pass to accumulate artist totals, one pass to sort bounded by the
     * number of distinct artists, and one pass to map to DTOs.
     * Space Complexity: O(a) where a = number of distinct artists in playback
     * history. The result list is always O(limit).
     *
     * @param limit the maximum number of rows to return
     * @return a list of exactly {@code limit} {@link TopArtistRowData} entries,
     *         padded with placeholders if fewer than {@code limit} artists have
     *         valid play history
     */
    public List<TopArtistRowData> buildTopArtistsData(int limit) {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return paddedResult(new ArrayList<>(), limit);

        List<PlaybackHistory> allPlays = playbackHistoryRepository
                .findByUserIdOrderByPlayedAtDesc(userId)
                .stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.toList());

        if (allPlays.isEmpty()) return paddedResult(new ArrayList<>(), limit);

        // Single pass — build ranked map with total seconds per artist
        Map<Artist, Integer> artistTotals = new HashMap<>();
        for (PlaybackHistory play : allPlays) {
            if (play.getSong() == null || play.getSong().getArtists() == null) continue;
            for (Artist artist : play.getSong().getArtists()) {
                artistTotals.merge(artist, play.getDurationPlayedSeconds(), Integer::sum);
            }
        }

        List<TopArtistRowData> result = artistTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(e -> {
                    ArtistListeningTimeService.ArtistTimeData timeData =
                            new ArtistListeningTimeService.ArtistTimeData(
                                    e.getKey(), e.getValue());
                    return new TopArtistRowData(
                            e.getKey(),
                            e.getKey().getName(),
                            timeData.formatTime()
                    );
                })
                .collect(Collectors.toList());

        return paddedResult(result, limit);
    }


    /**
     * <p>Builds all artist rows for the analytics modal synchronously, ranked by
     * total listening time descending. All artists in the library are included.</p>
     * <p>Those with no valid plays display a formatted time of "0m" and an empty
     * name field in the modal row.</p>
     *
     * <p>Used exclusively by the analytics "View All" modal path via
     * {@link GenericModalLoader}. The dashboard top-5 panel uses {@link #buildTopArtistsData(int)} instead,
     * keeping that path fast and unaffected by library size.
     *
     * <p>Performs two database fetches. One fetch for playback history and one for
     * the full artist list, then joins them in memory. The result is cached by
     * {@link AnalyticsCacheService} so subsequent modal opens within the same session incur
     * no additional database cost.
     *
     * <p>Time Complexity: O(n + a) where n = valid playback history rows and
     * a = total artists in the library. One pass to build the seconds map,
     * one pass to sort all artists, one pass to map to DTOs.
     * Space Complexity: O(a) for the full ranked result list.
     *
     * @return a {@link DoublyLinkedList} of {@link TopArtistRowData} for every
     *         artist in the library, with zero-time entries at the bottom for
     *         artists that have no valid play history
     */
    public DoublyLinkedList<TopArtistRowData> buildAllArtistsRankedData() {
        Long userId = userSessionService.getCurrentUserId();

        // Single pass builds seconds map from playback history
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

        // Fetch all artists and rank by listening time inline — unplayed artists get 0
        List<Artist> ranked = artistRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(
                        artistSecondsMap.getOrDefault(b.getArtistId(), 0),
                        artistSecondsMap.getOrDefault(a.getArtistId(), 0)))
                .collect(Collectors.toList());

        DoublyLinkedList<TopArtistRowData> result = new DoublyLinkedList<>();
        for (Artist artist : ranked) {
            int totalSeconds = artistSecondsMap.getOrDefault(artist.getArtistId(), 0);
            ArtistListeningTimeService.ArtistTimeData timeData =
                    new ArtistListeningTimeService.ArtistTimeData(artist, totalSeconds);
            result.add(new TopArtistRowData(
                    artist,
                    totalSeconds > 0 ? artist.getName() : "",
                    timeData.formatTime()
            ));
        }

        return result;
    }


    /**
     * Pads the given result list to exactly {@code limit} entries by appending
     * placeholder rows until the target size is reached.
     *
     * <p>Placeholder rows have a {@code null} artist reference and display "—"
     * for both name and listening time, ensuring the calling panel always renders
     * a complete list of the expected size regardless of library size or play
     * history depth.
     *
     * <p>Time Complexity: O(limit - result.size()) = O(limit) in the worst case.
     * Space Complexity: O(1) per placeholder added.
     *
     * @param result the list to pad; modified in place
     * @param limit  the target size the list should reach
     * @return the same list, guaranteed to contain exactly {@code limit} entries
     */
    private List<TopArtistRowData> paddedResult(List<TopArtistRowData> result, int limit) {
            while (result.size() < limit) {
                result.add(new TopArtistRowData(null, "—", "—"));
            }
            return result;
        }
}