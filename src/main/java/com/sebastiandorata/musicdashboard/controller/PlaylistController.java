package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.presentation.shared.SidebarBuilder;
import com.sebastiandorata.musicdashboard.presentation.playlist.PlaylistViewBuilder;
import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sebastiandorata.musicdashboard.utils.AppUtils.APP_WIDTH;

/**
 * Top-level controller for the Playlist page.
 * Owns the only mutable view-state: selectedPlaylist and displayMode.
 * Delegates all node construction to PlaylistViewBuilder.
 * Delegates all dialogs to PlaylistDialogHandler (via PlaylistViewBuilder).
 */
@Component
public class PlaylistController {

    @Autowired private PlaylistService    playlistService;
    @Lazy @Autowired private MusicPlayerService musicPlayerService;


    private Playlist selectedPlaylist = null;
    private String displayMode = "grid";


    private VBox sidebarRoot;
    private HBox headerBar;
    private VBox contentArea;
    private ScrollPane contentScroll;

    private PlaylistViewBuilder viewBuilder;

    @PostConstruct
    public void register() {
        MainController.registerPlaylist(this);
        viewBuilder = new PlaylistViewBuilder(playlistService, musicPlayerService, this);
    }

    public void show() {
        Scene scene = this.createScene();
        selectedPlaylist = null;
        displayMode      = "grid";

        try {
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/playlist.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }

        MainController.switchViews(scene);
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dark-page-bg");

        sidebarRoot = buildSidebar();
        root.setLeft(sidebarRoot);
        root.setCenter(buildCenter());

        return new Scene(root, APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    /**
     * Sidebar holds only the "+ New Playlist" button via SidebarBuilder.
     * Playlist rows are displayed in the center content area instead.
     */
    private VBox buildSidebar() {
        VBox newPlaylistSection = viewBuilder.buildSidebarNewButton();

        return SidebarBuilder.build(
                List.of("panels", "playlist-sidebar"),
                "My Playlists",
                true,
                List.of(),
                null,
                false,
                newPlaylistSection,
                "Return home",
                "← Dashboard",
                () -> MainController.navigateTo("dashboard")
        );
    }

    /**
     * Fixed header bar on top, then a separator, then a scrollable content area.
     * Both live nodes are kept as fields so refreshView() patches them in-place.
     */
    private VBox buildCenter() {
        headerBar    = buildHeaderBar();
        contentArea  = buildContentArea();

        contentScroll = new ScrollPane(contentArea);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        Separator sep = new Separator();
        sep.getStyleClass().add("playlist-header-separator");

        VBox center = new VBox(headerBar, sep, contentScroll);
        VBox.setVgrow(contentScroll, Priority.ALWAYS);
        center.setFillWidth(true);
        return center;
    }


    private HBox buildHeaderBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 20, 16, 20));
        bar.getStyleClass().add("playlist-header");

        if (selectedPlaylist == null) {
            Label title = new Label("View All Playlists");
            title.getStyleClass().add("txt-white-md-bld");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            bar.getChildren().addAll(title, spacer, buildToggleBtn("☰  List", "list"),
                    buildToggleBtn("⊞  Grid", "grid"));
        } else {
            Button backBtn = new Button("← Back");
            backBtn.getStyleClass().add("nav-btn-back");
            backBtn.setOnAction(e -> clearSelection());

            Label title = new Label(selectedPlaylist.getName());
            title.getStyleClass().add("txt-white-md-bld");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            bar.getChildren().addAll(backBtn, title, spacer,
                    buildToggleBtn("☰  List", "list"),
                    buildToggleBtn("⊞  Grid", "grid"));
        }

        // Mark the active toggle
        bar.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .filter(b -> b.getText().contains(displayMode.equals("list") ? "List" : "Grid"))
                .findFirst()
                .ifPresent(b -> b.getStyleClass().add("nav-btn-active"));

        return bar;
    }

    private Button buildToggleBtn(String label, String mode) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-btn");
        btn.setOnAction(e -> setDisplayMode(mode));
        return btn;
    }



    /**
     * No-selection state shows playlist browser (grid or list of all playlists).
     * Selected state shows song panel for the selected playlist.
     */
    private VBox buildContentArea() {
        VBox area = new VBox();
        area.setFillWidth(true);
        area.setPadding(new Insets(20));
        VBox.setVgrow(area, Priority.ALWAYS);

        if (selectedPlaylist == null) {
            List<Playlist> playlists = loadPlaylists();
            area.getChildren().add(viewBuilder.buildPlaylistBrowser(playlists, displayMode));
        } else {
            area.getChildren().add(viewBuilder.buildSongPanel(selectedPlaylist, displayMode));
        }

        return area;
    }


    /** Selects a playlist; header and content both update. */
    public void selectPlaylist(Playlist playlist) {
        this.selectedPlaylist = playlist;
        refreshView();
    }

    /** Clears selection; returns to the full playlist browser. */
    public void clearSelection() {
        this.selectedPlaylist = null;
        refreshView();
    }

    /** Re-fetches the selected playlist after a rename, then refreshes. */
    public void reloadSelected(Long playlistId) {
        playlistService.getPlaylistById(playlistId)
                .ifPresent(updated -> this.selectedPlaylist = updated);
        refreshView();
    }

    /** Switches display mode and refreshes. */
    public void setDisplayMode(String mode) {
        this.displayMode = mode;
        refreshView();
    }

    private void refreshView() {
        headerBar.getChildren().setAll(buildHeaderBar().getChildren());
        contentArea.getChildren().setAll(buildContentArea().getChildren());
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