package com.sebastiandorata.musicdashboard.presentation.graph;

import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * SRP: Solely responsible for constructing the SmoothLineChart node and its title row.
 * OCP: Axis configurations can be added without modifying callers.
 */
public class DashboardLineChartBuilder {

    private static final String[] MONTH_LABELS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static final int Y_AXIS_MAX = 750;
    private static final int Y_AXIS_TICK_UNIT = 50;

    /**
     * Time Complexity: O(m * s) — m = 12 months (constant), s ≤ 3 series = O(1) effectively
     * Space Complexity: O(m * s) = O(1)
     */
    public SmoothLineChart buildChart(Map<YearMonth, Integer> currentYearData,
                                      Map<YearMonth, Integer> previousYearData,
                                      Map<YearMonth, Integer> highestYearData,
                                      int selectedYear,
                                      Integer previousYear,
                                      Integer highestYear,
                                      boolean hasPreviousYear,
                                      boolean isPreviousYearHighest) {

        CategoryAxis xAxis = buildXAxis();
        NumberAxis yAxis = buildYAxis();

        SmoothLineChart chart = new SmoothLineChart(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.getStyleClass().addAll("dashboard-line-chart", "chart-series-line", "chart-line-symbol");


        // Add series in consistent order: Current, Previous, Highest
        chart.getData().add(buildSeries(currentYearData, selectedYear, true));

        if (hasPreviousYear) {
            chart.getData().add(buildSeries(previousYearData, previousYear, false));
        }

        if (!isPreviousYearHighest && highestYear != null && !highestYear.equals(selectedYear)) {
            chart.getData().add(buildSeries(highestYearData, highestYear, false));
        }

        return chart;
    }


    public HBox buildChartTitle(
            int selectedYear,
            Integer previousYear,
            Integer highestYear,
            boolean hasPreviousYear,
            boolean isPreviousYearHighest) {

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER);
        titleRow.getStyleClass().add("graph-chart-title-row");

        Label currentLabel = new Label(String.valueOf(selectedYear));
        currentLabel.getStyleClass().add("year-header-current");
        titleRow.getChildren().add(currentLabel);

        if (hasPreviousYear) {
            titleRow.getChildren().add(buildSeparator());
            Label prevLabel = new Label(String.valueOf(previousYear));
            prevLabel.getStyleClass().add("year-header-previous");
            titleRow.getChildren().add(prevLabel);
        }

        if (!isPreviousYearHighest && highestYear != null && !highestYear.equals(selectedYear)) {
            titleRow.getChildren().add(buildSeparator());
            Label highLabel = new Label(String.valueOf(highestYear));
            highLabel.getStyleClass().add("year-header-highest");
            titleRow.getChildren().add(highLabel);
        }

        return titleRow;
    }

    /**
     * Time Complexity: O(12) = O(1). Fixed iteration over 12 months
     * Space Complexity: O(12) = O(1)
     */
    private XYChart.Series<String, Number> buildSeries(
            Map<YearMonth, Integer> monthlyMinutes,
            int year,
            boolean isCurrentYear) {

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(String.valueOf(year));

        int currentCalendarYear = LocalDate.now().getYear();
        int currentCalendarMonth = LocalDate.now().getMonthValue();

        for (int m = 1; m <= 12; m++) {
            if (isCurrentYear && year == currentCalendarYear && m >= currentCalendarMonth) {
                continue;
            }
            YearMonth ym = YearMonth.of(year, m);
            Integer minutes = monthlyMinutes.get(ym);
            if (minutes != null) {
                double hours = minutes / 60.0;
                series.getData().add(new XYChart.Data<>(MONTH_LABELS[m - 1], hours));
            }
        }
        return series;
    }

    private CategoryAxis buildXAxis() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.getCategories().addAll(MONTH_LABELS);
        return xAxis;
    }

    private NumberAxis buildYAxis() {
        NumberAxis yAxis = new NumberAxis(0, Y_AXIS_MAX, Y_AXIS_TICK_UNIT);
        yAxis.setAutoRanging(false);

        return yAxis;
    }

    private Label buildSeparator() {
        Label sep = new Label("v");
        sep.getStyleClass().add("wt-smmd-bld");
        return sep;
    }
}