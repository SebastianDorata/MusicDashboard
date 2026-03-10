package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.Utils.Utils;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;

@Component
public class PlaylistController {

    public void show(){
        Scene scene = createScene();
        MainController.switchViews(scene);
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();

        return new Scene(root, Utils.APP_WIDTH, Utils.APP_HEIGHT);
    }
}
