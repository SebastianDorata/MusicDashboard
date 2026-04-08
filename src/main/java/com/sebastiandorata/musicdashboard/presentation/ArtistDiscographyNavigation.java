package com.sebastiandorata.musicdashboard.presentation;

import com.sebastiandorata.musicdashboard.controller.MyLibraryController;
import com.sebastiandorata.musicdashboard.entity.Artist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Centralized service for artist navigation across the application.

 * <p>Responsibility: Provide a consistent callback for drilling into an artist's discography.</p>
 * <p>Decouples controllers from the specific navigation implementation.</p>

 *  <p><b><u>Usage:</u></b></p>
 *   <p>private ArtistNavigationService artistNavigation;
 *   <p>topArtistsController.createPanel(artistNavigation.getArtistDrillInCallback());
 */
@Component
public class ArtistDiscographyNavigation {

    @Lazy @Autowired
    private MyLibraryController myLibraryController;// Lazy prevents circular dependencies during Spring initialization

    /**
     * Returns the reusable callback for artist drill-in navigation.
     *
     * When an artist is clicked anywhere in the app, this callback:
     * <ol>
     *     <li>Navigates to My Library</li>
     *     <li>Drills into that artist's discography view</li>
     * </ol>
     * @return {@code Consumer<Artist>} callback ready to pass to UI components
     */
    public Consumer<Artist> getArtistDrillInCallback() {
        return artist -> myLibraryController.showWithArtist(artist);
    }
}