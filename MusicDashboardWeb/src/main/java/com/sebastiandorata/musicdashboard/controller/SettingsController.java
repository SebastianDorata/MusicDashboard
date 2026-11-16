package com.sebastiandorata.musicdashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the Settings page.
 *
 * <p>The desktop Settings screen only showed the application sidebar, so the web page is the
 * shared shell with an empty content area. It is the reference example of a page built on the
 * layout fragments.
 *
 * <p>SRP: only maps the route to its template; it holds no state and calls no services.
 */
@Controller
public class SettingsController {

    /**
     * Renders the Settings page.
     *
     * @return the {@code settings} template name
     */
    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
