package com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.entity.MonthlyReport;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.MonthlyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.util.List;
import java.util.function.Consumer;

@Service
public class MonthlyReportViewModel {

    @Autowired
    private MonthlyReportService monthlyReportService;

    @Autowired
    private DataLoadingService dataLoadingService;

    public static class MonthlyReportData {
        public int month;
        public String monthName;
        public int totalSongs;
        public int totalMinutes;
        public String topSongName;
        public String topArtistName;
        public String topAlbumName;
        public String topGenreName;

        public MonthlyReportData(int month, int totalSongs, int totalMinutes,
                                 String topSongName, String topArtistName,
                                 String topAlbumName, String topGenreName) {
            this.month = month;
            this.monthName = Month.of(month).name();
            this.totalSongs = totalSongs;
            this.totalMinutes = totalMinutes;
            this.topSongName = topSongName;
            this.topArtistName = topArtistName;
            this.topAlbumName = topAlbumName;
            this.topGenreName = topGenreName;
        }
    }

    /**
     * Loads monthly report data asynchronously for a given year and month.
     * Time Complexity: O(n)
     */
    public void loadMonthlyReport(int year, int month, Consumer<MonthlyReportData> onSuccess) {
        dataLoadingService.loadAsync(() -> buildMonthlyReportData(year, month), onSuccess);
    }

    private MonthlyReportData buildMonthlyReportData(int year, int month) {
        MonthlyReport report = monthlyReportService.getOrGenerateMonthlyReport(year, month);

        return new MonthlyReportData(
                month,
                report.getTotalSongsPlayed() != null ? report.getTotalSongsPlayed() : 0,
                report.getTotalListeningTimeMinutes() != null ? report.getTotalListeningTimeMinutes() : 0,
                report.getTopSong() != null ? report.getTopSong().getTitle() : "—",
                report.getTopArtist() != null ? report.getTopArtist().getName() : "—",
                report.getTopAlbum() != null ? report.getTopAlbum().getTitle() : "—",
                report.getTopGenre() != null ? report.getTopGenre().getName() : "—"
        );
    }

    /**
     * Loads available months for a given year.
     * Time Complexity: O(n)
     */
    public void loadAvailableMonths(int year, Consumer<List<Integer>> onSuccess) {
        dataLoadingService.loadAsync(() -> monthlyReportService.getAvailableMonths(year), onSuccess);
    }
}