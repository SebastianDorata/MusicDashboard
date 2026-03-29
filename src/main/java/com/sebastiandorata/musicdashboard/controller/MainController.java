package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.controller.Analytics.AnalyticsController;
import com.sebastiandorata.musicdashboard.controller.Authentication.AuthenticationController;
import com.sebastiandorata.musicdashboard.controller.Dashboard.DashboardController;
import com.sebastiandorata.musicdashboard.controller.UserLibrary.MyLibraryController;
import com.sebastiandorata.musicdashboard.controller.UserLibrary.PlaylistController;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

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


    public static void switchViews(Scene scene) {
        mainStage.setScene(scene);
        mainStage.show();
    }
}

