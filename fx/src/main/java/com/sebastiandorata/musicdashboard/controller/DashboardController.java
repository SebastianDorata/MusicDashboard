package com.sebastiandorata.musicdashboard.controller;


import com.sebastiandorata.musicdashboard.presentation.ArtistDiscographyNavigation;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.PlaybackPanelController;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.RecentlyPlayedController;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.TopArtistsController;
import com.sebastiandorata.musicdashboard.presentation.graph.DashboardGraphController;
import com.sebastiandorata.musicdashboard.presentation.helpers.PlayerConfig;
import com.sebastiandorata.musicdashboard.presentation.shared.CardFactory;
import com.sebastiandorata.musicdashboard.presentation.shared.AppSidebar;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.PlaybackTrackingService;
import com.sebastiandorata.musicdashboard.service.Import.SongImportService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
    @Lazy   @Autowired private MusicPlayerService musicPlayerService;
    @Lazy   @Autowired private MyLibraryController myLibraryController;
            @Autowired private PlaybackPanelController playbackPanelController;
            @Autowired private RecentlyPlayedController recentlyPlayedController;
            @Autowired private TopArtistsController topArtistsController;
            @Autowired private DashboardGraphController dashboardGraphController;
            @Autowired private CardFactory cardFactory;
            @Autowired private ArtistDiscographyNavigation artistDiscographyNavigation;

    @Setter @Getter @Lazy @Autowired private SongImportService songImportService;
    @Setter @Getter @Lazy @Autowired private PlaybackTrackingService playbackTrackingService;


    private String activeRoute = "dashboard";

    public void show() {
        Scene scene = this.createScene();

        try {
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/globalStyle.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/buttons.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/graph.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/musicPlayer.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/dashboard.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/analytics.css")).toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }
        MainController.switchViews(scene);
    }

    private Scene createScene() {
        BorderPane root = new BorderPane();
        VBox left = AppSidebar.build("dashboard");
        root.setLeft(left);

        VBox rightHalf = new VBox(10);
        HBox top = createTopMenu();
        HBox bottom = createBottomMenu();

        // top is bound to 60% of scene height; bottom fills whatever remains.
        Scene scene = new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
        top.prefHeightProperty().bind(scene.heightProperty().multiply(0.60));

        VBox.setVgrow(top, Priority.NEVER);
        VBox.setVgrow(bottom, Priority.ALWAYS); // fills all remaining space — no gap at bottom

        rightHalf.getChildren().addAll(top, bottom);
        root.setCenter(rightHalf);

        return scene;
    }

    private HBox createTopMenu() {
        HBox root = new HBox();
        root.getStyleClass().add("HomepageTopMenu");

        // Center: player (grows to fill) stacked above stat cards (fixed height)
        VBox center = new VBox(20);
        center.getStyleClass().add("HomepageTopHalf");
        center.setMaxHeight(Double.MAX_VALUE);// Cap itself and preventing its children from filling it.
        center.setFillWidth(true);

        HBox playbackPanel = playbackPanelController.createPanel(
                artistDiscographyNavigation.getArtistDrillInCallback(),
                PlayerConfig.PlayerSize.LARGE);
        playbackPanel.setMaxWidth(Double.MAX_VALUE);
        playbackPanel.setPrefHeight(380); // Artwork fills the player naturally
        playbackPanel.setMaxHeight(380);  // Prevent the player from growing beyond this
        VBox.setVgrow(playbackPanel, Priority.NEVER); // Player stays fixed; cards take remaining space

        HBox cards = cardFactory.createStatCards(
                album -> myLibraryController.showWithAlbum(album),
                song  -> musicPlayerService.playSong(song));
        cards.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(cards, Priority.ALWAYS); // Both player and cards are fixed; no growing needed.
        cards.setMaxHeight(Double.MAX_VALUE); //lets the cards stretch to absorb whatever height top gains beyond the player's 380px.

        center.getChildren().addAll(playbackPanel, cards);
        HBox.setHgrow(center, Priority.ALWAYS);

        // Right: recently played capped to rightPanelPrefWidth so it doesn't crowd the player/stat-cards center column.
        VBox recentlyPlayedPanel = recentlyPlayedController.createPanel();
        recentlyPlayedPanel.setPrefWidth(AppUtils.rightPanelPrefWidth());
        recentlyPlayedPanel.setMinWidth(AppUtils.rightPanelPrefWidth());
        recentlyPlayedPanel.setMaxWidth(AppUtils.rightPanelPrefWidth());
        recentlyPlayedPanel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(recentlyPlayedPanel, Priority.ALWAYS);

        root.getChildren().addAll(center, recentlyPlayedPanel);
        return root;
    }

    private HBox createBottomMenu() {
        HBox root = new HBox();
        root.getStyleClass().add("HomepageBottomMenu");

        // Center: Graph fills whatever space the bottom section is allotted
        VBox center = new VBox(20);
        center.setPadding(new Insets(5));
        center.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        center.setFillWidth(true);

        HBox graphPanel = dashboardGraphController.createPanel();
        graphPanel.setMaxWidth(Double.MAX_VALUE);
        graphPanel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(graphPanel, Priority.ALWAYS);
        center.getChildren().add(graphPanel);
        HBox.setHgrow(center, Priority.ALWAYS);

        // Right: Top 5 artists matches rightPanelPrefWidth
        VBox topArtistsPanel = topArtistsController.createPanel(
                artistDiscographyNavigation.getArtistDrillInCallback());
        topArtistsPanel.setPrefWidth(AppUtils.rightPanelPrefWidth());
        topArtistsPanel.setMinWidth(AppUtils.rightPanelPrefWidth());
        topArtistsPanel.setMaxWidth(AppUtils.rightPanelPrefWidth());
        topArtistsPanel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(topArtistsPanel, Priority.ALWAYS);

        root.getChildren().addAll(center, topArtistsPanel);
        return root;
    }

}