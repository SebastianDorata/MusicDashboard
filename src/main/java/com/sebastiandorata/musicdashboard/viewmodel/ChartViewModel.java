package com.sebastiandorata.musicdashboard.viewmodel;

import com.sebastiandorata.musicdashboard.entity.YearEndReport;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.YearEndReportService;
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


    public void loadChartData(int selectedYear, Consumer<ChartData> onSuccess) {
        dataLoadingService.loadAsync(
                () -> {
                    Integer previousYear = selectedYear - 1;
                    List<Integer> allYears = yearEndReportService.getAvailableYears();
                    Integer highestYear = findHighestListeningYear(allYears);

                    Map<YearMonth, Integer> currentYearData =
                            yearEndReportService.getMonthlyListeningTime(selectedYear);

                    Map<YearMonth, Integer> previousYearData = new HashMap<>();
                    if (allYears.contains(previousYear)) {
                        previousYearData.putAll(
                                yearEndReportService.getMonthlyListeningTime(previousYear)
                        );
                    }

                    return new ChartData(
                            currentYearData,
                            previousYearData,
                            selectedYear,
                            previousYear,
                            highestYear,
                            allYears
                    );
                },
                onSuccess
        );
    }

    /**
     * Time Complexity: O(y) where y = number of years in history
     * Space Complexity: O(1) - returns fixed data structure
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

    public void loadYearComparison(int selectedYear, Consumer<YearComparisonData> onSuccess) {
        dataLoadingService.loadAsync(
                () -> {
                    List<Integer> allYears = yearEndReportService.getAvailableYears();
                    Integer previousYear = selectedYear - 1;
                    Integer highestYear = findHighestListeningYear(allYears);

                    boolean hasPreviousYear = allYears.contains(previousYear);
                    boolean hasHighestYear = highestYear != null && !highestYear.equals(selectedYear);

                    return new YearComparisonData(previousYear, highestYear, hasPreviousYear, hasHighestYear);
                },
                onSuccess
        );
    }

    public void loadAllTimeStats(Consumer<Integer> onSuccess) {
        dataLoadingService.loadAsync(
                () -> {
                    List<Integer> allYears = yearEndReportService.getAvailableYears();
                    int totalAllTimeHours = 0;

                    for (Integer year : allYears) {
                        YearEndReport report = yearEndReportService.getOrGenerateYearReport(year);
                        if (report != null) {
                            totalAllTimeHours += report.getTotalListeningTimeMinutes() / 60;
                        }
                    }

                    return totalAllTimeHours;
                },
                onSuccess
        );
    }


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
    public List<Integer> getAllAvailableYears() {
        return yearEndReportService.getAvailableYears();
    }
}