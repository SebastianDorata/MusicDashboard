package com.sebastiandorata.musicdashboard.controller.Authentication;

import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import com.sebastiandorata.musicdashboard.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Security {@link AuthenticationProvider} that delegates to {@link AuthenticationService}.
 *
 * <p>The existing lockout and brute-force logic ({@link LoginAttemptService}, including the
 * 5-second failed-attempt delay, which now simply runs on the request thread) stays the single
 * source of truth. This provider only translates its outcomes into Spring Security exceptions
 * carrying the same user-facing messages the desktop login card showed:
 * <ul>
 *   <li>empty fields: {@link BadCredentialsException} "Please enter your username and password."</li>
 *   <li>wrong credentials: {@link BadCredentialsException} with the remaining-attempts message</li>
 *   <li>lockout ({@link IllegalStateException}): {@link LockedException} with the lockout message</li>
 *   <li>any other failure: {@link AuthenticationServiceException} "Login failed: ..."</li>
 * </ul>
 *
 * <p>SRP: adapts the service to the Spring Security contract, nothing else.
 * <p>DIP: depends on the service abstractions, not on repositories.
 */
@Component
public class AppAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    /**
     * Authenticates the submitted username-or-email and password.
     *
     * @param authentication the unauthenticated username/password token
     * @return an authenticated token whose principal is an {@link AppUserPrincipal} with role {@code ROLE_USER}
     * @throws AuthenticationException if the fields are empty, the credentials are wrong, the account is locked or login fails
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName() == null ? "" : authentication.getName().trim();
        Object credentials = authentication.getCredentials();
        String password = credentials == null ? "" : credentials.toString();

        if (username.isEmpty() || password.isEmpty()) {
            throw new BadCredentialsException("Please enter your username and password.");
        }

        Optional<User> userOptional;
        try {
            userOptional = authenticationService.login(username, password);
        } catch (IllegalStateException e) {
            throw new LockedException(e.getMessage(), e);
        } catch (Exception e) {
            throw new AuthenticationServiceException("Login failed: " + e.getMessage(), e);
        }

        if (userOptional.isEmpty()) {
            int remaining = loginAttemptService.getRemainingAttempts(username);
            throw new BadCredentialsException("Incorrect username or password. "
                    + remaining + " attempt" + (remaining == 1 ? "" : "s") + " remaining.");
        }

        User user = userOptional.get();
        AppUserPrincipal principal = new AppUserPrincipal(
                user.getId(), user.getUsername(), user.getEmail(), LocalDateTime.now());
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * Declares that this provider handles username/password tokens.
     *
     * @param authentication the token type to check
     * @return {@code true} for {@link UsernamePasswordAuthenticationToken}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
