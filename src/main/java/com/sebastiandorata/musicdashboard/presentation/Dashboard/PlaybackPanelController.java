package com.sebastiandorata.musicdashboard.presentation.Dashboard;

import com.sebastiandorata.musicdashboard.presentation.helpers.PlayerConfig;
import com.sebastiandorata.musicdashboard.presentation.UIComponent;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.presentation.shared.AlbumArtView;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.IconFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller responsible for building and managing the playback panel UI.
 * This includes the album art, song/artist info, player controls, and progress slider.
 * Supports two layout sizes via {@link PlayerConfig.PlayerSize}.
 *
 * <p>This bean is prototype-scoped so that each call to {@link #createPanel} produces
 * a fully independent panel with its own state, preventing cross-panel contamination
 * when both a LARGE and SMALL panel are used in the same application session.
 */
@Component
@Scope("prototype")
public class PlaybackPanelController extends UIComponent {

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;

    @Override
    public List<String> getStylesheets() {
        return List.of("musicPlayer.css");
    }

    /**
     * Tracks whether playback is currently active.
     * Replaces the fragile {@code getUserData()} string approach with a typed property,
     * making play/pause state safe to read and bind against.
     */
    private final BooleanProperty playing = new SimpleBooleanProperty(false);

    private Button playPauseBtn;
    private AlbumArtView albumArtView;
    private HBox nowPlaying;
    private PlayerConfig config;

    /**
     * Creates the playback panel using the default {@link PlayerConfig.PlayerSize#LARGE} layout.
     *
     * @param onArtistClicked callback invoked when the artist name label is clicked
     * @return the fully constructed playback panel as an {@link HBox}
     */
    public HBox createPanel(Consumer<Artist> onArtistClicked) {
        return createPanel(onArtistClicked, PlayerConfig.PlayerSize.LARGE);
    }

    /**
     * Creates the playback panel with the specified size configuration.
     * Builds the album art view, info section, and wires them into the root container.
     *
     * <p>Because this controller is prototype-scoped, each call to this method
     * produces an independent panel with its own {@link PlayerConfig}, button, and
     * listener references. Calling this method twice on the same instance is not
     * supported and will overwrite the instance state from the first call.
     *
     * @param onArtistClicked callback invoked when the artist name label is clicked
     * @param size the desired panel size ({@code LARGE} or {@code SMALL})
     * @return the fully constructed playback panel as an {@link HBox}
     */
    public HBox createPanel(Consumer<Artist> onArtistClicked, PlayerConfig.PlayerSize size) {
        this.config = new PlayerConfig(size);

        nowPlaying = new HBox(0);
        nowPlaying.getStyleClass().add("now-playing-bar");
        nowPlaying.setAlignment(Pos.CENTER_LEFT);
        nowPlaying.setMaxHeight(Double.MAX_VALUE);

        albumArtView = new AlbumArtView(musicPlayerService);
        albumArtView.prefWidthProperty().bind(nowPlaying.heightProperty());
        nowPlaying.getChildren().add(albumArtView);

        VBox infoSection = createNowPlayingInfoSection(onArtistClicked);
        infoSection.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(infoSection, Priority.ALWAYS);

        nowPlaying.getChildren().add(infoSection);
        return nowPlaying;
    }

    /**
     * Orchestrates the construction of the now-playing info section.
     * Delegates label creation, progress section building, and listener
     * attachment to focused helper methods, then assembles the result.
     *
     * @param onArtistClicked callback invoked when the artist name label is clicked
     * @return the fully constructed info section as a {@link VBox}
     */
    private VBox createNowPlayingInfoSection(Consumer<Artist> onArtistClicked) {
        VBox infoSection = new VBox(config.getInfoSectionSpacing());
        infoSection.getStyleClass().add("now-playing-info");
        infoSection.setAlignment(Pos.TOP_LEFT);

        Label songTitle = buildSongTitleLabel();
        Label artistName = buildArtistNameLabel();
        HBox controls = createPlayerControls();

        infoSection.getChildren().addAll(songTitle, artistName, controls);

        ProgressSection progressSection = null;
        if (config.getSize() == PlayerConfig.PlayerSize.LARGE) {
            progressSection = buildProgressSection(infoSection);
        } else {
            VBox.setVgrow(controls, Priority.ALWAYS);
            controls.setAlignment(Pos.CENTER);
        }

        attachSongListener(songTitle, artistName, progressSection, onArtistClicked);
        attachTimeListener(progressSection);
        initializeCurrentSong(songTitle, artistName, progressSection, onArtistClicked);

        return infoSection;
    }

    /**
     * Creates the song title label with its default text and style classes.
     * Font size is determined by the current {@link PlayerConfig}.
     *
     * @return the configured song title {@link Label}
     */
    private Label buildSongTitleLabel() {
        Label songTitle = new Label("No song playing");
        songTitle.getStyleClass().addAll("Song-Name", "txt-Scale" + config.getTitleFontSize(), "empty-msg");
        return songTitle;
    }

    /**
     * Creates the artist name label with its default placeholder text and style classes.
     * Font size is determined by the current {@link PlayerConfig}.
     *
     * @return the configured artist name {@link Label}
     */
    private Label buildArtistNameLabel() {
        Label artistName = new Label("—");
        artistName.getStyleClass().addAll("Artist-Name", "txt-Scale" + config.getArtistFontSize());
        return artistName;
    }

    /**
     * Builds the large-mode progress section and appends it to the given info section.
     * This includes a spacer, a {@link Slider}, and a time label row showing elapsed
     * time on the left and remaining time on the right.
     *
     * <p>The slider and labels are wrapped in a {@link ProgressSection} holder so they
     * can be shared cleanly with listener methods without requiring field-level state.
     *
     * <p>Note: time label colours and font sizes are applied via inline style here
     * because they depend on runtime config values from {@link PlayerConfig}. Static
     * colour values should be moved to CSS if theming support is added in future.
     *
     * @param infoSection the parent {@link VBox} to append the progress components to
     * @return a {@link ProgressSection} holding references to the slider and time labels
     */
    private ProgressSection buildProgressSection(VBox infoSection) {
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        infoSection.getChildren().add(topSpacer);

        Slider slider = new Slider(0, 100, 0);
        slider.getStyleClass().add("now-playing-slider");


        Label currentTime = new Label("0:00");
        Label remainingTime = new Label("-0:00");
        currentTime.setStyle("-fx-text-fill: #999999; -fx-font-size: " + config.getTimeFontSize() + "px;");
        remainingTime.setStyle("-fx-text-fill: #999999; -fx-font-size: " + config.getTimeFontSize() + "px;");

        Region timeSpacer = new Region();
        HBox.setHgrow(timeSpacer, Priority.ALWAYS);
        HBox timeRow = new HBox(5, currentTime, timeSpacer, remainingTime);
        timeRow.getStyleClass().addAll("cntr-spc-sm","time-row");

        VBox timeBox = new VBox(5, slider, timeRow);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.getStyleClass().add("time-Box");

        infoSection.getChildren().add(timeBox);

        return new ProgressSection(slider, currentTime, remainingTime);
    }

    /**
     * Attaches a listener to {@link MusicPlayerService#currentSongProperty()} that
     * reactively updates the UI whenever the active song changes.
     *
     * <p>When a new song is set:
     * <ul>
     *   <li>The {@code is-playing} style class is added to the panel.</li>
     *   <li>Song title, artist name, slider max, and remaining time are updated
     *       via {@link #updateSongDisplay}.</li>
     *   <li>The play/pause button switches to its pause state.</li>
     * </ul>
     *
     * <p>When the song is cleared (set to {@code null}):
     * <ul>
     *   <li>The {@code is-playing} style class is removed.</li>
     *   <li>All labels are reset to their default placeholder values.</li>
     *   <li>The slider is reset to zero.</li>
     *   <li>The play/pause button switches back to its play state.</li>
     * </ul>
     *
     * @param songTitle the label displaying the song title
     * @param artistName the label displaying the artist name
     * @param progressSection the progress section to reset on song clear, or {@code null} in SMALL mode
     * @param onArtistClicked callback to attach to the artist label on click
     */
    private void attachSongListener(Label songTitle, Label artistName,
                                    ProgressSection progressSection,
                                    Consumer<Artist> onArtistClicked) {
        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                nowPlaying.getStyleClass().add("is-playing");
                updateSongDisplay(songTitle, artistName, newSong, progressSection, onArtistClicked);
                setPlayingState(true);
            } else {
                nowPlaying.getStyleClass().remove("is-playing");
                songTitle.setText("No song playing");
                artistName.setText("—");
                artistName.getStyleClass().removeAll("Cursor-Hand");
                artistName.setOnMouseClicked(null);
                if (progressSection != null) {
                    progressSection.slider.setValue(0);
                    progressSection.slider.setMax(100);
                    progressSection.currentTime.setText("0:00");
                    progressSection.remainingTime.setText("-0:00");
                }
                setPlayingState(false);
            }
        });
    }

    /**
     * Attaches a listener to {@link MusicPlayerService#currentTimeProperty()} that
     * updates the progress slider and time labels on each playback tick.
     *
     * <p>Updates are suppressed while the slider is being dragged by the user,
     * preventing the slider from jumping back during interaction. The slider's
     * seek behaviour on release is also wired here.
     *
     * <p>This method is a no-op if {@code progressSection} is {@code null},
     * since the progress slider only exists in large mode.
     *
     * @param progressSection the progress section containing the slider and time labels,
     *                        or {@code null} in small mode
     */
    private void attachTimeListener(ProgressSection progressSection) {
        if (progressSection == null) return;

        musicPlayerService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSection.slider.isPressed()) {
                progressSection.slider.setValue(newTime.toSeconds());
                progressSection.currentTime.setText(AppUtils.formatTime(newTime.toSeconds()));
                double remaining = progressSection.slider.getMax() - newTime.toSeconds();
                progressSection.remainingTime.setText("-" + AppUtils.formatTime(remaining));
            }
        });

        progressSection.slider.setOnMouseReleased(e ->
                musicPlayerService.seek(Math.round(progressSection.slider.getValue()))
        );
    }

    /**
     * Populates the UI immediately if a song is already loaded in {@link MusicPlayerService}
     * at the time the panel is constructed.
     *
     * <p>Without this, the panel would show placeholder values until the next song
     * change event fires, even if playback is already active. This ensures the panel
     * reflects the correct state on first render.
     *
     * @param songTitle the label displaying the song title
     * @param artistName the label displaying the artist name
     * @param progressSection the progress section to initialize, or {@code null} in SMALL mode
     * @param onArtistClicked callback to attach to the artist label on click
     */
    private void initializeCurrentSong(Label songTitle, Label artistName,
                                       ProgressSection progressSection,
                                       Consumer<Artist> onArtistClicked) {
        Song alreadyPlaying = musicPlayerService.getCurrentSong();
        if (alreadyPlaying != null) {
            updateSongDisplay(songTitle, artistName, alreadyPlaying, progressSection, onArtistClicked);
            setPlayingState(true);
        }
    }

    /**
     * Updates the song title, artist name, progress slider, and time labels
     * to reflect the currently playing {@link Song}.
     *
     * <p>The artist name label is made clickable when a valid {@link Artist} is present,
     * invoking {@code onArtistClicked} on click. The clickable style and handler are
     * removed if no artist is available.
     *
     * <p>Album art updates are intentionally excluded here. They're handled
     * internally by {@link AlbumArtView} via its own listener.
     *
     * @param songTitle the label to update with the song title
     * @param artistName the label to update with the artist name
     * @param song the song whose data should be displayed
     * @param progressSection the progress section to update, or {@code null} in SMALL mode
     * @param onArtistClicked callback to attach to the artist label on click
     */
    private void updateSongDisplay(Label songTitle, Label artistName, Song song,
                                   ProgressSection progressSection,
                                   Consumer<Artist> onArtistClicked) {
        songTitle.setText(song.getTitle());

        Artist firstArtist = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0)
                : null;

        artistName.setText(firstArtist != null ? firstArtist.getName() : "Unknown Artist");

        if (firstArtist != null) {
            artistName.getStyleClass().add("Cursor-Hand");
            artistName.setOnMouseClicked(e -> onArtistClicked.accept(firstArtist));
        } else {
            artistName.getStyleClass().removeAll("Cursor-Hand");
            artistName.setOnMouseClicked(null);
        }

        if (progressSection != null) {
            progressSection.slider.setMax(song.getDuration());
            progressSection.remainingTime.setText("-" + AppUtils.formatTime(song.getDuration()));
        }
    }

    /**
     * Builds the row of player control buttons (previous, play/pause, next)
     * and wires each to the appropriate {@link MusicPlayerService} action.
     *
     * <p>Play/pause state is tracked via the {@link #playing} property rather than
     * string-based {@code userData}, keeping the state typed and safe to read externally.
     *
     * @return the controls row as an {@link HBox}
     */
    private HBox createPlayerControls() {
        HBox controls = new HBox(config.getControlSpacing());
        controls.getStyleClass().addAll("cntr-spc-sm","player-controls");
        controls.setAlignment(Pos.CENTER);
        controls.setMaxHeight(Double.MAX_VALUE);

        Button prevBtn = new Button();
        prevBtn.setGraphic(IconFactory.createIcon("prev", config.getNavIconSize()));

        playPauseBtn = new Button();
        playPauseBtn.setGraphic(IconFactory.createIcon("play", config.getPlayPauseIconSize()));

        Button nextBtn = new Button();
        nextBtn.setGraphic(IconFactory.createIcon("next", config.getNavIconSize()));

        prevBtn.getStyleClass().addAll("player-btn", "player-btn-secondary", "Cursor");
        playPauseBtn.getStyleClass().addAll("player-btn", "player-btn-primary", "Cursor");
        nextBtn.getStyleClass().addAll("player-btn", "player-btn-secondary", "Cursor");

        prevBtn.setOnAction(e -> musicPlayerService.playPrevious());

        playPauseBtn.setOnAction(e -> {
            if (!playing.get()) {
                musicPlayerService.play();
                nowPlaying.getStyleClass().add("is-playing");
                setPlayingState(true);
            } else {
                musicPlayerService.pause();
                nowPlaying.getStyleClass().remove("is-playing");
                setPlayingState(false);
            }
        });

        nextBtn.setOnAction(e -> musicPlayerService.playNext());

        controls.getChildren().addAll(prevBtn, playPauseBtn, nextBtn);
        return controls;
    }

    /**
     * Updates the {@link #playing} property and refreshes the play/pause button icon
     * to match the new state.
     *
     * <p>Centralizing this logic here ensures that every code path that changes
     * playback state, song listener, time listener, button click, and initialization to
     * produce a consistent button icon without duplicating the icon-swap logic.
     *
     * @param isPlaying {@code true} to show the pause icon, {@code false} to show play
     */
    private void setPlayingState(boolean isPlaying) {
        playing.set(isPlaying);
        String icon = isPlaying ? "pause" : "play";
        playPauseBtn.setGraphic(IconFactory.createIcon(icon, config.getPlayPauseIconSize()));
    }

    /**
     * Immutable data holder grouping the progress slider and its associated time labels.
     *
     * <p>Used to pass large-mode UI components between helper methods without
     * requiring additional fields on the controller or long parameter lists.
     * The {@code timeRow} container is intentionally excluded as no method
     * requires access to it after construction.
     */
    private record ProgressSection(Slider slider, Label currentTime, Label remainingTime) {
        /**
         * @param slider        the playback progress slider
         * @param currentTime   the label showing elapsed playback time
         * @param remainingTime the label showing remaining playback time
         */
        private ProgressSection {
        }
    }
}