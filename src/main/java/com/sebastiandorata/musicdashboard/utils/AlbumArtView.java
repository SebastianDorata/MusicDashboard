package com.sebastiandorata.musicdashboard.utils;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class AlbumArtView {
    public ImageView albumArt;
    public Label artPlaceholder;

    public AlbumArtView(ImageView albumArt, Label artPlaceholder) {
        this.albumArt = albumArt;
        this.artPlaceholder = artPlaceholder;
    }
}
