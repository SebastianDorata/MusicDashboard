package com.sebastiandorata.musicdashboard.controller.Analytics;

import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.service.DataLoadingService;
import com.sebastiandorata.musicdashboard.service.YearEndReportService;
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

import java.time.Year;

@Component
public class AnalyticsController {

    @Lazy @Autowired private YearEndReportService yearEndReportService;
    @Autowired        private DataLoadingService  dataLoadingService;

    private ComboBox<Integer> yearSelector;
    private VBox topSongsList;
    private VBox topArtistsList;

    @PostConstruct
    public void register() {
        MainController.registerAnalytics(this);
    }

    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/dashboard.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/analytics.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found, using default styles");
        }
        MainController.switchViews(scene);
    }



    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dark-page-bg");
        root.setTop(createTopBar());
        root.setCenter(createCenterContent());
        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    private VBox createTopBar() {
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(20));
        topBar.getStyleClass().add("main-bkColour");

        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("Home");
        backBtn.getStyleClass().add("btn-blue");
        backBtn.setOnAction(e -> MainController.navigateTo("dashboard"));

        Label title = new Label("My Reports");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label yearLabel = new Label("Year:");
        yearLabel.getStyleClass().add("txt-white");

        yearSelector = createYearSelector();

        HBox yearBox = new HBox(10, yearLabel, yearSelector);
        yearBox.setAlignment(Pos.CENTER_RIGHT);

        headerRow.getChildren().addAll(backBtn, title, spacer, yearBox);
        topBar.getChildren().add(headerRow);
        return topBar;
    }

    private ScrollPane createCenterContent() {
        VBox centerContent = new VBox(20);
        centerContent.setPadding(new Insets(20));
        centerContent.setFillWidth(true);

        HBox mainContent = new HBox(20);
        mainContent.setFillHeight(true);

        VBox listsSection = createTopListsSection();
        HBox.setHgrow(listsSection, Priority.ALWAYS);
        mainContent.getChildren().add(listsSection);

        centerContent.getChildren().add(mainContent);

        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        return scrollPane;
    }

    private VBox createTopListsSection() {
        VBox container = new VBox(20);

        topSongsList   = new VBox(8);
        topArtistsList = new VBox(8);

        VBox topSongsContainer = CardFactory.createListPanel("Top 10 Songs", topSongsList);
        topSongsContainer.setPrefHeight(AppUtils.APP_HEIGHT * 0.5);

        VBox topArtistsContainer = CardFactory.createListPanel("Top 10 Artists", topArtistsList);
        topArtistsContainer.setPrefHeight(AppUtils.APP_HEIGHT * 0.5);

        container.getChildren().addAll(topSongsContainer, topArtistsContainer);
        return container;
    }



    private ComboBox<Integer> createYearSelector() {
        ComboBox<Integer> selector = new ComboBox<>();
        selector.setPrefWidth(150);
        selector.getStyleClass().add("year-selector");

        dataLoadingService.loadAsync(
                () -> yearEndReportService.getAvailableYears(),
                years -> {
                    selector.getItems().addAll(years);
                    if (!years.isEmpty()) {
                        selector.setValue(Year.now().getValue());
                    }
                }
        );


        selector.valueProperty().addListener((obs, old, val) -> {
            if (val != null) loadReportData(val,val);
        });

        return selector;
    }



    private void loadReportData(int year, int limit) {
        topSongsList.getChildren().clear();
        topArtistsList.getChildren().clear();

        dataLoadingService.loadAsync(
                () -> yearEndReportService.getTopSongsForYear(year,limit),
                songs -> {
                    topSongsList.getChildren().clear();
                    for (int i = 0; i < songs.size(); i++) {
                        topSongsList.getChildren().add(buildRankRow(i + 1, String.valueOf(songs.get(i).getValue())));
                    }
                }
        );

        dataLoadingService.loadAsync(
                () -> yearEndReportService.getTopArtistsForYear(year,limit),
                artists -> {
                    topArtistsList.getChildren().clear();
                    for (int i = 0; i < artists.size(); i++) {
                        topArtistsList.getChildren().add(buildRankRow(i + 1, String.valueOf(artists.get(i).getValue())));
                    }
                }
        );
    }

    private HBox buildRankRow(int rank, String name) {
        HBox row = new HBox(12);
        row.getStyleClass().add("analytics-rank-row");

        Label rankLbl = new Label(rank + ".");
        rankLbl.getStyleClass().add("analytics-rank-number");
        rankLbl.setMinWidth(28);

        Label nameLbl = new Label(name != null ? name : "Unknown");
        nameLbl.getStyleClass().add("analytics-rank-name");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        row.getChildren().addAll(rankLbl, nameLbl);
        return row;
    }
}