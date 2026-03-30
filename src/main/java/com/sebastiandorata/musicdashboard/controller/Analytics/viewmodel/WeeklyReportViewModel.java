package com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.entity.WeeklyReport;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.WeeklyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class WeeklyReportViewModel {

    @Autowired
    private WeeklyReportService weeklyReportService;

    @Autowired
    private DataLoadingService dataLoadingService;

    public static class WeeklyReportData {
        public int week;
        public int totalSongs;
        public int totalMinutes;
        public String topSongName;
        public String topArtistName;
        public String topAlbumName;
        public String topGenreName;

        public WeeklyReportData(int week, int totalSongs, int totalMinutes,
                                String topSongName, String topArtistName,
                                String topAlbumName, String topGenreName) {
            this.week = week;
            this.totalSongs = totalSongs;
            this.totalMinutes = totalMinutes;
            this.topSongName = topSongName;
            this.topArtistName = topArtistName;
            this.topAlbumName = topAlbumName;
            this.topGenreName = topGenreName;
        }
    }

    /**
     * Loads weekly report data asynchronously for a given year and week.
     * Time Complexity: O(n)
     */
    public void loadWeeklyReport(int year, int weekOfYear, Consumer<WeeklyReportData> onSuccess) {
        dataLoadingService.loadAsync(() -> buildWeeklyReportData(year, weekOfYear), onSuccess);
    }

    private WeeklyReportData buildWeeklyReportData(int year, int weekOfYear) {
        WeeklyReport report = weeklyReportService.getOrGenerateWeeklyReport(year, weekOfYear);

        return new WeeklyReportData(
                weekOfYear,
                report.getTotalSongsPlayed() != null ? report.getTotalSongsPlayed() : 0,
                report.getTotalListeningTimeMinutes() != null ? report.getTotalListeningTimeMinutes() : 0,
                report.getTopSong() != null ? report.getTopSong().getTitle() : "—",
                report.getTopArtist() != null ? report.getTopArtist().getName() : "—",
                report.getTopAlbum() != null ? report.getTopAlbum().getTitle() : "—",
                report.getTopGenre() != null ? report.getTopGenre().getName() : "—"
        );
    }

    /**
     * Loads available weeks for a given year.
     * Time Complexity: O(n)
     */
    public void loadAvailableWeeks(int year, Consumer<List<Integer>> onSuccess) {
        dataLoadingService.loadAsync(() -> weeklyReportService.getAvailableWeeks(year), onSuccess);
    }
}