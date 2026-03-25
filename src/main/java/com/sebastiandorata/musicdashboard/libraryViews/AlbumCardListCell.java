package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class AlbumCardListCell extends ListCell<Album> {

    private final ImageView art      = AppUtils.buildAlbumArt(60);
    private final Label     titleLbl = new Label();
    private final Label     infoLbl  = new Label();
    private final HBox      root;

    public AlbumCardListCell() {
        titleLbl.getStyleClass().add("wt-smmd-bld");
        infoLbl.getStyleClass().add("txt-grey-md");

        VBox text = new VBox(5, titleLbl, infoLbl);
        text.setAlignment(Pos.CENTER_LEFT);

        root = new HBox(15, art, text);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("artist-list-row");
    }

    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null) { setGraphic(null); return; }

        titleLbl.setText(album.getTitle());
        int sc = album.getSongs() != null ? album.getSongs().size() : 0;
        String yr = album.getReleaseYear() != null ? String.valueOf(album.getReleaseYear()) : "Unknown";
        infoLbl.setText(sc + " songs • " + yr);

        if (album.getAlbumArtPath() != null) {
            try { art.setImage(new Image("file:" + album.getAlbumArtPath(), true)); }
            catch (Exception e) { art.setImage(null); }
        } else {
            art.setImage(null);
        }

        setGraphic(root);
    }
}