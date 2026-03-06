package com.sebastiandorata.musicdashboard.Controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public class MainController {
    @Setter
    @Getter
    private static Stage mainStage;

    public static void switchViews(Scene scene) {
        mainStage.setScene(scene);
        mainStage.show();
    }
}

