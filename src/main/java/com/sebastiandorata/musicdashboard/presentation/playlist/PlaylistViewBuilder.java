package com.sebastiandorata.musicdashboard.presentation.playlist;

import com.sebastiandorata.musicdashboard.controller.PlaylistController;
import com.sebastiandorata.musicdashboard.presentation.helpers.RowConfig;
import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Builds all JavaFX nodes for the Playlist screen.
 *
 * <p><b><u>Public surface methods used by PlaylistController:</u></b></p>
 * <ul>
 *   <li>buildSidebarNewButton(): the sole content injected into the sidebar,just a "+ New Playlist" button.</li>
 *   <li>buildPlaylistBrowser(): center content when no playlist is selected,
 *       renders all user playlists as a card grid or list rows depending on displayMode.</li>
 *   <li>buildSongPanel(): center content when a playlist is selected, renders that playlist's
 *   songs with its action buttons (Play All, Rename, Delete).</li>
 * </ul>
 * <p>SRP: UI construction only, never mutates application state.</p>
 * <p>OCP: New display modes require only a new branch in the browser/song methods.</p>
 * <p>Time Complexity: O(n) where n = playlists or songs rendered.</p>
 * <p>Space Complexity: O(n).</p>
 */
public class PlaylistViewBuilder {

    private final PlaylistService    playlistService;
    private final MusicPlayerService musicPlayerService;
    private final PlaylistController controller;

    public PlaylistViewBuilder(PlaylistService playlistService, MusicPlayerService musicPlayerService, PlaylistController controller) {
        this.playlistService    = playlistService;
        this.musicPlayerService = musicPlayerService;
        this.controller         = controller;
    }

    /**
     * Returns a VBox containing only the "+ New Playlist" button.
     * Injected into SidebarBuilder's extraContent slot.
     */
    public VBox buildSidebarNewButton() {
        VBox section = new VBox(4);
        section.setFillWidth(true);

        Button newBtn = new Button("＋  New Playlist");
        newBtn.getStyleClass().add("nav-btn");
        newBtn.setMaxWidth(Double.MAX_VALUE);
        newBtn.setAlignment(Pos.CENTER_LEFT);
        newBtn.setOnAction(e -> PlaylistDialogHandler.showCreateDialog(playlistService, controller));

        section.getChildren().add(newBtn);
        return section;
    }

    /**
     * Renders all user playlists as either a card grid or a list of rows.
     *
     * @param playlists   all playlists belonging to the current user
     * @param displayMode "grid" | "list"
     */
    public VBox buildPlaylistBrowser(List<Playlist> playlists, String displayMode) {
        VBox browser = new VBox(16);
        browser.setFillWidth(true);
        VBox.setVgrow(browser, Priority.ALWAYS);

        if (playlists.isEmpty()) {
            browser.getChildren().add(buildEmptyBrowserState());
            return browser;
        }

        if ("grid".equals(displayMode)) {
            browser.getChildren().add(buildPlaylistGrid(playlists));
        } else {
            browser.getChildren().add(buildPlaylistList(playlists));
        }

        return browser;
    }

    private VBox buildEmptyBrowserState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));

        Label icon = new Label("🎵");
        icon.getStyleClass().add("playlist-empty-icon");

        Label msg = new Label("No playlists yet.\nClick \"＋ New Playlist\" to get started.");
        msg.getStyleClass().add("txt-grey-md");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(icon, msg);
        return box;
    }


    private TilePane buildPlaylistGrid(List<Playlist> playlists) {
        TilePane grid = new TilePane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPrefColumns(5);
        grid.setPadding(new Insets(4));

        for (Playlist p : playlists) {
            grid.getChildren().add(buildPlaylistCard(p));
        }
        return grid;
    }

    private VBox buildPlaylistCard(Playlist playlist) {
        // Artwork placeholder (coloured square)
        StackPane artBox = new StackPane();
        artBox.setPrefSize(130, 130);
        artBox.getStyleClass().add("playlist-card-art");

        Label artIcon = new Label("♪");
        artIcon.getStyleClass().add("album-art-placeholder");
        artBox.getChildren().add(artIcon);

        Label nameLbl = new Label(playlist.getName());
        nameLbl.getStyleClass().add("txt-white-sm-bld");
        nameLbl.setMaxWidth(130);
        nameLbl.setWrapText(false);

        int n = playlist.getSongs() != null ? playlist.getSongs().size() : 0;
        Label countLbl = new Label(n + " song" + (n == 1 ? "" : "s"));
        countLbl.getStyleClass().add("txt-grey-sm");

        VBox card = new VBox(8, artBox, nameLbl, countLbl);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("playlist-song-card");
        card.setPrefWidth(150);

        card.setOnMouseClicked(e -> controller.selectPlaylist(playlist));
        return card;
    }


    private VBox buildPlaylistList(List<Playlist> playlists) {
        VBox list = new VBox(4);
        list.setFillWidth(true);

        // Column headers
        Label nameH  = new Label("NAME");
        nameH.getStyleClass().add("txt-grey-md");
        HBox.setHgrow(nameH, Priority.ALWAYS);

        Label songsH = new Label("SONGS");
        songsH.setMinWidth(80);
        songsH.getStyleClass().add("txt-grey-md");

        HBox headers = new HBox(nameH, songsH);
        headers.setPadding(new Insets(4, 12, 4, 12));
        headers.getStyleClass().add("playlist-column-header");
        list.getChildren().add(headers);

        for (Playlist p : playlists) {
            list.getChildren().add(buildPlaylistListRow(p));
        }
        return list;
    }

    private HBox buildPlaylistListRow(Playlist playlist) {
        Rectangle icon = new Rectangle(32, 32);
        icon.setArcWidth(6);
        icon.setArcHeight(6);
        icon.getStyleClass().add("playlist-row-icon");

        Label nameLbl = new Label(playlist.getName());
        nameLbl.getStyleClass().add("txt-white-sm-bld");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        int n = playlist.getSongs() != null ? playlist.getSongs().size() : 0;
        Label countLbl = new Label(String.valueOf(n));
        countLbl.setMinWidth(80);
        countLbl.getStyleClass().add("playlist-row-count");

        HBox row = new HBox(12, icon, nameLbl, countLbl);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("playlist-row");

        row.setOnMouseClicked(e -> controller.selectPlaylist(playlist));
        return row;
    }

    /**
     * Renders the selected playlist's info, action buttons, and its songs.
     *
     * @param playlist    the currently selected playlist
     * @param displayMode "grid" | "list"
     */
    public VBox buildSongPanel(Playlist playlist, String displayMode) {
        VBox panel = new VBox(16);
        panel.setFillWidth(true);
        VBox.setVgrow(panel, Priority.ALWAYS);

        panel.getChildren().addAll(
                buildPlaylistInfoRow(playlist),
                buildActionRow(playlist),
                buildSongArea(playlist, displayMode)
        );

        return panel;
    }

    //Playlist info (art + name + description + count)

    private HBox buildPlaylistInfoRow(Playlist playlist) {
        StackPane artBox = new StackPane();
        artBox.setPrefSize(72, 72);
        artBox.getStyleClass().add("playlist-card-art");

        Label artIcon = new Label("♪");
        artIcon.getStyleClass().add("album-art-placeholder");
        artBox.getChildren().add(artIcon);

        Label typeTag = new Label("PLAYLIST");
        typeTag.getStyleClass().add("txt-grey-sm-bld");

        Label nameLbl = new Label(playlist.getName());
        nameLbl.getStyleClass().add("txt-white-bld-thirty");

        VBox info = new VBox(4, typeTag, nameLbl);

        if (playlist.getDescription() != null && !playlist.getDescription().isBlank()) {
            Label desc = new Label(playlist.getDescription());
            desc.getStyleClass().add("txt-grey-md");
            desc.setWrapText(true);
            info.getChildren().add(desc);
        }

        int n = playlist.getSongs() != null ? playlist.getSongs().size() : 0;
        Label countLbl = new Label(n + " song" + (n == 1 ? "" : "s"));
        countLbl.getStyleClass().add("txt-grey-md");
        info.getChildren().add(countLbl);

        HBox row = new HBox(16, artBox, info);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildActionRow(Playlist playlist) {
        int songCount = playlist.getSongs() != null ? playlist.getSongs().size() : 0;

        Button playAllBtn = new Button("▶  Play All");
        playAllBtn.getStyleClass().add("green-btn");
        playAllBtn.setDisable(songCount == 0);
        playAllBtn.setOnAction(e -> playAll(playlist));

        Button renameBtn = new Button("Rename");
        renameBtn.getStyleClass().add("playlist-rename-btn");
        renameBtn.setOnAction(e ->
                PlaylistDialogHandler.showRenameDialog(playlist, playlistService, controller));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("playlist-delete-btn");
        deleteBtn.setOnAction(e ->
                PlaylistDialogHandler.showDeleteDialog(playlist, playlistService, controller));

        HBox row = new HBox(10, playAllBtn, renameBtn, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Separator sep = new Separator();
        sep.getStyleClass().add("playlist-header-separator");

        VBox wrapper = new VBox(8, row, sep);
        HBox outer = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return outer;
    }

    private VBox buildSongArea(Playlist playlist, String displayMode) {
        VBox container = new VBox(4);
        VBox.setVgrow(container, Priority.ALWAYS);

        List<Song> songs = playlistService.getPlaylistSongs(playlist.getPlaylistId());

        if (songs.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));

            Label emIcon = new Label("🎶");
            emIcon.getStyleClass().add("playlist-empty-songs-icon");

            Label emMsg = new Label("This playlist is empty.\nAdd songs from My Library.");
            emMsg.getStyleClass().add("txt-grey-md");
            emMsg.setWrapText(true);
            emMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            empty.getChildren().addAll(emIcon, emMsg);
            container.getChildren().add(empty);
            return container;
        }

        if ("grid".equals(displayMode)) {
            container.getChildren().add(buildSongGrid(songs, playlist));
        } else {
            container.getChildren().add(buildColumnHeaders());
            for (int i = 0; i < songs.size(); i++) {
                container.getChildren().add(buildSongRow(i + 1, songs.get(i), playlist));
            }
        }
        return container;
    }

    private HBox buildColumnHeaders() {
        Label hashH = new Label("#");
        hashH.setMinWidth(32);
        hashH.getStyleClass().add("txt-grey-md");

        Label titleH = new Label("TITLE");
        titleH.getStyleClass().add("txt-grey-md");
        HBox.setHgrow(titleH, Priority.ALWAYS);

        Label durH = new Label("DURATION");
        durH.setMinWidth(80);
        durH.getStyleClass().add("txt-grey-md");

        HBox headers = new HBox(hashH, titleH, durH);
        headers.setPadding(new Insets(4, 12, 4, 12));
        headers.getStyleClass().add("playlist-column-header");
        return headers;
    }

    private HBox buildSongRow(int index, Song song, Playlist playlist) {
        RowConfig cfg = RowConfig.playlistSongRow();

        Label indexLbl = new Label(String.valueOf(index));
        indexLbl.setMinWidth(32);
        indexLbl.getStyleClass().add(cfg.getMetaLabelStyleClass());

        Label titleLbl = new Label(song.getTitle() != null ? song.getTitle() : "Unknown Title");
        titleLbl.getStyleClass().add(cfg.getPrimaryLabelStyleClass());

        String artistName = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName() : "Unknown Artist";
        Label artistLbl = new Label(artistName);
        artistLbl.getStyleClass().add(cfg.getSecondaryLabelStyleClass());

        VBox info = new VBox(2, titleLbl, artistLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label durLbl = new Label(AppUtils.formatDuration(song.getDuration()));
        durLbl.setMinWidth(cfg.getMetaLabelMinWidth());
        durLbl.getStyleClass().add(cfg.getMetaLabelStyleClass());

        HBox row = new HBox(12, indexLbl, info, durLbl);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setAlignment(cfg.getAlignment());
        row.getStyleClass().add(cfg.getRowStyleClass());

        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) musicPlayerService.playSong(song);
        });
        row.setOnContextMenuRequested(e ->
                buildSongContextMenu(song, playlist).show(row, e.getScreenX(), e.getScreenY()));

        return row;
    }

    private ScrollPane buildSongGrid(List<Song> songs, Playlist playlist) {
        TilePane grid = new TilePane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPrefColumns(5);
        grid.setPadding(new Insets(4));

        for (Song song : songs) {
            grid.getChildren().add(buildSongCard(song, playlist));
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private VBox buildSongCard(Song song, Playlist playlist) {
        StackPane artBox = new StackPane();
        artBox.setPrefSize(130, 130);
        artBox.getStyleClass().add("playlist-card-art");

        if (song.getAlbum() != null && song.getAlbum().getAlbumArtPath() != null) {
            try {
                ImageView iv = new ImageView(
                        new Image("file:" + song.getAlbum().getAlbumArtPath(), true));
                iv.setFitWidth(130);
                iv.setFitHeight(130);
                iv.setPreserveRatio(true);
                artBox.getChildren().add(iv);
            } catch (Exception ignored) {
                artBox.getChildren().add(artPlaceholder());
            }
        } else {
            artBox.getChildren().add(artPlaceholder());
        }

        Label titleLbl = new Label(song.getTitle() != null ? song.getTitle() : "Unknown");
        titleLbl.getStyleClass().add("txt-white-sm");
        titleLbl.setMaxWidth(130);

        String artistName = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName() : "Unknown Artist";
        Label artistLbl = new Label(artistName);
        artistLbl.getStyleClass().add("txt-grey-sm");
        artistLbl.setMaxWidth(130);

        Label durLbl = new Label(AppUtils.formatDuration(song.getDuration()));
        durLbl.getStyleClass().add("txt-grey-sm");

        VBox card = new VBox(6, artBox, titleLbl, artistLbl, durLbl);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("playlist-song-card");
        card.setPrefWidth(150);

        card.setOnMouseClicked(e -> musicPlayerService.playSong(song));
        card.setOnContextMenuRequested(e ->
                buildSongContextMenu(song, playlist).show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private Label artPlaceholder() {
        Label lbl = new Label("♪");
        lbl.getStyleClass().add("album-art-placeholder");
        return lbl;
    }

    //Context menu shared by list and grid song views

    private ContextMenu buildSongContextMenu(Song song, Playlist playlist) {
        MenuItem playItem = new MenuItem("▶  Play");
        playItem.setOnAction(e -> musicPlayerService.playSong(song));

        MenuItem removeItem = new MenuItem("✕  Remove from Playlist");
        removeItem.setOnAction(e -> {
            try {
                playlistService.removeSongFromPlaylist(playlist.getPlaylistId(), song);
                controller.selectPlaylist(playlist);
            } catch (Exception ex) {
                AppUtils.showError("Could not remove song: " + ex.getMessage());
            }
        });

        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(playItem, new SeparatorMenuItem(), removeItem);
        return menu;
    }

    private void playAll(Playlist playlist) {
        List<Song> songs = playlistService.getPlaylistSongs(playlist.getPlaylistId());
        if (!songs.isEmpty()) musicPlayerService.playSong(songs.get(0));
    }
}