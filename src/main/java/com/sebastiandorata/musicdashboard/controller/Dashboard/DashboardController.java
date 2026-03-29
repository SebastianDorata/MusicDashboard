package com.sebastiandorata.musicdashboard.controller.Dashboard;

import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.controller.UserLibrary.MyLibraryController;
import com.sebastiandorata.musicdashboard.service.*;
import com.sebastiandorata.musicdashboard.controllerUtils.ArtistDiscographyNavigation;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.CardFactory;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    @Autowired private PlaybackPanelController playbackPanelController;
    @Autowired private RecentlyPlayedController recentlyPlayedController;
    @Autowired private TopArtistsController topArtistsController;
    @Autowired private DashboardGraphController dashboardGraphController;
    @Autowired private CardFactory cardFactory;
    @Autowired private ArtistDiscographyNavigation artistDiscographyNavigation;

    @Lazy @Autowired
    private MusicPlayerService musicPlayerService;

    @Setter @Getter @Lazy @Autowired
    private SongService songService;

    @Setter @Getter @Lazy @Autowired
    private PlaybackTrackingService playbackTrackingService;

    @Autowired
    private UserSessionService userSessionService;

    // Used to navigate into an artist's discography from the Top 5 Artists panel and Now Playing bar
    @Lazy @Autowired private MyLibraryController myLibraryController;

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

        VBox left = createLeftMenu();
        VBox center = createCenterMenu();
        VBox right = createRightMenu();

        center.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        root.setLeft(left);
        root.setCenter(center);
        root.setRight(right);

        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }



    private VBox createLeftMenu() {
        VBox left = new VBox(20);
        left.setPadding(new Insets(20));
        left.getStyleClass().add("green-background");

        left.setMinWidth(220);
        left.setPrefWidth(250);
        left.setMaxWidth(300);

        Button libraryBtn = createLeftButton("My Library");
        Button playlistBtn = createLeftButton("My Playlist");
        Button importBtn = createLeftButton("Import Files");
        Button reportsBtn = createLeftButton("My Reports");

        libraryBtn.setOnAction(e -> MainController.navigateTo("library"));
        playlistBtn.setOnAction(e -> MainController.navigateTo("playlist"));
        importBtn.setOnAction(e -> MainController.navigateTo("import"));
        reportsBtn.setOnAction(e -> MainController.navigateTo("analytics"));

        left.getChildren().addAll(
                wrap(libraryBtn),
                wrap(playlistBtn),
                wrap(importBtn),
                wrap(reportsBtn)
        );

        return left;
    }

    private HBox wrap(Button btn) {
        HBox box = new HBox(btn);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Button createLeftButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("Left-Btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private VBox createCenterMenu() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(20));

        center.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Artist click navigates to My Library showing that artist's discography
        HBox playbackPanel = playbackPanelController.createPanel(artistDiscographyNavigation.getArtistDrillInCallback());
        HBox cards = cardFactory.createStatCards();
        HBox graphPanel = dashboardGraphController.createPanel();

        playbackPanel.setMaxWidth(Double.MAX_VALUE);
        cards.setMaxWidth(Double.MAX_VALUE);
        graphPanel.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(graphPanel, Priority.ALWAYS);
        graphPanel.setMaxHeight(Double.MAX_VALUE);

        center.getChildren().addAll(playbackPanel, cards, graphPanel);

        return center;
    }

    private VBox createRightMenu() {
        VBox right = new VBox(25);
        right.setPadding(new Insets(20));
        right.setPrefWidth(AppUtils.APP_WIDTH * 0.25);

        // Artist click navigates to My Library showing that artist's discography
        VBox topArtistsPanel = topArtistsController.createPanel(artistDiscographyNavigation.getArtistDrillInCallback());
        VBox recentlyPlayedPanel = recentlyPlayedController.createPanel();

        right.getChildren().addAll(topArtistsPanel, recentlyPlayedPanel);
        return right;
    }
}