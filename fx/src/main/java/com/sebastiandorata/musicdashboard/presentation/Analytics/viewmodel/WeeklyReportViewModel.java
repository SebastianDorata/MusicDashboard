package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.WeeklyReportData;
import com.sebastiandorata.musicdashboard.entity.WeeklyReport;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.handlers.WeeklyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
/**
 * Loads weekly report data asynchronously for a given year and ISO week number.
 *
 * <p>Delegates report generation to
 * {@link WeeklyReportService}
 * and maps the result to a {@link WeeklyReportData}
 * display record delivered to the JavaFX thread.</p>
 */
@Service
public class WeeklyReportViewModel {

    @Autowired
    private WeeklyReportService weeklyReportService;

    @Autowired
    private DataLoadingService dataLoadingService;

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