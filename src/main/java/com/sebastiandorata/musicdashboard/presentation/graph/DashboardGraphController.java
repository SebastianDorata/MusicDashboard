package com.sebastiandorata.musicdashboard.presentation.graph;

import com.sebastiandorata.musicdashboard.presentation.UIComponent;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Assembles the dashboard graph panel and orchestrates data loading.
 *
 * <p><b><u>Layout:</u></b> HBox split into two sections.</p>
 * <ul>
 *   <li>Left 25%: info section (all-time stats and deltas).</li>
 *   <li>Right 75%: chart title row and SmoothLineChart.</li>
 * </ul>
 *
 * <p>SRP: Assembles the panel and wires data to the view.
 * Data computation is delegated to ChartViewModel.
 * Chart node construction is delegated to DashboardLineChartBuilder.</p>
 *
 * <p>DIP: Depends on ChartViewModel (Spring-managed abstraction),
 * not on repository details.</p>
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
        panel.getStyleClass().addAll("dashboard-graph-panel", "panels");
        HBox.setHgrow(panel, Priority.ALWAYS);


        VBox infoSection = buildInfoSection();


        chartSection = buildChartSection();
        HBox.setHgrow(chartSection, Priority.ALWAYS);
        chartSection.setMaxWidth(Double.MAX_VALUE);

        panel.getChildren().addAll(infoSection, chartSection);

        loadInfoData();
        loadChartData();

        return panel;
    }

    //  Info Section (left 25%)
    private VBox buildInfoSection() {
        VBox info = new VBox();
        info.getStyleClass().add("graph-info-section");
        info.setPrefWidth(AppUtils.scale(300));  // was 300
        info.setMinWidth(AppUtils.scale(200));   // was 200

        Label header = new Label("All Time Stats");
        header.getStyleClass().addAll("txt-white-bld-thirty","txt-centre-underline");

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
        totalHoursValueLabel.getStyleClass().add("txt-white-bld-thirty");

        Label hoursUnit = new Label("Hours");
        hoursUnit.getStyleClass().add("txt-white-bld-thirty");

        HBox symbolAndValue = new HBox(4);
        symbolAndValue.setAlignment(Pos.CENTER_LEFT);
        symbolAndValue.getChildren().addAll(totalHoursValueLabel, hoursUnit);

        Label staticText = new Label("Total playback time");
        staticText.getStyleClass().add("txt-white-md-bld");

        VBox valueStack = new VBox(2);
        valueStack.setAlignment(Pos.CENTER_LEFT);
        valueStack.getChildren().addAll(symbolAndValue,staticText);

        row.getChildren().addAll(valueStack);
        return row;
    }

    private HBox buildDeltaVsPreviousRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("graph-delta-row");

        deltaVsPreviousSymbolLabel = new Label("▲");
        deltaVsPreviousSymbolLabel.getStyleClass().addAll("graph-delta-main", "lt-Green");

        deltaVsPreviousValueLabel = new Label("—");
        deltaVsPreviousValueLabel.getStyleClass().addAll("graph-delta-main", "lt-Green");

        Label staticText = new Label("hours v last year");
        staticText.getStyleClass().add("txt-white-md-bld");

        HBox symbolAndValue = new HBox(4);
        symbolAndValue.setAlignment(Pos.CENTER_LEFT);
        symbolAndValue.getChildren().addAll(deltaVsPreviousSymbolLabel, deltaVsPreviousValueLabel);

        VBox valueStack = new VBox(2);
        valueStack.setAlignment(Pos.CENTER_LEFT);
        valueStack.getChildren().addAll(symbolAndValue, staticText);

        row.getChildren().add(valueStack);
        return row;
    }

    private HBox buildDeltaVsAllTimeRow() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("graph-delta-row");

        deltaVsAllTimeSymbolLabel = new Label("▲");
        deltaVsAllTimeSymbolLabel.getStyleClass().addAll("graph-delta-main", "lt-Green");

        deltaVsAllTimeValueLabel = new Label("—");
        deltaVsAllTimeValueLabel.getStyleClass().addAll("graph-delta-main", "lt-Green");

        HBox symbolAndValue = new HBox(4);
            symbolAndValue.setAlignment(Pos.CENTER_LEFT);
            symbolAndValue.getChildren().addAll(deltaVsAllTimeSymbolLabel, deltaVsAllTimeValueLabel);

        Label hoursText = new Label("hours");
        hoursText.getStyleClass().add("txt-white-md-bld");
        Label allTimeText = new Label(" all time");
        allTimeText.getStyleClass().add("graph-delta-alltime");


        HBox staticText = new HBox(2);
            staticText.setAlignment(Pos.CENTER_LEFT);
            staticText.getChildren().addAll(hoursText, allTimeText);

        VBox valueStack = new VBox(2);
        valueStack.setAlignment(Pos.CENTER_LEFT);
        valueStack.getChildren().addAll(symbolAndValue, staticText);


        row.getChildren().addAll(valueStack);
        return row;
    }

    // Chart Section (right 75%)

    private VBox buildChartSection() {
        VBox section = new VBox();

        section.getStyleClass().add("graph-chart-section");

        section.setMaxWidth(Double.MAX_VALUE);
        section.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(section, Priority.ALWAYS);

        return section;
    }



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

            // Hide the legend from the chart,
            // overriding any CSS rules that might try to show it.
            chart.setLegendVisible(false);

            VBox.setVgrow(chart, Priority.ALWAYS);
            chartSection.getChildren().setAll(title, chart);
        });
    }

    private void applyDelta(Label symbolLabel, Label valueLabel, Integer delta, NumberFormat fmt) {
        if (delta == null) {
            symbolLabel.setText("—");
            symbolLabel.getStyleClass().setAll("graph-delta-main", "txt-grey-md");
            valueLabel.setText("");
            valueLabel.getStyleClass().setAll("graph-delta-main", "txt-grey-md");
            return;
        }

        boolean positive = delta >= 0;
        String colourClass = positive ? "lt-Green" : "graph-delta-negative";

        symbolLabel.setText(positive ? "▲" : "▼");
        symbolLabel.getStyleClass().setAll("graph-delta-main", colourClass);

        valueLabel.setText(fmt.format(Math.abs(delta)));
        valueLabel.getStyleClass().setAll("graph-delta-main", colourClass);
    }
}