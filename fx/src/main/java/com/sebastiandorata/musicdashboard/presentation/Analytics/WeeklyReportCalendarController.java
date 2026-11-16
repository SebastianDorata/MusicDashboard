package com.sebastiandorata.musicdashboard.presentation.Analytics;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Calendar controller for selecting weeks in a month.
 *
 * <p><b><u>Features:</u></b></p>
 * <ul>
 *   <li>Shows calendar for a given year and month.</li>
 *   <li>Hover highlighting on entire weeks (Sunday-Saturday).</li>
 *   <li>Navigation arrows to switch months.</li>
 *   <li>Click outside to close.</li>
 *   <li>Callback on week selection.</li>
 * </ul>
 *
 * <p>Time Complexity: O(1) for rendering (constant calendar size).</p>
 * <p>Space Complexity: O(1).</p>
 */
@Component
public class WeeklyReportCalendarController {

    @Getter
    private VBox calendarPane;
    private Label monthLabel;
    private GridPane calendarGrid;
    private Integer currentYear;
    private Integer currentMonth;
    private Integer highlightedWeek = null;
    private Consumer<Integer> onWeekSelected;
    private Runnable onClose;

    public void showCalendar(Integer year, Integer month, Consumer<Integer> weekCallback, Runnable closeCallback) {
        this.currentYear = year;
        this.currentMonth = month;
        this.onWeekSelected = weekCallback;
        this.onClose = closeCallback;
        this.highlightedWeek = null;

        buildCalendar();
    }

    private void buildCalendar() {
        if (calendarPane == null) {
            calendarPane = new VBox(10);
            calendarPane.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #404040; -fx-border-radius: 8; -fx-padding: 15;");
            calendarPane.setPrefWidth(350);
            calendarPane.setAlignment(Pos.TOP_CENTER);
            calendarPane.setOnMouseClicked(e -> e.consume());
        }

        calendarPane.getChildren().clear();

        HBox header = createCalendarHeader();
        calendarGrid = createCalendarGrid();

        calendarPane.getChildren().addAll(header, calendarGrid);
    }

    private HBox createCalendarHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("calendar-nav-btn");
        prevBtn.setStyle("-fx-padding: 5px 10px; -fx-font-size: 14px; -fx-background-color: #1e90ff; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;");
        prevBtn.setOnAction(e -> {
            goToPreviousMonth();
        });

        monthLabel = new Label(java.time.Month.of(currentMonth).toString() + " " + currentYear);
        monthLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #61dafb; -fx-font-weight: bold;");
        monthLabel.setMinWidth(150);
        monthLabel.setAlignment(Pos.CENTER);

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("calendar-nav-btn");
        nextBtn.setStyle("-fx-padding: 5px 10px; -fx-font-size: 14px; -fx-background-color: #1e90ff; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;");
        nextBtn.setOnAction(e -> {
            goToNextMonth();
        });

        header.getChildren().addAll(prevBtn, monthLabel, nextBtn);
        return header;
    }

    private GridPane createCalendarGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setStyle("-fx-padding: 10px;");

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #61dafb; -fx-alignment: center;");
            dayLabel.setMinWidth(40);
            dayLabel.setMinHeight(30);
            grid.add(dayLabel, i, 0);
        }

        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth);
        LocalDate firstDay = yearMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;

        int daysInMonth = yearMonth.lengthOfMonth();
        WeekFields weekFields = WeekFields.of(Locale.US);

        int row = 1;
        int col = firstDayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            int weekOfYear = date.get(weekFields.weekOfYear());

            VBox dayBox = createDayBox(day, weekOfYear);

            dayBox.setOnMouseEntered(e -> highlightWeek(weekOfYear, dayBox));
            dayBox.setOnMouseExited(e -> removeHighlight(dayBox, weekOfYear));
            dayBox.setOnMouseClicked(e -> selectWeek(weekOfYear));

            grid.add(dayBox, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        return grid;
    }

    private VBox createDayBox(int day, int week) {
        VBox box = new VBox();
        box.setPrefWidth(40);
        box.setPrefHeight(40);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-border-color: #404040; -fx-border-radius: 4; -fx-padding: 5;");

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.getStyleClass().add("wt-smmd-bld");

        box.getChildren().add(dayLabel);
        box.setUserData(week);
        return box;
    }

    private void highlightWeek(int week, VBox dayBox) {
        if (highlightedWeek != null && !highlightedWeek.equals(week)) {
            removeHighlight(dayBox, highlightedWeek);
        }

        dayBox.setStyle("-fx-background-color: #3a8f9f; -fx-border-color: #61dafb; -fx-border-radius: 4; -fx-padding: 5;");
        highlightedWeek = week;
    }

    private void removeHighlight(VBox dayBox, int week) {
        dayBox.setStyle("-fx-border-color: #404040; -fx-border-radius: 4; -fx-padding: 5; -fx-background-color: transparent;");
    }

    private void selectWeek(int week) {
        if (onWeekSelected != null) {
            onWeekSelected.accept(week);
        }
    }

    private void goToPreviousMonth() {
        if (currentMonth == 1) {
            currentMonth = 12;
            currentYear--;
        } else {
            currentMonth--;
        }
        updateCalendarDisplay();
    }

    private void goToNextMonth() {
        if (currentMonth == 12) {
            currentMonth = 1;
            currentYear++;
        } else {
            currentMonth++;
        }
        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        monthLabel.setText(java.time.Month.of(currentMonth).toString() + " " + currentYear);

        int gridIndex = calendarPane.getChildren().indexOf(calendarGrid);
        calendarGrid = createCalendarGrid();

        if (gridIndex >= 0) {
            calendarPane.getChildren().set(gridIndex, calendarGrid);
        }
    }

}