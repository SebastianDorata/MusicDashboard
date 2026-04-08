package com.sebastiandorata.musicdashboard.presentation.helpers;

import javafx.geometry.Pos;
import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for empty state UI components.
 * Standardizes empty state panels with customizable icons and messages.
 */
@Builder
@Getter
public class EmptyStateConfig {

    @Builder.Default
    private String icon = "♪";

    @Builder.Default
    private String iconStyleClass = "empty-state-icon";

    @Builder.Default
    private String message = "No items found";

    @Builder.Default
    private String messageStyleClass = "empty-msg";

    @Builder.Default
    private double minHeight = 300;

    @Builder.Default
    private double spacing = 12;

    @Builder.Default
    private Pos alignment = Pos.CENTER;

    public static EmptyStateConfig favourites() {
        return EmptyStateConfig.builder()
                .icon("♡")
                .iconStyleClass("favourites-empty-icon")
                .message("No favourites yet.\nClick ⋯ on any song and choose \"Add to Favourites\".")
                .messageStyleClass("favourites-empty-msg")
                .minHeight(300)
                .spacing(12)
                .build();
    }

    public static EmptyStateConfig playlist() {
        return EmptyStateConfig.builder()
                .icon("🎵")
                .iconStyleClass("playlist-empty-icon")
                .message("No playlists yet.\nClick \"+ New Playlist\" to start.")
                .messageStyleClass("txt-grey-md")
                .minHeight(300)
                .build();
    }

    public static EmptyStateConfig playlistSongs() {
        return EmptyStateConfig.builder()
                .icon("🎶")
                .iconStyleClass("playlist-empty-songs-icon")
                .message("This playlist is empty.\nAdd songs from My Library.")
                .messageStyleClass("empty-msg")
                .minHeight(200)
                .build();
    }

    public static EmptyStateConfig generic(String msg) {
        return EmptyStateConfig.builder()
                .icon("—")
                .message(msg)
                .minHeight(300)
                .build();
    }
}