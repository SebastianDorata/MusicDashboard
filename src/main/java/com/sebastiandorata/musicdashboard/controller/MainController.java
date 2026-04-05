package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.controller.Authentication.AuthenticationController;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
/**
 * Static navigation hub for the application.
 *
 * <p>Holds singleton references to every top-level page controller and the
 * primary {@link Stage}. {@link #navigateTo(String)} routes a
 * view key to the corresponding controller's {@code show()} method.
 * {@link #switchViews(Scene)} replaces the stage's active
 * scene.</p>
 */
public class MainController {
    @Setter
    @Getter
    private static Stage mainStage;

    private static AnalyticsController analyticsController;
    private static AuthenticationController authenticationController;
    private static DashboardController dashboardController;
    private static ImportController importController;
    private static MyLibraryController myLibraryController;
    private static PlaylistController playlistController;

    public static void registerAuth(AuthenticationController controller) {
        authenticationController = controller;
    }
    public static void registerDashboard(DashboardController controller) {
        dashboardController = controller;
    }
    public static void registerImport(ImportController controller) {
        importController = controller;
    }
    public static void registerLibrary(MyLibraryController controller) {
        myLibraryController = controller;
    }
    public static void registerPlaylist(PlaylistController controller) {
        playlistController = controller;
    }
    public static void registerAnalytics(AnalyticsController controller) {
        analyticsController = controller;
    }

    public static void navigateTo(String view) {
        switch (view) {
            case "dashboard"  -> dashboardController.show();
            case "library"    -> myLibraryController.show();
            case "auth"       -> authenticationController.show();
            case "import"    -> importController.show();
            case "analytics"    -> analyticsController.show();
            case "playlist"    -> playlistController.show();
            default           -> System.err.println("Unknown view: " + view);
        }
    }


    public static void switchViews(Scene newScene) {
        javafx.scene.Parent newRoot = newScene.getRoot();
        java.util.List<String> newStylesheets = new java.util.ArrayList<>(newScene.getStylesheets());

        // Detach root from the new scene
        newScene.setRoot(new javafx.scene.layout.Region());

        if (mainStage.getScene() == null) {
            javafx.scene.Scene scene = new javafx.scene.Scene(newRoot);
            scene.getStylesheets().addAll(newStylesheets);
            mainStage.setScene(scene);
            mainStage.show();
        } else {
            mainStage.getScene().getStylesheets().setAll(newStylesheets);
            mainStage.getScene().setRoot(newRoot);
        }
    }
}

