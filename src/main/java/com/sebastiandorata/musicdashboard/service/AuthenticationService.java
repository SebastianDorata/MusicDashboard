package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.controller.Authentication.AuthenticationController;
import com.sebastiandorata.musicdashboard.entity.User;
import com.sebastiandorata.musicdashboard.repository.UserRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles user registration and authentication.
 *
 * <p>Login is protected against brute force attacks via
 * {@link LoginAttemptService}, which enforces escalating lockout periods
 * after repeated failed attempts. Username enumeration is prevented by
 * always running BCrypt regardless of whether the account exists.
 */
@Service
public class AuthenticationService {

    @Setter
    @Getter
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    /**
     * Registers a new user account with the given credentials.
     *
     * <p>Throws {@link IllegalArgumentException} if the email or username is already taken.
     *
     * @param email    the email address for the new account
     * @param password the plaintext password — stored as a BCrypt hash
     * @param username the display username for the new account
     * @return the persisted {@link User} entity
     * @throws IllegalArgumentException if the email or username already exists
     */
    public User register(String email, String password, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Attempts to authenticate a user by username or email and password.
     *
     * <p><b><u>Security measures applied on every call:</u></b></p>
     * <ul>
     *   <li>Lockout check via {@link LoginAttemptService#isLockedOut(String)}
     *       before any database query.</li>
     *   <li>BCrypt always runs regardless of whether the account exists,
     *       preventing username enumeration via timing side-channel.</li>
     *   <li>A {@value LoginAttemptService#FAILED_ATTEMPT_DELAY_SECONDS}-second
     *       delay is applied on every failed attempt via
     *       {@link LoginAttemptService#recordFailedAttempt(String)}.</li>
     *   <li>Attempt counter resets on successful login via
     *       {@link LoginAttemptService#resetAttempts(String)}.</li>
     * </ul>
     *
     * <p>This method is called from a background thread in
     * {@link AuthenticationController} so the 5-second delay on failed
     * attempts does not block the JavaFX thread.</p>
     *
     * @param usernameOrEmail the username or email submitted at login
     * @param password the plaintext password to verify
     * @return an {@link Optional} containing the authenticated {@link User},or empty if credentials are invalid or the account is locked out
     * @throws IllegalStateException if the account is currently locked out,with a message suitable for display in the UI
     */
    public Optional<User> login(String usernameOrEmail, String password) {
        // Check lockout before any DB query
        if (loginAttemptService.isLockedOut(usernameOrEmail)) {
            throw new IllegalStateException(
                    loginAttemptService.getLockoutMessage(usernameOrEmail));
        }

        // Try to find by email first, then username
        Optional<User> userOptional = userRepository.findByEmail(usernameOrEmail);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(usernameOrEmail);
        }

        // Always run BCrypt to prevent username enumeration via timing
        String hashToCheck = userOptional
                .map(User::getPasswordHash)
                .orElse(loginAttemptService.getDummyHash());

        if (userOptional.isPresent() && passwordEncoder.matches(password, hashToCheck)) {
            loginAttemptService.resetAttempts(usernameOrEmail);
            User user = userOptional.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            return Optional.of(user);
        }

        // Records failed attempt and applies 5s delay and lockout if threshold reached
        loginAttemptService.recordFailedAttempt(usernameOrEmail);
        return Optional.empty();
    }
}