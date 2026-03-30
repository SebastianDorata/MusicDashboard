package com.sebastiandorata.musicdashboard.utils;

/**
 * Centralized constants for playback tracking and validation.
 *
 * Purpose: Single source of truth for playback rules, preventing duplication and inconsistency across analytics services.
 *
 * Time Complexity: O(1) - all operations are constant-time checks
 * Space Complexity: O(1) - single constant values
 */
public class PlaybackConstants {

    /**
     * Minimum duration (in seconds) for a play to be counted as valid.
     *
     * Rationale: Skips, accidents, and test plays are typically < 20 seconds.
     * This threshold filters out noise while capturing intentional listens.
     */
    public static final int MINIMUM_PLAY_SECONDS = 20;

    /**
     * Validates whether a recorded play duration should be counted in analytics.
     *
     * Handles three cases:
     *  - null duration: counts as invalid (song was auto-played, duration not captured)
     *  - < 20 seconds: counts as skip or accident
     *  - >= 20 seconds: counts as valid listen
     *
     * Time Complexity: O(1)
     *
     * @param durationSeconds the duration in seconds (may be null)
     * @return true if this play should be included in analytics, false otherwise
     */
    public static boolean isValidPlay(Integer durationSeconds) {
        return durationSeconds != null && durationSeconds >= MINIMUM_PLAY_SECONDS;
    }

    /**
     * Normalizes duration to a safe integer for calculations.
     * Null durations are treated as 0 (no playback recorded).
     *
     * Time Complexity: O(1)
     *
     * @param durationSeconds the duration in seconds (may be null)
     * @return the duration in seconds, or 0 if null
     */
    public static int safeDuration(Integer durationSeconds) {
        return durationSeconds != null ? durationSeconds : 0;
    }
}