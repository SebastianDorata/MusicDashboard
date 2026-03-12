package com.sebastiandorata.musicdashboard.Utils;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class AlbumArtComponents {
    public ImageView albumArt;
    public Label artPlaceholder;

    public AlbumArtComponents(ImageView albumArt, Label artPlaceholder) {
        this.albumArt = albumArt;
        this.artPlaceholder = artPlaceholder;
    }
}
