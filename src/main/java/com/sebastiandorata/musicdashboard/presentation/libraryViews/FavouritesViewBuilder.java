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

    /**
     * Constructs a {@code FavouritesViewBuilder}.
     *
     * @param ctx the {@link LibraryHandler} providing the {@code FavouriteService}
     *            and other shared services
     */
    public FavouritesViewBuilder(LibraryHandler ctx) {
        this.ctx             = ctx;
        this.songListBuilder = new SongViewBuilder(ctx);
    }

    /**
     * Builds and returns the complete Favourites view.
     *
     * <p>Fetches the current user's favourites via {@link LibraryHandler#favouriteService()}.
     * On failure an empty list is used so the UI degrades gracefully.</p>
     *
     * @param displayMode {@code "list"} for a list view, or any other value for a grid view
     * @return a {@link VBox} containing the header label and the song list or empty state
     */
    public VBox build(String displayMode) {
        VBox view = new VBox(12);
        view.setFillWidth(true);

        List<Song> songs;
        try {
            songs = ctx.favouriteService().getUserFavouritesSortedByDate();
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
        EmptyStateConfig.favourites();
        return empty;
    }
}