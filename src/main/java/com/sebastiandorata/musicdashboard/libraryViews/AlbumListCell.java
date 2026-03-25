package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class AlbumListCell extends ListCell<Album> {

    private final Label titleLbl = new Label();
    private final Label infoLbl  = new Label();
    private final VBox  root     = new VBox(3, titleLbl, infoLbl);

    public AlbumListCell() {
        titleLbl.getStyleClass().add("wt-smmd-bld");
        infoLbl.getStyleClass().add("txt-grey-md");
        root.getStyleClass().add("artist-list-row");
    }

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