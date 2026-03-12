package com.sebastiandorata.musicdashboard;


import com.sebastiandorata.musicdashboard.controller.AuthenticationController;
import com.sebastiandorata.musicdashboard.controller.DashboardController;
import com.sebastiandorata.musicdashboard.controller.MainController;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        // Start Spring Boot context
        applicationContext = new SpringApplicationBuilder(MusicDashboardApplication.class)
                .run();
    }


    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Music Dashboard");
        MainController.setMainStage(stage);


        AuthenticationController authController = applicationContext.getBean(AuthenticationController.class);
        authController.show();
    }
}