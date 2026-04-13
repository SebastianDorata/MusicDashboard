package com.sebastiandorata.musicdashboard.presentation;

import com.sebastiandorata.musicdashboard.controller.MyLibraryController;
import com.sebastiandorata.musicdashboard.entity.Artist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Centralized navigation hub for the application.
 *
 * Single source for all cross-page navigation callbacks.
 * Inject this bean wherever a navigation callback is needed instead
 * of wiring MyLibraryController directly at the call site.
 */
@Component
public class ArtistDiscographyNavigation {

    @Lazy @Autowired
    private MyLibraryController myLibraryController;

    public void navigateToArtist(Artist artist) {
        if (artist == null) return;
        myLibraryController.showWithArtist(artist);
    }

    /**
     * Use this when a Consumer<Artist> is required as a parameter.
     * For direct calls prefer navigateToArtist() instead.
     */
    public Consumer<Artist> getArtistDrillInCallback() {
        return this::navigateToArtist;
    }
}