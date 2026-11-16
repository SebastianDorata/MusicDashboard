package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.MonthlyReportData;
import com.sebastiandorata.musicdashboard.entity.MonthlyReport;
import com.sebastiandorata.musicdashboard.service.handlers.MonthlyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds monthly report data for a given year and month.
 *
 * <p>Delegates report generation to {@link MonthlyReportService} and maps the
 * result to a {@link MonthlyReportData} display record returned to the caller.</p>
 *
 * <p>SRP: this class only shapes report data for display; report generation
 * stays in {@link MonthlyReportService}.</p>
 */
@Service
public class MonthlyReportViewModel {

    @Autowired
    private MonthlyReportService monthlyReportService;

    /**
     * Returns the monthly report data for a given year and month, synchronously.
     *
     * <p>Time Complexity: O(n) where n = playback history rows for the month.</p>
     * <p>Space Complexity: O(1) beyond the report itself.</p>
     *
     * @param year  the calendar year of the report
     * @param month the month of the year (1-12)
     * @return the display record for the month
     */
    public MonthlyReportData getMonthlyReport(int year, int month) {
        return buildMonthlyReportData(year, month);
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
     * Returns the months of the given year that have listening data, synchronously.
     *
     * <p>Time Complexity: O(n) where n = playback history rows for the year.</p>
     * <p>Space Complexity: O(12) for the month list.</p>
     *
     * @param year the calendar year to inspect
     * @return the available month numbers (1-12) for the year
     */
    public List<Integer> getAvailableMonths(int year) {
        return monthlyReportService.getAvailableMonths(year);
    }
}
