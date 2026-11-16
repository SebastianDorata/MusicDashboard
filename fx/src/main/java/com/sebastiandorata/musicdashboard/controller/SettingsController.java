package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.presentation.shared.AppSidebar;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SettingsController {

    private String activeRoute = "Settings";

    public void show() {
        Scene scene = this.createScene();

        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/globalStyle.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/buttons.css")).toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }
        MainController.switchViews(scene);
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        VBox left = AppSidebar.build("settings");
        root.setLeft(left);

        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

}
