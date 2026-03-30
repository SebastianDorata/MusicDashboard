package com.sebastiandorata.musicdashboard.controller.Dashboard;

import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.YearEndReportService;
import com.sebastiandorata.musicdashboard.controller.Analytics.viewmodel.YearComparisonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


@Service
public class ChartViewModel {

    @Autowired
    private YearEndReportService yearEndReportService;

    @Autowired
    private DataLoadingService dataLoadingService;


    public static class AllTimeStatsData {
        public Integer totalAllTimeHours;        // Sum of all years
        public Integer currentYearHours;         // Current year total
        public Integer previousYearHours;        // Previous year total (if exists)
        public Integer highestYearHours;         // Highest year's total
        public Integer highestYear;              // Which year was highest
        public Integer deltaVsPrevious;          // Change from previous year (can be negative)
        public Integer deltaVsAllTimeHigh;       // Difference from all-time record
        public boolean hasPreviousYear;          // Whether previous year exists in data
        public boolean isPreviousYearHighest;    // True if previous year = highest year

        public AllTimeStatsData(Integer totalAllTimeHours, Integer currentYearHours,
                                Integer previousYearHours, Integer highestYearHours,
                                Integer highestYear, Integer deltaVsPrevious,
                                Integer deltaVsAllTimeHigh, boolean hasPreviousYear,
                                boolean isPreviousYearHighest) {
            this.totalAllTimeHours = totalAllTimeHours;
            this.currentYearHours = currentYearHours;
            this.previousYearHours = previousYearHours;
            this.highestYearHours = highestYearHours;
            this.highestYear = highestYear;
            this.deltaVsPrevious = deltaVsPrevious;
            this.deltaVsAllTimeHigh = deltaVsAllTimeHigh;
            this.hasPreviousYear = hasPreviousYear;
            this.isPreviousYearHighest = isPreviousYearHighest;
        }
    }

    /**
     * Public data class for chart rendering.
     * Contains all series data (current, previous, highest years) plus metadata.
     */
    public static class ChartData {
        public Map<YearMonth, Integer> currentYearData;
        public Map<YearMonth, Integer> previousYearData;
        public Map<YearMonth, Integer> highestYearData;
        public Integer selectedYear;
        public Integer previousYear;
        public Integer highestYear;
        public List<Integer> allYears;
        public boolean hasPreviousYear;
        public boolean isPreviousYearHighest;

        public ChartData(
                Map<YearMonth, Integer> currentYearData,
                Map<YearMonth, Integer> previousYearData,
                Map<YearMonth, Integer> highestYearData,
                Integer selectedYear,
                Integer previousYear,
                Integer highestYear,
                List<Integer> allYears,
                boolean hasPreviousYear,
                boolean isPreviousYearHighest) {
            this.currentYearData = currentYearData;
            this.previousYearData = previousYearData;
            this.highestYearData = highestYearData;
            this.selectedYear = selectedYear;
            this.previousYear = previousYear;
            this.highestYear = highestYear;
            this.allYears = allYears;
            this.hasPreviousYear = hasPreviousYear;
            this.isPreviousYearHighest = isPreviousYearHighest;
        }
    }


    /**
     * Time Complexity: O(y + n) where y = available years, n = total playback records
     * Space Complexity: O(m * s) where m = 12 months, s ≤ 3 series = O(1)
     */
    public void loadChartData(int selectedYear, Consumer<ChartData> onSuccess) {
        dataLoadingService.loadAsync(
                () -> {
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
                },
                onSuccess
        );
    }

    /**
     * Time Complexity: O(y) where y = number of years in history
     * Space Complexity: O(1). Returns fixed data structure
     */
    public void loadAllTimeStatsForGraph(int selectedYear, Consumer<AllTimeStatsData> onSuccess) {
        dataLoadingService.loadAsync(
                () -> {
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
                },
                onSuccess
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