package com.sebastiandorata.musicdashboard.controllerUtils;

import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.DashboardLineChartBuilder;
import com.sebastiandorata.musicdashboard.utils.SmoothLineChart;
import com.sebastiandorata.musicdashboard.viewmodel.ChartViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Assembles the dashboard graph panel and orchestrates data loading.
 *
 * Layout: HBox split into two sections.
 *   Left  25%: info section (all-time stats + deltas).
 *   Right 75%: chart title row + SmoothLineChart.
 *
 * SRP: Assembles the panel and wires data to the view.
 *      Data computation is delegated to ChartViewModel.
 *      Chart node construction is delegated to DashboardLineChartBuilder.
 * DIP: Depends on ChartViewModel (Spring-managed abstraction), not on repository details.
 */
@Component
public class DashboardGraphController extends UIComponent {

    @Autowired
    private ChartViewModel chartViewModel;

    private final DashboardLineChartBuilder chartBuilder = new DashboardLineChartBuilder();


    private Label totalHoursValueLabel;
    private Label deltaVsPreviousSymbolLabel;
    private Label deltaVsPreviousValueLabel;
    private Label deltaVsAllTimeSymbolLabel;
    private Label deltaVsAllTimeValueLabel;
    private VBox chartSection;



    public HBox createPanel() {
        HBox panel = new HBox();
        panel.getStyleClass().add("dashboard-graph-panel");
        panel.setSpacing(40);

        VBox infoSection = buildInfoSection();
            infoSection.setMinWidth(200);
            infoSection.setPrefWidth(250);

            chartSection = buildChartSection();
                HBox.setHgrow(chartSection, Priority.ALWAYS);
                chartSection.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(chartSection, Priority.ALWAYS);

        panel.getChildren().addAll(infoSection, chartSection);

        loadInfoData();
        loadChartData();

        return panel;
    }

    // ── Info Section (left 25%) ────────────────────────────────────

    private VBox buildInfoSection() {
        VBox info = new VBox();
        info.getStyleClass().add("graph-info-section");
        info.setPrefWidth(250);              // optional starting size
        info.setMinWidth(200);

        Label header = new Label("All Time Stats");
        header.getStyleClass().add("graph-info-header");

        HBox totalRow = buildTotalHoursRow();
        HBox deltaVsPreviousRow = buildDeltaVsPreviousRow();
        HBox deltaVsAllTimeRow = buildDeltaVsAllTimeRow();

        info.getChildren().addAll(header, totalRow, deltaVsPreviousRow, deltaVsAllTimeRow);
        return info;
    }

    private HBox buildTotalHoursRow() {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("graph-total-row");

        totalHoursValueLabel = new Label("—");
        totalHoursValueLabel.getStyleClass().add("graph-total-value");

        Label hoursUnit = new Label("Hours");
        hoursUnit.getStyleClass().add("graph-total-unit");

        row.getChildren().addAll(totalHoursValueLabel, hoursUnit);
        return row;
    }

    private HBox buildDeltaVsPreviousRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("graph-delta-row");

        deltaVsPreviousSymbolLabel = new Label("▲");
        deltaVsPreviousSymbolLabel.getStyleClass().addAll("graph-delta-symbol", "graph-delta-positive");

        deltaVsPreviousValueLabel = new Label("—");
        deltaVsPreviousValueLabel.getStyleClass().addAll("graph-delta-value", "graph-delta-positive");

        Label staticText = new Label("hours v last year");
        staticText.getStyleClass().add("graph-delta-static");

        row.getChildren().addAll(deltaVsPreviousSymbolLabel, deltaVsPreviousValueLabel, staticText);
        return row;
    }

    private HBox buildDeltaVsAllTimeRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("graph-delta-row");

        deltaVsAllTimeSymbolLabel = new Label("▲");
        deltaVsAllTimeSymbolLabel.getStyleClass().addAll("graph-delta-symbol", "graph-delta-positive");

        deltaVsAllTimeValueLabel = new Label("—");
        deltaVsAllTimeValueLabel.getStyleClass().addAll("graph-delta-value", "graph-delta-positive");

        Label hoursText = new Label("hours");
        hoursText.getStyleClass().add("graph-delta-static");

        Label allTimeText = new Label("all time");
        allTimeText.getStyleClass().add("graph-delta-alltime");

        row.getChildren().addAll(deltaVsAllTimeSymbolLabel, deltaVsAllTimeValueLabel, hoursText, allTimeText);
        return row;
    }

    // ── Chart Section (right 75%) ──────────────────────────────────

    private VBox buildChartSection() {
        VBox section = new VBox();

        section.getStyleClass().add("graph-chart-section");

        section.setMaxWidth(Double.MAX_VALUE);
        section.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(section, Priority.ALWAYS);

        return section;
    }

    // ── Data Loading ───────────────────────────────────────────────

    private void loadInfoData() {
        int currentYear = LocalDate.now().getYear();
        deltaVsAllTimeValueLabel.getStyleClass().add("year-header-highest");
        chartViewModel.loadAllTimeStatsForGraph(currentYear, statsData -> {
            NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);

            totalHoursValueLabel.setText(fmt.format(statsData.totalAllTimeHours));

            applyDelta(
                    deltaVsPreviousSymbolLabel,
                    deltaVsPreviousValueLabel,
                    statsData.hasPreviousYear ? statsData.deltaVsPrevious : null,
                    fmt
            );

            applyDelta(
                    deltaVsAllTimeSymbolLabel,
                    deltaVsAllTimeValueLabel,
                    statsData.deltaVsAllTimeHigh,
                    fmt
            );
        });
    }

    private void loadChartData() {
        int currentYear = LocalDate.now().getYear();
        chartViewModel.loadChartData(currentYear, chartData -> {
            HBox title = chartBuilder.buildChartTitle(
                    chartData.selectedYear,
                    chartData.previousYear,
                    chartData.highestYear,
                    chartData.hasPreviousYear,
                    chartData.isPreviousYearHighest
            );

            SmoothLineChart chart = chartBuilder.buildChart(
                    chartData.currentYearData,
                    chartData.previousYearData,
                    chartData.highestYearData,
                    chartData.selectedYear,
                    chartData.previousYear,
                    chartData.highestYear,
                    chartData.hasPreviousYear,
                    chartData.isPreviousYearHighest
            );

            // ── Programmatically hide the legend ──────────────────────
            // This ensures the legend is completely removed from the chart,
            // overriding any CSS rules that might try to show it.
            chart.setLegendVisible(false);
            // ──────────────────────────────────────────────────────────

            VBox.setVgrow(chart, Priority.ALWAYS);
            chartSection.getChildren().setAll(title, chart);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────

    /**
     * Applies the correct arrow symbol and colour class to a delta label pair.
     * Null delta (e.g. no previous year) shows an em-dash with no value.
     */
    private void applyDelta(Label symbolLabel, Label valueLabel, Integer delta, NumberFormat fmt) {
        if (delta == null) {
            symbolLabel.setText("—");
            symbolLabel.getStyleClass().setAll("graph-delta-symbol", "graph-delta-neutral");
            valueLabel.setText("");
            valueLabel.getStyleClass().setAll("graph-delta-value", "graph-delta-neutral");
            return;
        }

        boolean positive = delta >= 0;
        String colourClass = positive ? "graph-delta-positive" : "graph-delta-negative";

        symbolLabel.setText(positive ? "▲" : "▼");
        symbolLabel.getStyleClass().setAll("graph-delta-symbol", colourClass);

        valueLabel.setText(fmt.format(Math.abs(delta)));
        valueLabel.getStyleClass().setAll("graph-delta-value", colourClass);
    }
}