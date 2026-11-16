package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.dto.HistoryRowData;
import com.sebastiandorata.musicdashboard.dto.MonthlyReportData;
import com.sebastiandorata.musicdashboard.dto.TopAlbumRowData;
import com.sebastiandorata.musicdashboard.dto.TopArtistRowData;
import com.sebastiandorata.musicdashboard.dto.TopSongRowData;
import com.sebastiandorata.musicdashboard.dto.WeeklyReportData;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.ListeningHistoryViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.MonthlyReportViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopAlbumsViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopArtistsViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopSongsViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.WeeklyReportViewModel;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.YearEndReportViewModel;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.service.handlers.AnalyticsCacheService;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Serves the "My Reports" (analytics) page and the JSON endpoints behind its "view all" modals.
 *
 * <p>The desktop controller kept the selected report, the year selector and the weekly calendar
 * month in fields and swapped the centre area in place. On the web that state lives in the URL:
 * {@code /analytics?report=history|weekly|monthly|yearly|songs|albums|artists} plus the optional
 * {@code year}, {@code month}, {@code cyear} (calendar year) and {@code week} parameters. Each
 * request renders exactly one section on the server with Thymeleaf fragments from
 * {@code templates/analytics/}.</p>
 *
 * <p>The four paginated modals (listening history, top songs, top albums, top artists) fetch
 * their pages from {@code GET /api/analytics/{history|songs|albums|artists}?offset=&limit=},
 * which return a {@link PageResult} of small records, never entities.</p>
 *
 * <p>SRP: request mapping and mapping of view-model DTOs to view records; all data comes from the
 * existing view models and {@link AnalyticsCacheService}.</p>
 * <p>DIP: depends on the view-model beans, not on repositories.</p>
 * <p>Time Complexity: O(n) per request, where n = playback history rows scanned by the view model.</p>
 * <p>Space Complexity: O(p) for the rows of the page or preview that is rendered.</p>
 */
@Controller
public class AnalyticsController {

    /** Number of rows each preview section shows (the desktop used 20 everywhere). */
    private static final int PREVIEW_ROWS = 20;
    /** Largest page size a JSON endpoint accepts. */
    private static final int MAX_LIMIT = 100;
    /** Report keys the page understands, in sidebar order. */
    private static final Set<String> REPORTS =
            Set.of("history", "weekly", "monthly", "yearly", "songs", "albums", "artists");
    /** Short month names used on the monthly selector buttons and the bar chart. */
    private static final String[] MONTH_ABBR =
            {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    /** Medal labels of the wrapped rank lists. */
    private static final String[] MEDALS = {"🥇", "🥈", "🥉", "  4.", "  5."};
    /** Height in pixels of the tallest wrapped bar. */
    private static final int MAX_BAR_PX = 190;

    @Autowired private UserSessionService userSessionService;
    @Autowired private WeeklyReportViewModel weeklyReportViewModel;
    @Autowired private MonthlyReportViewModel monthlyReportViewModel;
    @Autowired private YearEndReportViewModel yearEndReportViewModel;
    @Autowired private ListeningHistoryViewModel listeningHistoryViewModel;
    @Autowired private TopSongsViewModel topSongsViewModel;
    @Autowired private TopAlbumsViewModel topAlbumsViewModel;
    @Autowired private TopArtistsViewModel topArtistsViewModel;
    @Autowired private AnalyticsCacheService analyticsCacheService;

    // ── Records ────────────────────────────────────────────────────

    /**
     * One entry of the analytics sidebar.
     *
     * @param key   the {@code report} URL parameter value
     * @param label the button text
     */
    public record NavEntry(String key, String label) {}

    /**
     * A page of rows returned by the paginated JSON endpoints.
     *
     * @param total  the number of rows across all pages
     * @param offset the zero-based index of the first row of this page
     * @param limit  the requested page size
     * @param items  the rows of this page
     * @param <T>    the row type
     */
    public record PageResult<T>(int total, int offset, int limit, List<T> items) {}

    /**
     * A listening-history row.
     *
     * @param songId     the played song's id, or {@code null} for an unknown song
     * @param songTitle  the song title ("Unknown" when the song is gone)
     * @param artistName the primary artist name
     * @param playedAt   the relative time text such as "3m ago"
     */
    public record HistoryItem(Long songId, String songTitle, String artistName, String playedAt) {}

    /**
     * A top-songs row.
     *
     * @param songId     the song id
     * @param songTitle  the song title
     * @param artistName the primary artist name
     */
    public record SongItem(Long songId, String songTitle, String artistName) {}

    /**
     * A top-albums row.
     *
     * @param albumId     the album id
     * @param albumTitle  the album title
     * @param releaseYear the release year text, or "Unknown"
     */
    public record AlbumItem(Long albumId, String albumTitle, String releaseYear) {}

    /**
     * A top-artists row.
     *
     * @param artistId      the artist id, or {@code null} for a placeholder row
     * @param artistName    the artist name ("—" for placeholders, empty for unplayed artists)
     * @param listeningTime the formatted listening time such as "2h 15m"
     */
    public record ArtistItem(Long artistId, String artistName, String listeningTime) {}

    /**
     * The data of one weekly or monthly report card.
     *
     * @param periodLabel  the card heading ("Week 12" or the upper-case month name)
     * @param totalSongs   the number of songs played
     * @param totalMinutes the listening minutes
     * @param topSong      the top song name
     * @param topArtist    the top artist name
     * @param topAlbum     the top album name
     * @param topGenre     the top genre name
     */
    public record ReportCard(String periodLabel, int totalSongs, int totalMinutes,
                             String topSong, String topArtist, String topAlbum, String topGenre) {}

    /**
     * One cell of the weekly calendar grid.
     *
     * @param day  the day of the month, or 0 for a blank leading cell
     * @param week the US week-of-year the day belongs to (0 for blank cells)
     */
    public record CalendarCell(int day, int week) {}

    /**
     * The weekly calendar for one month.
     *
     * @param label     the heading, e.g. {@code NOVEMBER 2026}
     * @param prevMonth the month number of the previous-month link
     * @param prevYear  the calendar year of the previous-month link
     * @param nextMonth the month number of the next-month link
     * @param nextYear  the calendar year of the next-month link
     * @param cells     the grid cells in row-major order, seven per row
     */
    public record CalendarView(String label, int prevMonth, int prevYear,
                               int nextMonth, int nextYear, List<CalendarCell> cells) {}

    /**
     * One monthly selector button.
     *
     * @param month the month number 1-12
     * @param label the three-letter upper-case label
     */
    public record MonthButton(int month, String label) {}

    /**
     * One row of a wrapped top-5 list.
     *
     * @param medal    the medal or rank text
     * @param title    the main text
     * @param subtitle the second line, or an empty string
     * @param plays    the play count
     */
    public record RankRow(String medal, String title, String subtitle, long plays) {}

    /**
     * One bar of the wrapped monthly chart.
     *
     * @param month    the three-letter month label
     * @param value    the value label above the bar ("" for no listening)
     * @param heightPx the bar height in pixels
     * @param peak     whether this is the busiest month (drawn green)
     */
    public record Bar(String month, String value, int heightPx, boolean peak) {}

    // ── Page ───────────────────────────────────────────────────────

    /**
     * Renders the My Reports page with the selected section.
     *
     * <p>Time Complexity: O(n) where n = playback history rows scanned by the view model.</p>
     * <p>Space Complexity: O(1) beyond the rendered rows.</p>
     *
     * @param report the section key; unknown values fall back to {@code history}
     * @param year   the year selected in the sidebar (current year when absent or not offered)
     * @param month  the weekly calendar month, or the monthly report month
     * @param cyear  the weekly calendar year (defaults to {@code year}; changes when paging months across a year)
     * @param week   the selected week of the year for the weekly report
     * @param model  the view model
     * @return the {@code analytics} template name
     */
    @GetMapping("/analytics")
    public String analytics(@RequestParam(defaultValue = "history") String report,
                            @RequestParam(required = false) Integer year,
                            @RequestParam(required = false) Integer month,
                            @RequestParam(required = false) Integer cyear,
                            @RequestParam(required = false) Integer week,
                            Model model) {
        String view = REPORTS.contains(report) ? report : "history";
        int current = Year.now().getValue();
        List<Integer> years = new ArrayList<>();
        for (int i = current; i >= current - 6; i--) years.add(i);
        int selectedYear = year != null && years.contains(year) ? year : current;

        model.addAttribute("report", view);
        model.addAttribute("year", selectedYear);
        model.addAttribute("years", years);
        model.addAttribute("nav", List.of(
                new NavEntry("history", "Listening History"),
                new NavEntry("weekly", "Weekly Summary"),
                new NavEntry("monthly", "Monthly Summary"),
                new NavEntry("yearly", "Yearly Wrapped"),
                new NavEntry("songs", "Top Songs"),
                new NavEntry("albums", "Top Albums"),
                new NavEntry("artists", "Top Artists")));

        switch (view) {
            case "weekly" -> prepareWeekly(model, selectedYear, month, cyear, week);
            case "monthly" -> prepareMonthly(model, selectedYear, month);
            case "yearly" -> prepareYearly(model, selectedYear);
            case "songs" -> model.addAttribute("songRows",
                    topSongsViewModel.getTopSongsWindow(0, PREVIEW_ROWS).stream().map(this::toItem).toList());
            case "albums" -> model.addAttribute("albumRows",
                    topAlbumsViewModel.getTopAlbumsWindow(0, PREVIEW_ROWS).stream().map(this::toItem).toList());
            case "artists" -> model.addAttribute("artistRows",
                    topArtistsViewModel.getTopArtistsData(PREVIEW_ROWS).stream().map(this::toItem).toList());
            default -> model.addAttribute("historyRows",
                    listeningHistoryViewModel.getHistoryWindow(0, PREVIEW_ROWS).stream().map(this::toItem).toList());
        }
        return "analytics";
    }

    /**
     * Adds the weekly calendar and, when a week is selected, its report card to the model.
     *
     * @param model  the view model
     * @param year   the report year
     * @param month  the requested calendar month, or {@code null} for the current month
     * @param cyear  the requested calendar year, or {@code null} for the report year
     * @param week   the selected week, or {@code null}
     */
    private void prepareWeekly(Model model, int year, Integer month, Integer cyear, Integer week) {
        int m = month != null && month >= 1 && month <= 12 ? month : LocalDate.now().getMonthValue();
        int cy = cyear != null && cyear >= 1900 && cyear <= 2200 ? cyear : year;
        model.addAttribute("month", m);
        model.addAttribute("calendar", buildCalendar(cy, m));
        model.addAttribute("calendarYear", cy);
        if (week != null && week >= 1 && week <= 53) {
            WeeklyReportData d = weeklyReportViewModel.getWeeklyReport(year, week);
            model.addAttribute("card", new ReportCard("Week " + d.week, d.totalSongs, d.totalMinutes,
                    d.topSongName, d.topArtistName, d.topAlbumName, d.topGenreName));
            model.addAttribute("selectedWeek", week);
        }
    }

    /**
     * Adds the month buttons and, when a month is selected, its report card to the model.
     *
     * @param model the view model
     * @param year  the report year
     * @param month the selected month, or {@code null}
     */
    private void prepareMonthly(Model model, int year, Integer month) {
        List<MonthButton> buttons = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            buttons.add(new MonthButton(i, Month.of(i).toString().substring(0, 3)));
        }
        model.addAttribute("monthButtons", buttons);
        if (month != null && month >= 1 && month <= 12) {
            MonthlyReportData d = monthlyReportViewModel.getMonthlyReport(year, month);
            model.addAttribute("card", new ReportCard(d.monthName, d.totalSongs, d.totalMinutes,
                    d.topSongName, d.topArtistName, d.topAlbumName, d.topGenreName));
            model.addAttribute("month", month);
        }
    }

    /**
     * Adds the wrapped data, its rank lists and its monthly bars to the model.
     *
     * <p>Time Complexity: O(n) for the view-model scan, O(1) for the 12 bars and 5-row lists.</p>
     *
     * @param model the view model
     * @param year  the wrapped year
     */
    private void prepareYearly(Model model, int year) {
        YearEndReportViewModel.WrappedData data = yearEndReportViewModel.getWrappedData(year);
        model.addAttribute("wrapped", data);

        List<RankRow> songs = new ArrayList<>();
        for (int i = 0; i < data.topSongs.size() && i < 5; i++) {
            Map.Entry<Song, Long> e = data.topSongs.get(i);
            songs.add(new RankRow(MEDALS[i], e.getKey().getTitle(),
                    ArtistUtils.getPrimaryArtistName(e.getKey()), e.getValue()));
        }
        model.addAttribute("topSongRanks", songs);

        List<RankRow> artists = new ArrayList<>();
        for (int i = 0; i < data.topArtists.size() && i < 5; i++) {
            Map.Entry<Artist, Long> e = data.topArtists.get(i);
            artists.add(new RankRow(MEDALS[i], e.getKey().getName(), "", e.getValue()));
        }
        model.addAttribute("topArtistRanks", artists);

        int max = data.monthlyMinutes.values().stream().mapToInt(v -> v).max().orElse(1);
        List<Bar> bars = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int minutes = data.monthlyMinutes.getOrDefault(YearMonth.of(year, m), 0);
            double h = Math.max(4, (max > 0 ? (double) minutes / max : 0) * MAX_BAR_PX);
            String value = minutes > 0 ? (minutes >= 60 ? (minutes / 60) + "h" : minutes + "m") : "";
            bars.add(new Bar(MONTH_ABBR[m - 1], value, (int) Math.round(h), minutes == max));
        }
        model.addAttribute("bars", bars);
        model.addAttribute("songsPerMonth", data.totalSongs / 12);
    }

    /**
     * Builds the weekly calendar grid for one month (weeks run Sunday to Saturday, US numbering).
     *
     * @param year  the calendar year
     * @param month the calendar month 1-12
     * @return the calendar view
     */
    private CalendarView buildCalendar(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        WeekFields weekFields = WeekFields.of(Locale.US);
        List<CalendarCell> cells = new ArrayList<>();
        int lead = ym.atDay(1).getDayOfWeek().getValue() % 7;
        for (int i = 0; i < lead; i++) cells.add(new CalendarCell(0, 0));
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            cells.add(new CalendarCell(d, ym.atDay(d).get(weekFields.weekOfYear())));
        }
        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);
        return new CalendarView(Month.of(month).toString() + " " + year,
                prev.getMonthValue(), prev.getYear(), next.getMonthValue(), next.getYear(), cells);
    }

    // ── JSON endpoints ─────────────────────────────────────────────

    /**
     * Returns one page of the current user's listening history.
     *
     * <p>Time Complexity: O(n) per request (the view model reads the user's history).</p>
     *
     * @param offset zero-based index of the first row
     * @param limit  page size, 1 to 100
     * @return the page
     * @throws ResponseStatusException 400 when offset or limit is out of range
     */
    @GetMapping("/api/analytics/history")
    @ResponseBody
    public PageResult<HistoryItem> history(@RequestParam(defaultValue = "0") int offset,
                                           @RequestParam(defaultValue = "25") int limit) {
        checkRange(offset, limit);
        return new PageResult<>(listeningHistoryViewModel.getHistoryCount(), offset, limit,
                listeningHistoryViewModel.getHistoryWindow(offset, limit).stream().map(this::toItem).toList());
    }

    /**
     * Returns one page of the current user's top songs.
     *
     * @param offset zero-based index of the first row
     * @param limit  page size, 1 to 100
     * @return the page
     * @throws ResponseStatusException 400 when offset or limit is out of range
     */
    @GetMapping("/api/analytics/songs")
    @ResponseBody
    public PageResult<SongItem> songs(@RequestParam(defaultValue = "0") int offset,
                                      @RequestParam(defaultValue = "10") int limit) {
        checkRange(offset, limit);
        return new PageResult<>(topSongsViewModel.getTopSongsCount(), offset, limit,
                topSongsViewModel.getTopSongsWindow(offset, limit).stream().map(this::toItem).toList());
    }

    /**
     * Returns one page of the current user's top albums.
     *
     * @param offset zero-based index of the first row
     * @param limit  page size, 1 to 100
     * @return the page
     * @throws ResponseStatusException 400 when offset or limit is out of range
     */
    @GetMapping("/api/analytics/albums")
    @ResponseBody
    public PageResult<AlbumItem> albums(@RequestParam(defaultValue = "0") int offset,
                                        @RequestParam(defaultValue = "10") int limit) {
        checkRange(offset, limit);
        return new PageResult<>(topAlbumsViewModel.getTopAlbumsCount(), offset, limit,
                topAlbumsViewModel.getTopAlbumsWindow(offset, limit).stream().map(this::toItem).toList());
    }

    /**
     * Returns one page of every artist ranked by the current user's listening time.
     *
     * <p>The full ranked list is cached per user for 60 seconds in {@link AnalyticsCacheService}
     * and paged in memory with {@link DoublyLinkedList#getWindow(int, int)}, as the desktop did.</p>
     *
     * <p>Time Complexity: O(n + a) on a cache miss, O(offset + limit) on a hit.</p>
     *
     * @param offset zero-based index of the first row
     * @param limit  page size, 1 to 100
     * @return the page
     * @throws ResponseStatusException 400 when offset or limit is out of range
     */
    @GetMapping("/api/analytics/artists")
    @ResponseBody
    public PageResult<ArtistItem> artists(@RequestParam(defaultValue = "0") int offset,
                                          @RequestParam(defaultValue = "20") int limit) {
        checkRange(offset, limit);
        Long userId = userSessionService.getCurrentUserId();
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not signed in");
        DoublyLinkedList<TopArtistRowData> all = analyticsCacheService.getOrLoad(
                userId, "topArtists", topArtistsViewModel::buildAllArtistsRankedData);
        return new PageResult<>(all.toList().size(), offset, limit,
                all.getWindow(offset, limit).stream().map(this::toItem).toList());
    }

    // ── Helpers ────────────────────────────────────────────────────

    /**
     * Validates paging parameters.
     *
     * @param offset zero-based start index
     * @param limit  page size
     * @throws ResponseStatusException 400 when offset is negative or limit is outside 1..100
     */
    private void checkRange(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "offset must be >= 0 and limit between 1 and " + MAX_LIMIT);
        }
    }

    /**
     * Maps a history DTO to a view record.
     *
     * @param h the DTO
     * @return the record (song id is {@code null} when the song is unknown)
     */
    private HistoryItem toItem(HistoryRowData h) {
        return new HistoryItem(h.song != null ? h.song.getSongID() : null, h.songTitle, h.artistName, h.playedAt);
    }

    /**
     * Maps a top-song DTO to a view record.
     *
     * @param s the DTO
     * @return the record
     */
    private SongItem toItem(TopSongRowData s) {
        return new SongItem(s.song != null ? s.song.getSongID() : null, s.songTitle, s.artistName);
    }

    /**
     * Maps a top-album DTO to a view record.
     *
     * @param a the DTO
     * @return the record
     */
    private AlbumItem toItem(TopAlbumRowData a) {
        return new AlbumItem(a.album != null ? a.album.getAlbumId() : null, a.albumTitle, a.releaseYear);
    }

    /**
     * Maps a top-artist DTO to a view record.
     *
     * @param a the DTO
     * @return the record (artist id is {@code null} for placeholder rows)
     */
    private ArtistItem toItem(TopArtistRowData a) {
        return new ArtistItem(a.artist != null ? a.artist.getArtistId() : null, a.artistName, a.listeningTime);
    }
}
