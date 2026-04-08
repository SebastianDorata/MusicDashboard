package com.sebastiandorata.musicdashboard;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Spring Boot application entry point.
 *
 * <p>Delegates launch to {@link JavaFxApplication} via
 * {@link javafx.application.Application#launch(Class, String[])} so that
 * JavaFX initializes its toolkit before Spring's bean context is created.</p>
 */
@SpringBootApplication
public class MusicDashboardApplication {

    public static void main(String[] args) {

        Application.launch(JavaFxApplication.class, args);
    }
}