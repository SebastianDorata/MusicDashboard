package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.controller.Authentication.AuthenticationController;
import jakarta.annotation.PostConstruct;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Navigation hub for the application.
 *
 * <p>Spring manages exactly one instance of this bean. Each top-level page
 * controller is injected directly — previously every controller called a
 * {@code registerX()} method here from its own {@code @PostConstruct},
 * hand-rolling a service locator that Spring's container already provides.
 *
 * <p>{@link #navigateTo(String)} and {@link #switchViews(Scene)} stay
 * accessible as static calls, since the rest of the codebase (view
 * builders, dialog handlers) invokes them as {@code MainController.navigateTo(...)}
 * from many places that aren't themselves Spring-managed. A self-reference
 * captured once at startup backs the static methods.
 */
@Component
public class MainController {

    @Setter
    @Getter
    private static Stage mainStage;

    private static MainController instance;

    @Lazy @Autowired private AnalyticsController analyticsController;
    @Lazy @Autowired private AuthenticationController authenticationController;
    @Lazy @Autowired private DashboardController dashboardController;
    @Lazy @Autowired private ImportController importController;
    @Lazy @Autowired private MyLibraryController myLibraryController;
    @Lazy @Autowired private PlaylistController playlistController;
    @Lazy @Autowired private SettingsController settingsController;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static void navigateTo(String view) {
        switch (view) {
            case "dashboard"  -> instance.dashboardController.show();
            case "library"    -> instance.myLibraryController.show();
            case "auth"       -> instance.authenticationController.show();
            case "import"     -> instance.importController.show();
            case "analytics"  -> instance.analyticsController.show();
            case "playlist"   -> instance.playlistController.show();
            case "settings"   -> instance.settingsController.show();
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