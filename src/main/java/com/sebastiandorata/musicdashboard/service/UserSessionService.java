package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.User;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Holds the currently authenticated user for the lifetime of the application.
 *
 * <p><b><u>Session tracking: how the average is stored and retrieved</u></b></p>
 *
 * <p><b><u>Storage:</u></b></p>
 * <ol>
 *   <li>User logs in via AuthenticationController.handleLogin().</li>
 *   <li>On success, UserSessionService.setCurrentUser(user) is called.</li>
 *   <li>setCurrentUser records sessionStart = LocalDateTime.now() in memory.
 *       No database write. The value lives only for this JVM run.</li>
 * </ol>
 *
 * <p><b><u>Retrieval:</u></b></p>
 * <ol>
 *   <li>DailyListeningStatsService.buildStatSnapshot() calls
 *       getSessionDurationSeconds().</li>
 *   <li>getSessionDurationSeconds() returns now minus sessionStart in seconds.
 *       This is the duration of the current session (open app to now).</li>
 *   <li>The value flows into StatSnapshot.todayAvgSessionSeconds, which
 *       StatCardsViewModel formats and hands to the stat card label.</li>
 * </ol>
 *
 * <p><b><u>Limitations:</u></b></p>
 * <ul>
 *   <li>History is lost on restart. The average is always the current session
 *       duration, not a historical mean across multiple sessions.</li>
 * </ul>
 */
@Getter
@Service
public class UserSessionService {

    private User currentUser;

    /**
     * In-memory session start time. Set when the user logs in.
     * Never null after the first successful login within this JVM run.
     */
    private LocalDateTime sessionStart;

    /**
     * Sets the authenticated user and records the session start time.
     *
     * <p>Called by {@code AuthenticationController} immediately after a
     * successful login. {@code sessionStart} is captured here so that
     * {@link #getSessionDurationSeconds()} can compute elapsed time from
     * the moment the user entered the app.
     *
     * @param user the authenticated user; must not be {@code null}
     */
    public void setCurrentUser(User user) {
        this.currentUser  = user;
        this.sessionStart = LocalDateTime.now();
    }

    /**
     * Returns the current user's ID, or {@code null} if no user is logged in.
     *
     * Time Complexity: O(1)
     */
    public Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * Returns the number of seconds elapsed since the user logged in.
     *
     * <p>This is used as the "average session length" stat card value,
     * in-memory only.Only one session is tracked per JVM run,
     * the value is always the duration of the current session, not a
     * historical average.
     *
     * <p>Returns {@code 0} if {@code sessionStart} has not been set (i.e.
     * no user has logged in during this run).
     *
     * Time Complexity: O(1)
     *
     * @return elapsed seconds since login, or {@code 0} if not logged in
     */
    public int getSessionDurationSeconds() {
        if (sessionStart == null) return 0;
        return (int) ChronoUnit.SECONDS.between(sessionStart, LocalDateTime.now());
    }
}