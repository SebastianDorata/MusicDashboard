package com.sebastiandorata.musicdashboard.utils;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;

//https://openjfx.io/javadoc/25/javafx.controls/javafx/scene/control/Cell.html
public class SongCell extends ListCell<Song> {

    private final HBox root;
    private final ImageView albumArt;
    private final Label title;
    private final Label artist;
    private final Button menuBtn;

    private final BiConsumer<Song, javafx.scene.Node> onMenuClicked;


    public SongCell(List<Song> queue, MusicPlayerService musicPlayerService, PlaylistService playlistService, FavouriteService favouriteService, BiConsumer<Song, javafx.scene.Node> onMenuClicked) {
        this.onMenuClicked = onMenuClicked;
        this.menuBtn = buildMenuButton();
        this.albumArt = AppUtils.buildAlbumArt(40);
        this.title    = new Label();
        this.artist   = new Label();
        title.getStyleClass().add("song-list-title");
        artist.getStyleClass().add("song-list-artist");

        VBox textBox = new VBox(title, artist);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root = new HBox(10, albumArt, textBox, spacer, menuBtn);
        root.setAlignment(Pos.CENTER_LEFT);
    }


    public SongCell() {
        this.onMenuClicked = null;
        this.menuBtn       = null;
        this.albumArt      = AppUtils.buildAlbumArt(40);
        this.title         = new Label();
        this.artist        = new Label();
        title.getStyleClass().add("song-list-title");
        artist.getStyleClass().add("song-list-artist");

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
            title.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");

            if (song.getArtists() != null && !song.getArtists().isEmpty()) {
                artist.setText(song.getArtists().get(0).getName() != null
                        ? song.getArtists().get(0).getName()
                        : "Unknown Artist");
            } else {
                artist.setText("Unknown Artist");
            }

            if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
                try {
                    albumArt.setImage(new Image("file:" + song.getAlbum().getAlbumArtPath(), true));
                } catch (Exception e) {
                    albumArt.setImage(null);
                }
            } else {
                albumArt.setImage(null);
            }

            if (menuBtn != null && onMenuClicked != null) {
                menuBtn.setOnAction(e -> onMenuClicked.accept(song, menuBtn));
            }

            setGraphic(root);
        }
    }


    private Button buildMenuButton() {
        Button btn = new Button("⋯");
        btn.getStyleClass().add("song-row-menu-btn");
        btn.setOnMousePressed(javafx.event.Event::consume);
        return btn;
    }
}