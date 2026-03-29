package com.sebastiandorata.musicdashboard.controller.UserLibrary;

import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.libraryViews.*;
import com.sebastiandorata.musicdashboard.service.*;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.CardFactory;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyLibraryController {

    @Lazy @Autowired private MusicPlayerService musicPlayerService;
    @Lazy @Autowired private SongService        songService;
    @Autowired        private PlaylistService   playlistService;
    @Autowired        private FavouriteService  favouriteService;
    @Autowired        private LibraryService    libraryService;

    private String  currentView        = "albums";
    private String  currentDisplayMode = "grid";
    private Album   currentAlbum       = null;
    private Artist  currentArtist      = null;

    private VBox         contentArea;
    private ToggleButton listToggle;
    private ToggleButton gridToggle;

    private LibraryHandler ctx;
    private SongHandler           menuHandler;
    private SongViewBuilder       songListBuilder;
    private AlbumViewBuilder      albumDetailBuilder;
    private ArtistViewBuilder     artistViewBuilder;
    private FavouritesViewBuilder favouritesBuilder;

    @PostConstruct
    public void register() {
        MainController.registerLibrary(this);
    }

    public void show() {
        currentView        = "albums";
        currentDisplayMode = "grid";
        currentAlbum       = null;
        currentArtist      = null;

        initBuilders();
        applyScene();
    }

    /**
     * Entry point from the dashboard Top 5 Artists panel.
     * Opens My Library already drilled into the given artist's discography (grid view).
     */
    public void showWithArtist(Artist artist) {
        currentView        = "artists";
        currentDisplayMode = "grid";
        currentAlbum       = null;
        currentArtist      = artist;

        initBuilders();
        applyScene();
    }

    private void applyScene() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/library.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }
        MainController.switchViews(scene);
    }

    private void initBuilders() {
        ctx = new LibraryHandler(
                musicPlayerService, playlistService, favouriteService,
                (song, node) -> menuHandler.show(song, node)
        );
        menuHandler        = new SongHandler(ctx);
        songListBuilder    = new SongViewBuilder(ctx);
        albumDetailBuilder = new AlbumViewBuilder(ctx, this::backFromAlbum, this::drillIntoArtist);
        artistViewBuilder  = new ArtistViewBuilder(ctx, this::drillIntoAlbum, libraryService);
        favouritesBuilder  = new FavouritesViewBuilder(ctx);
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dark-page-bg");
        root.setTop(createTopBar());
        root.setCenter(createContentArea());
        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    private VBox createTopBar() {
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(20));
        topBar.getStyleClass().add("main-bkColour");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Button homeBtn = new Button("Home");
        homeBtn.getStyleClass().add("btn-blue");
        homeBtn.setOnAction(e -> MainController.navigateTo("dashboard"));

        Label title = new Label("My Library");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(homeBtn, title, spacer);

        HBox tabs = new HBox(5);
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.getChildren().addAll(
                tabButton("Songs",      "songs"),
                tabButton("Albums",     "albums"),
                tabButton("Artists",    "artists"),
                tabButton("Favourites", "favourites")
        );

        ToggleGroup viewGroup = new ToggleGroup();

        listToggle = new ToggleButton("List");
        listToggle.setToggleGroup(viewGroup);
        listToggle.getStyleClass().add("btn-blue");
        listToggle.setOnAction(e -> switchDisplayMode("list"));

        gridToggle = new ToggleButton("Grid");
        gridToggle.setToggleGroup(viewGroup);
        gridToggle.setSelected(true);
        gridToggle.getStyleClass().add("btn-blue");
        gridToggle.setOnAction(e -> switchDisplayMode("grid"));

        Label viewLabel = new Label("View: ");
        viewLabel.getStyleClass().add("txt-white");

        HBox viewMode = new HBox(5, viewLabel, listToggle, gridToggle);
        viewMode.setAlignment(Pos.CENTER_LEFT);

        topBar.getChildren().addAll(header, tabs, viewMode);
        return topBar;
    }

    private ScrollPane createContentArea() {
        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));
        contentArea.setFillWidth(true);

        loadContent();

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        BorderPane.setAlignment(scroll, Pos.TOP_LEFT);
        return scroll;
    }

    private Button tabButton(String text, String view) {
        Button btn = new Button(text);
        btn.getStyleClass().add(view.equals(currentView) ? "tab-BtnA" : "tab-BtnB");
        btn.setOnMouseEntered(e -> { if (!view.equals(currentView)) btn.getStyleClass().add("btn-enter"); });
        btn.setOnMouseExited(e  -> { if (!view.equals(currentView)) btn.getStyleClass().remove("btn-enter"); });
        btn.setOnAction(e -> switchView(view));
        return btn;
    }

    private void switchView(String view) {
        currentView   = view;
        currentAlbum  = null;
        currentArtist = null;

        if (view.equals("artists") || view.equals("favourites")) {
            currentDisplayMode = "list";
            listToggle.setSelected(true);
            gridToggle.setVisible(view.equals("favourites"));
        } else if (view.equals("albums")) {
            currentDisplayMode = "grid";
            gridToggle.setSelected(true);
            gridToggle.setVisible(true);
        } else {
            gridToggle.setVisible(true);
        }

        loadContent();
    }

    private void switchDisplayMode(String mode) {
        currentDisplayMode = mode;
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

        if (currentAlbum != null) {
            contentArea.getChildren().add(albumDetailBuilder.build(currentAlbum));
            return;
        }

        if (currentArtist != null) {
            contentArea.getChildren().add(
                    artistViewBuilder.buildArtistDetail(currentArtist, currentDisplayMode, this::backFromArtist)
            );
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
        List<Song> songs = songService.getAllSongs();
        Label header = new Label("All Songs (" + songs.size() + ")");
        header.getStyleClass().add("song-header");
        contentArea.getChildren().add(header);

        if ("list".equals(currentDisplayMode)) {
            contentArea.getChildren().add(songListBuilder.buildListView(songs));
        } else {
            contentArea.getChildren().add(songListBuilder.buildGridView(songs));
        }
    }

    private void loadAlbumsView() {
        List<Album> albums = libraryService.getAllAlbums();
        Label header = new Label("All Albums (" + albums.size() + ")");
        header.getStyleClass().add("song-header");
        contentArea.getChildren().add(header);

        if ("list".equals(currentDisplayMode)) {
            contentArea.getChildren().add(buildAlbumsListView(albums));
        } else {
            contentArea.getChildren().add(buildAlbumsGridView(albums));
        }
    }

    private void loadArtistsView() {
        List<Artist> artists = libraryService.getAllArtists();
        Label header = new Label("Artists (" + artists.size() + ")");
        header.getStyleClass().add("song-header");
        contentArea.getChildren().add(header);
        contentArea.getChildren().add(artistViewBuilder.buildArtistList(artists, this::drillIntoArtist));
        gridToggle.setVisible(false);
    }

    private void loadFavouritesView() {
        contentArea.getChildren().add(favouritesBuilder.build(currentDisplayMode));
        gridToggle.setVisible(true);
    }

    private ListView<Album> buildAlbumsListView(List<Album> albums) {
        ListView<Album> lv = new ListView<>();
        lv.setPrefHeight(AppUtils.APP_HEIGHT - 160);
        lv.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(lv, Priority.ALWAYS);
        lv.getStyleClass().add("main-bkColour");
        lv.setCellFactory(list -> new AlbumCardListCell());
        lv.getItems().addAll(albums);
        lv.setOnMouseClicked(e -> {
            Album selected = lv.getSelectionModel().getSelectedItem();
            if (selected != null) drillIntoAlbum(selected);
        });
        return lv;
    }

    private TilePane buildAlbumsGridView(List<Album> albums) {
        TilePane grid = new TilePane();
        grid.getStyleClass().add("tile-pane");
        grid.setPrefColumns(5);
        for (Album album : albums) {
            VBox card = CardFactory.createAlbumCard(album, musicPlayerService);
            card.setOnMouseClicked(e -> drillIntoAlbum(album));
            grid.getChildren().add(card);
        }
        return grid;
    }
}