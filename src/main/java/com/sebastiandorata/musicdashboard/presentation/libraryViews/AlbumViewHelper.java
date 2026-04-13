package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

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
 * <p>SRP: Only responsible for building album display nodes.</p>
 */
public class AlbumViewHelper {

    private final MusicPlayerService musicPlayerService;
    private final Consumer<Album>    onAlbumSelected;
    private final BiConsumer<Album, AlbumEditDialog.Result> onAlbumEdit;

    /**
     * @param musicPlayerService used by album cards to play the first song
     * @param onAlbumSelected    callback invoked when an album is clicked
     * @param onAlbumEdit        callback invoked when the user saves album edits;
     *                           receives the album and the dialog result
     */
    public AlbumViewHelper(MusicPlayerService musicPlayerService,
                           Consumer<Album> onAlbumSelected,
                           BiConsumer<Album, AlbumEditDialog.Result> onAlbumEdit) {
        this.musicPlayerService = musicPlayerService;
        this.onAlbumSelected    = onAlbumSelected;
        this.onAlbumEdit        = onAlbumEdit;
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
     * Builds a virtualized row-based album grid.
     * Albums are grouped into rows of COLUMNS cards each.
     * Only visible rows are rendered at any time.
     * Right-clicking a card opens the album edit dialog.
     *
     * @param albums the albums to display
     * @param sort   the sort strategy to apply
     * @return a ScrollPane containing the virtualized grid
     */
    public ScrollPane buildGridView(List<Album> albums, SortStrategy sort) {
        List<Album> sorted = albums.stream()
                .sorted(sort.getAlbumComparator())
                .toList();

        final int COLUMNS = 8;

        List<List<Album>> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i += COLUMNS) {
            rows.add(sorted.subList(i,
                    Math.min(i + COLUMNS, sorted.size())));
        }

        ListView<List<Album>> gridView = new ListView<>();
        gridView.getStyleClass().add("tile-pane");
        gridView.setFixedCellSize(276);
        gridView.getItems().addAll(rows);

        gridView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(List<Album> row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                HBox rowBox = new HBox(20);
                rowBox.setPadding(new Insets(8));
                rowBox.setMaxWidth(Double.MAX_VALUE);
                rowBox.setAlignment(Pos.CENTER);

                for (Album album : row) {
                    VBox card = CardFactory.createAlbumCard(album, musicPlayerService);
                    attachClickHandler(card, album); //right click to edit album
                    attachEditContextMenu(card, album);
                    HBox.setHgrow(card, Priority.ALWAYS);
                    rowBox.getChildren().add(card);
                }
                setGraphic(rowBox);
            }
        });

        ScrollPane scroll = new ScrollPane(gridView);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent; " + "-fx-background: transparent;");
        gridView.setStyle("-fx-background-color: transparent;");
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
                AlbumEditDialog.show(album).ifPresent(result -> onAlbumEdit.accept(album, result))
        );

        menu.getItems().add(editItem);

        node.setOnContextMenuRequested(e ->
                menu.show(node, e.getScreenX(), e.getScreenY())
        );
    }

    private void attachClickHandler(Node node, Album album) {
        node.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                onAlbumSelected.accept(album);
            }
        });
    }
}