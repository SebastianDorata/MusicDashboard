package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * A {@link ListCell} that renders an {@link Album} as a horizontal card row,
 * showing a 60px album art thumbnail on the left and the album title and
 * metadata (song count and release year) stacked on the right.
 *
 * <p>Intended for use in list views where albums are displayed with artwork,
 * such as the artist detail or album browser screens.</p>
 *
 * <p>A static {@link #buildRow(Album)} helper is also provided so that the
 * same visual row can be embedded directly into a {@link VBox} Used by the
 * alphabet-grouped album list in {@code MyLibraryController}.</p>
 */
public class AlbumCardListCell extends ListCell<Album> {

    private final ImageView art      = AppUtils.buildAlbumArt(60);
    private final Label     titleLbl = new Label();
    private final Label     infoLbl  = new Label();
    private final HBox      root;

    /**
     * Constructs an {@code AlbumCardListCell}, initializing and arranging
     * the artwork thumbnail, title label, and info label into a single
     * horizontal row.
     */
    public AlbumCardListCell() {
        titleLbl.getStyleClass().add("wt-smmd-bld");
        infoLbl.getStyleClass().add("txt-grey-md");

        VBox text = new VBox(5, titleLbl, infoLbl);
        text.setAlignment(Pos.CENTER_LEFT);

        root = new HBox(15, art, text);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("artist-list-row");
    }

    /**
     * Updates the cell's content for the given {@link Album}.
     *
     * <p>Sets the title label, builds the info string from song count and
     * release year, and attempts to load the album art image from the file
     * system. If the art path is absent or the image fails to load, the
     * {@link ImageView} is left empty.</p>
     *
     * @param album the album to display, or {@code null} if the cell is empty
     * @param empty {@code true} if this cell represents an empty row
     */
    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null) { setGraphic(null); return; }
        setGraphic(buildRow(album));
    }

    /**
     * Builds and returns a standalone {@link HBox} row for the given album,
     * without requiring a {@link javafx.scene.control.ListView} context.
     *
     * <p>This is used by the alphabet-grouped album list view in
     * {@code MyLibraryController} where each row is added directly to a
     * {@link javafx.scene.layout.VBox} rather than a {@code ListView}.</p>
     *
     * @param album the album to render; must not be {@code null}
     * @return a fully populated {@link HBox} row ready to add to the scene graph
     */
    public static HBox buildRow(Album album) {
        ImageView art = AppUtils.buildAlbumArt(60);

        Label titleLbl = new Label(album.getTitle());
        titleLbl.getStyleClass().add("wt-smmd-bld");

        int sc = album.getSongs() != null ? album.getSongs().size() : 0;
        String yr = album.getReleaseYear() != null ? String.valueOf(album.getReleaseYear()) : "Unknown";
        Label infoLbl = new Label(sc + " songs • " + yr);
        infoLbl.getStyleClass().add("txt-grey-md");

        if (album.getAlbumArtPath() != null) {
            try { art.setImage(new Image("file:" + album.getAlbumArtPath(), true)); }
            catch (Exception ignored) {}
        }

        VBox text = new VBox(5, titleLbl, infoLbl);
        text.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(15, art, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("artist-list-row");
        return row;
    }
}