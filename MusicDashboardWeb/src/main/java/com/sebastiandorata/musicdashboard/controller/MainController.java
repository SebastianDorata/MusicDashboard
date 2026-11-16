package com.sebastiandorata.musicdashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entry-point controller of the web application.
 *
 * <p>The desktop {@code MainController} was the navigation hub: {@code navigateTo(view)} switched
 * the JavaFX scene. On the web, URL routing replaces it: every page controller maps its own URL
 * ({@code /dashboard}, {@code /library}, ...) and links in the templates navigate between them.
 * Only the root URL needs a handler, sending visitors to the dashboard; Spring Security sends
 * anonymous visitors on to {@code /login}.
 *
 * <p>SRP: only the root redirect.
 */
@Controller
public class MainController {

    /**
     * Redirects the root URL to the dashboard.
     *
     * @return a redirect to {@code /dashboard}
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
}
