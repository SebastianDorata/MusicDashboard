package com.sebastiandorata.musicdashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks failed login attempts per username and enforces escalating lockout
 * periods to protect against brute force attacks.
 *
 * <p>Lockout schedule per username (resets on successful login):
 * <ul>
 *   <li>Attempts 1–5:   no lockout</li>
 *   <li>After 5 fails:  5-minute lockout</li>
 *   <li>After 10 fails: 10-minute lockout</li>
 *   <li>After 15 fails: 30-minute lockout</li>
 *   <li>After 20 fails: permanent lockout — manual reset required</li>
 * </ul>
 *
 * <p>All state is held in memory. Lockout state is lost on application
 * restart, which is intentional for a single-user desktop application.
 *
 * <p>A 5-second delay is applied on every failed attempt regardless of
 * lockout state, slowing down automated credential stuffing attempts.
 *
 * <p>Time Complexity: O(1) for all operations (HashMap lookups).
 * Space Complexity: O(u) where u = number of distinct usernames that
 * have had at least one failed attempt.
 */
@Service
public class LoginAttemptService {

    /**
     * Dummy hash used when no account exists for the submitted username.
     * BCrypt is always run against this hash to ensure the response time
     * is identical whether or not the account exists, preventing username
     * enumeration via timing analysis.
     */
    private static final String DUMMY_HASH =
            "$2a$10$dummyhashfortimingprotectionxxxxxxxxxxxxxxxxxxxxxxxx";

    private static final int MAX_ATTEMPTS_PER_TIER = 5;
    private static final int PERMANENT_LOCKOUT_TIER = 4;

    /** Lockout durations in minutes, indexed by tier (0-based). */
    private static final int[] LOCKOUT_MINUTES = {5, 10, 30};

    /** Delay in seconds applied after every failed attempt. */
    private static final int FAILED_ATTEMPT_DELAY_SECONDS = 5;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * Holds attempt state per username.
     * Key: username or email as submitted at login.
     */
    private final Map<String, AttemptRecord> attempts = new HashMap<>();

    // ── Public API ─────────────────────────────────────────────────

    /**
     * Returns whether the given username is currently locked out.
     *
     * <p>For timed lockouts, the lock is automatically lifted if the
     * lockout period has expired. For permanent lockouts (tier 4),
     * this always returns {@code true} until
     * {@link #resetAttempts(String)} is called manually.
     *
     * <p>Time Complexity: O(1)
     *
     * @param username the username or email to check
     * @return {@code true} if the account is currently locked out
     */
    public boolean isLockedOut(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) return false;

        if (record.tier >= PERMANENT_LOCKOUT_TIER) return true;

        if (record.lockedUntil != null && LocalDateTime.now().isBefore(record.lockedUntil)) {
            return true;
        }

        // Lockout period has expired — clear the lock but preserve tier
        if (record.lockedUntil != null && LocalDateTime.now().isAfter(record.lockedUntil)) {
            record.lockedUntil = null;
        }

        return false;
    }

    /**
     * Returns a human-readable message describing the current lockout
     * state for the given username, suitable for display in the UI.
     *
     * <p>Time Complexity: O(1)
     *
     * @param username the username or email to check
     * @return a descriptive lockout message, or an empty string if not
     *         locked out
     */
    public String getLockoutMessage(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) return "";

        if (record.tier >= PERMANENT_LOCKOUT_TIER) {
            return "Account permanently locked. Please contact support.";
        }

        if (record.lockedUntil != null && LocalDateTime.now().isBefore(record.lockedUntil)) {
            long minutesLeft = java.time.Duration.between(
                    LocalDateTime.now(), record.lockedUntil).toMinutes() + 1;
            return "Account locked. Try again in " + minutesLeft + " minute"
                    + (minutesLeft == 1 ? "" : "s") + ".";
        }

        return "";
    }

    /**
     * Records a failed login attempt for the given username and applies
     * the 5-second delay. If the attempt count reaches the tier threshold,
     * a lockout is applied and the tier is incremented for the next breach.
     *
     * <p>Time Complexity: O(1)
     *
     * @param username the username or email that failed authentication
     */
    public void recordFailedAttempt(String username) {
        applyFailedAttemptDelay();

        AttemptRecord record = attempts.computeIfAbsent(username, k -> new AttemptRecord());
        record.failedAttempts++;

        if (record.failedAttempts >= MAX_ATTEMPTS_PER_TIER) {
            record.failedAttempts = 0;
            applyLockout(record);
        }
    }

    /**
     * Resets all failed attempt state for the given username.
     * Called after a successful login or a manual admin reset.
     *
     * <p>Time Complexity: O(1)
     *
     * @param username the username or email to reset
     */
    public void resetAttempts(String username) {
        attempts.remove(username);
    }

    /**
     * Returns the dummy BCrypt hash used for timing-safe authentication
     * checks when no account exists for the submitted username.
     *
     * <p>Always running BCrypt ensures that the response time is identical
     * whether or not an account exists, preventing username enumeration
     * via timing side-channel analysis.
     *
     * @return the dummy BCrypt hash string
     */
    public String getDummyHash() {
        return DUMMY_HASH;
    }

    /**
     * Returns the number of remaining attempts before the next lockout
     * tier is triggered for the given username.
     *
     * <p>Time Complexity: O(1)
     *
     * @param username the username or email to check
     * @return remaining attempts before lockout, or {@code MAX_ATTEMPTS_PER_TIER}
     *         if no failed attempts have been recorded
     */
    public int getRemainingAttempts(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) return MAX_ATTEMPTS_PER_TIER;
        return MAX_ATTEMPTS_PER_TIER - record.failedAttempts;
    }

    // ── Private helpers ────────────────────────────────────────────

    /**
     * Applies the appropriate lockout duration based on the current tier
     * and increments the tier for the next breach.
     *
     * @param record the attempt record to update
     */
    private void applyLockout(AttemptRecord record) {
        if (record.tier >= PERMANENT_LOCKOUT_TIER) return;

        if (record.tier < LOCKOUT_MINUTES.length) {
            record.lockedUntil = LocalDateTime.now()
                    .plusMinutes(LOCKOUT_MINUTES[record.tier]);
        }

        record.tier++;

        if (record.tier >= PERMANENT_LOCKOUT_TIER) {
            record.lockedUntil = null; // permanent — no expiry
        }
    }

    /**
     * Blocks the calling thread for {@value #FAILED_ATTEMPT_DELAY_SECONDS}
     * seconds to slow down automated brute force attempts.
     *
     * <p>This runs on the background thread inside
     * {@link AuthenticationService#login}, not on the JavaFX thread,
     * so the UI remains responsive during the delay.
     */
    private void applyFailedAttemptDelay() {
        try {
            Thread.sleep(FAILED_ATTEMPT_DELAY_SECONDS * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Inner record ───────────────────────────────────────────────

    /**
     * Holds the attempt state for a single username.
     */
    private static class AttemptRecord {
        /** Number of failed attempts in the current tier window. */
        int failedAttempts = 0;

        /**
         * Current lockout tier (0-based).
         * 0 = first lockout (5 min), 1 = second (10 min),
         * 2 = third (30 min), 3+ = permanent.
         */
        int tier = 0;

        /** The time at which the current timed lockout expires, or null if not locked. */
        LocalDateTime lockedUntil = null;
    }
}