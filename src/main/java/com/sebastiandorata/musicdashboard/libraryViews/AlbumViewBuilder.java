package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class AlbumViewBuilder {

    private final LibraryContext ctx;
    private final Runnable onBack;
    private final Consumer<Artist> onArtistClick;

    public AlbumViewBuilder(LibraryContext ctx, Runnable onBack, Consumer<Artist> onArtistClick) {
        this.ctx           = ctx;
        this.onBack        = onBack;
        this.onArtistClick = onArtistClick;
    }

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
        back.getStyleClass().add("btn-blue");
        back.setOnAction(e -> onBack.run());

        Label title = new Label(album.getTitle());
        title.getStyleClass().add("section-title");

        row.getChildren().addAll(back, title);
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
                artwork.setImage(new Image("file:" + album.getAlbumArtPath(), true));
            } catch (Exception ignored) {}
        }

        section.getChildren().add(artwork);
        return section;
    }

    private HBox buildArtistSection(Album album) {
        HBox section = new HBox();
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
        List<Artist> artists = album.getArtists();
        if (artists != null && !artists.isEmpty()) return artists;

        if (album.getSongs() == null) return List.of();

        return album.getSongs().stream()
                .filter(s -> s.getArtists() != null)
                .flatMap(s -> s.getArtists().stream())
                .filter(a -> a != null && a.getName() != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private ListView<Song> buildSongList(Album album) {
        ListView<Song> lv = new ListView<>();
        lv.setPrefHeight(400);
        lv.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(lv, Priority.ALWAYS);
        lv.getStyleClass().add("album-songs-list");

        if (album.getSongs() == null || album.getSongs().isEmpty()) {
            lv.setPlaceholder(new Label("No songs in this album"));
            return lv;
        }

        List<Song> sorted = album.getSongs().stream()
                .sorted((a, b) -> {
                    int ta = a.getTrackNum() != null && a.getTrackNum() > 0 ? a.getTrackNum() : Integer.MAX_VALUE;
                    int tb = b.getTrackNum() != null && b.getTrackNum() > 0 ? b.getTrackNum() : Integer.MAX_VALUE;
                    return Integer.compare(ta, tb);
                })
                .collect(Collectors.toList());

        lv.setCellFactory(list -> new AlbumSongCell(ctx));
        lv.getItems().addAll(sorted);

        lv.setOnMouseClicked(e -> {
            Song selected = lv.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ctx.musicPlayerService.setQueue(sorted);
                ctx.musicPlayerService.playSong(selected);
            }
        });

        return lv;
    }

}