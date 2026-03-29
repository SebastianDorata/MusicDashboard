package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class FavouritesViewBuilder {

    private final LibraryHandler ctx;
    private final SongViewBuilder songListBuilder;

    public FavouritesViewBuilder(LibraryHandler ctx) {
        this.ctx             = ctx;
        this.songListBuilder = new SongViewBuilder(ctx);
    }

    public VBox build(String displayMode) {
        VBox view = new VBox(12);
        view.setFillWidth(true);

        List<Song> songs;
        try {
            songs = ctx.favouriteService.getUserFavouritesSortedByDate();
        } catch (Exception e) {
            songs = List.of();
        }

        Label header = new Label("Favourites (" + songs.size() + ")");
        header.getStyleClass().add("song-header");
        view.getChildren().add(header);

        if (songs.isEmpty()) {
            view.getChildren().add(buildEmptyState());
        } else if ("list".equals(displayMode)) {
            view.getChildren().add(songListBuilder.buildListView(songs));
        } else {
            view.getChildren().add(songListBuilder.buildGridView(songs));
        }

        return view;
    }

    private VBox buildEmptyState() {
        VBox empty = new VBox(12);
        empty.setAlignment(Pos.CENTER);
        empty.setMinHeight(300);

        Label icon = new Label("♡");
        icon.getStyleClass().add("favourites-empty-icon");

        Label msg = new Label("No favourites yet.\nClick ⋯ on any song and choose \"Add to Favourites\".");
        msg.getStyleClass().add("favourites-empty-msg");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        empty.getChildren().addAll(icon, msg);
        return empty;
    }
}