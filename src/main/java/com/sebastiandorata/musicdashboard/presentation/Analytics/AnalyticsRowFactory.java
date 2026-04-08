package com.sebastiandorata.musicdashboard.presentation.Analytics;

import com.sebastiandorata.musicdashboard.dto.HistoryRowData;
import com.sebastiandorata.musicdashboard.dto.TopAlbumRowData;
import com.sebastiandorata.musicdashboard.dto.TopArtistRowData;
import com.sebastiandorata.musicdashboard.dto.TopSongRowData;
import com.sebastiandorata.musicdashboard.presentation.helpers.RowConfig;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Static factory for analytics row UI components.
 *
 * <p><b><u>Clickable rows:</u></b></p>
 * <ul>
 *   <li>Song rows: single click starts playback via an onPlay Runnable.</li>
 *   <li>Album rows: single click navigates to album drill-down via an onOpen Runnable.</li>
 *   <li>History rows: single click starts playback via an onPlay Runnable.</li>
 * </ul>
 *
 * <p><b><u>Every row type has:</u></b></p>
 * <ul>
 *   <li>A no-arg overload (original name), non-clickable, keeps old call sites compiling.</li>
 *   <li>A two-arg overload (item + Runnable), clickable version used by AnalyticsController.</li>
 * </ul>
 *
 * <p>SRP: Only responsible for building row nodes from ViewModel DTOs.
 * No Spring dependencies, pure UI construction.</p>
 *
 * <p>Time Complexity: O(1) per row.</p>
 * <p>Space Complexity: O(1) per row.</p>
 */
public class AnalyticsRowFactory {

    private AnalyticsRowFactory() {}



    /** Non-clickable overload. Keeps existing call sites unchanged. */
    public static HBox createHistoryRow(HistoryRowData item) {
        return buildHistoryRow(item, null);
    }

    /**
     * Clickable overload. Single click invokes {@code onPlay}.
     *
     * @param item   The history row data (must have a non-null {@code song} entity field)
     * @param onPlay Runnable invoked on click {@code () -> musicPlayerService.playSong(item.song)}
     */
    public static HBox createHistoryRow(HistoryRowData item, Runnable onPlay) {
        return buildHistoryRow(item, onPlay);
    }

    private static HBox buildHistoryRow(HistoryRowData item, Runnable onPlay) {
        RowConfig config = RowConfig.historyRow();
        HBox row = buildBaseRow(config);

        Label songLabel   = buildLabel(item.songTitle,  config.getPrimaryLabelStyleClass(),   config.getPrimaryLabelMinWidth());
        Label artistLabel = buildLabel(item.artistName, config.getSecondaryLabelStyleClass(), config.getSecondaryLabelMinWidth());
        Label dateLabel   = buildLabel(item.playedAt,   config.getMetaLabelStyleClass(),      0);

        HBox.setHgrow(songLabel, Priority.ALWAYS);
        row.getChildren().addAll(songLabel, artistLabel, dateLabel);

        if (onPlay != null) applyClickable(row, onPlay);
        return row;
    }


    /** Non-clickable overload Keeps existing call sites unchanged. */
    public static HBox topSongRow(TopSongRowData item) {
        return buildTopSongRow(item, null);
    }

    /** Non-clickable overload. Keeps existing call sites unchanged. */
    public static HBox createTopSongRow(TopSongRowData item) {
        return buildTopSongRow(item, null);
    }

    /**
     * Clickable overload. Single click invokes {@code onPlay}.
     *
     * @param item   The song row data (must have a non-null {@code song} entity field)
     * @param onPlay Runnable invoked on click {@code () -> musicPlayerService.playSong(item.song)}
     */
    public static HBox createTopSongRow(TopSongRowData item, Runnable onPlay) {
        return buildTopSongRow(item, onPlay);
    }

    private static HBox buildTopSongRow(TopSongRowData item, Runnable onPlay) {
        RowConfig config = RowConfig.topSongRow();
        HBox row = buildBaseRow(config);

        Label songLabel   = buildLabel(item.songTitle,  config.getPrimaryLabelStyleClass(),   config.getPrimaryLabelMinWidth());
        Label artistLabel = buildLabel(item.artistName, config.getSecondaryLabelStyleClass(), 0);

        HBox.setHgrow(songLabel, Priority.ALWAYS);
        row.getChildren().addAll(songLabel, artistLabel);

        if (onPlay != null) applyClickable(row, onPlay);
        return row;
    }


    /** Non-clickable overload. Keeps existing call sites unchanged. */
    public static HBox topAlbumRow(TopAlbumRowData item) {
        return buildTopAlbumRow(item, null);
    }
    /** Non-clickable overload. Keeps existing call sites unchanged. */
    public static HBox createTopAlbumRow(TopAlbumRowData item) {
        return buildTopAlbumRow(item, null);
    }

    /**
     * Clickable overload. Single click invokes {@code onOpen}.
     *
     * @param item   The album row data (must have a non-null {@code album} entity field)
     * @param onOpen Runnable invoked on click {@code () -> myLibraryController.showWithAlbum(item.album)}
     */
    public static HBox createTopAlbumRow(TopAlbumRowData item, Runnable onOpen) {
        return buildTopAlbumRow(item, onOpen);
    }

    private static HBox buildTopAlbumRow(TopAlbumRowData item, Runnable onOpen) {
        RowConfig config = RowConfig.topAlbumRow();
        HBox row = buildBaseRow(config);

        Label albumLabel = buildLabel(item.albumTitle, config.getPrimaryLabelStyleClass(), config.getPrimaryLabelMinWidth());
        Label yearLabel  = buildLabel("(" + item.releaseYear + ")", config.getSecondaryLabelStyleClass(), 0);

        HBox.setHgrow(albumLabel, Priority.ALWAYS);
        row.getChildren().addAll(albumLabel, yearLabel);

        if (onOpen != null) applyClickable(row, onOpen);
        return row;
    }


    public static HBox artistRow(TopArtistRowData item) {
        return buildArtistRow(item);
    }

    public static HBox createArtistRow(TopArtistRowData item) {
        return buildArtistRow(item);
    }

    private static HBox buildArtistRow(TopArtistRowData item) {
        RowConfig config = RowConfig.analyticsArtistRow();
        HBox row = buildBaseRow(config);

        Label artistLabel = buildLabel(item.artistName, config.getPrimaryLabelStyleClass(), config.getPrimaryLabelMinWidth());
        Label timeLabel   = buildLabel(item.listeningTime, config.getMetaLabelStyleClass(), 0);

        HBox.setHgrow(artistLabel, Priority.ALWAYS);
        row.getChildren().addAll(artistLabel, timeLabel);
        return row;
    }


    private static HBox buildBaseRow(RowConfig config) {
        HBox row = new HBox(config.getSpacing());
        row.setAlignment(config.getAlignment());
        row.setPadding(new Insets(8));
        row.getStyleClass().add(config.getRowStyleClass());
        return row;
    }

    private static Label buildLabel(String text, String styleClass, double minWidth) {
        Label label = new Label(text != null ? text : "—");
        label.getStyleClass().add(styleClass);
        if (minWidth > 0) label.setMinWidth(minWidth);
        return label;
    }

    /**
     * Applies clickable styling and a click handler to a row.
     * Adds a hand cursor and a subtle opacity change on hover.
     */
    private static void applyClickable(HBox row, Runnable onClick) {
        row.setStyle(row.getStyle() + "; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> row.setOpacity(0.75));
        row.setOnMouseExited(e  -> row.setOpacity(1.0));
        row.setOnMouseClicked(e -> onClick.run());
    }
}