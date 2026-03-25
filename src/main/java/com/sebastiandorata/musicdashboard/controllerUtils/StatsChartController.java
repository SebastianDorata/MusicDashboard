package com.sebastiandorata.musicdashboard.controllerUtils;

import com.sebastiandorata.musicdashboard.utils.AnalyticsChartBuilder;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.viewmodel.ChartViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Component controller for the statistics chart on the dashboard.
 */
@Component
public class StatsChartController extends UIComponent {

    @Autowired
    private ChartViewModel chartViewModel;

    private final AnalyticsChartBuilder chartBuilder = new AnalyticsChartBuilder();

    private VBox  monthlyChartArea;
    private Label allTimeHoursLabel;

    public VBox createGraphSection() {
        VBox graphBox = new VBox(15);
        graphBox.setPrefWidth(AppUtils.APP_WIDTH * 0.50);
        graphBox.setPrefHeight(AppUtils.APP_HEIGHT * 0.35);
        graphBox.setPadding(new Insets(20));
        graphBox.getStyleClass().add("analytics-dark-bg");

        HBox header   = createChartHeader();
        HBox statsRow = createChartStatsRow();

        monthlyChartArea = new VBox(10);
        monthlyChartArea.getStyleClass().add("chart-area");
        monthlyChartArea.setPrefHeight(150);

        graphBox.getChildren().addAll(header, statsRow, monthlyChartArea);


        loadChartData(LocalDate.now().getYear());

        return graphBox;
    }



    public HBox createChartHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("year-comparison-header");

        int currentYear  = LocalDate.now().getYear();
        int previousYear = currentYear - 1;

        Label currentYearLabel = new Label(String.valueOf(currentYear));
        currentYearLabel.getStyleClass().add("year-header-current");

        Label separator = new Label("v");
        separator.getStyleClass().add("year-header-separator");

        Label previousYearLabel = new Label(String.valueOf(previousYear));
        previousYearLabel.getStyleClass().add("year-header-previous");

        header.getChildren().addAll(currentYearLabel, separator, previousYearLabel);
        return header;
    }


    private HBox createChartStatsRow() {
        HBox statsRow = new HBox(20);
        statsRow.setPadding(new Insets(0, 0, 15, 0));

        VBox allTimeBox = new VBox(2);
        Label allTimeTitle = new Label("All Time");
        allTimeTitle.getStyleClass().add("txt-grey-sm");

        allTimeHoursLabel = new Label("—");
        allTimeHoursLabel.getStyleClass().add("txt-white-md-bld");

        allTimeBox.getChildren().addAll(allTimeTitle, allTimeHoursLabel);
        statsRow.getChildren().add(allTimeBox);
        return statsRow;
    }


    private void loadChartData(int year) {
        chartViewModel.loadChartData(year, chartData ->
                chartBuilder.buildChartWithData(
                        monthlyChartArea,
                        chartData.currentYearData,
                        chartData.previousYearData,
                        chartData.selectedYear,
                        chartData.previousYear,
                        chartData.highestYear
                )
        );

        chartViewModel.loadAllTimeStats(hours ->
                allTimeHoursLabel.setText(String.valueOf(hours))
        );
    }
}