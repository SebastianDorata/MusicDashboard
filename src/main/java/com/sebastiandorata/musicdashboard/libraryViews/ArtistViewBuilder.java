package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.service.LibraryService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ArtistViewBuilder {

    private final LibraryHandler ctx;
    private final SongViewBuilder songListBuilder;
    private final Consumer<Album>     onAlbumSelected;
    private final LibraryService      libraryService;

    public ArtistViewBuilder(LibraryHandler ctx,
                             Consumer<Album> onAlbumSelected,
                             LibraryService libraryService) {
        this.ctx             = ctx;
        this.songListBuilder = new SongViewBuilder(ctx);
        this.onAlbumSelected = onAlbumSelected;
        this.libraryService  = libraryService;
    }

    public ScrollPane buildArtistList(List<Artist> artists, Consumer<Artist> onArtistSelected) {
        List<Artist> sorted = artists.stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        Map<String, List<Artist>> grouped = new LinkedHashMap<>();
        for (Artist artist : sorted) {
            String letter = artist.getName().substring(0, 1).toUpperCase();
            if (!Character.isLetter(letter.charAt(0))) letter = "#";
            grouped.computeIfAbsent(letter, k -> new ArrayList<>()).add(artist);
        }

        VBox content = new VBox(0);
        content.setFillWidth(true);

        for (Map.Entry<String, List<Artist>> entry : grouped.entrySet()) {
            content.getChildren().add(buildAlphaDivider(entry.getKey()));
            for (Artist artist : entry.getValue()) {
                content.getChildren().add(buildArtistRow(artist, onArtistSelected));
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private HBox buildAlphaDivider(String letter) {
        HBox divider = new HBox(10);
        divider.setAlignment(Pos.CENTER_LEFT);
        divider.getStyleClass().add("artist-alpha-divider");

        Label letterLabel = new Label(letter);
        letterLabel.getStyleClass().add("artist-alpha-letter");

        Region line = new Region();
        line.getStyleClass().add("artist-alpha-line");
        HBox.setHgrow(line, Priority.ALWAYS);

        divider.getChildren().addAll(letterLabel, line);
        return divider;
    }

    private HBox buildArtistRow(Artist artist, Consumer<Artist> onArtistSelected) {
        HBox row = new HBox(0);
        row.getStyleClass().add("artist-list-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(2);
        Label name = new Label(artist.getName());
        name.getStyleClass().add("artist-list-name");

        int songs  = artist.getSongs()  != null ? artist.getSongs().size()  : 0;
        int albums = artist.getAlbums() != null ? artist.getAlbums().size() : 0;
        Label info = new Label(songs + " songs • " + albums + " albums");
        info.getStyleClass().add("artist-list-info");

        text.getChildren().addAll(name, info);
        HBox.setHgrow(text, Priority.ALWAYS);
        row.getChildren().add(text);

        row.setOnMouseClicked(e -> onArtistSelected.accept(artist));
        return row;
    }

    public VBox buildArtistDetail(Artist artist, String displayMode, Runnable onBack) {
        VBox detail = new VBox(15);
        detail.setFillWidth(true);
        VBox.setVgrow(detail, Priority.ALWAYS);

        detail.getChildren().add(buildArtistBackRow(artist, onBack));
        detail.getChildren().add(buildAlbumSection(artist, displayMode));

        return detail;
    }

    private HBox buildArtistBackRow(Artist artist, Runnable onBack) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("album-detail-header");

        Button back = new Button("← Back to Artists");
        back.getStyleClass().add("btn-blue");
        back.setOnAction(e -> onBack.run());

        Label name = new Label(artist.getName());
        name.getStyleClass().add("artist-detail-name");

        row.getChildren().addAll(back, name);
        return row;
    }

    private Node buildAlbumSection(Artist artist, String displayMode) {
        List<Album> albums = libraryService.resolveAlbumsForArtist(artist);

        if (albums.isEmpty()) {
            Label empty = new Label("No albums found for this artist.");
            empty.getStyleClass().add("txt-grey-md");
            return empty;
        }

        if ("grid".equals(displayMode)) {
            return songListBuilder.buildAlbumGridView(albums, () -> {}, onAlbumSelected);
        } else {
            return buildAlbumListView(albums);
        }
    }

    private ListView<Album> buildAlbumListView(List<Album> albums) {
        ListView<Album> lv = new ListView<>();
        lv.setPrefHeight(600);
        lv.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(lv, Priority.ALWAYS);
        lv.getStyleClass().add("main-bkColour");
        lv.setCellFactory(list -> new AlbumListCell());
        lv.getItems().addAll(albums);
        lv.setOnMouseClicked(e -> {
            Album selected = lv.getSelectionModel().getSelectedItem();
            if (selected != null) onAlbumSelected.accept(selected);
        });
        return lv;
    }
}