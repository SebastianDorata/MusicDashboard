package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.dto.AllTimeStatsData;
import com.sebastiandorata.musicdashboard.dto.ChartData;
import com.sebastiandorata.musicdashboard.dto.StatCardsData;
import com.sebastiandorata.musicdashboard.dto.TopArtistRowData;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.PlaybackHistory;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.StatCardsViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopArtistsViewModel;
import com.sebastiandorata.musicdashboard.presentation.graph.ChartViewModel;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Serves the My Dashboard page and the JSON endpoints that refresh its panels.
 *
 * <p>The desktop {@code DashboardController} assembled a stat card row, a Recently Played panel,
 * a Top 5 Artists panel and a line chart around the large player. On the web the page itself
 * ({@code GET /dashboard}) is rendered with the initial data already in the model, so it is complete
 * without JavaScript; {@code dashboard.js} then calls the {@code /api/dashboard/**} endpoints to
 * redraw the chart and to refresh the panels whenever the playing song changes.</p>
 *
 * <p>SRP: only maps routes and flattens view-model results into JSON-friendly records; all data
 * is computed by {@link StatCardsViewModel}, {@link TopArtistsViewModel}, {@link ChartViewModel}
 * and {@link PlaybackTrackingService}.</p>
 *
 * <p>DIP: depends on those services, never on repositories. Entities are never returned from the
 * JSON endpoints; they are mapped to the records below.</p>
 */
@Controller
public class DashboardController {

    /** Number of rows the Recently Played panel shows (desktop {@code MAX_RECENT_ITEMS}). */
    private static final int MAX_RECENT_ITEMS = 10;

    /** Number of rows the Top Artists panel shows. */
    private static final int TOP_ARTISTS_LIMIT = 5;

    @Autowired private StatCardsViewModel statCardsViewModel;
    @Autowired private TopArtistsViewModel topArtistsViewModel;
    @Autowired private ChartViewModel chartViewModel;
    @Autowired private PlaybackTrackingService playbackTrackingService;
    @Autowired private UserSessionService userSessionService;

    /**
     * One clickable stat-card label (a top song or a top album).
     *
     * @param name    the display name, or "—" when there is none
     * @param songId  id of the song to play on double-click; {@code null} for album labels or when absent
     * @param albumId id of the album to open in My Library; {@code null} when there is no album
     */
    public record StatLabel(String name, Long songId, Long albumId) {
    }

    /**
     * The five dashboard stat cards.
     *
     * @param playbackValue formatted value of today's playback duration
     * @param playbackUnit  "Hours" or "Minutes"
     * @param avgValue      formatted value of the average session length
     * @param avgUnit       "m" or "s"
     * @param todaySong     the Top Song Today label
     * @param todayAlbum    the Top Album Today label
     * @param weeklySong    the Top Song This Week label
     * @param weeklyAlbum   the Top Album This Week label
     */
    public record StatCardsJson(String playbackValue, String playbackUnit, String avgValue, String avgUnit,
                                StatLabel todaySong, StatLabel todayAlbum,
                                StatLabel weeklySong, StatLabel weeklyAlbum) {
    }

    /**
     * One Recently Played row.
     *
     * @param songId       id of the song to play
     * @param title        the song title ("Unknown" when the row has no song)
     * @param relativeTime text such as "5m ago" or "Yesterday"
     */
    public record RecentRow(Long songId, String title, String relativeTime) {
    }

    /**
     * One Top Artists row.
     *
     * @param artistId id of the artist to open in My Library; {@code null} for placeholder rows
     * @param name     the artist name, or "—" for placeholder rows
     * @param time     formatted listening time, or "—" for placeholder rows
     */
    public record TopArtistRow(Long artistId, String name, String time) {
    }

    /**
     * One line of the chart.
     *
     * @param year   the calendar year the line shows
     * @param role   "current", "previous" or "highest" (decides the colour)
     * @param points exactly 12 values (January to December) in hours; {@code null} for months without data
     */
    public record ChartSeries(int year, String role, List<Double> points) {
    }

    /**
     * The all-time statistics shown beside the chart.
     *
     * @param totalAllTimeHours  total listening hours across every year
     * @param hasPreviousYear    whether the year before the selected one has data
     * @param deltaVsPrevious    hours difference to the previous year (meaningful only when {@code hasPreviousYear})
     * @param deltaVsAllTimeHigh hours difference to the all-time high year
     */
    public record AllTimeStatsJson(int totalAllTimeHours, boolean hasPreviousYear,
                                   int deltaVsPrevious, int deltaVsAllTimeHigh) {
    }

    /**
     * The chart payload: series, year metadata and the all-time stats for the selected year.
     *
     * @param selectedYear          the year shown as the current line
     * @param previousYear          the year before {@code selectedYear}
     * @param highestYear           the all-time highest year, or {@code null}
     * @param hasPreviousYear       whether a previous-year line is drawn
     * @param isPreviousYearHighest whether the previous year is also the highest year
     * @param years                 every year with data plus the current calendar year, newest first
     * @param series                the lines to draw, in order current, previous, highest
     * @param stats                 the all-time statistics for the selected year
     */
    public record ChartJson(int selectedYear, int previousYear, Integer highestYear, boolean hasPreviousYear,
                            boolean isPreviousYearHighest, List<Integer> years, List<ChartSeries> series,
                            AllTimeStatsJson stats) {
    }

    /**
     * Renders the dashboard with its initial data in the model.
     *
     * <p>Model attributes: {@code stats} ({@link StatCardsJson}), {@code recent} (list of
     * {@link RecentRow}), {@code topArtists} (list of {@link TopArtistRow}), {@code years}
     * (selectable chart years, newest first), {@code selectedYear} and {@code allTime}
     * ({@link AllTimeStatsJson} for the selected year).</p>
     *
     * @param model the MVC model to fill
     * @return the {@code dashboard} template name
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        int year = LocalDate.now().getYear();
        model.addAttribute("stats", buildStats());
        model.addAttribute("recent", buildRecent());
        model.addAttribute("topArtists", buildTopArtists());
        model.addAttribute("years", selectableYears());
        model.addAttribute("selectedYear", year);
        model.addAttribute("allTime", toAllTime(chartViewModel.getAllTimeStatsForGraph(year)));
        return "dashboard";
    }

    /**
     * Returns the five stat cards.
     *
     * @return the current stat card values
     */
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public StatCardsJson stats() {
        return buildStats();
    }

    /**
     * Returns the Recently Played rows (the newest {@value #MAX_RECENT_ITEMS}).
     *
     * @return the rows, newest first
     */
    @GetMapping("/api/dashboard/recent")
    @ResponseBody
    public List<RecentRow> recent() {
        return buildRecent();
    }

    /**
     * Returns the Top 5 Artists All-Time rows, padded with "—" placeholders.
     *
     * @return exactly five rows
     */
    @GetMapping("/api/dashboard/top-artists")
    @ResponseBody
    public List<TopArtistRow> topArtists() {
        return buildTopArtists();
    }

    /**
     * Returns the chart series and all-time stats for one year.
     *
     * <p>Time Complexity: O(y + n) where y = years with history, n = playback rows.</p>
     * <p>Space Complexity: O(1): at most three series of twelve values.</p>
     *
     * @param year the selected year; defaults to the current calendar year
     * @return the chart payload
     * @throws ResponseStatusException {@code BAD_REQUEST} when the year is outside 1900-2200
     */
    @GetMapping("/api/dashboard/chart")
    @ResponseBody
    public ChartJson chart(@RequestParam(required = false) Integer year) {
        int selected = year != null ? year : LocalDate.now().getYear();
        if (selected < 1900 || selected > 2200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year: " + selected);
        }
        ChartData data = chartViewModel.getChartData(selected);

        List<ChartSeries> series = new ArrayList<>();
        series.add(new ChartSeries(selected, "current", twelveMonths(data.currentYearData, selected, true)));
        if (data.hasPreviousYear) {
            series.add(new ChartSeries(data.previousYear, "previous",
                    twelveMonths(data.previousYearData, data.previousYear, false)));
        }
        if (!data.isPreviousYearHighest && data.highestYear != null && !data.highestYear.equals(selected)) {
            series.add(new ChartSeries(data.highestYear, "highest",
                    twelveMonths(data.highestYearData, data.highestYear, false)));
        }
        AllTimeStatsJson stats = toAllTime(chartViewModel.getAllTimeStatsForGraph(selected));
        return new ChartJson(selected, data.previousYear, data.highestYear, data.hasPreviousYear,
                data.isPreviousYearHighest, selectableYears(), series, stats);
    }

    /**
     * Maps the view model's stat card data to the JSON record.
     *
     * @return the stat cards
     */
    private StatCardsJson buildStats() {
        StatCardsData d = statCardsViewModel.getStatCardsData();
        return new StatCardsJson(d.playbackDurationValue(), d.playbackDurationUnit(),
                d.averageSessionValue(), d.averageSessionUnit(),
                songLabel(d.todayTopSongName(), d.todayTopSong()),
                albumLabel(d.todayTopAlbumName(), d.todayTopAlbum()),
                songLabel(d.weeklyTopSongName(), d.weeklyTopSong()),
                albumLabel(d.weeklyTopAlbumName(), d.weeklyTopAlbum()));
    }

    /**
     * Builds a song label; it is inert (no ids) when the song is absent.
     *
     * @param name the display name
     * @param song the song, may be {@code null}
     * @return the label
     */
    private StatLabel songLabel(String name, Song song) {
        if (song == null) return new StatLabel(name, null, null);
        Album album = song.getAlbum();
        return new StatLabel(name, song.getSongID(), album != null ? album.getAlbumId() : null);
    }

    /**
     * Builds an album label; it is inert (no ids) when the album is absent.
     *
     * @param name  the display name
     * @param album the album, may be {@code null}
     * @return the label
     */
    private StatLabel albumLabel(String name, Album album) {
        if (album == null) return new StatLabel(name, null, null);
        return new StatLabel(name, null, album.getAlbumId());
    }

    /**
     * Loads the current user's newest plays, limited to {@value #MAX_RECENT_ITEMS}.
     *
     * @return the rows, newest first; empty without a signed-in user
     */
    private List<RecentRow> buildRecent() {
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) return List.of();
        List<RecentRow> rows = new ArrayList<>();
        for (PlaybackHistory h : playbackTrackingService.getRecentlyPlayed(userId)) {
            if (rows.size() >= MAX_RECENT_ITEMS) break;
            Song song = h.getSong();
            rows.add(new RecentRow(song != null ? song.getSongID() : null,
                    song != null ? song.getTitle() : "Unknown",
                    AppUtils.formatRelativeTime(h.getPlayedAt())));
        }
        return rows;
    }

    /**
     * Loads the top artists panel rows.
     *
     * @return exactly {@value #TOP_ARTISTS_LIMIT} rows
     */
    private List<TopArtistRow> buildTopArtists() {
        List<TopArtistRow> rows = new ArrayList<>();
        for (TopArtistRowData r : topArtistsViewModel.getTopArtistsData(TOP_ARTISTS_LIMIT)) {
            rows.add(new TopArtistRow(r.artist != null ? r.artist.getArtistId() : null,
                    r.artistName, r.listeningTime));
        }
        return rows;
    }

    /**
     * Lists the years offered by the chart's year selector.
     *
     * @return every year with history plus the current calendar year, newest first
     */
    private List<Integer> selectableYears() {
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        years.addAll(chartViewModel.getAllAvailableYears());
        years.add(LocalDate.now().getYear());
        return new ArrayList<>(years);
    }

    /**
     * Flattens a sparse month map into twelve values in hours.
     *
     * <p>Mirrors the desktop series builder: months without data are {@code null}, and for the
     * current line in the running calendar year the current month onwards is left out because
     * it is incomplete.</p>
     *
     * @param minutes       minutes per month
     * @param year          the year the map belongs to
     * @param isCurrentLine whether this is the selected-year line
     * @return twelve values, January first
     */
    private List<Double> twelveMonths(Map<YearMonth, Integer> minutes, int year, boolean isCurrentLine) {
        LocalDate today = LocalDate.now();
        List<Double> out = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            if (isCurrentLine && year == today.getYear() && m >= today.getMonthValue()) {
                out.add(null);
                continue;
            }
            Integer value = minutes != null ? minutes.get(YearMonth.of(year, m)) : null;
            out.add(value != null ? value / 60.0 : null);
        }
        return out;
    }

    /**
     * Maps the all-time stats DTO to its JSON record.
     *
     * @param s the DTO
     * @return the record
     */
    private AllTimeStatsJson toAllTime(AllTimeStatsData s) {
        return new AllTimeStatsJson(s.totalAllTimeHours, s.hasPreviousYear,
                s.deltaVsPrevious != null ? s.deltaVsPrevious : 0,
                s.deltaVsAllTimeHigh != null ? s.deltaVsAllTimeHigh : 0);
    }
}
