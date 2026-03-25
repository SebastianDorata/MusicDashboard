package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.controllerUtils.PlaybackPanelController;
import com.sebastiandorata.musicdashboard.controllerUtils.RecentlyPlayedController;
import com.sebastiandorata.musicdashboard.controllerUtils.StatsChartController;
import com.sebastiandorata.musicdashboard.controllerUtils.TopArtistsController;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.SongService;
import com.sebastiandorata.musicdashboard.service.UserSessionService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.CardFactory;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Main Dashboard Orchestrator
 *
 * Responsibility: Assemble UI from component controllers
 * Does NOT: Implement individual features (those are in component controllers)
 *
 * Benefits:
 * - Clean separation of concerns
 * - Each component is independently testable
 * - Easy to add/remove/modify features
 *
 * Dashboard Layout:
 * - Left: Navigation menu
 * - Center: Now Playing bar + Stat cards (5 cards) + Graph section
 * - Right: Top Artists + Recently Played
 */
@Component
public class DashboardController {

    // Injected component controllers
    @Autowired
    private PlaybackPanelController playbackPanelController;

    @Autowired
    private RecentlyPlayedController recentlyPlayedController;

    @Autowired
    private TopArtistsController topArtistsController;

    @Autowired
    private StatsChartController statsChartController;

    @Autowired
    private CardFactory cardFactory;

    @Lazy
    @Autowired
    private MusicPlayerService musicPlayerService;

    @Setter
    @Getter
    @Lazy
    @Autowired
    private SongService songService;

    @Setter
    @Getter
    @Lazy
    @Autowired
    private PlaybackTrackingService playbackTrackingService;

    @Autowired
    private UserSessionService userSessionService;

    @PostConstruct
    public void register() {
        MainController.registerDashboard(this);
    }

    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/dashboard.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/graph.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }

        MainController.switchViews(scene);
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
            root.setLeft(createLeftMenu());
            root.setCenter(createCenterMenu());
            root.setRight(createRightMenu());
        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }


    private VBox createLeftMenu() {
        VBox left = new VBox(20);
            left.setPadding(new Insets(20));
            left.setPrefWidth(AppUtils.APP_WIDTH * 0.25);
            left.getStyleClass().add("green-background");

        javafx.scene.control.Button libraryBtn = createLeftButton("My Library");
        javafx.scene.control.Button playlistBtn = createLeftButton("My Playlist");
        javafx.scene.control.Button importBtn = createLeftButton("Import Files");
        javafx.scene.control.Button reportsBtn = createLeftButton("My Reports");

        libraryBtn.setOnAction(e -> MainController.navigateTo("library"));
        playlistBtn.setOnAction(e -> MainController.navigateTo("playlist"));
        importBtn.setOnAction(e -> MainController.navigateTo("import"));
        reportsBtn.setOnAction(e -> MainController.navigateTo("analytics"));

        left.getChildren().addAll(
                wrap(libraryBtn), wrap(playlistBtn), wrap(importBtn), wrap(reportsBtn)
        );
        return left;
    }

    private HBox wrap(Button btn) {
        return new HBox(btn);
    }

    private Button createLeftButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("Left-Btn");
        return btn;
    }


    private VBox createCenterMenu() {
        VBox center = new VBox(20);
            center.setPadding(new Insets(20));
            center.setPrefWidth(AppUtils.APP_WIDTH * 0.5);


            HBox playbackPanel = playbackPanelController.createPanel();
            HBox cards = cardFactory.createStatCards();
            VBox graphSection = statsChartController.createGraphSection();

        center.getChildren().addAll(playbackPanel, cards, graphSection);

        return center;
    }


    private VBox createRightMenu() {
        VBox right = new VBox(25);
            right.setPadding(new Insets(20));
            right.setPrefWidth(AppUtils.APP_WIDTH * 0.25);


        VBox topArtistsPanel = topArtistsController.createPanel();
        VBox recentlyPlayedPanel = recentlyPlayedController.createPanel();

        right.getChildren().addAll(topArtistsPanel, recentlyPlayedPanel);
        return right;
    }

}