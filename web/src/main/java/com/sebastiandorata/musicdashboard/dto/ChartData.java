package com.sebastiandorata.musicdashboard.dto;

import com.sebastiandorata.musicdashboard.presentation.graph.ChartViewModel;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Data transfer object carrying the three monthly listening time series
 * (current year, previous year, all-time highest year) needed to render
 * the dashboard line chart and its colour-coded title row.
 *
 * <p>Each series is a sparse {@code Map<YearMonth, Integer>} of minutes.
 * Months with no listening activity are absent from the map. Boolean flags
 * control which series are drawn, preventing empty lines for years with
 * no data.</p>
 */

public class ChartData {
    /** Listening minutes per month for the selected year. */
    public Map<YearMonth, Integer> currentYearData;
    /** Listening minutes per month for the previous year; empty when there is no previous-year history. */
    public Map<YearMonth, Integer> previousYearData;
    /** Listening minutes per month for the all-time highest year; empty when it is the current or previous year. */
    public Map<YearMonth, Integer> highestYearData;
    /** The year currently shown in the chart. */
    public Integer selectedYear;
    /** The year immediately before {@code selectedYear}. */
    public Integer previousYear;
    /** The year with the highest total listening time. */
    public Integer highestYear;
    /** All years with playback history, newest first. */
    public List<Integer> allYears;
    /** {@code true} if playback history exists for {@code previousYear}. */
    public boolean hasPreviousYear;
    /** {@code true} if {@code previousYear} equals {@code highestYear}. */
    public boolean isPreviousYearHighest;



    /**
     * Data transfer object carrying all series data and metadata required to render the dashboard line chart.
     * <p>Consumed by {@code DashboardController}, which passes it to the dashboard line chart.
     * Built by {@link ChartViewModel#getChartData(int)}.
     *
     * <p>Up to three series are rendered: the current year (always present),
     * the previous year (present when {@code hasPreviousYear} is {@code true}),
     * and the all-time highest year (present when it differs from both the current and previous years).
     *
     * @param currentYearData       month-to-minute listening map for the current year
     * @param previousYearData      month-to-minute listening map for the previous year; empty when {@code hasPreviousYear} is {@code false}
     * @param highestYearData       month-to-minute listening map for the all-time highest year; empty when it equals the current or previous year
     * @param selectedYear          the calendar year currently current in the UI
     * @param previousYear          the year immediately before {@code selectedYear}
     * @param highestYear           the calendar year with the highest total listening time
     * @param allYears              all years for which playback history exists, newest first
     * @param hasPreviousYear       {@code true} if playback history exists for {@code previousYear}
     * @param isPreviousYearHighest {@code true} if {@code previousYear} equals {@code highestYear}, used to avoid rendering a duplicate series
     */
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
