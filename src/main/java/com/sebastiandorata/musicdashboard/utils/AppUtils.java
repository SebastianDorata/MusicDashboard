package com.sebastiandorata.musicdashboard.utils;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;

public class AppUtils {

    public static final int APP_WIDTH  = 1800;
    public static final int APP_HEIGHT = 1200;

    // ── Time formatting ───────────────────────────────────────────

    // Formats a LocalDateTime as "3m ago"
    public static String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "";
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24)   return hours + "h ago";
        long days = hours / 24;
        if (days == 1)    return "Yesterday";
        return days + "d ago";
    }
    public static String formatTime(double seconds) {
        int totalSeconds = (int) seconds;
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    // Formats a duration in seconds as "3:45"
    public static String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) return "--:--";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }


    public static GridPane buildDialogGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        return grid;
    }


    public static void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().getStylesheets()
                    .add(AppUtils.class.getResource("/globalStyle.css").toExternalForm());
        } catch (Exception ignored) {}
    }


    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }




    public static ImageView buildAlbumArt(int size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }


    public static void logError(String componentName, String message, Exception exception) {
        System.err.println("[" + componentName + "] " + message);
        if (exception != null) exception.printStackTrace();
    }

    public static void logError(String componentName, String message) {
        logError(componentName, message, null);
    }

    public static void logInfo(String componentName, String message) {
        System.out.println("[" + componentName + "] " + message);
    }
}