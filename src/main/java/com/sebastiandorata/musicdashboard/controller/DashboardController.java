package com.sebastiandorata.musicdashboard.controller;


import com.sebastiandorata.musicdashboard.presentation.ArtistDiscographyNavigation;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.TopArtistsController;
import com.sebastiandorata.musicdashboard.presentation.graph.DashboardGraphController;
import com.sebastiandorata.musicdashboard.presentation.helpers.PlayerConfig;
import com.sebastiandorata.musicdashboard.presentation.helpers.SidebarBuilder;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.SongImportService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * Top-level controller for the Dashboard page.
 *
 * <p>Assembles a three-column {@link BorderPane} layout:
 * a left sidebar with navigation and account actions, a center area containing
 * the playback panel, stat cards, and line chart, and a right panel with the
 * Top 5 Artists and Recently Played lists. Artist drill-in navigation is
 * centralized through {@link ArtistDiscographyNavigation}.</p>
 */
@Component
public class DashboardController {

    @Autowired private com.sebastiandorata.musicdashboard.presentation.Dashboard.PlaybackPanelController playbackPanelController;
    @Autowired private com.sebastiandorata.musicdashboard.presentation.Dashboard.RecentlyPlayedController recentlyPlayedController;
    @Autowired private TopArtistsController topArtistsController;
    @Autowired private DashboardGraphController dashboardGraphController;
    @Autowired private CardFactory cardFactory;
    @Autowired private ArtistDiscographyNavigation artistDiscographyNavigation;

    @Lazy @Autowired private MusicPlayerService musicPlayerService;
    @Setter @Getter @Lazy @Autowired private SongImportService songImportService;
    @Setter @Getter @Lazy @Autowired private PlaybackTrackingService playbackTrackingService;
    @Autowired private UserSessionService userSessionService;
    @Lazy @Autowired private MyLibraryController myLibraryController;

    private String activeRoute = "dashboard";

    @PostConstruct
    public void register() {
        MainController.registerDashboard(this);
    }

    public void show() {
        Scene scene = this.createScene();

        try {
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/analytics.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/reports.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/graph.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }

        MainController.switchViews(scene);
    }


    private Scene createScene() {
        BorderPane root = new BorderPane();

        VBox left = createLeftMenu();
        VBox center = createCenterMenu();
        VBox right = createRightMenu();

        center.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        root.setLeft(left);
        root.setCenter(center);
        root.setRight(right);
        //BorderPane.setVgrow(right, Priority.ALWAYS);

        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }




    private VBox createLeftMenu() {

        var entries = List.of(
                new SidebarBuilder.NavEntry("♫", "My Library",  "library",   () -> { activeRoute = "library";   MainController.navigateTo("library");   }),
                new SidebarBuilder.NavEntry("≡", "My Playlist", "playlist",  () -> { activeRoute = "playlist";  MainController.navigateTo("playlist");  }),
                new SidebarBuilder.NavEntry("↓", "Import Files","import",    () -> { activeRoute = "import";    MainController.navigateTo("import");    }),
                new SidebarBuilder.NavEntry("◫", "My Reports",  "analytics", () -> { activeRoute = "analytics"; MainController.navigateTo("analytics"); })
        );

        VBox sidebar = SidebarBuilder.build(
                List.of("panels", "sidebar"),
                "My Dashboard",
                true,
                entries,
                null,
                true,
                null,
                null,
                null,
                null
        );

        Separator lblSeperator = new Separator();
        lblSeperator.getStyleClass().add("sidebar-sep");

        Label accountLabel = new Label("My Account");
        accountLabel.getStyleClass().add("sidebar-section-label");

        Button linkAccountBtn = SidebarBuilder.buildNavButton("⊕", "Link Account");
        Button settingsBtn    = SidebarBuilder.buildNavButton("⚙", "Settings");
        Button exportBtn      = SidebarBuilder.buildNavButton("↑", "Export Data");

        linkAccountBtn.setOnAction(e -> { /* TODO */ });//These are just placeholders for the presentation.
        settingsBtn   .setOnAction(e -> { /* TODO */ });
        exportBtn     .setOnAction(e -> { /* TODO */ });


        int spacerIndex = sidebar.getChildren().size();// Inserting before the spacer lets the account section sit directly below the nav entries.
        sidebar.getChildren().addAll(spacerIndex,
                List.of(lblSeperator, accountLabel, linkAccountBtn, settingsBtn, exportBtn));

        return sidebar;
    }





    private VBox createCenterMenu() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(20));
        center.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        center.setFillWidth(true);

        // Artist click navigates to My Library showing that artist's discography
        HBox playbackPanel = playbackPanelController.createPanel(
                artistDiscographyNavigation.getArtistDrillInCallback(),
                PlayerConfig.PlayerSize.LARGE
        );

        // Create stat cards with navigation behavior:
        // Single-click on any album or song name: Navigates to album view
        // Double-click on song names: Plays the song immediately
        HBox cards = cardFactory.createStatCards(
                album -> {
                    // Single-click: Navigate to album drill-down view
                    myLibraryController.showWithAlbum(album);
                },
                song -> {
                    // Double-click: Play the song immediately
                    musicPlayerService.playSong(song);
                }
        );

        HBox graphPanel = dashboardGraphController.createPanel();

        playbackPanel.setMaxWidth(Double.MAX_VALUE);
        cards.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cards, Priority.ALWAYS);

        graphPanel.setMaxWidth(Double.MAX_VALUE);
        graphPanel.setMinHeight(200);

        VBox.setVgrow(graphPanel, Priority.ALWAYS);
        graphPanel.setMaxHeight(Double.MAX_VALUE);

        center.getChildren().addAll(playbackPanel, cards, graphPanel);

        return center;
    }

    private VBox createRightMenu() {
        VBox right = new VBox(10);
        right.setStyle("-fx-padding: 10px");
        right.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        right.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
        right.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(right, Priority.ALWAYS);

        // Artist click navigates to My Library showing that artist's discography
        VBox topArtistsPanel = topArtistsController.createPanel(artistDiscographyNavigation.getArtistDrillInCallback());
        VBox recentlyPlayedPanel = recentlyPlayedController.createPanel();

        right.getChildren().addAll(topArtistsPanel, recentlyPlayedPanel);
        return right;
    }
}