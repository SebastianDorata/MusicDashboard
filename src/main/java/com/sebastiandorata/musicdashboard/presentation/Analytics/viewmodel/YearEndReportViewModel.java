package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.YearEndReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ViewModel for the Year-End report view.
 *
 * Loads all data needed to render the full wrapped experience in a single
 * async call: The persisted YearEndReport summary, top-5 songs, top-5
 * artists, and the month-by-month listening breakdown.
 *
 * <p>Time Complexity : O(n). Four queries that all share the same playback history scan inside YearEndReportService.
 * <p>Space Complexity: O(n) for the returned lists.
 */
@Service
public class YearEndReportViewModel {

    @Autowired
    private YearEndReportService yearEndReportService;

    @Autowired
    private DataLoadingService dataLoadingService;

    /** All data needed to render one year's wrapped screen. */
    public static class WrappedData {
        public final int    year;
        public final int    totalMinutes;
        public final int    totalSongs;
        public final String topSongTitle;
        public final String topSongArtist;
        public final String topArtistName;
        public final String topAlbumTitle;
        public final String topGenreName;

        public final List<Map.Entry<Song, Long>>   topSongs;
        public final List<Map.Entry<Artist, Long>> topArtists;
        public final Map<YearMonth, Integer> monthlyMinutes;

        public WrappedData(int year,
                           int totalMinutes, int totalSongs,
                           String topSongTitle, String topSongArtist,
                           String topArtistName, String topAlbumTitle,
                           String topGenreName,
                           List<Map.Entry<Song, Long>>   topSongs,
                           List<Map.Entry<Artist, Long>> topArtists,
                           Map<YearMonth, Integer>       monthlyMinutes) {
            this.year           = year;
            this.totalMinutes   = totalMinutes;
            this.totalSongs     = totalSongs;
            this.topSongTitle   = topSongTitle;
            this.topSongArtist  = topSongArtist;
            this.topArtistName  = topArtistName;
            this.topAlbumTitle  = topAlbumTitle;
            this.topGenreName   = topGenreName;
            this.topSongs       = topSongs;
            this.topArtists     = topArtists;
            this.monthlyMinutes = monthlyMinutes;
        }


        public String totalHoursFormatted() {
            double hours = totalMinutes / 60.0;
            return String.format("%.1f", hours);
        }

        /** Used to show the month with the most playback hours.
         * Busiest month name, or "—" if no data.
         */
        public String busiestMonthName() {
            return monthlyMinutes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey().getMonth()
                            .getDisplayName(java.time.format.TextStyle.FULL,
                                    java.util.Locale.getDefault()))
                    .orElse("—");
        }
    }

    /**
     * Loads all wrapped data for {@code year} on a background thread,
     * then calls {@code onSuccess} on the JavaFX thread.
     */
    public void loadWrappedData(int year, Consumer<WrappedData> onSuccess) {
        dataLoadingService.loadAsync(() -> buildWrappedData(year), onSuccess);
    }



    private WrappedData buildWrappedData(int year) {
        YearEndReport report    = yearEndReportService.getOrGenerateYearReport(year);
        List<Map.Entry<Song,   Long>> topSongs   = yearEndReportService.getTopSongsForYear(year, 5);
        List<Map.Entry<Artist, Long>> topArtists = yearEndReportService.getTopArtistsForYear(year, 5);
        Map<YearMonth, Integer>       monthly    = yearEndReportService.getMonthlyListeningTime(year);

        int    totalMinutes = nvl(report.getTotalListeningTimeMinutes());
        int    totalSongs   = nvl(report.getTotalSongsPlayed());

        String topSongTitle  = report.getTopSong()   != null ? report.getTopSong().getTitle()   : "—";
        String topSongArtist = (report.getTopSong()  != null
                && report.getTopSong().getArtists()  != null
                && !report.getTopSong().getArtists().isEmpty())
                ? report.getTopSong().getArtists().stream().findFirst()
    .map(Artist::getName).orElse("Unknown Artist") : "—";
        String topArtist     = report.getTopArtist() != null ? report.getTopArtist().getName()  : "—";
        String topAlbum      = report.getTopAlbum()  != null ? report.getTopAlbum().getTitle()  : "—";
        String topGenre      = report.getTopGenre()  != null ? report.getTopGenre().getName()   : "—";

        return new WrappedData(year,
                totalMinutes, totalSongs,
                topSongTitle, topSongArtist,
                topArtist, topAlbum, topGenre,
                topSongs, topArtists, monthly);
    }

    private static int nvl(Integer v) {
        return v != null ? v : 0;
    }
}