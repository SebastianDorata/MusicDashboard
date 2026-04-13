package com.sebastiandorata.musicdashboard.presentation.shared;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.PlaybackPanelController;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;

/**
 * Self-contained JavaFX node that displays album art for the currently
 * playing song, falling back to a musical-note placeholder when no art
 * is available.
 *
 * <p>Extends {@link StackPane} so it can be added directly to any layout and
 * exposes {@link #prefWidthProperty()} for binding, which is how
 * {@link PlaybackPanelController}
 * keeps the art square relative to panel height.</p>
 */
public class AlbumArtView extends StackPane {

    private final ImageView albumArt;
    private final Label     artPlaceholder;
    private static final double CORNER_RADIUS = 20;


    /**
     * Constructs the album art panel and wires it to the player.
     *
     * <p>The {@link ImageView} dimensions are bound to the pane's live size so
     * the artwork stays square as the layout changes. A {@link Rectangle} clip
     * with arcWidth/arcHeight = 40 (20 px radius) is applied to round the
     * top-left and bottom-left corners. The right-side arcs are pushed off-screen
     * by extending the clip width by {@value #CORNER_RADIUS} px, so only the left
     * corners appear rounded without needing a custom
     * {@link javafx.scene.shape.Path}.</p>
     *
     * <p>A listener on {@link MusicPlayerService#currentSongProperty()} keeps
     * the artwork current for every song change. {@link #updateArt(Song)} is
     * also called immediately so the panel is populated if a song is already
     * playing when the component is first built.</p>
     *
     * @param musicPlayerService the shared player service, must not be {@code null}
     */
    public AlbumArtView(MusicPlayerService musicPlayerService) {
        albumArt = new ImageView();
        albumArt.setPreserveRatio(true);
        albumArt.setSmooth(true);
        albumArt.fitWidthProperty().bind(widthProperty());// Bind image dimensions to the pane so it stays square when resized
        albumArt.fitHeightProperty().bind(heightProperty());
        artPlaceholder = new Label("♪");
        artPlaceholder.getStyleClass().add("album-art-placeholder");

        getChildren().addAll(artPlaceholder, albumArt);
        getStyleClass().add("album-art-container");

        Rectangle clip = new Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        clip.widthProperty().bind(widthProperty().add(CORNER_RADIUS)); // right arcs pushed off-screen
        clip.heightProperty().bind(heightProperty());
        setClip(clip);


        // Wire listener — updates art whenever the playing song changes
        musicPlayerService.currentSongProperty().addListener(
                (obs, oldSong, newSong) -> updateArt(newSong)
        );

        // Populate immediately if a song is already playing when panel is built
        updateArt(musicPlayerService.getCurrentSong());
    }


    private void updateArt(Song song) {
        Album  album   = (song  != null) ? song.getAlbum()        : null;
        String artPath = (album != null) ? album.getAlbumArtPath() : null;

        if (artPath != null && !artPath.isEmpty()) {
            try {
                String uri = new File(artPath).toURI().toURL().toString();
                Image image = new Image(uri, true);
                albumArt.setImage(image);
                albumArt.setVisible(true);
                artPlaceholder.setVisible(false);
            } catch (Exception e) {
                System.out.println("Failed to load art: " + e.getMessage());
                showPlaceholder();
            }
        } else {
            showPlaceholder();
        }
    }


    private void showPlaceholder() {
        albumArt.setImage(null);
        albumArt.setVisible(false);
        artPlaceholder.setVisible(true);
    }
}