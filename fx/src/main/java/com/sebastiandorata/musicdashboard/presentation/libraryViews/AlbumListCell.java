package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

/**
 * A lightweight {@link ListCell} that renders an {@link Album} as a compact
 * two-line row showing the album title and a summary of its song count and
 * release year.
 *
 * <p>Unlike {@link AlbumCardListCell}, this cell has no artwork thumbnail and
 * is suited for denser list views such as the artist detail album list.</p>
 */
public class AlbumListCell extends ListCell<Album> {

    private final Label titleLbl = new Label();
    private final Label infoLbl  = new Label();
    private final VBox  root     = new VBox(3, titleLbl, infoLbl);

    public AlbumListCell() {
        titleLbl.getStyleClass().add("wt-smmd-bld");
        infoLbl.getStyleClass().add("txt-grey-md");
        root.getStyleClass().add("artist-list-row");
    }

    /**
     * Updates the cell's content for the given {@link Album}.
     *
     * <p>Populates the title label and builds an info string combining the
     * album's song count and release year. If the release year is {@code null},
     * "Unknown" is displayed in its place.</p>
     *
     * @param album the album to display, or {@code null} if the cell is empty
     * @param empty {@code true} if this cell represents an empty row
     */
    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null) { setGraphic(null); return; }

        titleLbl.setText(album.getTitle());
        int songs = album.getSongs() != null ? album.getSongs().size() : 0;
        String year = album.getReleaseYear() != null
                ? String.valueOf(album.getReleaseYear()) : "Unknown";
        infoLbl.setText(songs + " songs • " + year);
        setGraphic(root);
    }
}