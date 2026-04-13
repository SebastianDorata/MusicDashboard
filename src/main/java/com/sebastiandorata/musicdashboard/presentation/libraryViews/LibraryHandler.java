package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import javafx.scene.Node;

import java.util.function.BiConsumer;


/**
 * A lightweight context record that bundles the core services and callbacks
 * required by the library view layer.
 *
 * <p>{@code LibraryHandler} is passed through the view-builder and cell
 * hierarchy so that each component can access shared services without
 * constructor-level dependency injection on every individual class.</p>
 *
 * <p>Adding behaviour to the song context menu (the ⋯ button) does not
 * require changes to any view builder or cell. Add a menu item in
 * {@link SongHandler#show} instead — it receives this record and therefore
 * has access to {@code editDialog} automatically.</p>
 *
 * @param musicPlayerService the service responsible for playback control and queue management
 * @param playlistService    the service used to read and modify user playlists
 * @param favouriteService   the service used to query and toggle song favourites
 * @param onSongMenu         a {@link BiConsumer} invoked when a song's context-menu button is
 *                           pressed; receives the target {@link Song} and the anchor
 *                           {@link Node} used to position the menu
 * @param editDialog         the dialog used to edit song metadata; passed here so
 *                           {@link SongHandler} can offer an Edit action on every
 *                           song menu regardless of which view triggered it
 */
public record LibraryHandler(MusicPlayerService musicPlayerService, PlaylistService playlistService,
                             FavouriteService favouriteService, BiConsumer<Song, Node> onSongMenu,
                             SongEditDialog editDialog) {

}