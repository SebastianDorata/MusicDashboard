package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.dto.StatSnapshot;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.PlaybackHistoryRepository;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.PlaybackConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides daily and weekly listening statistics for the dashboard stat cards.
 *
 * <p><b>Recording path</b></p>
 * <ol>
 * <li>The browser plays a song through its HTML5 audio element. The server-side
 *    {@code MusicPlayerService} (one instance per browser session) keeps the queue and tracking flags.</li>
 * <li>After 20 seconds of playback the browser calls {@code POST /api/player/tracked}.
 *    The endpoint creates the {@code PlaybackHistory} row, with {@code playedAt} set to
 *    {@code LocalDateTime.now()}.</li>
 * <li>When the track ends, the browser calls {@code POST /api/player/ended}. The row is marked
 *    completed and its duration is set to the full track length.</li>
 * </ol>
 * <p>{@code playedAt} is stored with no zone. On a UTC server the value is UTC wall-clock time;
 * on a developer machine in Toronto it is Eastern time. The database holds whatever the server's
 * local clock said at that instant. Only plays of at least 20 seconds count as valid.</p>
 *
 * <p><b>Retrieval path</b></p>
 * <ol>
 * <li>{@code DashboardController} renders the dashboard page. The stat cards are refreshed by a
 *    browser call to {@code /api/dashboard/stats}.</li>
 * <li>The stats request runs on the request thread and calls {@link #buildStatSnapshot()}, which
 *    returns the snapshot directly. No background loader and no UI thread are involved.</li>
 * <li>{@code buildStatSnapshot()} takes the logged-in user ID from {@code UserSessionService} and
 *    queries {@code PlaybackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId)}, returning
 *    all of that user's rows, newest first.</li>
 * <li>Each row's {@code playedAt} is converted to Toronto local time by {@link #toTorontoTime(LocalDateTime)}.
 *    The day and week boundaries are also computed in Toronto time using {@code LocalDate.now(TORONTO)},
 *    so both sides of the comparison use the same zone.</li>
 * <li>The filtered lists are passed to the compute helpers, which build the {@link StatSnapshot}.</li>
 * <li>Session duration comes from {@code UserSessionService.getSessionDurationSeconds()}. It is computed in
 *    memory as now minus the login time, so it involves no database call.</li>
 * </ol>
 *
 * <p><b>Time conversion</b></p>
 * <p>
 * {@code playedAt} is stored with no zone. On a UTC server, a song played at
 * 23:30 Toronto time is stored as 04:30 the next UTC day. Without the
 * conversion, {@code LocalDate.now()} on a UTC server returns the UTC date, so
 * the filter window is wrong and misses plays from 19:00 to 23:59.
 * </p>
 * <p>
 * After the conversion, stored values are re-interpreted as Toronto time
 * before comparison, so the window is effectively: Toronto-23:59.
 * </p>
 * <p>Time Complexity: O(n). One DB fetch + O(n) in-memory stream passes.</p>
 * <p>Space Complexity: O(n) for the fetched list.</p>
 */
@Service
public class DailyListeningStatsService {

    @Autowired
    private PlaybackHistoryRepository playbackHistoryRepository;

    @Autowired
    private UserSessionService userSessionService;

    /**
     * The display timezone for all day/week boundary calculations.
     * Day resets at midnight Toronto time (05:00 UTC).
     */
    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    /**
     * The zone used when interpreting stored {@code playedAt} values.
     * playedAt is written with {@code LocalDateTime.now()} which uses the JVM
     * system clock. It must re-interpret it using the same zone it was
     * recorded in before converting to Toronto time.
     */
    private static final ZoneId SYSTEM = ZoneId.systemDefault();

    /**
     * Builds all stat-card values from a single database fetch.
     *
     * <p>Retrieval path summary:
     * <ol>
     *   <li>Fetch all rows for this user from {@code playback_history} ordered
     *       newest-first (one query).</li>
     *   <li>Convert each {@code playedAt} from system zone to Toronto time via
     *       {@link #toTorontoTime(LocalDateTime)}.</li>
     *   <li>Partition into {@code todayPlays} and {@code weeklyPlays} using
     *       Toronto-time boundaries.</li>
     *   <li>Derive all stats from those in-memory sublists. No extra queries.</li>
     *   <li>Session duration read from {@link UserSessionService#getSessionDurationSeconds()}
     *       No DB call, in-memory subtraction (now - sessionStart).</li>
     * </ol>
     *
     * @return a fully populated {@link StatSnapshot}; never {@code null}
     */
    public StatSnapshot buildStatSnapshot() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return emptySnapshot();

        List<PlaybackHistory> allPlays =
                playbackHistoryRepository.findByUserIdOrderByPlayedAtDesc(userId);

        if (allPlays.isEmpty()) return emptySnapshot();

        // All boundaries are derived from Toronto time, not system/UTC time.
        // LocalDate.now(TORONTO) returns the current calendar date as seen in
        // Toronto, regardless of what zone the server runs in.
        LocalDate today          = LocalDate.now(TORONTO);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd   = today.atTime(23, 59, 59);

        // Week runs Sunday → Saturday (US locale)
        LocalDate weekStart       = today.minusDays(
                today.get(WeekFields.of(Locale.US).dayOfWeek()) - 1);
        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt   = weekStart.plusDays(6).atTime(23, 59, 59);

        // Convert each stored timestamp to Toronto time before comparing
        List<PlaybackHistory> todayPlays = allPlays.stream()
                .filter(h -> inRange(toTorontoTime(h.getPlayedAt()), todayStart, todayEnd))
                .collect(Collectors.toList());

        List<PlaybackHistory> weeklyPlays = allPlays.stream()
                .filter(h -> inRange(toTorontoTime(h.getPlayedAt()), weekStartDt, weekEndDt))
                .collect(Collectors.toList());

        return new StatSnapshot(
                computeMinutes(todayPlays),
                userSessionService.getSessionDurationSeconds(), // in-memory, no DB call
                computeTopSong(todayPlays),
                computeTopAlbum(todayPlays),
                computeTopSong(weeklyPlays),
                computeTopAlbum(weeklyPlays)
        );
    }

    /**
     * Returns a zeroed-out {@link StatSnapshot} used when no user is logged in
     * or when the playback history is empty.
     *
     * @return a {@link StatSnapshot} with all numeric fields set to zero and
     *         all entity references set to {@code null}
     */
    private static StatSnapshot emptySnapshot() {
        return new StatSnapshot(0, 0, null, null, null, null);
    }




    /**
     * Computes total valid listening time in minutes from a list of playback records.
     *
     * <p>Only plays passing {@link PlaybackConstants#isValidPlay(Integer)} are
     * included. Duration is truncated to minutes via integer division.
     *
     * <p>Time Complexity: O(n) where n = plays.size()
     *
     * @param plays the list of playback records to sum
     * @return total listening time in minutes
     */
    private int computeMinutes(List<PlaybackHistory> plays) {
        return plays.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .mapToInt(PlaybackHistory::getDurationPlayedSeconds)
                .sum() / 60;
    }

    /**
     * Computes the average valid play duration in seconds from a list of
     * playback records.
     *
     * <p>Only plays passing {@link PlaybackConstants#isValidPlay(Integer)} are
     * included. Returns zero if no valid plays exist.
     *
     * <p>Time Complexity: O(n) where n = plays.size()
     *
     * @param plays the list of playback records to average
     * @return average play duration in seconds, or {@code 0} if no valid plays
     */
    private int computeAvgSeconds(List<PlaybackHistory> plays) {
        List<Integer> valid = plays.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .map(PlaybackHistory::getDurationPlayedSeconds)
                .collect(Collectors.toList());
        if (valid.isEmpty()) return 0;
        return valid.stream().mapToInt(Integer::intValue).sum() / valid.size();
    }

    /**
     * Returns the most frequently played {@link Song} from a list of playback
     * records, counting only valid plays.
     *
     * <p>Time Complexity: O(n) where n = plays.size()
     *
     * @param plays the list of playback records to analyze
     * @return the top {@link Song} by play count, or {@code null} if none
     */
    private Song computeTopSong(List<PlaybackHistory> plays) {
        return plays.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .collect(Collectors.groupingBy(PlaybackHistory::getSong, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns the most frequently played {@link Album} from a list of playback
     * records, counting only valid plays. Records with a {@code null} album are excluded.
     *
     * <p>Time Complexity: O(n) where n = plays.size()
     *
     * @param plays the list of playback records to analyze
     * @return the top {@link Album} by play count, or {@code null} if none
     */
    private Album computeTopAlbum(List<PlaybackHistory> plays) {
        return plays.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .map(h -> h.getSong().getAlbum())
                .filter(a -> a != null)
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns the most frequently credited {@link Artist} from a list of
     * playback records, counting only valid plays. Multi-artist songs
     * contribute a count to each of their artists.
     *
     * <p>Time Complexity: O(n * a) where n = plays.size() and a = average artists per song.
     *
     * @param plays the list of playback records to analyze
     * @return the top {@link Artist} by credited play count, or {@code null} if none
     */
    private Artist computeTopArtist(List<PlaybackHistory> plays) {
        return plays.stream()
                .filter(h -> PlaybackConstants.isValidPlay(h.getDurationPlayedSeconds()))
                .flatMap(h -> h.getSong().getArtists().stream())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    /**
     * Converts a stored {@code playedAt} value to Toronto local time.
     *
     * <p>Why this is necessary:
     * {@code playedAt} is recorded with {@code LocalDateTime.now()} which uses
     * the JVM system clock and carries no timezone. On a UTC server a play at
     * 23:30 Toronto time is stored as 04:30 the next UTC day. To filter by
     * Toronto calendar date we must re-attach the system zone and then shift
     * to Toronto before comparing against Toronto-time boundaries.
     *
     * <p>Conversion chain:
     * <pre>
     *   stored (no zone) atZone(SYSTEM) re-attach the zone it was recorded in
     *    withZoneSameInstant(TORONTO) shift to Toronto, same instant
     *    toLocalDateTime() strip zone for plain comparison
     * </pre>
     *
     * Time Complexity: O(1)
     *
     * @param stored the {@code playedAt} value as written by the server
     * @return the equivalent Toronto wall-clock time
     */
    private LocalDateTime toTorontoTime(LocalDateTime stored) {
        return stored.atZone(SYSTEM)
                .withZoneSameInstant(TORONTO)
                .toLocalDateTime();
    }

    /**
     * Returns {@code true} if {@code t} falls within [{@code start}, {@code end}] inclusive.
     *
     * <p>Time Complexity: O(1)
     *
     * @param t     the timestamp to test
     * @param start the inclusive lower bound
     * @param end   the inclusive upper bound
     * @return {@code true} if {@code t} is within the range
     */
    private boolean inRange(LocalDateTime t, LocalDateTime start, LocalDateTime end) {
        return !t.isBefore(start) && !t.isAfter(end);
    }
}