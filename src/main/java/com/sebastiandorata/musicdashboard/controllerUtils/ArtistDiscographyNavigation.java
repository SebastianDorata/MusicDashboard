package com.sebastiandorata.musicdashboard.controllerUtils;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.controller.UserLibrary.MyLibraryController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Centralized service for artist navigation across the application.
 *
 * Responsibility: Provide a consistent callback for drilling into an artist's discography.
 * Decouples controllers from the specific navigation implementation.
 *
 * Usage:
 *   @Autowired private ArtistNavigationService artistNavigation;
 *   topArtistsController.createPanel(artistNavigation.getArtistDrillInCallback());
 */
@Component
public class ArtistDiscographyNavigation {

    @Lazy @Autowired
    private MyLibraryController myLibraryController;// Lazy prevents circular dependencies during Spring initialization

    /**
     * Returns the reusable callback for artist drill-in navigation.
     *
     * When an artist is clicked anywhere in the app, this callback:
     * 1. Navigates to My Library
     * 2. Drills into that artist's discography view
     *
     * @return Consumer<Artist> callback ready to pass to UI components
     */
    public Consumer<Artist> getArtistDrillInCallback() {
        return artist -> myLibraryController.showWithArtist(artist);
    }
}