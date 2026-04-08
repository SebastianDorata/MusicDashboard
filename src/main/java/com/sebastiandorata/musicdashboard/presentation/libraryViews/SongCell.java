package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * A {@link ListCell} that renders a {@link Song} as a horizontal row containing
 * an optional 40px album-art thumbnail, the song title, the primary artist name,
 * and an optional context-menu trigger button.
 *
 * <p>Two constructors are provided:</p>
 * <ul>
 *   <li>The full constructor wires up the menu button and is used in interactive
 *       song lists where the user can add songs to playlists, toggle favourites,
 *       etc.</li>
 *   <li>The no-arg constructor omits the menu button and is suited for read-only
 *       contexts such as playlist preview panels.</li>
 * </ul>
 *
 * <p>Album art loading can be suppressed via the {@code showArt} parameter to
 * avoid the per-row disk I/O overhead in large list views.</p>
 *
 * @see <a href="https://openjfx.io/javadoc/25/javafx.controls/javafx/scene/control/Cell.html">JavaFX Cell</a>
 */
public class SongCell extends ListCell<Song> {

    private final HBox  root;
    private final ImageView albumArt;
    private final Label titleLabel;
    private final Label separatorLabel;
    private final Label artistLabel;
    private final Button menuBtn;
    private final boolean showArt;

    private final BiConsumer<Song, javafx.scene.Node> onMenuClicked;


    /**
     * Constructs a fully interactive {@code SongCell} with a context-menu button.
     *
     * @param queue the current playback queue, used to set context when a song is played
     * @param musicPlayerService service for playback control
     * @param playlistService service for playlist management
     * @param favouriteService service for favourites management
     * @param onMenuClicked callback invoked when the menu button is pressed; receives the cell's {@link Song} and the button as the anchor node
     * @param showArt {@code false} to skip album art entirely, improving scroll performance in large list views
     */
    public SongCell(List<Song> queue,
                    MusicPlayerService musicPlayerService,
                    PlaylistService playlistService,
                    FavouriteService favouriteService,
                    BiConsumer<Song, javafx.scene.Node> onMenuClicked,
                    boolean showArt) {

        this.onMenuClicked = onMenuClicked;
        this.showArt       = showArt;
        this.menuBtn       = buildMenuButton();
        this.albumArt      = showArt ? AppUtils.buildAlbumArt(40) : null;

        this.titleLabel     = new Label();
        this.separatorLabel = new Label(" · ");
        this.artistLabel    = new Label();

        titleLabel.getStyleClass().add("song-list-title");
        separatorLabel.getStyleClass().add("song-list-separator");
        artistLabel.getStyleClass().add("song-list-artist");

        HBox inlineText = new HBox(0, titleLabel, separatorLabel, artistLabel);
        inlineText.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inlineText, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root = showArt
                ? new HBox(12, albumArt, inlineText, spacer, menuBtn)
                : new HBox(12, inlineText, spacer, menuBtn);

        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("song-list-row");
        root.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));

        // Suppress right-click on the whole row
        root.setOnContextMenuRequested(Event::consume);
    }

    //Read-only constructor (no menu, no art)
    public SongCell() {
        this.onMenuClicked  = null;
        this.showArt        = false;
        this.menuBtn        = null;
        this.albumArt       = null;
        this.titleLabel     = new Label();
        this.separatorLabel = new Label(" · ");
        this.artistLabel    = new Label();

        titleLabel.getStyleClass().add("song-list-title");
        separatorLabel.getStyleClass().add("song-list-separator");
        artistLabel.getStyleClass().add("song-list-artist");

        HBox inlineText = new HBox(0, titleLabel, separatorLabel, artistLabel);
        inlineText.setAlignment(Pos.CENTER_LEFT);

        root = new HBox(12, inlineText);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("song-list-row");
        root.setPadding(new Insets(10, 16, 10, 16));
        root.setOnContextMenuRequested(javafx.event.Event::consume);

    }

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);
        if (empty || song == null) {
            setGraphic(null);
            return;
        }

        titleLabel.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");

        String artistName = (song.getArtists() != null && !song.getArtists().isEmpty()
                && song.getArtists().get(0).getName() != null)
                ? song.getArtists().get(0).getName()
                : "Unknown Artist";
        artistLabel.setText(artistName);

        if (showArt && albumArt != null) {
            if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
                try {
                    albumArt.setImage(new Image("file:" + song.getAlbum().getAlbumArtPath(), true));
                } catch (Exception e) {
                    albumArt.setImage(null);
                }
            } else {
                albumArt.setImage(null);
            }
        }

        if (menuBtn != null && onMenuClicked != null) {
            menuBtn.setOnAction(e -> onMenuClicked.accept(song, menuBtn));
        }

        setGraphic(root);
    }

    private Button buildMenuButton() {
        Button btn = new Button("⋯");
        btn.getStyleClass().add("song-row-menu-btn");
        btn.setOnMousePressed(javafx.event.Event::consume);
        return btn;
    }
}