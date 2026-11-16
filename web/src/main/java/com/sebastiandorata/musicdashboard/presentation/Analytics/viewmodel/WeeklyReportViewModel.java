package com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel;

import com.sebastiandorata.musicdashboard.dto.WeeklyReportData;
import com.sebastiandorata.musicdashboard.entity.WeeklyReport;
import com.sebastiandorata.musicdashboard.service.handlers.WeeklyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds weekly report data for a given year and ISO week number.
 *
 * <p>Delegates report generation to {@link WeeklyReportService} and maps the
 * result to a {@link WeeklyReportData} display record returned to the caller.</p>
 *
 * <p>SRP: this class only shapes report data for display; report generation
 * stays in {@link WeeklyReportService}.</p>
 */
@Service
public class WeeklyReportViewModel {

    @Autowired
    private WeeklyReportService weeklyReportService;

    /**
     * Returns the weekly report data for a given year and week, synchronously.
     *
     * <p>Time Complexity: O(n) where n = playback history rows for the week.</p>
     * <p>Space Complexity: O(1) beyond the report itself.</p>
     *
     * @param year       the calendar year of the report
     * @param weekOfYear the ISO week number (1-53)
     * @return the display record for the week
     */
    public WeeklyReportData getWeeklyReport(int year, int weekOfYear) {
        return buildWeeklyReportData(year, weekOfYear);
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
     * Returns the ISO weeks of the given year that have listening data, synchronously.
     *
     * <p>Time Complexity: O(n) where n = playback history rows for the year.</p>
     * <p>Space Complexity: O(53) for the week list.</p>
     *
     * @param year the calendar year to inspect
     * @return the available ISO week numbers for the year
     */
    public List<Integer> getAvailableWeeks(int year) {
        return weeklyReportService.getAvailableWeeks(year);
    }
}
