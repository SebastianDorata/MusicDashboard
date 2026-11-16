package com.sebastiandorata.musicdashboard.presentation.Analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Displays weekly/monthly reports.
 *
 * SRP: Only responsible for rendering report data as UI.
 * Time Complexity: O(1)
 * Space Complexity: O(1)
 */
public class ReportDisplayController {

    /**
     * Creates a report card for weekly or monthly data.
     */
    public static VBox createReportCard(String periodLabel, int totalSongs, int totalMinutes, String topSong, String topArtist, String topAlbum, String topGenre) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #2a2a2a; -fx-border-radius: 8; -fx-border-color: #404040;");
        card.getStyleClass().add("report-card");

        Label periodLabel_ui = new Label(periodLabel);
        periodLabel_ui.setStyle("-fx-font-size: 16px; -fx-text-fill: #61dafb; -fx-font-weight: bold;");

        Label totalLabel = new Label(totalMinutes + " minutes | " + totalSongs + " songs");
        totalLabel.getStyleClass().add("txt-white-sm");

        HBox statsRow = createStatsRow("Top Song", topSong);
        HBox artistRow = createStatsRow("Top Artist", topArtist);
        HBox albumRow = createStatsRow("Top Album", topAlbum);
        HBox genreRow = createStatsRow("Top Genre", topGenre);

        card.getChildren().addAll(
                periodLabel_ui,
                totalLabel,
                new Separator(),
                statsRow,
                artistRow,
                albumRow,
                genreRow
        );

        return card;
    }

    private static HBox createStatsRow(String label, String value) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelUI = new Label(label + ":");
        labelUI.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc; -fx-min-width: 80px;");

        Label valueUI = new Label(value);
        valueUI.getStyleClass().add("wt-smmd-bld");
        valueUI.setWrapText(true);

        row.getChildren().addAll(labelUI, valueUI);
        return row;
    }
}