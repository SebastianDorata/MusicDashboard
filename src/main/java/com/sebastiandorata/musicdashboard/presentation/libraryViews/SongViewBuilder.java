package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds reusable song and album display components, list views, card grids,
 * and album grids, used in the library.
 *
 * <p>Two constructors are available:</p>
 * <ul>
 *   <li>The full constructor accepts a {@link SongEditDialog} so that right-clicking
 *       a song row opens the edit dialog.</li>
 *   <li>The convenience constructor omits the dialog for read-only contexts such as
 *       the Favourites or Artist detail screens.</li>
 * </ul>
 *
 * <p><b><u>Click behaviour on every song row:</u></b></p>
 * <ul>
 *   <li>Double left-click: play song and set queue.</li>
 *   <li>Right-click: open edit dialog (if available).</li>
 *   <li>Single left-click: no action.</li>
 * </ul>
 *
 * <p>List views group songs under A-Z section dividers and include a vertical
 * {@link AlphabetBar} on the right edge. Clicking a letter scrolls the list to
 * that section instantly.</p>
 *
 * <p>All list and grid builders accept an optional {@link SortStrategy} parameter.
 * Overloads without a sort argument default to {@link SortStrategy#ALPHABETICAL}
 * for backward compatibility.</p>
 */
public class SongViewBuilder {

    private final LibraryHandler ctx;
    private final SongEditDialog editDialog;

    /**
     * Constructs a {@code SongViewBuilder} with edit-dialog support.
     *
     * @param ctx the {@link LibraryHandler} providing services and callbacks
     * @param editDialog the dialog opened when the user right-clicks a song row,
     *                   may be {@code null} to disable editing
     */
    public SongViewBuilder(LibraryHandler ctx, SongEditDialog editDialog) {
        this.ctx        = ctx;
        this.editDialog = editDialog;
    }

    /**
     * Constructs a read-only {@code SongViewBuilder} without an edit dialog.
     * Right-clicking a song row will have no effect.
     *
     * @param ctx the {@link LibraryHandler} providing services and callbacks
     */
    public SongViewBuilder(LibraryHandler ctx) {
        this.ctx        = ctx;
        this.editDialog = null;
    }

    /**
     * Builds a scrollable list of songs grouped under A–Z section dividers,
     * with a vertical {@link AlphabetBar} on the right that jumps to each section.
     *
     * <p>Songs are sorted by the given strategy before grouping. Double
     * left-clicking a row starts playback and sets the full sorted list as the
     * queue. Right-clicking opens the {@link SongEditDialog} if one was supplied.</p>
     *
     * @param songs the songs to display
     * @param sort  the {@link SortStrategy} to apply before rendering
     * @return a {@link BorderPane} with the grouped song list in the center
     *         and the {@link AlphabetBar} on the right
     */
    public BorderPane buildListView(List<Song> songs, SortStrategy sort) {
        List<Song> sorted = sortSongs(songs, sort);

        // Group into sections for the alphabet dividers
        Map<String, List<Song>> grouped = new LinkedHashMap<>();
        for (Song song : sorted) {
            String key = firstLetterKey(song.getTitle());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(song);
        }

        // Build a flat list with divider markers interleaved
        // Use a record to distinguish dividers from songs
        List<Object> flatItems = new ArrayList<>();
        Map<String, Object> anchors = new LinkedHashMap<>();

        for (Map.Entry<String, List<Song>> entry : grouped.entrySet()) {
            String letter = entry.getKey();
            flatItems.add(letter);          // String = divider
            anchors.put(letter, letter);
            flatItems.addAll(entry.getValue()); // Song = row
        }

        // Single virtualized ListView for all 4700 songs + dividers
        ListView<Object> listView = new ListView<>();
        listView.getItems().addAll(flatItems);
        listView.getStyleClass().add("song-list-view");
        listView.setFixedCellSize(52); // Set fixed height so JavaFX
        // doesn't measure every cell

        listView.setCellFactory(lv -> new ListCell<Object>() {
            private final SongCell songCell = new SongCell(
                    sorted,
                    ctx.musicPlayerService(),
                    ctx.playlistService(),
                    ctx.favouriteService(),
                    ctx.onSongMenu(),
                    false
            );

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("");
                    return;
                }

                if (item instanceof String letter) {
                    // Render as alphabet divider
                    setGraphic(null);
                    setText(letter);
                    getStyleClass().setAll("alpha-divider");
                    setMouseTransparent(false);
                    setOnMouseClicked(null);
                } else if (item instanceof Song song) {
                    // Render as song row using the reusable SongCell
                    setText(null);
                    getStyleClass().setAll("song-list-cell");
                    songCell.updateItem(song, false);
                    Node row = songCell.getGraphic();
                    setGraphic(row);

                    if (row != null) {
                        setOnMouseClicked(e -> {
                            if (e.getButton() == MouseButton.SECONDARY) {
                                if (editDialog != null) editDialog.show(song, () -> {});
                                e.consume();
                                return;
                            }
                            if (e.getClickCount() == 2
                                    && e.getButton() == MouseButton.PRIMARY) {
                                ctx.musicPlayerService().setQueue(sorted);
                                ctx.musicPlayerService().playSong(song);
                            }
                        });
                    }
                }
            }
        });

        VBox.setVgrow(listView, Priority.ALWAYS);

        // Build alphabet bar using the letter keys as anchors
        // AlphabetBar needs Node anchors — map letters to their
        // index in the ListView so clicking scrolls to that position
        Map<String, Node> alphabetAnchors = new LinkedHashMap<>();
        AlphabetBar alphabetBar = new AlphabetBar(listView, flatItems, anchors);

        BorderPane layout = new BorderPane();
        layout.setCenter(listView);
        layout.setRight(alphabetBar);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    /**
     * Builds a grouped, scrollable song list using {@link SortStrategy#ALPHABETICAL}.
     * Equivalent to calling {@link #buildListView(List, SortStrategy)} with
     * {@code SortStrategy.ALPHABETICAL}.
     *
     * @param songs the songs to display
     * @return a {@link BorderPane} with the grouped song list and alphabet bar
     */
    public BorderPane buildListView(List<Song> songs) {
        return buildListView(songs, SortStrategy.ALPHABETICAL);
    }

    /**
     * Builds a {@link TilePane} card grid of songs sorted by the given strategy.
     *
     * <p>Each card is created by {@link CardFactory#createSongCard} and clicking
     * it starts playback with the full sorted list as the queue.</p>
     *
     * @param songs the songs to display
     * @param sort  the {@link SortStrategy} to apply before rendering
     * @return a configured {@link TilePane} ready to be added to the scene graph
     */
    public TilePane buildGridView(List<Song> songs, SortStrategy sort) {
        List<Song> sorted = sortSongs(songs, sort);
        TilePane grid = buildBaseTilePane();

        for (Song song : sorted) {
            VBox card = CardFactory.createSongCard(song, ctx.musicPlayerService());
            card.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) { e.consume(); return; }
                ctx.musicPlayerService().setQueue(sorted);
                ctx.musicPlayerService().playSong(song);
            });
            card.setOnContextMenuRequested(javafx.event.Event::consume);
            grid.getChildren().add(card);
        }
        return grid;
    }

    /**
     * Builds a song card grid using {@link SortStrategy#ALPHABETICAL}.
     * Equivalent to calling {@link #buildGridView(List, SortStrategy)} with
     * {@code SortStrategy.ALPHABETICAL}.
     *
     * @param songs the songs to display
     * @return a configured {@link TilePane} ready to be added to the scene graph
     */
    public TilePane buildGridView(List<Song> songs) {
        return buildGridView(songs, SortStrategy.ALPHABETICAL);
    }

    /**
     * Builds a {@link TilePane} card grid of albums.
     *
     * <p>Each album card is created by {@link CardFactory#createAlbumCard}.
     * Clicking a card fires both {@code onAlbumClick} and {@code onSelect}
     * so the caller can navigate to the album detail view.</p>
     *
     * @param albums       the albums to display
     * @param onAlbumClick a {@link Runnable} invoked after {@code onSelect} on each click
     * @param onSelect     a {@link java.util.function.Consumer} receiving the clicked album
     * @return a configured {@link TilePane} ready to be added to the scene graph
     */
    public TilePane buildAlbumGridView(List<com.sebastiandorata.musicdashboard.entity.Album> albums,
                                       Runnable onAlbumClick,
                                       java.util.function.Consumer<com.sebastiandorata.musicdashboard.entity.Album> onSelect) {
        TilePane grid = buildBaseTilePane();
        for (var album : albums) {
            VBox card = CardFactory.createAlbumCard(album, ctx.musicPlayerService());
            card.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) { e.consume(); return; }
                onSelect.accept(album);
                onAlbumClick.run();
            });
            card.setOnContextMenuRequested(javafx.event.Event::consume);
            grid.getChildren().add(card);
        }
        return grid;
    }

    private TilePane buildBaseTilePane() {
        TilePane grid = new TilePane();
        grid.getStyleClass().add("tile-pane");
        grid.setPrefColumns(5);
        return grid;
    }

    private List<Song> sortSongs(List<Song> songs, SortStrategy sort) {
        return songs.stream()
                .sorted(sort.getSongComparator())
                .toList();
    }

    /**
     * Returns the uppercase first letter of the given title, or {@code "#"} for
     * titles that start with a digit or any non-alphabetic character.
     *
     * @param title the song or album title; may be {@code null}
     * @return a single uppercase letter, or {@code "#"}
     */
    private String firstLetterKey(String title) {
        if (title == null || title.isBlank()) return "#";
        char c = Character.toUpperCase(title.trim().charAt(0));
        return Character.isLetter(c) ? String.valueOf(c) : "#";
    }
}