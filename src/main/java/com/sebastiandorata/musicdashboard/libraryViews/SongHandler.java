package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.List;


public class SongHandler {

    private final LibraryHandler ctx;

    public SongHandler(LibraryHandler ctx) {
        this.ctx = ctx;
    }


    public void show(Song song, Node anchor) {
        ContextMenu menu = new ContextMenu();

        MenuItem addToPlaylist = new MenuItem("＋  Add to Playlist");
        addToPlaylist.setOnAction(e -> showAddToPlaylistDialog(song));

        MenuItem toggleFav = buildFavouriteMenuItem(song);

        menu.getItems().addAll(addToPlaylist, toggleFav);
        menu.show(anchor, Side.BOTTOM, 0, 0);
    }



    private MenuItem buildFavouriteMenuItem(Song song) {
        boolean isFav;
        try {
            isFav = ctx.favouriteService.isFavourited(song);
        } catch (Exception e) {
            isFav = false;
        }

        MenuItem item = new MenuItem(isFav ? "♥  Remove from Favourites" : "♡  Add to Favourites");
        item.setOnAction(e -> {
            try {
                ctx.favouriteService.toggleFavourite(song);
            } catch (Exception ex) {
                AppUtils.showError("Could not update favourites: " + ex.getMessage());
            }
        });
        return item;
    }



    private void showAddToPlaylistDialog(Song song) {
        List<Playlist> playlists;
        try {
            playlists = ctx.playlistService.getCurrentUserPlaylists();
        } catch (Exception e) {
            AppUtils.showError("Could not load playlists: " + e.getMessage());
            return;
        }

        if (playlists.isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("No Playlists");
            info.setHeaderText(null);
            info.setContentText("You have no playlists yet. Create one from the Playlists screen.");
            info.showAndWait();
            return;
        }

        Dialog<Playlist> dialog = new Dialog<>();
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Choose a playlist for \"" + song.getTitle() + "\"");

        ListView<Playlist> picker = new ListView<>();
        picker.setPrefHeight(200);
        picker.getItems().addAll(playlists);
        picker.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Playlist pl, boolean empty) {
                super.updateItem(pl, empty);
                if (empty || pl == null) { setText(null); return; }
                int n = pl.getSongs() != null ? pl.getSongs().size() : 0;
                setText(pl.getName() + "  (" + n + " songs)");
            }
        });

        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(true);
        picker.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> okBtn.setDisable(sel == null));

        dialog.setResultConverter(btn -> btn == ButtonType.OK
                ? picker.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(selected -> {
            try {
                ctx.playlistService.addSongToPlaylist(selected.getPlaylistId(), song);
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Added");
                ok.setHeaderText(null);
                ok.setContentText("\"" + song.getTitle() + "\" added to \"" + selected.getName() + "\".");
                ok.showAndWait();
            } catch (IllegalArgumentException ex) {
                Alert already = new Alert(Alert.AlertType.INFORMATION);
                already.setTitle("Already Added");
                already.setHeaderText(null);
                already.setContentText("That song is already in \"" + selected.getName() + "\".");
                already.showAndWait();
            } catch (Exception ex) {
                AppUtils.showError("Could not add song to playlist: " + ex.getMessage());
            }
        });
    }
}