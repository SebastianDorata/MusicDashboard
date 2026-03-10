package com.sebastiandorata.musicdashboard.Utils;
import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

//https://openjfx.io/javadoc/25/javafx.controls/javafx/scene/control/Cell.html
public class SongCell extends ListCell<Song> {

    private final HBox root;
    private final ImageView albumArt;
    private final Label title;
    private final Label artist;

    public SongCell() {
        albumArt = new ImageView();
        albumArt.setFitWidth(40);
        albumArt.setFitHeight(40);
        albumArt.setPreserveRatio(true);

        title = new Label();
        title.getStyleClass().add("song-title");

        artist = new Label();
        artist.getStyleClass().add("song-artist");

        VBox textBox = new VBox(title, artist);
        textBox.setAlignment(Pos.CENTER_LEFT);

        root = new HBox(10, albumArt, textBox);
        root.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);

        if (empty || song == null) {
            setGraphic(null);
        } else {
            // Title
            title.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");

            // Artist (use first artist in list if available)
            if (song.getArtists() != null && !song.getArtists().isEmpty()) {
                artist.setText(song.getArtists().get(0).getName() != null
                        ? song.getArtists().get(0).getName()
                        : "Unknown Artist");
            } else {
                artist.setText("Unknown Artist");
            }

            // Album art
            if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
                try {
                    Image image = new Image("file:" + song.getAlbum().getAlbumArtPath(), true);
                    albumArt.setImage(image);
                } catch (Exception e) {
                    albumArt.setImage(null); // fallback if image fails to load
                }
            } else {
                albumArt.setImage(null); // no album art
            }

            setGraphic(root);
        }
    }
}