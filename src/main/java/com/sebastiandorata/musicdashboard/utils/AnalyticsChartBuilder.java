package com.sebastiandorata.musicdashboard.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.time.YearMonth;
import java.util.*;

public class AnalyticsChartBuilder {


    public HBox createChartHeader() {
        HBox header = new HBox(15);
        header.setPadding(new Insets(15, 15, 0, 15));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("All Time Stats");
        title.getStyleClass().add("analytics-section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox yearComparisonBox = new HBox(10);
        yearComparisonBox.getStyleClass().add("year-comparison-header");
        yearComparisonBox.setAlignment(Pos.CENTER);
        yearComparisonBox.setId("yearComparisonBox");

        header.getChildren().addAll(title, spacer, yearComparisonBox);
        return header;
    }


    public VBox createAllTimeStatsBox() {
        VBox statsBox = new VBox(12);
        statsBox.getStyleClass().add("all-time-stats-box");

        Label titleLabel = new Label("All Time Stats");
        titleLabel.getStyleClass().add("all-time-title");

        Label allTimeHoursLabel = new Label("—");
        allTimeHoursLabel.getStyleClass().add("all-time-hours");
        allTimeHoursLabel.setId("allTimeHoursLabel");

        Label hoursUnit = new Label("Hours");
        hoursUnit.getStyleClass().add("all-time-unit");

        VBox comparisonBox = createComparisonStat();

        statsBox.getChildren().addAll(titleLabel, allTimeHoursLabel, hoursUnit, comparisonBox);
        return statsBox;
    }

    private VBox createComparisonStat() {
        VBox box = new VBox(8);
        box.getStyleClass().add("comparison-stat-box");

        HBox row = new HBox(6);
        row.getStyleClass().add("comparison-row");

        Label arrowLabel = new Label("▲");
        arrowLabel.getStyleClass().addAll("comparison-arrow", "comparison-arrow-positive");

        Label valueLabel = new Label("—");
        valueLabel.getStyleClass().addAll("comparison-value", "comparison-value-positive");

        Label hoursText = new Label("hours");
        hoursText.getStyleClass().add("comparison-label");

        row.getChildren().addAll(arrowLabel, valueLabel, hoursText);

        Label compLabel = new Label("vs highest year");
        compLabel.getStyleClass().add("comparison-label");

        box.getChildren().addAll(row, compLabel);
        return box;
    }


    public void buildChartWithData(VBox chartArea, Map<YearMonth, Integer> currentYearData, Map<YearMonth, Integer> previousYearData,
                                   Integer selectedYear, Integer previousYear, Integer highestYear) {

        chartArea.getChildren().clear();

        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(10));

        Set<YearMonth> allMonths = new TreeSet<>();
        allMonths.addAll(currentYearData.keySet());
        allMonths.addAll(previousYearData.keySet());

        if (allMonths.isEmpty()) {
            Label noData = new Label("No data available");
            noData.getStyleClass().add("txt-grey-md");
            chartArea.getChildren().add(noData);
            return;
        }

        for (YearMonth month : allMonths) {
            HBox monthRow = createColorCodedMonthRow(month, selectedYear, previousYear,
                    highestYear, currentYearData, previousYearData);
            chartBox.getChildren().add(monthRow);
        }

        ScrollPane scrollPane = new ScrollPane(chartBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("trans-background");
        chartArea.getChildren().add(scrollPane);
    }

    private HBox createColorCodedMonthRow(YearMonth month, int selectedYear, int previousYear,
                                          Integer highestYear,
                                          Map<YearMonth, Integer> currentYearData,
                                          Map<YearMonth, Integer> previousYearData) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));

        Label monthLabel = new Label(month.toString());
        monthLabel.setPrefWidth(70);
        monthLabel.getStyleClass().add("txt-white-sm");

        Integer currentValue = currentYearData.getOrDefault(month, 0);
        VBox currentColumn = createColoredBar(currentValue, "monthly-bar-current");

        HBox previousColumn = new HBox();
        if (previousYearData.containsKey(month)) {
            Integer prevValue = previousYearData.getOrDefault(month, 0);
            VBox prevCol = createColoredBar(prevValue, "monthly-bar-previous");
            previousColumn.getChildren().add(prevCol);
        }

        row.getChildren().addAll(monthLabel, currentColumn, previousColumn);
        return row;
    }

    private VBox createColoredBar(Integer minutes, String styleClass) {
        Pane bar = new Pane();
        bar.getStyleClass().add(styleClass);
        bar.setPrefHeight(18);
        bar.setPrefWidth(Math.min(150, minutes / 3));

        Label label = new Label(minutes + "m");
        label.getStyleClass().add("txt-white-sm");
        label.setPrefWidth(40);

        VBox column = new VBox(2);
        column.getChildren().addAll(bar, label);
        return column;
    }


    public void displayYearComparison(HBox yearComparisonBox, int selectedYear,
                                      Integer previousYear, Integer highestYear,
                                      boolean hasPreviousYear, boolean hasHighestYear) {
        yearComparisonBox.getChildren().clear();

        Label currentYearLabel = new Label(String.valueOf(selectedYear));
        currentYearLabel.getStyleClass().add("year-current");
        yearComparisonBox.getChildren().add(currentYearLabel);

        if (!hasPreviousYear && !hasHighestYear) {
            return;
        }

        Label sep1 = new Label("v");
        sep1.getStyleClass().add("year-separator");
        yearComparisonBox.getChildren().add(sep1);

        if (hasPreviousYear) {
            Label prevYearLabel = new Label(String.valueOf(previousYear));
            prevYearLabel.getStyleClass().add("year-previous");
            yearComparisonBox.getChildren().add(prevYearLabel);
        } else {
            Label prevUnavailable = new Label("data not available");
            prevUnavailable.getStyleClass().add("year-previous-unavailable");
            yearComparisonBox.getChildren().add(prevUnavailable);
        }

        if (!hasHighestYear) {
            return;
        }

        Label sep2 = new Label("v");
        sep2.getStyleClass().add("year-separator");
        yearComparisonBox.getChildren().add(sep2);

        Label highestYearLabel = new Label(String.valueOf(highestYear));
        highestYearLabel.getStyleClass().add("year-highest");
        yearComparisonBox.getChildren().add(highestYearLabel);
    }
}