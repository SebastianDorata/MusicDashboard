package com.sebastiandorata.musicdashboard.presentation.helpers;

import javafx.geometry.Pos;
import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for row-based UI components.
 * Supports history, artist, song, and album rows with customizable styling and spacing.
 */
@Builder
@Getter
public class RowConfig {

    @Builder.Default
    private double spacing = 10;

    @Builder.Default
    private String rowStyleClass = "analytics-list-item";

    @Builder.Default
    private String primaryLabelStyleClass = "txt-white-sm";

    @Builder.Default
    private String secondaryLabelStyleClass = "analytics-row-secondary";

    @Builder.Default
    private String metaLabelStyleClass = "analytics-row-meta";

    @Builder.Default
    private double primaryLabelMinWidth = 200;

    @Builder.Default
    private double secondaryLabelMinWidth = 150;

    @Builder.Default
    private double metaLabelMinWidth = 100;

    @Builder.Default
    private Pos alignment = Pos.CENTER_LEFT;


    public static RowConfig historyRow() {
        return RowConfig.builder()
                .spacing(15)
                .rowStyleClass("analytics-list-item")
                .primaryLabelStyleClass("txt-white-sm")
                .secondaryLabelStyleClass("analytics-row-secondary")
                .metaLabelStyleClass("analytics-row-meta")
                .primaryLabelMinWidth(200)
                .secondaryLabelMinWidth(150)
                .build();
    }

    public static RowConfig topSongRow() {
        return RowConfig.builder()
                .spacing(15)
                .rowStyleClass("analytics-list-item")
                .primaryLabelStyleClass("txt-white-sm")
                .secondaryLabelStyleClass("analytics-row-secondary")
                .primaryLabelMinWidth(200)
                .build();
    }

    public static RowConfig topAlbumRow() {
        return RowConfig.builder()
                .spacing(15)
                .rowStyleClass("analytics-list-item")
                .primaryLabelStyleClass("txt-white-sm")
                .secondaryLabelStyleClass("analytics-row-meta")
                .primaryLabelMinWidth(200)
                .build();
    }

    /**
     * Row config for the analytics Top Artists view.
     * Distinct from artistRow() which is used by the dashboard panel
     * and references dashboard-specific CSS classes (artist-name, artist-time).
     */
    public static RowConfig analyticsArtistRow() {
        return RowConfig.builder()
                .spacing(15)
                .rowStyleClass("analytics-list-item")
                .primaryLabelStyleClass("txt-white-sm")
                .metaLabelStyleClass("analytics-row-secondary")
                .primaryLabelMinWidth(200)
                .build();
    }


    public static RowConfig playlistSongRow() {
        return RowConfig.builder()
                .spacing(12)
                .rowStyleClass("playlist-song-row")
                .primaryLabelStyleClass("txt-white-sm")
                .secondaryLabelStyleClass("playlist-song-artist")
                .metaLabelStyleClass("playlist-song-main")
                .primaryLabelMinWidth(300)
                .metaLabelMinWidth(80)
                .build();
    }
}