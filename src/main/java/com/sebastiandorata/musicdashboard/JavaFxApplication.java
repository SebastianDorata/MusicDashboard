package com.sebastiandorata.musicdashboard;


import com.sebastiandorata.musicdashboard.Controller.AuthenticationController;
import com.sebastiandorata.musicdashboard.Controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;

public class JavaFxApplication extends Application {



    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Music Dashboard");
        MainController.setMainStage(stage);

        new AuthenticationController().show();
    }
}