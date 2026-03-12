package com.sebastiandorata.musicdashboard.Utils;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;


public class CardFactory {


    public static VBox createSongCard(Song song, MusicPlayerService musicPlayerService) {
        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setPrefHeight(220);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);
        card.getStyleClass().add("dashboard-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        // Album art
        ImageView albumArt = new ImageView();
        albumArt.setFitWidth(130);
        albumArt.setFitHeight(130);
        albumArt.setPreserveRatio(true);

        if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
            try {
                Image image = new Image("file:" + song.getAlbum().getAlbumArtPath(), true);
                albumArt.setImage(image);
            } catch (Exception e) {
                albumArt.setImage(null);
            }
        }

        // Song title
        Label title = new Label(song.getTitle());
        title.getStyleClass().addAll("txt-white-sm-bld");
        title.setWrapText(true);
        title.setMaxWidth(130);

        // Artist name
        String artistName = "Unknown Artist";
        if (song.getArtists() != null && !song.getArtists().isEmpty()) {
            artistName = song.getArtists().get(0).getName();
        }
        Label artist = new Label(artistName);
        artist.getStyleClass().addAll("txt-grey-sm");
        artist.setWrapText(true);
        artist.setMaxWidth(130);

        card.getChildren().addAll(albumArt, title, artist);

        // Click to play
        card.setOnMouseClicked(e -> musicPlayerService.playSong(song));

        return card;
    }


    public static VBox createAlbumCard(Album album, MusicPlayerService musicPlayerService) {
        VBox card = new VBox(10);
            card.setPrefWidth(160);
            card.setPrefHeight(220);
            card.setPadding(new Insets(15));
            card.setAlignment(Pos.TOP_CENTER);
            card.getStyleClass().add("dashboard-card");
            card.setCursor(javafx.scene.Cursor.HAND);

        // Album art
        ImageView albumArt = new ImageView();
            albumArt.setFitWidth(130);
            albumArt.setFitHeight(130);
            albumArt.setPreserveRatio(true);

        if (album.getAlbumArtPath() != null) {
            try {
                Image image = new Image("file:" + album.getAlbumArtPath(), true);
                albumArt.setImage(image);
            } catch (Exception e) {
                albumArt.setImage(null);
            }
        }

        // Album title
        Label title = new Label(album.getTitle());
        title.getStyleClass().addAll("txt-white-sm-bld");
        title.setWrapText(true);
        title.setMaxWidth(130);

        // Year
        String year = album.getReleaseDate() != null
                ? String.valueOf(album.getReleaseDate().getYear())
                : "Unknown";
        Label yearLabel = new Label(year);
        yearLabel.getStyleClass().addAll("txt-grey-sm");

        card.getChildren().addAll(albumArt, title, yearLabel);

        // Click to play first song
        card.setOnMouseClicked(e -> {
            if (album.getSongs() != null && !album.getSongs().isEmpty()) {
                musicPlayerService.playSong(album.getSongs().get(0));
            }
        });

        return card;
    }


    public static VBox createArtistCard(Artist artist, MusicPlayerService musicPlayerService) {
        VBox card = new VBox(10);
            card.setPrefWidth(160);
            card.setPrefHeight(220);
            card.setPadding(new Insets(15));
            card.setAlignment(Pos.TOP_CENTER);
            card.getStyleClass().add("dashboard-card");
            card.setCursor(javafx.scene.Cursor.HAND);

        // Artist placeholder (circular initial)
        Label artistInitial = new Label(
                artist.getName().substring(0, 1).toUpperCase()
        );
        artistInitial.getStyleClass().add("wh-grn-style");

        // Artist name
        Label name = new Label(artist.getName());
            name.getStyleClass().addAll("txt-white-sm-bld");
            name.setWrapText(true);
            name.setMaxWidth(130);

        // Song count
        int songCount = artist.getSongs() != null ? artist.getSongs().size() : 0;
            Label songsLabel = new Label(songCount + " songs");
            songsLabel.getStyleClass().addAll("txt-grey-sm");

        card.getChildren().addAll(artistInitial, name, songsLabel);

        // Click to play first song
        card.setOnMouseClicked(e -> {
            if (artist.getSongs() != null && !artist.getSongs().isEmpty()) {
                musicPlayerService.playSong(artist.getSongs().get(0));
            }
        });

        return card;
    }


    public static VBox createStatCard(String title, String value) {
        VBox card = new VBox(10);
            card.setPrefWidth(180);
            card.setPrefHeight(120);
            card.setPadding(new Insets(15));
            card.getStyleClass().add("stat-card-sm");

        Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("txt-white-sm-bld");

        Label valueLabel = new Label(value);
            valueLabel.getStyleClass().add("txt-white-sm");

        card.getChildren().addAll(titleLabel, valueLabel);

        return card;
    }
}