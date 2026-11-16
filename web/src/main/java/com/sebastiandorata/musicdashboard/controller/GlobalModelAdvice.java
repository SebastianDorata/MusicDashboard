package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds model attributes that every Thymeleaf template can use.
 *
 * <p>Currently exposes {@code currentUsername}, the name of the logged-in user.
 *
 * <p>SRP: shared view-model data only.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private UserSessionService userSessionService;

    /**
     * Returns the logged-in username for every view.
     *
     * @return the username, or {@code null} when nobody is logged in
     */
    @ModelAttribute("currentUsername")
    public String currentUsername() {
        return userSessionService.getCurrentUsername();
    }
}
