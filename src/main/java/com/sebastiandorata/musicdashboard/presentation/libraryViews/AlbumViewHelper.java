package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builds album list and grid views for the My Library page.
 *
 * <p>Extracted from MyLibraryController to keep view construction
 * out of the controller. Receives all dependencies as constructor
 * parameters so it has no Spring dependency and is instantiated
 * directly by the controller.</p>
 *
 * <p>Right-clicking any album card or list row opens a context menu
 * with an "Edit Album" option. The edit dialog allows updating the
 * album title, release year, and genre (genre is applied across all
 * songs in the album). The {@code onAlbumEdit} callback is responsible
 * for persisting the changes.</p>
 *
 * <p><b><u>Grid view sizing:</u></b></p>
 * <p>{@link #buildGridView} uses a {@link FlowPane} rather than a
 * virtualized, manually row-chunked layout. {@code FlowPane} measures its
 * children (all built to a fixed width by {@link CardFactory}) and
 * automatically fits as many per row as the available width allows,
 * reflowing continuously as the window is resized — the same behaviour
 * CSS Grid {@code auto-fill} gives on the web. This keeps column count
 * fully responsive (5 per row on a wide monitor, 3 on a laptop, recalculated
 * live while dragging) and keeps the door open for a future adjustable
 * icon-size setting, since changing card width alone reflows the grid with
 * no extra logic required.</p>
 *
 * <p>SRP: Only responsible for building album display nodes.</p>
 */
public class AlbumViewHelper {

    private final MusicPlayerService musicPlayerService;
    private final Consumer<Album>    onAlbumSelected;
    private final BiConsumer<Album, AlbumEditDialog.Result> onAlbumEdit;
    private final Consumer<Album> onAlbumDelete;

    /**
     * @param musicPlayerService used by album cards to play the first song
     * @param onAlbumSelected    callback invoked when an album is clicked
     * @param onAlbumEdit        callback invoked when the user saves album edits;
     *                           receives the album and the dialog result
     */
    public AlbumViewHelper(MusicPlayerService musicPlayerService,
                           Consumer<Album> onAlbumSelected,
                           BiConsumer<Album, AlbumEditDialog.Result> onAlbumEdit,
                           Consumer<Album> onAlbumDelete) {
        this.musicPlayerService = musicPlayerService;
        this.onAlbumSelected    = onAlbumSelected;
        this.onAlbumEdit        = onAlbumEdit;
        this.onAlbumDelete      = onAlbumDelete;
    }

    /**
     * Builds a grouped, scrollable album list with an alphabet bar.
     * Albums are grouped by first letter with divider labels.
     * Right-clicking a row opens the album edit dialog.
     *
     * @param albums the albums to display
     * @param sort   the sort strategy to apply
     * @return a BorderPane with the list center and alphabet bar right
     */
    public BorderPane buildListView(List<Album> albums, SortStrategy sort) {
        List<Album> sorted = albums.stream()
                .sorted(sort.getAlbumComparator())
                .toList();

        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.getStyleClass().add("main-bkColour");

        Map<String, List<Album>> grouped = new LinkedHashMap<>();
        for (Album album : sorted) {
            String title = album.getTitle() != null ? album.getTitle() : "";
            char c = title.isBlank() ? '#'
                    : Character.toUpperCase(title.trim().charAt(0));
            String key = Character.isLetter(c) ? String.valueOf(c) : "#";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(album);
        }

        Map<String, Node> anchors = new LinkedHashMap<>();

        for (Map.Entry<String, List<Album>> entry : grouped.entrySet()) {
            String letter = entry.getKey();

            Label divider = new Label(letter);
            divider.getStyleClass().add("alpha-divider");
            divider.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().add(divider);
            anchors.put(letter, divider);

            for (Album album : entry.getValue()) {
                HBox row = AlbumCardListCell.buildRow(album);
                attachClickHandler(row, album);// right click to edit album
                attachEditContextMenu(row, album);
                content.getChildren().add(row);
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        AlphabetBar alphabetBar = new AlphabetBar(scrollPane, anchors);

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollPane);
        layout.setRight(alphabetBar);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }


    /**
     * Builds a fluid, reflowing album card grid using an optimized, lazy-loading FlowPane.
     *
     * <p>To prevent scrolling UI lag with large music collections, this method implements
     * virtualization-like batch loading. It renders a light initial batch of cards, and
     * continuously streams in subsequent batches on-demand as the user scrolls toward the
     * bottom of the viewport.</p>
     *
     * @param albums the albums to display
     * @param sort   the sort strategy to apply
     * @return a {@link ScrollPane} containing the high-performance reflowing grid
     */
    public ScrollPane buildGridView(List<Album> albums, SortStrategy sort) {
        List<Album> sorted = albums.stream()
                .sorted(sort.getAlbumComparator())
                .toList();

        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("flow-pane");
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(0, 20, 0, 20));
        grid.setAlignment(javafx.geometry.Pos.CENTER); // Kept Option 1 from earlier for clean spacing

        // Performance Constants
        final int BATCH_SIZE = 40;
        final int[] loadedCount = {0}; // Wrapped in array to allow modification inside lambda

        // Task runner to append the next block of items to the scene graph
        Runnable loadNextBatch = () -> {
            int start = loadedCount[0];
            int end = Math.min(start + BATCH_SIZE, sorted.size());
            if (start >= end) return;

            for (int i = start; i < end; i++) {
                Album album = sorted.get(i);
                VBox card = CardFactory.createAlbumCard(album, musicPlayerService);

                // Extra layer of optimization: cache node graphics as bitmaps while scrolling
                card.setCache(true);
                card.setCacheHint(javafx.scene.CacheHint.SPEED);

                attachClickHandler(card, album);
                attachEditContextMenu(card, album);
                grid.getChildren().add(card);
            }
            loadedCount[0] = end;
        };

        // Render initial visible batch instantly
        loadNextBatch.run();

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        grid.setStyle("-fx-background-color: transparent;");

        // Intersection Observer: Check if user is scrolling near the page boundary
        scroll.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            // Trigger the next batch when user scrolls past 85% of the current height
            if (newVal.doubleValue() > 0.85 && loadedCount[0] < sorted.size()) {
                javafx.application.Platform.runLater(loadNextBatch);
            }
        });

        return scroll;
    }


    /**
     * Attaches a right-click context menu to the given node that opens
     * the {@link AlbumEditDialog} for the specified album.
     *
     * @param node  the UI node to attach the context menu to
     * @param album the album associated with this node
     */
    private void attachEditContextMenu(Node node, Album album) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏  Edit Album");
        editItem.setOnAction(e ->
                AlbumEditDialog.show(album).ifPresent(result -> onAlbumEdit.accept(album, result)));

        MenuItem deleteItem = new MenuItem("🗑  Delete Album");
        deleteItem.setOnAction(e -> confirmAndDeleteAlbum(album));

        menu.getItems().addAll(editItem, deleteItem);

        node.setOnContextMenuRequested(e ->
                menu.show(node, e.getScreenX(), e.getScreenY()));
    }

    private void confirmAndDeleteAlbum(Album album) {
        int songCount = album.getSongs() != null ? album.getSongs().size() : 0;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Album");
        confirm.setHeaderText("Delete \"" + album.getTitle() + "\"?");
        confirm.setContentText("This permanently deletes the album and all " + songCount
                + " song(s) it contains, including their playback history, favourites, "
                + "and playlist entries. This cannot be undone.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && onAlbumDelete != null) {
                try {
                    onAlbumDelete.accept(album);
                } catch (Exception ex) {
                    AppUtils.showError("Could not delete album: " + ex.getMessage());
                }
            }
        });
    }

    private void attachClickHandler(Node node, Album album) {
        node.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onAlbumSelected.accept(album);
            }
        });
    }
}
