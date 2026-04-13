package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.service.LibraryService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Builds the artist list view and the artist detail view for the library's
 * Artists screen.
 *
 * <p>The <em>list view</em> groups artists alphabetically with divider headers
 * and renders each entry as a two-line row (name + song/album counts). The
 * <em>detail view</em> shows the selected artist's albums either as a
 * scrollable card grid or a compact list, controlled by the {@code displayMode}
 * parameter.</p>
 *
 * <p>Album resolution is delegated to {@link LibraryService} so that albums
 * linked only through songs (rather than a direct artist–album relationship)
 * are also surfaced.</p>
 */
public class ArtistViewBuilder {

    private final LibraryHandler ctx;
    private final SongViewBuilder songListBuilder;
    private final Consumer<Album>     onAlbumSelected;
    private final LibraryService      libraryService;

    /**
     * Constructs an {@code ArtistViewBuilder}.
     *
     * @param ctx the {@link LibraryHandler} providing services and callbacks
     * @param onAlbumSelected callback invoked when the user selects an album from
     *                        the artist's album list or grid
     * @param libraryService  service used to resolve albums associated with an artist
     */
    public ArtistViewBuilder(LibraryHandler ctx,
                             Consumer<Album> onAlbumSelected,
                             LibraryService libraryService) {
        this.ctx             = ctx;
        this.songListBuilder = new SongViewBuilder(ctx);
        this.onAlbumSelected = onAlbumSelected;
        this.libraryService  = libraryService;
    }

    /**
     * Builds a scrollable, alphabetically grouped list of artists.
     *
     * <p>Artists are sorted case-insensitively and divided by their first
     * letter. Names that do not start with a letter are placed under the
     * "#" divider. Each row fires {@code onArtistSelected} when clicked.</p>
     *
     * @param artists          the full list of {@link Artist} objects to display
     * @param onArtistSelected callback invoked with the clicked {@link Artist}
     * @return a {@link VBox} containing the grouped artist list, intended to be
     *         placed inside the page's single outer scroll pane
     */
    public BorderPane buildArtistList(List<Artist> artists, Consumer<Artist> onArtistSelected) {
        List<Artist> sorted = artists.stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        Map<String, List<Artist>> grouped = new LinkedHashMap<>();
        for (Artist artist : sorted) {
            String letter = artist.getName().substring(0, 1).toUpperCase();
            if (!Character.isLetter(letter.charAt(0))) letter = "#";
            grouped.computeIfAbsent(letter, k -> new ArrayList<>()).add(artist);
        }

        VBox content = new VBox(0);
        content.setFillWidth(true);
        Map<String, Node> anchors = new LinkedHashMap<>();

        for (Map.Entry<String, List<Artist>> entry : grouped.entrySet()) {
            HBox divider = buildAlphaDivider(entry.getKey());
            anchors.put(entry.getKey(), divider);
            content.getChildren().add(divider);
            for (Artist artist : entry.getValue()) {
                content.getChildren().add(buildArtistRow(artist, onArtistSelected));
            }
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        AlphabetBar alphabetBar = new AlphabetBar(scrollPane, anchors);

        BorderPane layout = new BorderPane();
        layout.setCenter(scrollPane);
        layout.setRight(alphabetBar);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }


    private HBox buildAlphaDivider(String letter) {
        HBox divider = new HBox(10);
        divider.setAlignment(Pos.CENTER_LEFT);
        divider.getStyleClass().add("artist-alpha-divider");

        Label letterLabel = new Label(letter);
        letterLabel.getStyleClass().add("artist-alpha-letter");

        Region line = new Region();
        line.getStyleClass().add("artist-alpha-line");
        HBox.setHgrow(line, Priority.ALWAYS);

        divider.getChildren().addAll(letterLabel, line);
        return divider;
    }

    private HBox buildArtistRow(Artist artist, Consumer<Artist> onArtistSelected) {
        HBox row = new HBox(0);
        row.getStyleClass().add("artist-list-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(2);
        Label name = new Label(artist.getName());
        name.getStyleClass().add("artist-list-name");

        int songs  = artist.getSongs()  != null ? artist.getSongs().size()  : 0;
        int albums = artist.getAlbums() != null ? artist.getAlbums().size() : 0;
        Label info = new Label(songs + " songs • " + albums + " albums");
        info.getStyleClass().add("artist-list-info");

        text.getChildren().addAll(name, info);
        HBox.setHgrow(text, Priority.ALWAYS);
        row.getChildren().add(text);

        row.setOnMouseClicked(e -> onArtistSelected.accept(artist));
        return row;
    }

    /**
     * Builds the artist detail view, consisting of a back-navigation header
     * followed by the artist's albums in either grid or list layout.
     *
     * @param artist      the {@link Artist} whose detail is being shown
     * @param displayMode {@code "grid"} for a card grid, any other value for a
     *                    compact list view
     * @param onBack      callback invoked when the user presses "Back to Artists"
     * @return a {@link VBox} containing the header and album section
     */
    public VBox buildArtistDetail(Artist artist, String displayMode, Runnable onBack) {
        VBox detail = new VBox(15);
        detail.setFillWidth(true);
        VBox.setVgrow(detail, Priority.ALWAYS);

        detail.getChildren().add(buildArtistBackRow(artist, onBack));
        detail.getChildren().add(buildAlbumSection(artist, displayMode));

        return detail;
    }

    private HBox buildArtistBackRow(Artist artist, Runnable onBack) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("album-detail-header");

        Button back = new Button("← Back to Artists");
        back.getStyleClass().add("btn-blue");
        back.setOnAction(e -> onBack.run());

        Label name = new Label(artist.getName());
        name.getStyleClass().add("artist-detail-name");

        row.getChildren().addAll(back, name);
        return row;
    }

    private Node buildAlbumSection(Artist artist, String displayMode) {
        List<Album> albums = libraryService.resolveAlbumsForArtist(artist);

        if (albums.isEmpty()) {
            Label empty = new Label("No albums found for this artist.");
            empty.getStyleClass().add("txt-grey-md");
            return empty;
        }

        if ("grid".equals(displayMode)) {
            return songListBuilder.buildAlbumGridView(albums, () -> {}, onAlbumSelected);
        } else {
            return buildAlbumListView(albums);
        }
    }

    private VBox buildAlbumListView(List<Album> albums) {
        VBox list = new VBox(0);
        list.setFillWidth(true);
        VBox.setVgrow(list, Priority.ALWAYS);
        for (Album album : albums) {
            AlbumListCell cell = new AlbumListCell();
            cell.updateItem(album, false);
            javafx.scene.Node row = cell.getGraphic();
            if (row == null) {
                // Fallback: build a plain label row if cell graphic is unavailable
                Label lbl = new Label(album.getTitle());
                lbl.getStyleClass().add("artist-list-name");
                lbl.getStyleClass().add("artist-list-row");
                row = lbl;
            }
            final Album a = album;
            row.setOnMouseClicked(e -> onAlbumSelected.accept(a));
            row.getStyleClass().add("artist-list-row");
            list.getChildren().add(row);
        }
        return list;
    }
}