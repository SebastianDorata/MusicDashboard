package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.List;


/**
 * Handles song-level user interactions by presenting a {@link ContextMenu}
 * anchored to the triggering UI element.
 *
 * <p>The menu currently exposes two actions:</p>
 * <ul>
 *   <li><strong>Add to Playlist</strong> – opens a playlist-picker dialog
 *       and adds the song via {@link PlaylistService}</li>
 *   <li><strong>Toggle Favourite</strong> – adds or removes the song from the
 *       current user's favourites via {@link FavouriteService}</li>
 * </ul>
 *
 * <p>Error handling is done inline with {@link com.sebastiandorata.musicdashboard.utils.AppUtils#showError(String)}
 * so that service failures surface to the user without crashing the view.</p>
 */
public class SongHandler {

    private final LibraryHandler ctx;

    /**
     * Constructs a {@code SongHandler}.
     *
     * @param ctx the {@link LibraryHandler} providing the playlist, favourite,
     *            and music-player services
     */
    public SongHandler(LibraryHandler ctx) {
        this.ctx = ctx;
    }


    /**
     * Displays the song context menu anchored below the given node.
     *
     * @param song   the {@link Song} the menu actions will operate on
     * @param anchor the {@link Node} used as the menu's display anchor
     */
    public void show(Song song, Node anchor) {
        ContextMenu menu = new ContextMenu();

        MenuItem addToPlaylist = new MenuItem("＋  Add to Playlist");
        addToPlaylist.setOnAction(e -> showAddToPlaylistDialog(song));

        MenuItem toggleFav = buildFavouriteMenuItem(song);

        MenuItem editSong = buildEditMenuItem(song);
        menu.getItems().addAll(addToPlaylist, toggleFav, editSong);
        menu.show(anchor, Side.BOTTOM, 0, 0);
    }



    private MenuItem buildFavouriteMenuItem(Song song) {
        boolean isFav;
        try {
            isFav = ctx.favouriteService().isFavourited(song);
        } catch (Exception e) {
            isFav = false;
        }

        MenuItem item = new MenuItem(isFav ? "♥  Remove from Favourites" : "♡  Add to Favourites");
        item.setOnAction(e -> {
            try {
                ctx.favouriteService().toggleFavourite(song);
            } catch (Exception ex) {
                AppUtils.showError("Could not update favourites: " + ex.getMessage());
            }
        });
        return item;
    }



    private void showAddToPlaylistDialog(Song song) {
        List<Playlist> playlists;
        try {
            playlists = ctx.playlistService().getCurrentUserPlaylists();
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
                ctx.playlistService().addSongToPlaylist(selected.getPlaylistId(), song);
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

    /**
     * Builds the Edit menu item.
     * Delegates to {@link SongEditDialog} via {@link LibraryHandler#editDialog}.
     * If no dialog was supplied (read-only context), the item is disabled.
     */
    private MenuItem buildEditMenuItem(Song song) {
        MenuItem item = new MenuItem("✎  Edit Song");
        if (ctx.editDialog() != null) {
            item.setOnAction(e -> ctx.editDialog().show(song, () -> {}));
        } else {
            item.setDisable(true);
        }
        return item;
    }
}