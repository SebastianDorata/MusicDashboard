package com.sebastiandorata.musicdashboard.utils;

import java.time.LocalDateTime;

/**
 * Shared utility methods used across the web edition.
 *
 * <p>Contains:
 * <ul>
 *   <li>Time and duration formatting helpers</li>
 *   <li>Structured error logging</li>
 * </ul>
 *
 * <p>The desktop edition of this class also held JavaFX window-size
 * constants, dialog builders and an album-art {@code ImageView} builder.
 * Those have no server-side equivalent: sizing is handled by CSS, dialogs
 * by the shared modal helper in {@code static/js/app.js}, and album art by
 * the {@code /media/art/album/{id}} endpoint.</p>
 */
public class AppUtils {

    private AppUtils() {}

    /**
     * Formats a timestamp relative to now, e.g. {@code "5m ago"} or {@code "Yesterday"}.
     *
     * <p>Time Complexity: O(1)</p>
     *
     * @param time the timestamp to describe; may be {@code null}
     * @return a short relative-time label, or an empty string when {@code time} is {@code null}
     */
    public static String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "";
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24)   return hours + "h ago";
        long days = hours / 24;
        if (days == 1)    return "Yesterday";
        return days + "d ago";
    }

    /**
     * Formats a playback position as {@code m:ss}.
     *
     * <p>Time Complexity: O(1)</p>
     *
     * @param seconds the position in seconds
     * @return the position formatted as minutes and zero-padded seconds
     */
    public static String formatTime(double seconds) {
        int totalSeconds = (int) seconds;
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    /**
     * Formats a track length as {@code m:ss}.
     *
     * <p>Time Complexity: O(1)</p>
     *
     * @param seconds the track length in seconds; may be {@code null}
     * @return the formatted length, or {@code "--:--"} when the length is unknown
     */
    public static String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) return "--:--";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Writes a component-tagged error to standard error.
     *
     * @param componentName the class or component reporting the error
     * @param message       a short description of what failed
     * @param exception     the cause; may be {@code null}
     */
    public static void logError(String componentName, String message, Exception exception) {
        System.err.println("[" + componentName + "] " + message);
        if (exception != null) exception.printStackTrace();
    }
}
