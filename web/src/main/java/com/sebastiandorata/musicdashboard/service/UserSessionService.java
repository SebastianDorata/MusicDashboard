package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.controller.Authentication.AppUserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Resolves the current request's logged-in user from the Spring Security context.
 *
 * <p>A web server serves many users at once, so this singleton is stateless: nothing user-specific
 * is stored in it. Every method reads the {@link AppUserPrincipal} that
 * {@code AppAuthenticationProvider} placed in the {@link SecurityContextHolder} for the current
 * request thread.
 *
 * <p>The session duration used by the daily stat card is the time since this user's login
 * ({@code loginAt} on the principal), tracked per HTTP session.
 *
 * <p>SRP: only answers "who is logged in on this thread, and since when".
 */
@Service
public class UserSessionService {

    /**
     * Returns the current user's ID, or {@code null} if no user is logged in.
     *
     * <p>Time Complexity: O(1)
     *
     * @return the user id, or {@code null}
     */
    public Long getCurrentUserId() {
        AppUserPrincipal p = principal();
        return p != null ? p.id() : null;
    }

    /**
     * Returns the current user's username, or {@code null} if no user is logged in.
     *
     * <p>Time Complexity: O(1)
     *
     * @return the username, or {@code null}
     */
    public String getCurrentUsername() {
        AppUserPrincipal p = principal();
        return p != null ? p.username() : null;
    }

    /**
     * Returns the number of seconds elapsed since the current user logged in.
     *
     * <p>Time Complexity: O(1)
     *
     * @return elapsed seconds since login, or {@code 0} when nobody is logged in
     */
    public int getSessionDurationSeconds() {
        AppUserPrincipal p = principal();
        if (p == null || p.loginAt() == null) return 0;
        return (int) ChronoUnit.SECONDS.between(p.loginAt(), LocalDateTime.now());
    }

    /**
     * Extracts the {@link AppUserPrincipal} from the security context.
     *
     * @return the principal, or {@code null} for anonymous or unauthenticated requests
     */
    private AppUserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) return null;
        return auth.getPrincipal() instanceof AppUserPrincipal p ? p : null;
    }
}
