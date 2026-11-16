package com.sebastiandorata.musicdashboard.controller.Authentication;

import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import com.sebastiandorata.musicdashboard.service.LoginAttemptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Spring Security configuration for the web application.
 *
 * <p>Registers a {@link BCryptPasswordEncoder} bean for use across the service layer. BCrypt is used by
 * {@link AuthenticationService} to hash passwords at registration and verify them at login, ensuring
 * credentials are never stored in plaintext. The work factor defaults to 10.
 *
 * <p>It also defines the {@link SecurityFilterChain}: the login page, registration and static
 * assets are public, every other URL requires a logged-in user. Form login posts to {@code /login}
 * (parameters {@code username} and {@code password}), always continues to {@code /dashboard}, and
 * on failure stores the message in the session under {@link #LOGIN_ERROR_ATTRIBUTE} so
 * {@link AuthenticationController} can show it once. Logout is {@code POST /logout}. CSRF
 * protection stays enabled.
 *
 * <p>SRP: only security wiring lives here; credential checks are in {@link AppAuthenticationProvider}.
 */
@Configuration
public class SecurityConfig {

    /** Session attribute holding the one-time login failure message. */
    public static final String LOGIN_ERROR_ATTRIBUTE = "LOGIN_ERROR_MESSAGE";

    /**
     * Creates and registers the {@link BCryptPasswordEncoder} bean.
     *
     * <p>Injected into {@link AuthenticationService}
     * and {@link LoginAttemptService} via Spring's dependency injection.
     *
     * @return a {@link BCryptPasswordEncoder} instance with default work factor 10
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Builds the HTTP security filter chain.
     *
     * <p>{@link AppAuthenticationProvider} is deliberately <b>not</b> registered here with
     * {@code http.authenticationProvider(...)}. It is the only {@code AuthenticationProvider}
     * bean, so Spring Security already installs it in the global authentication manager,
     * which is the parent of this chain's manager. Registering it on the chain as well makes
     * Spring try it twice for every wrong password (chain first, then parent), which would
     * count two failed attempts and apply the failed-attempt delay twice.</p>
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the configuration cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/icons/**",
                                "/favicon.ico", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler(failureHandler())
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    /**
     * Creates the failure handler that stores the message for the login page and redirects.
     *
     * @return a handler redirecting to {@code /login?error}
     */
    private AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            request.getSession().setAttribute(LOGIN_ERROR_ATTRIBUTE, exception.getMessage());
            response.sendRedirect(request.getContextPath() + "/login?error");
        };
    }
}
