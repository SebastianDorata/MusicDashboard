package com.sebastiandorata.musicdashboard.Utils;

import javafx.scene.image.ImageView;

import java.time.LocalDateTime;

public class Utils {
    public static final int APP_WIDTH = 1614;
    public static final int APP_HEIGHT = 900;


    // Formats a LocalDateTime to be readable
    // Uses in the Recently Played card
    // TODO: Same helper can be reused on AnalyticsController
    public static String formatRelativeTime(LocalDateTime time) {
        if (time == null) return "";
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1)   return "Just now";
        if (minutes < 60)  return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24)    return hours + "h ago";
        long days = hours / 24;
        if (days == 1)     return "Yesterday";
        return days + "d ago";
    }
}

