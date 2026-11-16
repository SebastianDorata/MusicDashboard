package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;

/**
 * Holds all mutable view state for the My Library page.
 *
 * <p>Extracted from MyLibraryController to separate state
 * management from control logic. Reset methods provide
 * named entry points for the three ways the library can
 * be opened: default, with a specific album, with a
 * specific artist.</p>
 */
public class LibraryState {

    public String       currentView        = "albums";
    public String       currentDisplayMode = "grid";
    public Album        currentAlbum       = null;
    public Artist       currentArtist      = null;
    public SortStrategy currentSort        = SortStrategy.ALPHABETICAL;
    public Genre        currentGenreFilter = null;

    public void resetToDefault() {
        currentView        = "albums";
        currentDisplayMode = "grid";
        currentAlbum       = null;
        currentArtist      = null;
        currentSort        = SortStrategy.ALPHABETICAL;
        currentGenreFilter = null;
    }

    public void resetToAlbum(Album album) {
        resetToDefault();
        currentDisplayMode = "list";
        currentAlbum       = album;
    }

    public void resetToArtist(Artist artist) {
        resetToDefault();
        currentView        = "artists";
        currentArtist      = artist;
    }
}