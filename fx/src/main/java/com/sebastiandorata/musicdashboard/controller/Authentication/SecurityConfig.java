package com.sebastiandorata.musicdashboard.controller.Authentication;

import com.sebastiandorata.musicdashboard.service.AuthenticationService;
import com.sebastiandorata.musicdashboard.service.LoginAttemptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


/**
 * Spring Security configuration.
 *
 * <p>Registers a {@link BCryptPasswordEncoder} bean for use across the service layer. BCrypt is used by
 * {@link AuthenticationService} to hash passwords at registration and verify them at login, ensuring
 * credentials are never stored in plaintext.
 *
 * <p>The work factor defaults to 10, which provides a reasonable balance
 * between security strength and hashing time on modern hardware.
 */
@Configuration
public class SecurityConfig {



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
}

