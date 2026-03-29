package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.CardFactory;
import com.sebastiandorata.musicdashboard.utils.SongCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;


public class SongViewBuilder {

    private final LibraryHandler ctx;

    public SongViewBuilder(LibraryHandler ctx) {
        this.ctx = ctx;
    }


    public ListView<Song> buildListView(List<Song> songs) {
        ListView<Song> lv = new ListView<>();
        lv.setPrefHeight(AppUtils.APP_HEIGHT - 160);
        lv.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(lv, Priority.ALWAYS);
        lv.getStyleClass().add("main-bkColour");
        lv.setCellFactory(list -> new SongCell(
                songs,
                ctx.musicPlayerService,
                ctx.playlistService,
                ctx.favouriteService,
                ctx.onSongMenu
        ));
        lv.getItems().addAll(songs);
        lv.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                ctx.musicPlayerService.setQueue(songs);
                ctx.musicPlayerService.playSong(selected);
            }
        });
        return lv;
    }


    public TilePane buildGridView(List<Song> songs) {
        TilePane grid = buildBaseTilePane();
        for (Song song : songs) {
            VBox card = CardFactory.createSongCard(song, ctx.musicPlayerService);
            card.setOnMouseClicked(e -> {
                ctx.musicPlayerService.setQueue(songs);
                ctx.musicPlayerService.playSong(song);
            });
            grid.getChildren().add(card);
        }
        return grid;
    }


    public TilePane buildAlbumGridView(List<com.sebastiandorata.musicdashboard.entity.Album> albums,
                                       Runnable onAlbumClick,
                                       java.util.function.Consumer<com.sebastiandorata.musicdashboard.entity.Album> onSelect) {
        TilePane grid = buildBaseTilePane();
        for (var album : albums) {
            VBox card = CardFactory.createAlbumCard(album, ctx.musicPlayerService);
            card.setOnMouseClicked(e -> {
                onSelect.accept(album);
                onAlbumClick.run();
            });
            grid.getChildren().add(card);
        }
        return grid;
    }

    private TilePane buildBaseTilePane() {
        TilePane grid = new TilePane();
        grid.getStyleClass().add("tile-pane");
        grid.setPrefColumns(5);
        return grid;
    }
}