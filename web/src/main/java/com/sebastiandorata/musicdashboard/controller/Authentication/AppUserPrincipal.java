package com.sebastiandorata.musicdashboard.controller.Authentication;

import java.io.Serializable;
import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Immutable identity of a logged-in user, stored in the Spring Security context.
 *
 * <p>Carries only the few values the web layer needs on every request: the user id, username,
 * email and the moment of login. It deliberately holds neither the JPA {@code User} entity (which
 * would drag lazy proxies into the HTTP session) nor the password hash.
 *
 * <p>SRP: this record-like class only describes who is logged in; authentication lives in
 * {@link AppAuthenticationProvider} and lookups in
 * {@link com.sebastiandorata.musicdashboard.service.UserSessionService}.
 *
 * @param id       the database id of the user
 * @param username the user's username
 * @param email    the user's email address, possibly empty
 * @param loginAt  the moment the user logged in
 */
public record AppUserPrincipal(Long id, String username, String email, LocalDateTime loginAt)
        implements Principal, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Returns the username, as required by {@link Principal}.
     *
     * @return the username of the logged-in user
     */
    @Override
    public String getName() {
        return username;
    }
}
