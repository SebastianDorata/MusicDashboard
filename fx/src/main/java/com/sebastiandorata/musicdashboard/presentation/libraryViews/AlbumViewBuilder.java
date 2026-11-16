package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.utils.AppUtils;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Builds the full album detail view displayed when a user selects an album.
 *
 * <p>The view is composed of four sections stacked vertically:</p>
 * <ul>
 *   <li>A back-navigation row with the album title</li>
 *   <li>A 300×300 artwork panel</li>
 *   <li>A clickable artist name that navigates to the artist detail view</li>
 *   <li>A {@link ListView} of the album's songs sorted by track number,
 *       backed by {@link AlbumSongCell} rows</li>
 * </ul>
 *
 * <p>Artist resolution falls back from the album's own artist list to
 * artists inferred across its songs when no direct relationship exists.</p>
 */
public class AlbumViewBuilder {

    private final LibraryHandler ctx;
    private final Runnable onBack;
    private final Consumer<Artist> onArtistClick;

    /**
     * Constructs an {@code AlbumViewBuilder}.
     *
     * @param ctx           the {@link LibraryHandler} providing services and callbacks
     * @param onBack        callback invoked when the user presses "Back to Albums"
     * @param onArtistClick callback invoked when the user clicks the artist name label,
     *                      receiving the first resolved {@link Artist}
     */
    public AlbumViewBuilder(LibraryHandler ctx, Runnable onBack, Consumer<Artist> onArtistClick) {
        this.ctx           = ctx;
        this.onBack        = onBack;
        this.onArtistClick = onArtistClick;
    }

    /**
     * Builds and returns the complete album detail {@link VBox} for the given album.
     *
     * @param album the {@link Album} to display
     * @return a {@link VBox} containing the back row, artwork, artist section,
     *         and song list
     */
    public VBox build(Album album) {
        VBox detail = new VBox(15);
        detail.setFillWidth(true);

        detail.getChildren().addAll(
                buildBackRow(album),
                buildArtworkSection(album),
                buildArtistSection(album),
                buildSongList(album)
        );
        VBox.setVgrow(detail, Priority.ALWAYS);

        return detail;
    }

    private HBox buildBackRow(Album album) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("album-detail-header");

        Button back = new Button("← Back to Albums");
        back.getStyleClass().addAll("nav-btn-back","txt-white-md-bld");
        back.setOnAction(e -> onBack.run());

        row.getChildren().add(back);
        return row;
    }

    private VBox buildArtworkSection(Album album) {
        VBox section = new VBox();
        section.getStyleClass().add("album-artwork-section");

        ImageView artwork = new ImageView();
        artwork.setFitWidth(300);
        artwork.setFitHeight(300);
        artwork.setPreserveRatio(true);

        if (album.getAlbumArtPath() != null) {
            try {
                artwork.setImage(new Image(AppUtils.toImageUri(album.getAlbumArtPath()), true));
            } catch (Exception ignored) {}
        }

        section.getChildren().add(artwork);
        return section;
    }

    private VBox buildArtistSection(Album album) {
        VBox section = new VBox();

        Label title = new Label(album.getTitle());
        title.getStyleClass().add("section-title");
        section.getChildren().add(title);

        section.getStyleClass().add("album-artist-section");

        List<Artist> artists = resolveArtists(album);

        if (artists.isEmpty()) {
            Label lbl = new Label("Unknown Artist");
            lbl.getStyleClass().add("album-artist-label");
            lbl.getStyleClass().add("txt-grey-md");
            section.getChildren().add(lbl);
            return section;
        }

        String names = artists.stream().map(Artist::getName).collect(Collectors.joining(", "));
        Label lbl = new Label(names);
        lbl.getStyleClass().add("album-artist-label");
        lbl.setOnMouseClicked(e -> onArtistClick.accept(artists.get(0)));
        section.getChildren().add(lbl);
        return section;
    }

    private List<Artist> resolveArtists(Album album) {
        if (album.getArtists() != null && !album.getArtists().isEmpty()) {
            return new ArrayList<>(album.getArtists());
        }

        if (album.getSongs() == null) return List.of();

        return album.getSongs().stream()
                .filter(s -> s.getArtists() != null)
                .flatMap(s -> s.getArtists().stream())
                .filter(a -> a != null && a.getName() != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private VBox buildSongList(Album album) {
        VBox list = new VBox(0);
        list.setFillWidth(true);
        VBox.setVgrow(list, Priority.ALWAYS);
        list.getStyleClass().add("album-songs-list");

        if (album.getSongs() == null || album.getSongs().isEmpty()) {
            Label empty = new Label("No songs in this album");
            empty.getStyleClass().add("txt-grey-md");
            list.getChildren().add(empty);
            return list;
        }

        List<Song> sorted = album.getSongs().stream()
                .sorted((a, b) -> {
                    int ta = a.getTrackNum() != null && a.getTrackNum() > 0 ? a.getTrackNum() : Integer.MAX_VALUE;
                    int tb = b.getTrackNum() != null && b.getTrackNum() > 0 ? b.getTrackNum() : Integer.MAX_VALUE;
                    return Integer.compare(ta, tb);
                })
                .collect(Collectors.toList());

        for (Song song : sorted) {
            AlbumSongCell cell = new AlbumSongCell(ctx);
            cell.updateItem(song, false);
            javafx.scene.Node row = cell.getGraphic();
            if (row != null) {
                row.setOnMouseClicked(e -> {
                    ctx.musicPlayerService().setQueue(sorted);
                    ctx.musicPlayerService().playSong(song);
                });
                list.getChildren().add(row);
            }
        }

        return list;
    }
}