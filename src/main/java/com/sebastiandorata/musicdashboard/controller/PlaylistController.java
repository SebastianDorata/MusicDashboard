package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class PlaylistController {

    @Autowired        private PlaylistService    playlistService;
    @Lazy @Autowired  private MusicPlayerService musicPlayerService;
    @Autowired        private FavouriteService   favouriteService;


    private Playlist selectedPlaylist = null;

    private VBox playlistSidebar;
    private VBox detailPanel;

    private record PlaylistFormResult(String name, String description) {}

    @PostConstruct
    public void register() {
        MainController.registerPlaylist(this);
    }



    public void show() {
        selectedPlaylist = null;
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/playlist.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }
        MainController.switchViews(scene);
    }



    private Scene createScene() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("dark-page-bg");
        mainLayout.setTop(createTopBar());
        mainLayout.setCenter(createBody());
        return new Scene(mainLayout, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    private HBox createTopBar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(20, 20, 10, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("main-bkColour");

        Button backBtn = new Button("Home");
        backBtn.getStyleClass().add("btn-blue");
        backBtn.setOnAction(e -> MainController.navigateTo("dashboard"));

        Label title = new Label("My Playlists");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBtn = new Button("+ New Playlist");
        newBtn.getStyleClass().add("btn-blue");
        newBtn.setOnAction(e -> showCreatePlaylistDialog());

        bar.getChildren().addAll(backBtn, title, spacer, newBtn);
        return bar;
    }

    private HBox createBody() {
        HBox body = new HBox();
        body.getStyleClass().add("dark-page-bg");

        playlistSidebar = buildSidebar();
        detailPanel     = buildDetailPanel();

        ScrollPane sideScroll = new ScrollPane(playlistSidebar);
        sideScroll.setFitToWidth(true);
        sideScroll.setPrefWidth(280);
        sideScroll.setMinWidth(240);
        sideScroll.getStyleClass().add("playlist-sidebar-scroll");

        ScrollPane detailScroll = new ScrollPane(detailPanel);
        detailScroll.setFitToWidth(true);
        HBox.setHgrow(detailScroll, Priority.ALWAYS);
        detailScroll.getStyleClass().add("playlist-detail-scroll");

        body.getChildren().addAll(sideScroll, detailScroll);
        return body;
    }



    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(16));
        sidebar.getStyleClass().add("playlist-sidebar");

        Label heading = new Label("Playlists");
        heading.getStyleClass().add("playlist-sidebar-heading");
        VBox.setMargin(heading, new Insets(0, 0, 8, 4));
        sidebar.getChildren().add(heading);

        List<Playlist> playlists = loadPlaylists();
        if (playlists.isEmpty()) {
            Label msg = new Label("No playlists yet.\nClick \"+ New Playlist\" to start.");
            msg.getStyleClass().add("playlist-sidebar-empty");
            msg.setWrapText(true);
            VBox.setMargin(msg, new Insets(20, 4, 0, 4));
            sidebar.getChildren().add(msg);
        } else {
            for (Playlist p : playlists) {
                sidebar.getChildren().add(buildPlaylistRow(p));
            }
        }
        return sidebar;
    }

    private HBox buildPlaylistRow(Playlist playlist) {
        boolean selected = selectedPlaylist != null
                && selectedPlaylist.getPlaylistId().equals(playlist.getPlaylistId());

        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("playlist-row");
        if (selected) row.getStyleClass().add("playlist-row-selected");

        Rectangle icon = new Rectangle(32, 32);
        icon.setArcWidth(6);
        icon.setArcHeight(6);


        VBox text = new VBox(2);
        Label name = new Label(playlist.getName());
        name.getStyleClass().add("wt-smmd-bld");
        name.setMaxWidth(160);

        int n = playlist.getSongs() != null ? playlist.getSongs().size() : 0;
        Label count = new Label(n + " song" + (n == 1 ? "" : "s"));
        count.getStyleClass().add("txt-grey-sm");
        text.getChildren().addAll(name, count);
        HBox.setHgrow(text, Priority.ALWAYS);

        row.getChildren().addAll(icon, text);
        row.setOnMouseClicked(e -> {
            selectedPlaylist = playlist;
            refreshView();
        });
        return row;
    }



    private VBox buildDetailPanel() {
        VBox panel = new VBox();
        panel.setPadding(new Insets(24));
        panel.getStyleClass().add("dark-page-bg");

        if (selectedPlaylist == null) {
            panel.getChildren().add(buildEmptyState());
        } else {
            panel.getChildren().addAll(
                    buildPlaylistHeader(selectedPlaylist),
                    buildSongList(selectedPlaylist)
            );
        }
        return panel;
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(80));

        Label icon = new Label("🎵");
        icon.getStyleClass().add("playlist-empty-icon");

        Label msg = new Label("Select a playlist to view its songs");
        msg.getStyleClass().add("playlist-empty-msg");

        box.getChildren().addAll(icon, msg);
        return box;
    }

    private VBox buildPlaylistHeader(Playlist playlist) {
        VBox header = new VBox(12);
        header.setPadding(new Insets(0, 0, 20, 0));

        HBox nameRow = new HBox(16);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Rectangle icon = new Rectangle(64, 64);
        icon.setArcWidth(10);
        icon.setArcHeight(10);

        VBox info = new VBox(4);

        Label typeTag = new Label("PLAYLIST");
        typeTag.getStyleClass().add("playlist-type-tag");

        Label nameLabel = new Label(playlist.getName());
        nameLabel.getStyleClass().add("playlist-detail-name");

        info.getChildren().addAll(typeTag, nameLabel);

        if (playlist.getDescription() != null && !playlist.getDescription().isBlank()) {
            Label desc = new Label(playlist.getDescription());
            desc.getStyleClass().add("playlist-description-label");
            desc.setWrapText(true);
            info.getChildren().add(desc);
        }

        int songCount = playlist.getSongs() != null ? playlist.getSongs().size() : 0;
        Label countLabel = new Label(songCount + " song" + (songCount == 1 ? "" : "s"));
        countLabel.getStyleClass().add("playlist-song-count");
        info.getChildren().add(countLabel);

        nameRow.getChildren().addAll(icon, info);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button playAllBtn = new Button("▶  Play All");
        playAllBtn.getStyleClass().add("btn-blue");
        playAllBtn.setDisable(songCount == 0);
        playAllBtn.setOnAction(e -> playAll(playlist));

        Button renameBtn = new Button("Rename");
        renameBtn.getStyleClass().add("playlist-rename-btn");
        renameBtn.setOnAction(e -> showRenameDialog(playlist));

        Button deleteBtn = new Button("Delete Playlist");
        deleteBtn.getStyleClass().add("playlist-delete-btn");
        deleteBtn.setOnAction(e -> showDeleteDialog(playlist));

        actions.getChildren().addAll(playAllBtn, renameBtn, deleteBtn);

        Separator sep = new Separator();
        sep.getStyleClass().add("playlist-header-separator");

        header.getChildren().addAll(nameRow, actions, sep);
        return header;
    }

    private VBox buildSongList(Playlist playlist) {
        VBox container = new VBox(4);
        List<Song> songs = playlistService.getPlaylistSongs(playlist.getPlaylistId());

        if (songs.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));

            Label emIcon = new Label("🎶");
            emIcon.getStyleClass().add("playlist-empty-songs-icon");

            Label emMsg = new Label("This playlist is empty.\nAdd songs from My Library.");
            emMsg.getStyleClass().add("playlist-empty-songs-msg");
            emMsg.setWrapText(true);
            emMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            empty.getChildren().addAll(emIcon, emMsg);
            container.getChildren().add(empty);
            return container;
        }

        HBox headers = new HBox();
        headers.setPadding(new Insets(4, 12, 4, 12));
        headers.getStyleClass().add("playlist-column-header");

        Label hashH = new Label("#");
        hashH.setMinWidth(32);
        hashH.getStyleClass().add("playlist-column-label");

        Label titleH = new Label("TITLE");
        titleH.getStyleClass().add("playlist-column-label");
        HBox.setHgrow(titleH, Priority.ALWAYS);

        Label durH = new Label("DURATION");
        durH.setMinWidth(80);
        durH.getStyleClass().add("playlist-column-label");

        headers.getChildren().addAll(hashH, titleH, durH);
        container.getChildren().add(headers);

        for (int i = 0; i < songs.size(); i++) {
            container.getChildren().add(buildSongRow(i + 1, songs.get(i), playlist));
        }
        return container;
    }

    private HBox buildSongRow(int index, Song song, Playlist playlist) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("playlist-song-row");

        Label indexLbl = new Label(String.valueOf(index));
        indexLbl.setMinWidth(32);
        indexLbl.getStyleClass().add("playlist-song-index");

        VBox info = new VBox(2);
        Label titleLbl = new Label(song.getTitle() != null ? song.getTitle() : "Unknown Title");
        titleLbl.getStyleClass().add("playlist-song-title");

        String artist = (song.getArtists() != null && !song.getArtists().isEmpty())
                ? song.getArtists().get(0).getName() : "Unknown Artist";
        Label artistLbl = new Label(artist);
        artistLbl.getStyleClass().add("playlist-song-artist");

        info.getChildren().addAll(titleLbl, artistLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label durLbl = new Label(AppUtils.formatDuration(song.getDuration()));
        durLbl.setMinWidth(80);
        durLbl.getStyleClass().add("playlist-song-duration");

        row.getChildren().addAll(indexLbl, info, durLbl);

        row.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) musicPlayerService.playSong(song);
        });

        ContextMenu ctx = new ContextMenu();

        MenuItem playItem = new MenuItem("▶  Play");
        playItem.setOnAction(e -> musicPlayerService.playSong(song));

        MenuItem favItem = buildFavouriteMenuItem(song);

        MenuItem removeItem = new MenuItem("✕  Remove from Playlist");
        removeItem.setOnAction(e -> {
            try {
                playlistService.removeSongFromPlaylist(playlist.getPlaylistId(), song);
                refreshView();
            } catch (Exception ex) {
                AppUtils.showError("Could not remove song: " + ex.getMessage());
            }
        });

        ctx.getItems().addAll(playItem, favItem, new SeparatorMenuItem(), removeItem);
        row.setOnContextMenuRequested(e -> ctx.show(row, e.getScreenX(), e.getScreenY()));
        return row;
    }

    private MenuItem buildFavouriteMenuItem(Song song) {
        boolean isFav;
        try {
            isFav = favouriteService.isFavourited(song);
        } catch (Exception e) {
            isFav = false;
        }
        MenuItem item = new MenuItem(isFav ? "♥  Remove from Favourites" : "♡  Add to Favourites");
        item.setOnAction(e -> {
            try {
                favouriteService.toggleFavourite(song);
            } catch (Exception ex) {
                AppUtils.showError("Could not update favourites: " + ex.getMessage());
            }
        });
        return item;
    }



    private Optional<PlaylistFormResult> showPlaylistFormDialog(
            String dialogTitle, String headerText, String initialName, String initialDesc) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(dialogTitle);
        dialog.setHeaderText(headerText);
        AppUtils.styleDialog(dialog);

        GridPane grid = AppUtils.buildDialogGrid();

        TextField nameField = new TextField(initialName != null ? initialName : "");
        nameField.setPromptText("Playlist name");
        nameField.setPrefWidth(260);

        TextField descField = new TextField(initialDesc != null ? initialDesc : "");
        descField.setPromptText("Description (optional)");
        descField.setPrefWidth(260);

        grid.add(new Label("Name:"),        0, 0);
        grid.add(nameField,                 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField,                 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((o, old, val) -> okBtn.setDisable(val.trim().isEmpty()));
        Platform.runLater(nameField::requestFocus);

        return dialog.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .map(r -> new PlaylistFormResult(nameField.getText().trim(), descField.getText().trim()));
    }

    private void showCreatePlaylistDialog() {
        showPlaylistFormDialog("New Playlist", "Create a new playlist", null, null)
                .ifPresent(result -> {
                    try {
                        Playlist created = playlistService.createPlaylist(result.name(), result.description());
                        selectedPlaylist = created;
                        refreshView();
                    } catch (Exception e) {
                        AppUtils.showError("Could not create playlist: " + e.getMessage());
                    }
                });
    }

    private void showRenameDialog(Playlist playlist) {
        showPlaylistFormDialog(
                "Rename Playlist",
                "Rename \"" + playlist.getName() + "\"",
                playlist.getName(),
                playlist.getDescription()
        ).ifPresent(result -> {
            try {
                playlistService.updatePlaylist(
                        playlist.getPlaylistId(),
                        result.name(), result.description(), null);
                playlistService.getPlaylistById(playlist.getPlaylistId())
                        .ifPresent(updated -> selectedPlaylist = updated);
                refreshView();
            } catch (Exception e) {
                AppUtils.showError("Could not rename playlist: " + e.getMessage());
            }
        });
    }

    private void showDeleteDialog(Playlist playlist) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Playlist");
        alert.setHeaderText("Delete \"" + playlist.getName() + "\"?");
        alert.setContentText("This permanently deletes the playlist. Songs remain in your library.");
        alert.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                playlistService.deletePlaylist(playlist.getPlaylistId());
                selectedPlaylist = null;
                refreshView();
            } catch (Exception e) {
                AppUtils.showError("Could not delete playlist: " + e.getMessage());
            }
        });
    }


    private void refreshView() {
        playlistSidebar.getChildren().setAll(buildSidebar().getChildren());
        detailPanel.getChildren().setAll(buildDetailPanel().getChildren());
    }

    private void playAll(Playlist playlist) {
        List<Song> songs = playlistService.getPlaylistSongs(playlist.getPlaylistId());
        if (!songs.isEmpty()) musicPlayerService.playSong(songs.get(0));
    }

    private List<Playlist> loadPlaylists() {
        try {
            return playlistService.getCurrentUserPlaylists();
        } catch (Exception e) {
            System.err.println("Could not load playlists: " + e.getMessage());
            return List.of();
        }
    }

}