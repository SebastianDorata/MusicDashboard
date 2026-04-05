package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.MonthlyReportData;
import com.sebastiandorata.musicdashboard.entity.MonthlyReport;
import com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.MonthlyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
/**
 * Loads monthly report data asynchronously for a given year and month.
 *
 * <p>Delegates report generation to
 * {@link MonthlyReportService}
 * and maps the result to a {@link MonthlyReportData}
 * display record delivered to the JavaFX thread.</p>
 */
@Service
public class MonthlyReportViewModel {

    @Autowired
    private MonthlyReportService monthlyReportService;

    @Autowired
    private DataLoadingService dataLoadingService;

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