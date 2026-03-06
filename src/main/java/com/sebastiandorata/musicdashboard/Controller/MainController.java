package com.sebastiandorata.musicdashboard.Controller;

import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public class MainController {
    @Getter
    @Setter
    private static Stage mainStage;

    public static void switchViews(Scene scene){
        if(mainStage != null){
           mainStage.setScene(scene);
           mainStage.show();
        }
    }
}
