package com.sebastiandorata.musicdashboard;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JavaFxApplication extends Application {



    @Override
    public void start(Stage stage) throws Exception {
        VBox root = new VBox();
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Music Dashboard");
        stage.setScene(scene);
        stage.show();

    }
}