package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.libraryViews.*;
import com.sebastiandorata.musicdashboard.presentation.ArtistDiscographyNavigation;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.service.*;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level controller for the My Library page.
 *
 * <p>Manages all mutable view state and routes to the correct
 * view builder. All UI construction is delegated to:
 * {@link LibraryTopBarBuilder} — top bar and filter controls,
 * {@link AlbumViewHelper} — album list and grid views,
 * {@link SongViewBuilder} — song list and grid views,
 * {@link AlbumViewBuilder} — album detail drill-down,
 * {@link ArtistViewBuilder} — artist list and detail views,
 * {@link FavouritesViewBuilder} — favourites view.
 *
 * <p>Artist navigation is centralized through
 * {@link ArtistDiscographyNavigation} so clicking an artist
 * name anywhere in the app routes through one place.</p>
 */
@Component
public class MyLibraryController {


    @Lazy @Autowired private MusicPlayerService   musicPlayerService;
    @Lazy @Autowired private SongImportService    songImportService;
    @Autowired private PlaylistService            playlistService;
    @Autowired private FavouriteService           favouriteService;
    @Autowired private LibraryService             libraryService;
    @Autowired private GenreFilterService         genreFilterService;
    @Autowired private SongRepository             songRepository;
    @Autowired private AlbumRepository            albumRepository;
    @Autowired private ArtistRepository           artistRepository;
    @Autowired private GenreRepository            genreRepository;
    @Autowired private ArtistDiscographyNavigation artistNavigation;

    private final LibraryState state = new LibraryState();

    private final Map<String, Button> tabButtons = new LinkedHashMap<>();
    private ToggleButton listToggle;
    private ToggleButton gridToggle;
    private HBox filterControlsBox;
    private ComboBox<SortStrategy> sortComboBox;
    private ComboBox<LibraryTopBarBuilder.GenreOption>  genreComboBox;
    private VBox contentArea;
    private BorderPane sceneRoot;
    private ScrollPane outerScrollPane;

    private SongHandler menuHandler;
    private SongViewBuilder songListBuilder;
    private AlbumViewBuilder albumDetailBuilder;
    private AlbumViewHelper albumViewHelper;
    private ArtistViewBuilder artistViewBuilder;
    private FavouritesViewBuilder favouritesBuilder;

    @PostConstruct
    public void register() {
        MainController.registerLibrary(this);
    }


    public void show() {
        state.resetToDefault();
        initBuilders();
        applyScene();
    }

    public void showWithArtist(Artist artist) {
        state.resetToArtist(artist);
        initBuilders();
        applyScene();
    }

    public void showWithAlbum(Album album) {
        Album fullAlbum = libraryService.getAlbumWithFullDetails(
                album.getAlbumId());
        state.resetToAlbum(fullAlbum);
        initBuilders();
        applyScene();
    }


    private void applyScene() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/globalStyle.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/buttons.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/library.css")).toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }
        MainController.switchViews(scene);
    }

    private void initBuilders() {
        SongEditDialog editDialog = new SongEditDialog(songRepository, albumRepository, artistRepository, genreRepository);

        LibraryHandler ctx = new LibraryHandler(musicPlayerService, playlistService, favouriteService,
                (song, node) -> menuHandler.show(song, node), editDialog);

        menuHandler        = new SongHandler(ctx);
        songListBuilder    = new SongViewBuilder(ctx, editDialog);

        // Artist drill-in uses the centralized navigation service
        albumDetailBuilder = new AlbumViewBuilder(ctx, this::backFromAlbum, artist -> artistNavigation.navigateToArtist(artist));
        albumViewHelper    = new AlbumViewHelper(musicPlayerService, this::drillIntoAlbum);
        artistViewBuilder  = new ArtistViewBuilder(ctx, this::drillIntoAlbum, libraryService);
        favouritesBuilder  = new FavouritesViewBuilder(ctx);
    }

    private Scene createScene() {
        sceneRoot = new BorderPane();
        sceneRoot.getStyleClass().add("dark-page-bg");
        sceneRoot.setTop(buildTopBar());

        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));
        contentArea.setFillWidth(true);

        outerScrollPane = new ScrollPane(contentArea);
        outerScrollPane.setFitToWidth(true);
        outerScrollPane.setFitToHeight(false);
        VBox.setVgrow(outerScrollPane, Priority.ALWAYS);
        BorderPane.setAlignment(outerScrollPane, Pos.TOP_LEFT);

        // Wire up before loadContent so sceneRoot is fully ready
        sceneRoot.setCenter(outerScrollPane);
        loadContent();

        return new Scene(sceneRoot, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }


    private VBox buildTopBar() {
        LibraryTopBarBuilder.Result result = LibraryTopBarBuilder.build(
                tabButtons,
                genreRepository,
                state.currentView,
                state.currentDisplayMode,
                this::switchView,
                this::switchDisplayMode,
                genre -> {
                    state.currentGenreFilter = genre;
                    loadContent();
                },
                sort -> {
                    state.currentSort = sort;
                    loadContent();
                }
        );

        listToggle        = result.listToggle();
        gridToggle        = result.gridToggle();
        filterControlsBox = result.filterControlsBox();
        sortComboBox      = result.sortComboBox();
        genreComboBox     = result.genreComboBox();
        return result.topBar();
    }


    private void switchView(String view) {
        state.currentView        = view;
        state.currentAlbum       = null;
        state.currentArtist      = null;
        state.currentSort        = SortStrategy.ALPHABETICAL;
        state.currentGenreFilter = null;

        // Reset dropdowns without triggering their onChange callbacks
        if (sortComboBox  != null) sortComboBox.setValue(SortStrategy.ALPHABETICAL);
        if (genreComboBox != null && !genreComboBox.getItems().isEmpty())
            genreComboBox.setValue(genreComboBox.getItems().getFirst());

        // Update tab button styles
        tabButtons.forEach((v, btn) ->
                LibraryTopBarBuilder.updateTabStyle(btn, v, view));

        switch (view) {
            case "artists" -> {
                state.currentDisplayMode = "list";
                listToggle.setSelected(true);
                gridToggle.setVisible(false);
                filterControlsBox.setVisible(false);
            }
            case "favourites" -> {
                state.currentDisplayMode = "list";
                listToggle.setSelected(true);
                gridToggle.setVisible(true);
                filterControlsBox.setVisible(false);
            }
            case "albums" -> {
                state.currentDisplayMode = "grid";
                gridToggle.setSelected(true);
                gridToggle.setVisible(true);
                filterControlsBox.setVisible(true);
            }
            default -> {
                // songs
                state.currentDisplayMode = "list";
                listToggle.setSelected(true);
                gridToggle.setVisible(false);
                filterControlsBox.setVisible(true);
            }
        }

        updateToggleStyles();
        loadContent();
    }

    private void switchDisplayMode(String mode) {
        state.currentDisplayMode = mode;
        updateToggleStyles();
        loadContent();
    }

    private void drillIntoAlbum(Album album) {
        state.currentAlbum = libraryService.getAlbumWithFullDetails(album.getAlbumId());
        state.currentDisplayMode = "list";
        loadContent();
    }

    private void backFromAlbum() {
        state.currentAlbum       = null;
        state.currentView        = "albums";
        state.currentDisplayMode = "grid";
        gridToggle.setSelected(true);
        loadContent();
    }

    private void drillIntoArtist(Artist artist) {
        state.currentArtist      = artist;
        state.currentAlbum       = null;
        state.currentView        = "artists";
        state.currentDisplayMode = "grid";
        gridToggle.setVisible(true);
        gridToggle.setSelected(true);
        filterControlsBox.setVisible(false);
        loadContent();
    }

    private void backFromArtist() {
        state.currentArtist      = null;
        state.currentDisplayMode = "list";
        listToggle.setSelected(true);
        loadContent();
    }


    private void loadContent() {
        contentArea.getChildren().clear();
        sceneRoot.setCenter(outerScrollPane);

        if (state.currentAlbum != null) {
            contentArea.getChildren().add(
                    albumDetailBuilder.build(state.currentAlbum));
            return;
        }
        if (state.currentArtist != null) {
            contentArea.getChildren().add(
                    artistViewBuilder.buildArtistDetail(
                            state.currentArtist,
                            state.currentDisplayMode,
                            this::backFromArtist));
            return;
        }
        switch (state.currentView) {
            case "songs"      -> loadSongsView();
            case "albums"     -> loadAlbumsView();
            case "artists"    -> loadArtistsView();
            case "favourites" -> loadFavouritesView();
        }
    }

    private void loadSongsView() {
        List<Song> songs = genreFilterService.filterSongsByGenre(
                songImportService.getAllSongs(),
                state.currentGenreFilter);

        Label header = new Label("All Songs (" + songs.size() + ")");
        header.getStyleClass().add("view-header");
        header.setPadding(new Insets(20, 20, 4, 20));

        if ("list".equals(state.currentDisplayMode)) {
            BorderPane listWithBar = songListBuilder.buildListView(
                    songs, state.currentSort);
            VBox wrapper = new VBox(0, header, listWithBar);
            wrapper.setFillWidth(true);
            wrapper.getStyleClass().add("main-bkColour");
            VBox.setVgrow(listWithBar, Priority.ALWAYS);
            sceneRoot.setCenter(wrapper);
        } else {
            contentArea.getChildren().addAll(header,
                    songListBuilder.buildGridView(songs, state.currentSort));
        }
    }

    private void loadAlbumsView() {
        List<Album> albums = genreFilterService.filterAlbumsByGenre(
                libraryService.getAllAlbums(),
                state.currentGenreFilter);

        Label header = new Label("All Albums (" + albums.size() + ")");
        header.getStyleClass().add("view-header");
        header.setPadding(new Insets(20, 20, 4, 20));

        if ("list".equals(state.currentDisplayMode)) {
            BorderPane listWithBar = albumViewHelper.buildListView(
                    albums, state.currentSort);
            VBox wrapper = new VBox(0, header, listWithBar);
            wrapper.setFillWidth(true);
            wrapper.getStyleClass().add("main-bkColour");
            VBox.setVgrow(listWithBar, Priority.ALWAYS);
            sceneRoot.setCenter(wrapper);
        } else {
            ScrollPane gridScroll = albumViewHelper.buildGridView(
                    albums, state.currentSort);
            VBox wrapper = new VBox(0, header, gridScroll);
            wrapper.setFillWidth(true);
            wrapper.getStyleClass().add("main-bkColour");
            VBox.setVgrow(gridScroll, Priority.ALWAYS);
            sceneRoot.setCenter(wrapper);
        }
    }

    private void loadArtistsView() {
        List<Artist> artists = libraryService.getAllArtists();

        Label header = new Label("Artists (" + artists.size() + ")");
        header.getStyleClass().add("view-header");
        header.setPadding(new Insets(20, 20, 4, 20));

        // Artist list, clicking a row drills into that artist.
        // Artist name in playback panel uses artistNavigation directly
        BorderPane listWithBar = artistViewBuilder.buildArtistList(
                artists, this::drillIntoArtist);
        VBox wrapper = new VBox(0, header, listWithBar);
        wrapper.setFillWidth(true);
        wrapper.getStyleClass().add("main-bkColour");
        VBox.setVgrow(listWithBar, Priority.ALWAYS);
        sceneRoot.setCenter(wrapper);
        gridToggle.setVisible(false);
    }

    private void loadFavouritesView() {
        contentArea.getChildren().add(
                favouritesBuilder.build(state.currentDisplayMode));
        gridToggle.setVisible(true);
    }

    private void updateToggleStyles() {
        listToggle.getStyleClass().removeAll("nav-btn", "nav-btn-active");
        gridToggle.getStyleClass().removeAll("nav-btn", "nav-btn-active");
        listToggle.getStyleClass().add(
                "list".equals(state.currentDisplayMode)
                        ? "nav-btn-active" : "nav-btn");
        gridToggle.getStyleClass().add(
                "grid".equals(state.currentDisplayMode)
                        ? "nav-btn-active" : "nav-btn");
    }
}