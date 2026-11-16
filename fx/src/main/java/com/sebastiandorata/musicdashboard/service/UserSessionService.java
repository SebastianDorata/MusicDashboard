package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.User;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Holds the currently authenticated user for the lifetime of the application.
 *
 * <p><b><u>Session tracking: how the timer works</u></b></p>
 *
 * <p><b><u>Storage:</u></b></p>
 * <ol>
 *   <li>{@code JavaFxApplication.start()} calls {@link #recordAppStart()} once
 *       as the very first thing after the Spring context is ready. This stamps
 *       {@code appStart} with the true "app open" time.</li>
 *   <li>When the user logs in, {@link #setCurrentUser(User)} stamps
 *       {@code sessionStart} with the login time. Kept separately in case
 *       future features need to distinguish "since launch" from "since login".</li>
 * </ol>
 *
 * <p><b><u>Retrieval:</u></b></p>
 * <ol>
 *   <li>{@code DailyListeningStatsService.buildStatSnapshot()} calls
 *       {@link #getSessionDurationSeconds()}.</li>
 *   <li>{@code getSessionDurationSeconds()} returns {@code now - appStart},
 *       so the stat card reflects total time the app has been open, including
 *       time spent on the login screen.</li>
 *   <li>The value flows into {@code StatSnapshot.todayAvgSessionSeconds}, which
 *       {@code StatCardsViewModel} formats and hands to the stat card label.</li>
 * </ol>
 *
 * <p><b><u>Limitations:</u></b></p>
 * <ul>
 *   <li>All state is in-memory and lost on restart. Only the current run is tracked.</li>
 * </ul>
 */
@Getter
@Service
public class UserSessionService {

    private User currentUser;

    /**
     * Recorded once in {@code JavaFxApplication.start()} — the true "app open"
     * timestamp. {@link #getSessionDurationSeconds()} uses this so the stat card
     * measures time from launch, not from login.
     */
    private LocalDateTime appStart;

    /**
     * Recorded when the user successfully logs in. Kept separately from
     * {@code appStart} in case future features need to distinguish between
     * "time since launch" and "time since this login".
     */
    private LocalDateTime sessionStart;

    /**
     * Records the moment the JavaFX application window first opens.
     * Called exactly once from {@code JavaFxApplication.start()}.
     * Subsequent calls are no-ops so a re-shown auth screen cannot reset the clock.
     */
    public void recordAppStart() {
        if (appStart == null) {
            appStart = LocalDateTime.now();
        }
    }

    /**
     * Sets the authenticated user and records the login time.
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
     * Returns the number of seconds elapsed since the application launched.
     *
     * <p>Uses {@code appStart} (set in {@code JavaFxApplication.start()}) rather
     * than {@code sessionStart} (set at login), so the timer reflects total time
     * the app has been open, including time spent on the login screen.
     *
     * <p>Falls back to {@code sessionStart} if {@link #recordAppStart()} was never
     * called, and returns {@code 0} if neither has been set.
     *
     * Time Complexity: O(1)
     *
     * @return elapsed seconds since app launch, or {@code 0} if unavailable
     */
    public int getSessionDurationSeconds() {
        LocalDateTime reference = appStart != null ? appStart : sessionStart;
        if (reference == null) return 0;
        return (int) ChronoUnit.SECONDS.between(reference, LocalDateTime.now());
    }
}