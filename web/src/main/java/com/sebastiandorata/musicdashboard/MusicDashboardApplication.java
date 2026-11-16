package com.sebastiandorata.musicdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the web edition.
 *
 * <p>The desktop edition delegates launch to a JavaFX {@code Application}
 * so the JavaFX toolkit starts before Spring. The web edition has no
 * toolkit to initialise: {@link SpringApplication#run(Class, String...)}
 * starts the embedded servlet container and the Spring MVC controllers
 * take the place of the JavaFX scenes.</p>
 */
@SpringBootApplication
public class MusicDashboardApplication {

    /**
     * Starts the embedded web server and the Spring application context.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(MusicDashboardApplication.class, args);
    }
}
