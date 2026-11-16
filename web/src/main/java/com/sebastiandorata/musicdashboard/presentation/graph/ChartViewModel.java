package com.sebastiandorata.musicdashboard.presentation.graph;

import com.sebastiandorata.musicdashboard.dto.AllTimeStatsData;
import com.sebastiandorata.musicdashboard.dto.ChartData;
import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import com.sebastiandorata.musicdashboard.service.handlers.YearEndReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the data series for the dashboard line chart.
 *
 * <p>Resolves the current, previous, and all-time-highest-year monthly
 * listening series in a single call, and separately computes all-time
 * aggregate statistics for the graph info panel. Delegates report
 * generation to {@link YearEndReportService}
 * and computation to helper methods, keeping the controller free of
 * domain logic.</p>
 *
 * <p>Time Complexity: O(y + n) where y = available years, n = total playback records<br>
 * Space Complexity: O(m) where m = 12 months per series × up to 3 series</p>
 */
@Service
public class ChartViewModel {

    @Autowired
    private YearEndReportService yearEndReportService;

    /**
     * Returns the monthly listening series for the selected year, the previous
     * year, and the all-time-highest year, in one call.
     *
     * <p>Time Complexity: O(y + n) where y = available years, n = total playback records</p>
     * <p>Space Complexity: O(m * s) where m = 12 months, s ≤ 3 series = O(1)</p>
     *
     * @param selectedYear the year shown on the chart
     * @return the chart series and the year metadata needed to label them
     */
    public ChartData getChartData(int selectedYear) {
        Integer previousYear = selectedYear - 1;
        List<Integer> allYears = yearEndReportService.getAvailableYears();
        Integer highestYear = findHighestListeningYear(allYears);

        boolean hasPreviousYear = allYears.contains(previousYear);
        boolean isPreviousYearHighest = hasPreviousYear && previousYear.equals(highestYear);

        Map<YearMonth, Integer> currentYearData =
                yearEndReportService.getMonthlyListeningTime(selectedYear);

        Map<YearMonth, Integer> previousYearData = new HashMap<>();
        if (hasPreviousYear) {
            previousYearData.putAll(
                    yearEndReportService.getMonthlyListeningTime(previousYear)
            );
        }

        Map<YearMonth, Integer> highestYearData = new HashMap<>();
        if (!isPreviousYearHighest && highestYear != null && !highestYear.equals(selectedYear)) {
            highestYearData.putAll(
                    yearEndReportService.getMonthlyListeningTime(highestYear)
            );
        }

        return new ChartData(
                currentYearData,
                previousYearData,
                highestYearData,
                selectedYear,
                previousYear,
                highestYear,
                allYears,
                hasPreviousYear,
                isPreviousYearHighest
        );
    }

    /**
     * Returns the all-time aggregate statistics for the graph info panel.
     *
     * <p>Time Complexity: O(y) where y = number of years in history</p>
     * <p>Space Complexity: O(1). Returns fixed data structure</p>
     *
     * @param selectedYear the year shown on the chart
     * @return the all-time totals and comparisons for the selected year
     */
    public AllTimeStatsData getAllTimeStatsForGraph(int selectedYear) {
        List<Integer> allYears = yearEndReportService.getAvailableYears();
        int totalAllTimeHours = 0;
        int currentYearHours = 0;
        int previousYearHours = 0;
        int highestYearHours = 0;
        Integer highestYear = null;

        for (Integer year : allYears) {
            YearEndReport report = yearEndReportService.getOrGenerateYearReport(year);
            if (report == null) continue;

            int yearHours = report.getTotalListeningTimeMinutes() / 60;
            totalAllTimeHours += yearHours;

            if (year == selectedYear) {
                currentYearHours = yearHours;
            }
            if (year == selectedYear - 1) {
                previousYearHours = yearHours;
            }

            if (yearHours > highestYearHours) {
                highestYearHours = yearHours;
                highestYear = year;
            }
        }

        Integer deltaVsPrevious = previousYearHours > 0 ?
                currentYearHours - previousYearHours : 0;
        Integer deltaVsAllTimeHigh = currentYearHours - highestYearHours;

        boolean hasPreviousYear = allYears.contains(selectedYear - 1);
        boolean isPreviousYearHighest = hasPreviousYear &&
                (selectedYear - 1 == highestYear);

        return new AllTimeStatsData(
                totalAllTimeHours,
                currentYearHours,
                previousYearHours,
                highestYearHours,
                highestYear,
                deltaVsPrevious,
                deltaVsAllTimeHigh,
                hasPreviousYear,
                isPreviousYearHighest
        );
    }

    /**
     * Retrieves all available years with listening data.
     *
     * Time Complexity: O(n) where n = playback history size
     * Space Complexity: O(y) where y = unique years
     *
     * Reserved for future features that may need year selection or filtering.
     * Current use: Informational for graph header labels.
     *
     * @return all years with listening data, as returned by {@code YearEndReportService#getAvailableYears()}
     */
    public List<Integer> getAllAvailableYears() {
        return yearEndReportService.getAvailableYears();
    }

    /**
     * Finds the year with the highest total listening time.
     *
     * Time Complexity: O(y) where y = number of years
     * Space Complexity: O(1)
     *
     * @param years list of years with listening data
     * @return the year with highest listening time, or null if list is empty
     */
    private Integer findHighestListeningYear(List<Integer> years) {
        if (years == null || years.isEmpty()) return null;

        Integer highestYear = null;
        int maxHours = 0;

        for (Integer year : years) {
            try {
                YearEndReport report = yearEndReportService.getOrGenerateYearReport(year);
                if (report != null) {
                    int hours = report.getTotalListeningTimeMinutes() / 60;
                    if (hours > maxHours) {
                        maxHours = hours;
                        highestYear = year;
                    }
                }
            } catch (Exception ignored) {}
        }

        return highestYear;
    }
}
