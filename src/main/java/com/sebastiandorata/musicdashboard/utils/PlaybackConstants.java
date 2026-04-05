package com.sebastiandorata.musicdashboard.utils;

/**
 * Centralized constants for playback tracking and validation.
 *
 * Single source for playback rules, preventing duplication and inconsistency across analytics services.
 *
 * Time Complexity: O(1). All operations are constant-time checks
 * Space Complexity: O(1). Single constant values
 */
public class PlaybackConstants {

    /**
     * Minimum duration for a play to be counted as valid.
     *
     * <p>Filter out Skips, accidents, and test plays to
     * only capturing intentional listens.
     */
    public static final int MINIMUM_PLAY_SECONDS = 20;

    /**
     * Validates whether a recorded play duration should be counted in analytics.
     *
     * Handles three cases:
     * <ol>
     *  <li>null duration: counts as invalid (song was auto-played, duration not captured)</li>
     *  <li>&lt; 20 seconds: counts as skip or accident</li>
     *  <li>>= 20 seconds: counts as valid listen   </li>
     * </ol>
     *
     * Time Complexity: O(1)
     *
     * @param durationSeconds the duration in seconds (may be null)
     * @return true if this play should be included in analytics, false otherwise
     */
    public static boolean isValidPlay(Integer durationSeconds) {
        return durationSeconds != null && durationSeconds >= MINIMUM_PLAY_SECONDS;
    }


}