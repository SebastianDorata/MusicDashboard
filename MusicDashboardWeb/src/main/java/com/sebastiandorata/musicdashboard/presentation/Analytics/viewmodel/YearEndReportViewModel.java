package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import com.sebastiandorata.musicdashboard.service.handlers.YearEndReportService;
import com.sebastiandorata.musicdashboard.utils.ArtistUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the Year-End report view.
 *
 * <p>Builds all data needed to render the full wrapped experience in a single
 * call: the persisted YearEndReport summary, top-5 songs, top-5 artists, and
 * the month-by-month listening breakdown.</p>
 *
 * <p>Time Complexity : O(n). Four queries that all share the same playback
 * history scan inside YearEndReportService.
 * Space Complexity: O(n) for the returned lists.</p>
 */
@Service
public class YearEndReportViewModel {

    @Autowired private YearEndReportService yearEndReportService;

    /** All data needed to render one year's wrapped screen. */
    public static class WrappedData {
        /** The calendar year summarised. */
        public final int    year;
        /** Total listening time in the year, in minutes. */
        public final int    totalMinutes;
        /** Total songs played in the year. */
        public final int    totalSongs;
        /** Title of the top song, or "—". */
        public final String topSongTitle;
        /** Primary artist of the top song, or "—". */
        public final String topSongArtist;
        /** Name of the top artist, or "—". */
        public final String topArtistName;
        /** Title of the top album, or "—". */
        public final String topAlbumTitle;
        /** Name of the top genre, or "—". */
        public final String topGenreName;

        /** The top songs with their play counts. */
        public final List<Map.Entry<Song, Long>>   topSongs;
        /** The top artists with their play counts. */
        public final List<Map.Entry<Artist, Long>> topArtists;
        /** Listening minutes keyed by month; months with no activity are absent. */
        public final Map<YearMonth, Integer> monthlyMinutes;

        /**
         * Creates the wrapped-data bundle for one year.
         *
         * @param year           the calendar year summarised
         * @param totalMinutes   total listening time in minutes
         * @param totalSongs     total songs played
         * @param topSongTitle   title of the top song, or "—"
         * @param topSongArtist  primary artist of the top song, or "—"
         * @param topArtistName  name of the top artist, or "—"
         * @param topAlbumTitle  title of the top album, or "—"
         * @param topGenreName   name of the top genre, or "—"
         * @param topSongs       the top songs with their play counts
         * @param topArtists     the top artists with their play counts
         * @param monthlyMinutes listening minutes keyed by month
         */
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

        /**
         * Formats the total listening time as hours with one decimal place.
         *
         * @return the total listening time in hours, for example "42.5"
         */
        public String totalHoursFormatted() {
            double hours = totalMinutes / 60.0;
            return String.format("%.1f", hours);
        }

        /**
         * Returns the full display name of the month with the most listening time.
         *
         * @return the localised month name, or "—" if there is no monthly data
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
     * Returns the wrapped (year-end) data for one year, synchronously.
     *
     * <p>Time Complexity: O(n). Four service calls that share one playback history scan.</p>
     * <p>Space Complexity: O(n) for the returned lists.</p>
     *
     * @param year the calendar year to summarise
     * @return the full wrapped data for the year
     */
    public WrappedData getWrappedData(int year) {
        return buildWrappedData(year);
    }

    private WrappedData buildWrappedData(int year) {
        YearEndReport report    = yearEndReportService.getOrGenerateYearReport(year);
        List<Map.Entry<Song,   Long>> topSongs   = yearEndReportService.getTopSongsForYear(year, 5);
        List<Map.Entry<Artist, Long>> topArtists = yearEndReportService.getTopArtistsForYear(year, 5);
        Map<YearMonth, Integer>       monthly    = yearEndReportService.getMonthlyListeningTime(year);

        int totalMinutes = nvl(report.getTotalListeningTimeMinutes());
        int totalSongs   = nvl(report.getTotalSongsPlayed());

        String topSongTitle  = report.getTopSong()   != null ? report.getTopSong().getTitle()   : "—";
        // Replaced multi-line stream with ArtistUtils
        String topSongArtist = ArtistUtils.getPrimaryArtistName(report.getTopSong());
        if (report.getTopSong() == null) topSongArtist = "—";

        String topArtist = report.getTopArtist() != null
                ? ArtistUtils.getArtistName(report.getTopArtist()) : "—";
        String topAlbum  = report.getTopAlbum()  != null ? report.getTopAlbum().getTitle()  : "—";
        String topGenre  = report.getTopGenre()  != null ? report.getTopGenre().getName()   : "—";

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
