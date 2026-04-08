package com.sebastiandorata.musicdashboard.presentation.Dashboard;

import com.sebastiandorata.musicdashboard.dto.TopArtistRowData;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.TopArtistsViewModel;
import com.sebastiandorata.musicdashboard.presentation.UIComponent;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Builds and refreshes the Top 5 Artists All-Time panel on the Dashboard.
 *
 * <p>Loads artist rows from {@link TopArtistsViewModel}
 * asynchronously and re-fetches on every song change to keep rankings current.
 * Artist name labels are clickable and fire the {@code onArtistClicked}
 * callback for drill-in navigation to My Library.</p>
 */
@Component
public class TopArtistsController extends UIComponent {

    @Autowired
    private TopArtistsViewModel topArtistsViewModel;

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;


    /**
     * Creates the Top 5 Artists panel for the dashboard right menu.
     *
     * <p>Displays exactly 5 artist rows ranked by total listening time,
     * refreshing automatically whenever the currently playing song changes
     * via a listener on {@link MusicPlayerService#currentSongProperty()}.
     *
     * <p>Placeholder rows with "—" are shown when fewer than 5 artists
     * have valid play history, ensuring the panel always renders a complete
     * 5-row list.
     *
     * @param onArtistClicked callback invoked with the clicked {@link Artist} entity, used to navigate to the artist's discography
     * in My Library; decouples this panel from any specific navigation target (DIP)
     * @return the fully constructed top artists {@link VBox} panel
     */
    public VBox createPanel(Consumer<Artist> onArtistClicked) {
        VBox topArtist = new VBox(0);

        // Responsive fix: clamped width helper
        topArtist.setPrefWidth(AppUtils.rightPanelPrefWidth());

        // Do NOT write setMaxHeight or VBox.setVgrow(Priority.ALWAYS/SOMETIMES) again.
        // The TopArtists panel is intentionally fixed-height (content-sized: title + exactly 5 rows).
        // Let the parent give extra space to RecentlyPlayed instead.
        topArtist.getStyleClass().addAll("top-artists-panel", "panels");

        Label title = new Label("Top 5 Artists All-Time");
        title.getStyleClass().addAll("txt-white-bld-thirty", "txt-centre-underline");
        title.setPadding(new Insets(0, 0, 10, 0));

        VBox artistRows = new VBox(0);
        artistRows.setFillWidth(true);

        topArtist.getChildren().addAll(title, artistRows);

        refreshTopArtists(artistRows, onArtistClicked);

        musicPlayerService.currentSongProperty().addListener((obs, oldSong, newSong) -> {
            if (newSong != null) {
                new Thread(() -> {
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> refreshTopArtists(artistRows, onArtistClicked));
                }).start();
            }
        });

        return topArtist;
    }

    /**
     * Clears and rebuilds all artist rows from fresh data.
     * Rebuilding rather than patching removes implicit index coupling. (Bug fix)
     *
     * <p>Passes a limit of 5 to {@link TopArtistsViewModel#loadTopArtistsData(Consumer, int)}
     * so the dashboard panel always displays exactly 5 rows.
     *
     * @param artistRows the {@link VBox} to clear and repopulate with artist rows
     * @param onArtistClicked callback invoked with the clicked {@link Artist} entity
     */
    private void refreshTopArtists(VBox artistRows, Consumer<Artist> onArtistClicked) {
        topArtistsViewModel.loadTopArtistsData(data -> {
            artistRows.getChildren().clear();
            for (TopArtistRowData rowData : data) {
                artistRows.getChildren().add(buildArtistRow(rowData, onArtistClicked));
            }
        }, 5);
    }

    /**
     * Builds a single artist row for the top artists panel.
     *
     * <p>Rows for real artists (non-placeholder) have a click handler that invokes
     * {@code onArtistClicked}. Placeholder rows with a {@code null} artist reference
     * are rendered as non-interactive text-only rows.
     *
     * <p>Time Complexity: O(1)
     *
     * @param data The {@link TopArtistRowData} to render
     * @param onArtistClicked callback invoked with the clicked {@link Artist} entity when the name label is clicked
     * @return a fully constructed {@link HBox} row ready to add to the panel
     */
    private HBox buildArtistRow(TopArtistRowData data, Consumer<Artist> onArtistClicked) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("artist-row");

        Label nameLabel = new Label(data.artistName);
        nameLabel.getStyleClass().add("wt-smmd-bld");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        if (data.artist != null) {
            nameLabel.getStyleClass().add("Cursor-Hand");
            nameLabel.setOnMouseClicked(e -> onArtistClicked.accept(data.artist));
        }

        Label timeLabel = new Label(data.listeningTime);
        timeLabel.getStyleClass().add("artist-time");
        timeLabel.setMinWidth(70);

        row.getChildren().addAll(nameLabel, timeLabel);
        return row;
    }
}