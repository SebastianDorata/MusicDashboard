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
 * @param musicPlayerService the service responsible for playback control and queue management
 * @param playlistService    the service used to read and modify user playlists
 * @param favouriteService   the service used to query and toggle song favourites
 * @param onSongMenu         a {@link BiConsumer} invoked when a song's context-menu button is
 *                           pressed; receives the target {@link Song} and the anchor
 *                           {@link Node} used to position the menu
 */
public record LibraryHandler(MusicPlayerService musicPlayerService, PlaylistService playlistService,
                             FavouriteService favouriteService, BiConsumer<Song, Node> onSongMenu) {

}