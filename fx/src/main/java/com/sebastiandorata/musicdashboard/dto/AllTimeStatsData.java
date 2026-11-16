package com.sebastiandorata.musicdashboard.dto;


import com.sebastiandorata.musicdashboard.presentation.graph.ChartViewModel;
import com.sebastiandorata.musicdashboard.presentation.graph.DashboardGraphController;

import java.util.function.Consumer;
/**
 * Data transfer object holding pre-computed all-time listening statistics
 * for the dashboard graph info panel.
 *
 * <p>Carries total listening hours across all years, current and previous
 * year hours, the all-time high year and its hour count, year-over-year
 * deltas, and boolean flags indicating whether prior-year data exists and
 * whether the previous year is the all-time record holder.</p>
 */

public class AllTimeStatsData {
    public Integer totalAllTimeHours;
    public Integer currentYearHours;
    public Integer previousYearHours;
    public Integer highestYearHours;
    public Integer highestYear;
    public Integer deltaVsPrevious;
    public Integer deltaVsAllTimeHigh;
    public boolean hasPreviousYear;
    public boolean isPreviousYearHighest;


    /**
     * Data transfer object carrying all-time and year-comparative listening
     * statistics for the dashboard graph info section.
     *
     * <p>Consumed by {@link DashboardGraphController}
     * to populate the left-side stat labels alongside the line chart.
     * Built by {@link ChartViewModel#loadAllTimeStatsForGraph(int, Consumer)}.
     *
     * @param totalAllTimeHours      sum of all listening hours across every recorded year
     * @param currentYearHours       total listening hours for the selected year
     * @param previousYearHours      total listening hours for the year before the selected year; only meaningful when {@code hasPreviousYear} is {@code true}
     * @param highestYearHours       total listening hours for the year with the highest recorded total
     * @param highestYear            the calendar year with the highest total listening time
     * @param deltaVsPrevious        difference in hours between the current year and the previous year; negative values indicate a decrease
     * @param deltaVsAllTimeHigh     difference in hours between the current year and the all-time high year
     * @param hasPreviousYear        {@code true} if playback history exists for the year before the selected year
     * @param isPreviousYearHighest  {@code true} if the previous year is also the all-time highest year, used to avoid rendering a duplicate series on the chart
     */
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
