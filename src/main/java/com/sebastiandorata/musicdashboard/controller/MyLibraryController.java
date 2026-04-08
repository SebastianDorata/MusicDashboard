package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.libraryViews.*;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.service.*;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Top-level controller for the My Library page.
 *
 * <p>Manages all mutable view state: the active tab (songs, albums,
 * artists, favourites), display mode (grid or list), the currently
 * selected album and artist, sort strategy, and genre filter. Delegates
 * all node construction to the view-builder hierarchy
 * ({@link SongViewBuilder},
 * {@link AlbumViewBuilder},
 * {@link ArtistViewBuilder},
 * {@link FavouritesViewBuilder}).
 * Supports direct entry via {@link #showWithAlbum(Album)}
 * and {@link #showWithArtist(Artist)}
 * for cross-page navigation.</p>
 */
@Component
public class MyLibraryController {

    @Lazy @Autowired private MusicPlayerService musicPlayerService;
    @Lazy @Autowired private SongImportService songImportService;
    @Autowired private PlaylistService   playlistService;
    @Autowired private FavouriteService  favouriteService;
    @Autowired private LibraryService    libraryService;
    @Autowired private GenreFilterService genreFilterService;
    @Autowired private SongRepository songRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private GenreRepository genreRepository;

    private String  currentView        = "albums";
    private String  currentDisplayMode = "grid";
    private Album   currentAlbum       = null;
    private Artist  currentArtist      = null;
    private SortStrategy currentSort = SortStrategy.ALPHABETICAL;
    private Genre currentGenreFilter = null;
    private VBox         contentArea;
    private ToggleButton listToggle;
    private ToggleButton gridToggle;
    private final java.util.Map<String, Button> tabButtons = new java.util.LinkedHashMap<>();
    private ComboBox<SortStrategy> sortComboBox;
    private ComboBox<GenreOption> genreComboBox;
    private HBox filterControlsBox;
    private LibraryHandler ctx;
    private SongHandler           menuHandler;
    private SongViewBuilder       songListBuilder;
    private AlbumViewBuilder      albumDetailBuilder;
    private ArtistViewBuilder     artistViewBuilder;
    private FavouritesViewBuilder favouritesBuilder;
    private BorderPane sceneRoot;
    private ScrollPane outerScrollPane;

    @PostConstruct
    public void register() {
        MainController.registerLibrary(this);
    }

    public void show() {
        currentView        = "albums";
        currentDisplayMode = "grid";
        currentAlbum       = null;
        currentArtist      = null;
        currentSort        = SortStrategy.ALPHABETICAL;
        currentGenreFilter = null;

        initBuilders();
        applyScene();
    }

    public void showWithArtist(Artist artist) {
        currentView        = "artists";
        currentDisplayMode = "grid";
        currentAlbum       = null;
        currentArtist      = artist;
        currentSort        = SortStrategy.ALPHABETICAL;
        currentGenreFilter = null;

        initBuilders();
        applyScene();
    }

    public void showWithAlbum(Album album) {
        currentView        = "albums";
        currentDisplayMode = "list";
        currentAlbum       = album;
        currentArtist      = null;
        currentSort        = SortStrategy.ALPHABETICAL;
        currentGenreFilter = null;

        initBuilders();
        applyScene();
    }

    private void applyScene() {
        Scene scene = this.createScene();

        try {
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/library.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }

        MainController.switchViews(scene);
    }

    private void initBuilders() {
        ctx = new LibraryHandler(
                musicPlayerService, playlistService, favouriteService,
                (song, node) -> menuHandler.show(song, node)
        );

        SongEditDialog editDialog = new SongEditDialog(
                songRepository, albumRepository, artistRepository, genreRepository
        );

        menuHandler        = new SongHandler(ctx);
        songListBuilder    = new SongViewBuilder(ctx, editDialog);
        albumDetailBuilder = new AlbumViewBuilder(ctx, this::backFromAlbum, this::drillIntoArtist);
        artistViewBuilder  = new ArtistViewBuilder(ctx, this::drillIntoAlbum, libraryService);
        favouritesBuilder  = new FavouritesViewBuilder(ctx);
    }

    private Scene createScene() {
        sceneRoot = new BorderPane();
        sceneRoot.getStyleClass().add("dark-page-bg");
        sceneRoot.setTop(createTopBar());
        outerScrollPane = createContentArea();
        sceneRoot.setCenter(outerScrollPane);
        return new Scene(sceneRoot, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    private VBox createTopBar() {
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(20));
        topBar.getStyleClass().add("header-background");
        topBar.setAlignment(Pos.CENTER);

        // Header row
        StackPane header = new StackPane();
        header.setMaxWidth(Double.MAX_VALUE);

        Button homeBtn = new Button("← Dashboard");
        homeBtn.getStyleClass().addAll("nav-btn-back", "txt-white-md-bld");
        homeBtn.setOnAction(e -> MainController.navigateTo("dashboard"));
        StackPane.setAlignment(homeBtn, Pos.CENTER_LEFT);

        Label title = new Label("My Library");
        title.getStyleClass().addAll("txt-white-bld-forty", "txt-centre-underline");
        StackPane.setAlignment(title, Pos.CENTER);

        header.getChildren().addAll(homeBtn, title);

        // Tab buttons on the left
        HBox leftGroup = new HBox(10);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        for (String[] pair : new String[][]{
                {"Songs", "songs"}, {"Albums", "albums"},
                {"Artists", "artists"}, {"Favourites", "favourites"}}) {
            Button btn = tabButton(pair[0], pair[1]);
            tabButtons.put(pair[1], btn);
            leftGroup.getChildren().add(btn);
        }

        // Toggles center
        ToggleGroup viewGroup = new ToggleGroup();

        listToggle = new ToggleButton("List");
        listToggle.setToggleGroup(viewGroup);
        listToggle.getStyleClass().addAll("nav-btn", "txt-white-md-bld");
        listToggle.setOnAction(e -> switchDisplayMode("list"));

        gridToggle = new ToggleButton("Grid");
        gridToggle.setToggleGroup(viewGroup);
        gridToggle.setSelected(true);
        gridToggle.getStyleClass().addAll("nav-btn-active", "txt-white-md-bld");
        gridToggle.setOnAction(e -> switchDisplayMode("grid"));

        HBox centerGroup = new HBox(10, listToggle, gridToggle);
        centerGroup.setAlignment(Pos.CENTER);

        // Filter controls on the right
        filterControlsBox = createFilterControlsBox();
        HBox rightGroup = new HBox(filterControlsBox);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        // StackPane aligns all three independently
        StackPane controlsRow = new StackPane();
        controlsRow.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(leftGroup, Pos.CENTER_LEFT);
        StackPane.setAlignment(centerGroup, Pos.CENTER);
        StackPane.setAlignment(rightGroup, Pos.CENTER_RIGHT);

        leftGroup.setPickOnBounds(false);//Only register mouse events on the actual visible content of each node
        centerGroup.setPickOnBounds(false);
        rightGroup.setPickOnBounds(false);

        controlsRow.getChildren().addAll(leftGroup, centerGroup, rightGroup);

        topBar.getChildren().addAll(header, controlsRow);
        return topBar;
    }

    /**
     * Creates the genre filter and sort dropdown controls.
     * Visibility is controlled by switchView() based on current view.
     */
    private HBox createFilterControlsBox() {
        HBox box = new HBox(15);
        box.getStyleClass().addAll("dropDown-options");

        // Genre dropdown
        Label genreLabel = new Label("Genre:");
        genreLabel.getStyleClass().add("txt-white-ttl-bld");

        genreComboBox = new ComboBox<>();
        genreComboBox.setPrefWidth(150);
        genreComboBox.getStyleClass().addAll("combo-box","txt-white-sm");
        genreComboBox.setOnAction(e -> {
            GenreOption selected = genreComboBox.getValue();
            currentGenreFilter = selected != null ? selected.genre : null;
            loadContent();
        });

        refreshGenreDropdown();

        // dropdown
        Label sortLabel = new Label("Sort by:");
        sortLabel.getStyleClass().add("txt-white-ttl-bld");

        sortComboBox = new ComboBox<>();
        sortComboBox.setPrefWidth(150);
        sortComboBox.getStyleClass().addAll("combo-box","txt-white-sm");
        sortComboBox.getItems().addAll(SortStrategy.values());
        sortComboBox.setValue(SortStrategy.ALPHABETICAL);
        sortComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SortStrategy strategy) {
                return strategy != null ? strategy.getDisplayName() : "";
            }

            @Override
            public SortStrategy fromString(String string) {
                return SortStrategy.ALPHABETICAL;
            }
        });
        sortComboBox.setOnAction(e -> {
            currentSort = sortComboBox.getValue();
            loadContent();
        });

        box.getChildren().addAll(genreLabel, genreComboBox, sortLabel, sortComboBox);
        return box;
    }

    /**
     * Refreshes the genre dropdown with current genres from the database.
     */
    private void refreshGenreDropdown() {
        genreComboBox.getItems().clear();
        genreComboBox.getItems().add(new GenreOption(null, "All Genres"));

        List<Genre> allGenres = genreRepository.findAll();
        for (Genre genre : allGenres) {
            genreComboBox.getItems().add(new GenreOption(genre, genre.getName()));
        }

        genreComboBox.setValue(genreComboBox.getItems().get(0)); // Default to "All Genres"
    }

    /**
     * Display genres in ComboBox.
     */
    private static class GenreOption {
        Genre genre;
        String display;

        GenreOption(Genre genre, String display) {
            this.genre = genre;
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private ScrollPane createContentArea() {
        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));
        contentArea.setFillWidth(true);

        loadContent();

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        BorderPane.setAlignment(scroll, Pos.TOP_LEFT);
        return scroll;
    }

    private Button tabButton(String text, String view) {
        Button btn = new Button(text);
        updateTabButtonStyle(btn, view);
        btn.setOnMouseEntered(e -> {
            if (!view.equals(currentView)) btn.getStyleClass().add("btn-enter");
        });
        btn.setOnMouseExited(e -> {
            if (!view.equals(currentView)) btn.getStyleClass().remove("btn-enter");
        });
        btn.setOnAction(e -> switchView(view));
        return btn;
    }

    /**
     * Updates tab button styling based on whether it's the active tab.
     * Songs, Albums, Artist, and Favourites.
     */
    private void updateTabButtonStyle(Button btn, String view) {
        btn.getStyleClass().removeAll("nav-btn", "nav-btn-active");

        if (view.equals(currentView)) {
            btn.getStyleClass().add("nav-btn-active");
        } else {
            btn.getStyleClass().add("nav-btn");
        }
    }

    private void switchView(String view) {
        currentView   = view;
        currentAlbum  = null;
        currentArtist = null;

        currentSort = SortStrategy.ALPHABETICAL;
        currentGenreFilter = null;
        if (sortComboBox != null) sortComboBox.setValue(SortStrategy.ALPHABETICAL);
        if (genreComboBox != null) genreComboBox.setValue(genreComboBox.getItems().get(0));

        tabButtons.forEach((v, btn) -> updateTabButtonStyle(btn, v));

        if (view.equals("artists") || view.equals("favourites")) {
            currentDisplayMode = "list";
            listToggle.setSelected(true);
            gridToggle.setVisible(view.equals("favourites"));
            filterControlsBox.setVisible(false);
        } else if (view.equals("albums")) {
            currentDisplayMode = "grid";
            gridToggle.setSelected(true);
            gridToggle.setVisible(true);
            filterControlsBox.setVisible(true);
        } else {
            currentDisplayMode = "list";
            listToggle.setSelected(true);
            gridToggle.setVisible(false);
            filterControlsBox.setVisible(true);
        }

        updateToggleStyles();  // ← one call handles all branches
        loadContent();
    }

    private void switchDisplayMode(String mode) {
        currentDisplayMode = mode;
        updateToggleStyles();
        loadContent();
    }

    private void drillIntoAlbum(Album album) {
        currentAlbum       = album;
        currentDisplayMode = "list";
        loadContent();
    }

    private void backFromAlbum() {
        currentAlbum       = null;
        currentView        = "albums";
        currentDisplayMode = "grid";
        gridToggle.setSelected(true);
        loadContent();
    }

    private void drillIntoArtist(Artist artist) {
        currentArtist      = artist;
        currentAlbum       = null;
        currentView        = "artists";
        currentDisplayMode = "grid";
        gridToggle.setVisible(true);
        gridToggle.setSelected(true);
        filterControlsBox.setVisible(false);
        loadContent();
    }

    private void backFromArtist() {
        currentArtist      = null;
        currentDisplayMode = "list";
        listToggle.setSelected(true);
        loadContent();
    }

    private void loadContent() {
        contentArea.getChildren().clear();
        sceneRoot.setCenter(outerScrollPane); // Reset default; list views override this

        if (currentAlbum != null) {
            contentArea.getChildren().add(albumDetailBuilder.build(currentAlbum));
            return;
        }
        if (currentArtist != null) {
            contentArea.getChildren().add(
                    artistViewBuilder.buildArtistDetail(currentArtist, currentDisplayMode, this::backFromArtist));
            return;
        }
        switch (currentView) {
            case "songs"      -> loadSongsView();
            case "albums"     -> loadAlbumsView();
            case "artists"    -> loadArtistsView();
            case "favourites" -> loadFavouritesView();
        }
    }

    private void loadSongsView() {
        List<Song> songs = songImportService.getAllSongs();
        songs = genreFilterService.filterSongsByGenre(songs, currentGenreFilter);

        Label header = new Label("All Songs (" + songs.size() + ")");
        header.getStyleClass().add("view-header");
        header.setPadding(new Insets(20, 20, 4, 20));

        if ("list".equals(currentDisplayMode)) {
            BorderPane listWithBar = songListBuilder.buildListView(songs, currentSort);
            VBox wrapper = new VBox(0, header, listWithBar);
            wrapper.setFillWidth(true);
            wrapper.getStyleClass().add("main-bkColour");
            VBox.setVgrow(listWithBar, Priority.ALWAYS);
            sceneRoot.setCenter(wrapper);
        } else {
            contentArea.getChildren().addAll(header, songListBuilder.buildGridView(songs, currentSort));
            // outerScrollPane is already set as center from loadContent()
        }
    }

    private void loadAlbumsView() {
        List<Album> albums = libraryService.getAllAlbums();
        albums = genreFilterService.filterAlbumsByGenre(albums, currentGenreFilter);

        Label header = new Label("All Albums (" + albums.size() + ")");
        header.getStyleClass().add("view-header");
        header.setPadding(new Insets(20, 20, 4, 20));

        if ("list".equals(currentDisplayMode)) {
            BorderPane listWithBar = buildAlbumsListView(albums, currentSort);
            VBox wrapper = new VBox(0, header, listWithBar);
            wrapper.setFillWidth(true);
            wrapper.getStyleClass().add("main-bkColour");
            VBox.setVgrow(listWithBar, Priority.ALWAYS);
            sceneRoot.setCenter(wrapper);
        } else {
            contentArea.getChildren().addAll(header, buildAlbumsGridView(albums, currentSort));
        }
    }

    private void loadArtistsView() {
        List<Artist> artists = libraryService.getAllArtists();
        Label header = new Label("Artists (" + artists.size() + ")");
        header.getStyleClass().add("txt-white-md-bld");
        contentArea.getChildren().add(header);
        contentArea.getChildren().add(artistViewBuilder.buildArtistList(artists, this::drillIntoArtist));
        gridToggle.setVisible(false);
    }

    private void loadFavouritesView() {
        contentArea.getChildren().add(favouritesBuilder.build(currentDisplayMode));
        gridToggle.setVisible(true);
    }

    private BorderPane buildAlbumsListView(List<Album> albums, SortStrategy sort) {
        List<Album> sorted = albums.stream()
                .sorted(sort.getAlbumComparator())
                .toList();

        // Group albums by first letter under dividers
        VBox content = new VBox(0);
        content.setFillWidth(true);
        content.getStyleClass().add("main-bkColour");

        Map<String, List<Album>> grouped = new LinkedHashMap<>();
        for (Album album : sorted) {
            String title = album.getTitle() != null ? album.getTitle() : "";
            char c = title.isBlank() ? '#' : Character.toUpperCase(title.trim().charAt(0));
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

            // Use a ListView for each letter group so AlbumCardListCell still works
            for (Album album : entry.getValue()) {
                new AlbumCardListCell();
                HBox row = AlbumCardListCell.buildRow(album);
                row.setOnMouseClicked(e -> drillIntoAlbum(album));
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

    private TilePane buildAlbumsGridView(List<Album> albums, SortStrategy sort) {
        List<Album> sorted = albums.stream()
                .sorted(sort.getAlbumComparator())
                .toList();

        TilePane grid = new TilePane();
        grid.getStyleClass().add("tile-pane");
        grid.setPrefColumns(4);
        grid.setPrefTileWidth(200);
        grid.setPrefTileHeight(220);

        for (Album album : sorted) {
            VBox card = CardFactory.createAlbumCard(album, musicPlayerService);
            card.getStyleClass().add("album-grid-card");
            card.setOnMouseClicked(e -> drillIntoAlbum(album));
            grid.getChildren().add(card);
        }
        return grid;
    }

    private void updateToggleStyles() {
        listToggle.getStyleClass().removeAll("nav-btn", "nav-btn-active");
        gridToggle.getStyleClass().removeAll("nav-btn", "nav-btn-active");
        listToggle.getStyleClass().add(currentDisplayMode.equals("list") ? "nav-btn-active" : "nav-btn");
        gridToggle.getStyleClass().add(currentDisplayMode.equals("grid") ? "nav-btn-active" : "nav-btn");
    }
}