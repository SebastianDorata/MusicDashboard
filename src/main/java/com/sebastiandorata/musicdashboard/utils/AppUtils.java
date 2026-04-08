package com.sebastiandorata.musicdashboard.utils;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;

import java.time.LocalDateTime;

/**
 * Shared utility methods and application-wide constants.
 * <p>Contains:
 * <ul>
 *   <li>Application window dimension constants</li>
 *   <li>Time and duration formatting helpers</li>
 *   <li>JavaFX dialog construction helpers</li>
 *   <li>Error display helper</li>
 *   <li>Album art {@link ImageView} builder</li>
 *   <li>Structured error logging</li>
 * </ul>
 */
public class AppUtils {
    public static final double APP_WIDTH  = Screen.getPrimary().getVisualBounds().getWidth();
    public static final double APP_HEIGHT = Screen.getPrimary().getVisualBounds().getHeight();
    public static final double SCALE      = APP_HEIGHT / 1080.0;


    public static double scale(double value) {
        return value * SCALE;
    }

    /**
     * Returns a right-panel preferred width scaled to the screen.
     * Replaces the raw  APP_WIDTH * 0.25  calls scattered across controllers.
     * Clamped so it never becomes unreasonably wide on an ultrawide monitor.
     */
    public static double rightPanelPrefWidth() {
        double raw = APP_WIDTH * 0.20;
        return Math.clamp(raw, 240, 380);
    }


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
                    .add(AppUtils.class.getResource("/css/globalStyle.css").toExternalForm());
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
        iv.setFitWidth(scale(size));
        iv.setFitHeight(scale(size));
        iv.setPreserveRatio(true);
        return iv;
    }


    public static void logError(String componentName, String message, Exception exception) {
        System.err.println("[" + componentName + "] " + message);
        if (exception != null) exception.printStackTrace();
    }


}