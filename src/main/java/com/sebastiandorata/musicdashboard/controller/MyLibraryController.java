package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.Utils.CardFactory;
import com.sebastiandorata.musicdashboard.Utils.SongCell;
import com.sebastiandorata.musicdashboard.Utils.Utils;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.SongService;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;




@Component
public class MyLibraryController {
    @Autowired
    private MusicPlayerService musicPlayerService;

    @Lazy
    @Autowired
    private SongService songService;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @PostConstruct
    public void register() {
        MainController.registerLibrary(this);
    }



    private String currentView        = "songs";
    private String currentDisplayMode = "list";

    private BorderPane mainLayout;
    private VBox       contentArea;
    private ToggleButton listView;
    private ToggleButton gridView;



    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }
        MainController.switchViews(scene);
    }

    private Scene createScene() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #121212;");
        mainLayout.setTop(createTopBar());
        mainLayout.setCenter(createContentScrollPane());
        return new Scene(mainLayout, Utils.APP_WIDTH, Utils.APP_HEIGHT);
    }


    private VBox createTopBar() {
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(20));
        topBar.getStyleClass().add("main-bkColour");

        // Header row: Home button + title
        HBox navBarHeader = new HBox(15);
        navBarHeader.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("Home");
        backBtn.getStyleClass().add("btn-blue");
        backBtn.setOnAction(e -> MainController.navigateTo("dashboard"));

        Label title = new Label("My Library");
        title.getStyleClass().add("section-title");


        Region regionOne = new Region(); //https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/layout/Region.html
        HBox.setHgrow(regionOne, Priority.ALWAYS);

            navBarHeader.getChildren().addAll(backBtn, title, regionOne);

        // Tab row: Songs / Albums / Artists
        HBox navTabs = new HBox(5);
        navTabs.setAlignment(Pos.CENTER_LEFT);
        navTabs.getChildren().addAll(
                createTabButton("Songs",   "songs"),
                createTabButton("Albums",  "albums"),
                createTabButton("Artists", "artists")
        );
        // View mode toggle — FIX: both buttons share the same ToggleGroup
        // Artists view only supports list mode (grid not useful for name-only display)
        ToggleGroup viewGroup = new ToggleGroup();

        listView = new ToggleButton("List");
        listView.setToggleGroup(viewGroup);
        listView.setSelected(true);
        listView.getStyleClass().add("btn-blue");
        listView.setOnAction(e -> switchDisplayMode("list"));

        gridView = new ToggleButton("Grid");
        gridView.setToggleGroup(viewGroup);
        gridView.getStyleClass().add("btn-blue");
        gridView.setOnAction(e -> switchDisplayMode("grid"));

        Label viewLabel = new Label("View: ");
        viewLabel.getStyleClass().add("txt-white");

        HBox viewMode = new HBox(5, viewLabel, listView, gridView);
        viewMode.setAlignment(Pos.CENTER_LEFT);

        topBar.getChildren().addAll(navBarHeader, navTabs, viewMode);
        return topBar;
    }

    private Button createTabButton(String text, String view) {
        Button btn = new Button(text);
            btn.getStyleClass().add(view.equals(currentView) ? "tab-BtnA" : "tab-BtnB");

            btn.setOnMouseEntered(e -> {
                if (!view.equals(currentView)) btn.getStyleClass().add("btn-enter");
            });
            btn.setOnMouseExited(e -> {
                if (!view.equals(currentView)) btn.getStyleClass().remove("btn-enter");
            });

            btn.setOnAction(e -> switchView(view));
        return btn;
    }



    private ScrollPane createContentScrollPane() {
        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));
        loadContent();

        ScrollPane scrollPane = new ScrollPane(contentArea);
            scrollPane.setFitToWidth(true);
            scrollPane.getStyleClass().add("scroll-pane");
            return scrollPane;
    }

    private void switchView(String view) {
        currentView = view;

        // Artists only supports list view. This hides grid toggle when on artists tab
        boolean showGridOption = !view.equals("artists");
            gridView.setVisible(showGridOption);
            if (!showGridOption) {
                currentDisplayMode = "list";
                listView.setSelected(true);
            }

            loadContent();
    }

    private void switchDisplayMode(String mode) {
        currentDisplayMode = mode;
        loadContent();
    }

    private void loadContent() {
        contentArea.getChildren().clear();
        switch (currentView) {
            case "songs"-> loadSongsView();
            case "albums"-> loadAlbumsView();
            case "artists"-> loadArtistsView();
        }
    }


    private void loadSongsView() {
        List<Song> songs = songService.getAllSongs();
        Label header = new Label("All Songs (" + songs.size() + ")");
            header.getStyleClass().add("song-header");

            if (currentDisplayMode.equals("list")) {
                contentArea.getChildren().addAll(header, createSongsListView(songs));
            } else {
                contentArea.getChildren().addAll(header, createSongsGridView(songs));
            }
    }

    private ListView<Song> createSongsListView(List<Song> songs) {
        ListView<Song> listView = new ListView<>();
            listView.setPrefHeight(600);
            listView.setStyle("-fx-background-color: #1e1e1e;");
            listView.setCellFactory(list -> new SongCell());
            listView.getItems().addAll(songs);

            listView.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSong, newSong) -> {
                        if (newSong != null) {
                            // Set the full song list as the queue before playing
                            // TODO: Replace songs list with user-defined queue when queue management is built
                            musicPlayerService.setQueue(songs);
                            musicPlayerService.playSong(newSong);
                        }
                    }
            );
            return listView;
    }

    private TilePane createSongsGridView(List<Song> songs) {
        TilePane grid = buildGridPane();
        for (Song song : songs) {
            VBox card = CardFactory.createSongCard(song, musicPlayerService);

            // Override card click to also set the queue
            // TODO: Replace songs list with user-defined queue when queue management is built
            card.setOnMouseClicked(e -> {
                musicPlayerService.setQueue(songs);
                musicPlayerService.playSong(song);
            });

            grid.getChildren().add(card);
        }
        return grid;
    }



    private void loadAlbumsView() {
        List<Album> albums = albumRepository.findAll();
        Label header = new Label("All Albums (" + albums.size() + ")");
        header.getStyleClass().add("song-header");

        if (currentDisplayMode.equals("list")) {
            contentArea.getChildren().addAll(header, createAlbumsListView(albums));
        } else {
            contentArea.getChildren().addAll(header, createAlbumsGridView(albums));
        }
    }

    private ListView<Album> createAlbumsListView(List<Album> albums) {
        ListView<Album> listView = new ListView<>();
            listView.setPrefHeight(600);
            listView.getStyleClass().add("main-bkColour");

            listView.setCellFactory(list -> new ListCell<>() {
                private final HBox root = new HBox(15);
                private final ImageView art = new ImageView();
                private final Label titleLbl = new Label();
                private final Label infoLbl  = new Label();

                {
                art.setFitWidth(60);
                art.setFitHeight(60);
                art.setPreserveRatio(true);
                titleLbl.getStyleClass().add("wt-smmd-bld");
                infoLbl.getStyleClass().add("txt-grey-md");
                VBox text = new VBox(5, titleLbl, infoLbl);
                text.setAlignment(Pos.CENTER_LEFT);
                root.setAlignment(Pos.CENTER_LEFT);
                root.getChildren().addAll(art, text);
            }

            @Override
            protected void updateItem(Album album, boolean empty) {
                super.updateItem(album, empty);
                if (empty || album == null) {
                    setGraphic(null);
                } else {
                    titleLbl.setText(album.getTitle());
                    int songCount = album.getSongs() != null ? album.getSongs().size() : 0;
                    String year   = album.getReleaseDate() != null
                            ? String.valueOf(album.getReleaseDate().getYear()) : "Unknown";
                    infoLbl.setText(songCount + " songs • " + year);

                    if (album.getAlbumArtPath() != null) {
                        try { art.setImage(new Image("file:" + album.getAlbumArtPath(), true)); }
                        catch (Exception e) { art.setImage(null); }
                    } else {
                        art.setImage(null);
                    }
                    setGraphic(root);
                }
            }
        });

        listView.getItems().addAll(albums);
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Album selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getSongs() != null && !selected.getSongs().isEmpty()) {
                    // Set album songs as the queue, play first
                    // TODO: Replace with user-defined queue when queue management is built
                    musicPlayerService.setQueue(selected.getSongs());
                    musicPlayerService.playSong(selected.getSongs().get(0));
                }
            }
        });
        return listView;
    }

    private TilePane createAlbumsGridView(List<Album> albums) {
        TilePane grid = buildGridPane();
        for (Album album : albums) {
            VBox card = CardFactory.createAlbumCard(album, musicPlayerService);

            // Override card click to also set the queue
            card.setOnMouseClicked(e -> {
                if (album.getSongs() != null && !album.getSongs().isEmpty()) {
                    // TODO: Replace with user-defined queue when queue management is built
                    musicPlayerService.setQueue(album.getSongs());
                    musicPlayerService.playSong(album.getSongs().get(0));
                }
            });

            grid.getChildren().add(card);
        }
        return grid;
    }

    // ----------------------------------------------------------------
    // Artists — list view only (alphabetical by name)
    // ----------------------------------------------------------------

    private void loadArtistsView() {
        List<Artist> artists = artistRepository.findAll();

        // Sort alphabetically by name
        artists.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Label header = new Label("All Artists (" + artists.size() + ")");
        header.getStyleClass().add("song-header");

        contentArea.getChildren().addAll(header, createArtistsListView(artists));
    }

    private ListView<Artist> createArtistsListView(List<Artist> artists) {
        ListView<Artist> listView = new ListView<>();
        listView.setPrefHeight(600);
        listView.setStyle("-fx-background-color: #1e1e1e;");

        listView.setCellFactory(list -> new ListCell<>() {
            private final HBox  root    = new HBox(15);
            private final Label initial = new Label();  // coloured initial circle
            private final Label nameLbl = new Label();
            private final Label infoLbl = new Label();

            {
                // Circular initial badge
                initial.setStyle(
                        "-fx-background-color: #4CAF50;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 18px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-min-width: 48; -fx-min-height: 48;" +
                                "-fx-max-width: 48; -fx-max-height: 48;" +
                                "-fx-alignment: center;" +
                                "-fx-background-radius: 24;"
                );

                nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
                infoLbl.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 13px;");

                VBox text = new VBox(3, nameLbl, infoLbl);
                text.setAlignment(Pos.CENTER_LEFT);

                root.setAlignment(Pos.CENTER_LEFT);
                root.getChildren().addAll(initial, text);
            }

            @Override
            protected void updateItem(Artist artist, boolean empty) {
                super.updateItem(artist, empty);
                if (empty || artist == null) {
                    setGraphic(null);
                } else {
                    initial.setText(artist.getName().substring(0, 1).toUpperCase());
                    nameLbl.setText(artist.getName());

                    int songCount  = artist.getSongs()  != null ? artist.getSongs().size()  : 0;
                    int albumCount = artist.getAlbums() != null ? artist.getAlbums().size() : 0;
                    infoLbl.setText(songCount + " songs • " + albumCount + " albums");

                    setGraphic(root);
                }
            }
        });

        listView.getItems().addAll(artists);

        // Double-click to play all songs by this artist
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Artist selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getSongs() != null && !selected.getSongs().isEmpty()) {
                    // TODO: Replace with artist-scoped queue when queue management is built.
                    //       This is where "playing all songs by artist" context should be set,
                    //       so Next/Prev stays within that artist's songs only.
                    musicPlayerService.setQueue(selected.getSongs());
                    musicPlayerService.playSong(selected.getSongs().get(0));
                }
            }
        });

        return listView;
    }


    private TilePane buildGridPane() {
        TilePane grid = new TilePane();
        grid.setPadding(new Insets(10));
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPrefColumns(5);
        return grid;
    }
}