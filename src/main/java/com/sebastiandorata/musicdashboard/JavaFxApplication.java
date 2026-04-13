package com.sebastiandorata.musicdashboard;

import com.sebastiandorata.musicdashboard.controller.Authentication.AuthenticationController;
import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * JavaFX {@link Application} entry point.
 *
 * <p>Bootstraps the Spring application context in {@link #init()}, sets up
 * the primary {@link Stage} and hands the initial scene to
 * {@link AuthenticationController}.</p>
 */
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() throws Exception {
        applicationContext = new SpringApplicationBuilder(MusicDashboardApplication.class)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage stage) throws Exception {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring context failed to initialize in init()");
        }

        stage.setTitle("Music Dashboard");
        stage.setMaximized(false);
        stage.setWidth(AppUtils.APP_WIDTH);
        stage.setHeight(AppUtils.APP_HEIGHT);
        stage.centerOnScreen();
        MainController.setMainStage(stage);

        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            System.out.println("Scene changed: " + newScene);
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> {
                    System.out.println("Key pressed: " + e.getCode());
                    if (e.getCode() == javafx.scene.input.KeyCode.F5) {
                        System.out.println("Reloading CSS...");
                        reloadCSS(newScene);
                    }
                });
            }
        });

        AuthenticationController authController = applicationContext.getBean( AuthenticationController.class);
        authController.show();
    }



    private void reloadCSS(javafx.scene.Scene scene) {
        java.util.List<String> sheets = new java.util.ArrayList<>(scene.getStylesheets());
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(sheets);
    }

    @Override
    public void stop() throws Exception {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}