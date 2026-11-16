package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.presentation.helpers.EmptyStateConfig;
import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Builds the Favourites screen, displaying songs the current user has marked
 * as favourites sorted by the date they were added.
 *
 * <p>The view supports two display modes controlled by the {@code displayMode}
 * parameter passed to {@link #build(String)}:</p>
 * <ul>
 *   <li>{@code "list"} – renders songs in a scrollable {@link javafx.scene.control.ListView}</li>
 *   <li>any other value – renders songs as an album-art card grid</li>
 * </ul>
 *
 * <p>When the favourites list is empty, an empty-state placeholder is shown
 * instead of the song list.</p>
 */

public class FavouritesViewBuilder {

    private final LibraryHandler ctx;
    private final SongViewBuilder songListBuilder;

    public FavouritesViewBuilder(LibraryHandler ctx) {
        this.ctx             = ctx;
        this.songListBuilder = new SongViewBuilder(ctx);
    }

    /**
     * Builds the Favourites view from an already filtered + sorted list.
     * Called by MyLibraryController once genre filter and sort strategy
     * have been applied, mirroring loadSongsView() / loadAlbumsView().
     */
    public VBox build(String displayMode, List<Song> songs) {
        VBox view = new VBox(12);
        view.setFillWidth(true);

        Label header = new Label("Favourites (" + songs.size() + ")");
        header.getStyleClass().add("song-header");
        view.getChildren().add(header);

        if (songs.isEmpty()) {
            view.getChildren().add(buildEmptyState());
        } else if ("list".equals(displayMode)) {
            view.getChildren().add(songListBuilder.buildListView(songs));
        }

        return view;
    }

    /**
     * Legacy no-arg-filter overload, kept for any other call sites.
     * Delegates to {@link #build(String, List)} with the default
     * date-sorted, unfiltered favourites list.
     */
    public VBox build(String displayMode) {
        List<Song> songs;
        try {
            songs = ctx.favouriteService().getUserFavouritesSortedByDate();
        } catch (Exception e) {
            songs = List.of();
        }
        return build(displayMode, songs);
    }

    private VBox buildEmptyState() {
        VBox empty = new VBox(12);
        EmptyStateConfig.favourites();
        return empty;
    }
}