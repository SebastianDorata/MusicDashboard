package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.dto.HistoryRowData;
import com.sebastiandorata.musicdashboard.dto.TopAlbumRowData;
import com.sebastiandorata.musicdashboard.dto.TopSongRowData;
import com.sebastiandorata.musicdashboard.presentation.Analytics.*;
import com.sebastiandorata.musicdashboard.presentation.Analytics.viewmodel.*;
import com.sebastiandorata.musicdashboard.presentation.Dashboard.PlaybackPanelController;
import com.sebastiandorata.musicdashboard.presentation.helpers.PlayerConfig;
import com.sebastiandorata.musicdashboard.presentation.shared.SidebarBuilder;
import com.sebastiandorata.musicdashboard.service.MusicPlayerService;
import com.sebastiandorata.musicdashboard.service.handlers.*;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;

import static com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsSectionBuilder.buildSection;
import static com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsSectionBuilder.populateRows;
import static com.sebastiandorata.musicdashboard.utils.AppUtils.APP_WIDTH;


/**
 * Top-level controller for the Analytics page.
 * <p>Responsibilities:</p>
 *<ol>
 *   <li>Layout assembly (sidebar, center area, bottom player)</li>
 *   <li>Year selector and view-routing logic</li>
 *   <li>Delegating row creation to {@link AnalyticsRowFactory}</li>
 *   <li>Delegating section scaffolding to {@link AnalyticsSectionBuilder}</li>
 *   <li>Delegating modal wiring to {@link AnalyticsPaginatedModal} via {@link GenericModalLoader}, which pages entirely in memory using {@link DoublyLinkedList}</li>
 *   <li>Delegating the year-end "Wrapped" view to {@link YearWrappedViewController}</li>
 *</ol>
 */
@Component
public class AnalyticsController {


    @Lazy @Autowired private WeeklyReportViewModel       weeklyReportViewModel;
    @Lazy @Autowired private MonthlyReportViewModel      monthlyReportViewModel;
    @Lazy @Autowired private ListeningHistoryViewModel   listeningHistoryViewModel;
    @Lazy @Autowired private TopSongsViewModel           topSongsViewModel;
    @Lazy @Autowired private TopAlbumsViewModel          topAlbumsViewModel;
    @Lazy @Autowired private TopArtistsViewModel         topArtistsViewModel;

    @Autowired private DataLoadingService         dataLoadingService;
    @Autowired private GenericModalLoader         genericModalLoader;
    @Autowired private AnalyticsCacheService      analyticsCacheService;
    @Autowired private ListeningHistoryService    listeningHistoryService;
    @Autowired private ListeningPaginationService paginationService;
    @Lazy @Autowired private MusicPlayerService   musicPlayerService;

    @Lazy @Autowired private MyLibraryController  myLibraryController;
    @Lazy @Autowired private PlaybackPanelController playbackPanelController;
    @Autowired private WeeklyReportCalendarController weeklyCalendarController;
    @Autowired private YearWrappedViewController wrappedViewController;

    private ComboBox<Integer> yearSelector;
    private VBox centerContent;
    private StackPane mainPane;
    private String currentView = "history";
    private BorderPane content;


    @PostConstruct
    public void register() {
        MainController.registerAnalytics(this);
    }


    public void show() {
        Scene scene = this.createScene();

        try {
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/wrapped.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/musicPlayer.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/analytics.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/reports.css").toExternalForm());

        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }

        MainController.switchViews(scene);
    }






    private Scene createScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dark-page-bg");

        root.setLeft(createLeftMenu());
        root.setCenter(createCenterArea());
        root.setBottom(createBottomPlayer());

        showListeningHistory();
        StackPane mainPane = new StackPane(root);

        Scene scene = new Scene(mainPane, APP_WIDTH, AppUtils.APP_HEIGHT);
        return scene;
    }

    private VBox createLeftMenu() {
        var entries = List.of(
                new SidebarBuilder.NavEntry("", "Listening History", "history", () -> { currentView = "history"; showListeningHistory(); refreshSidebar(); }),
                new SidebarBuilder.NavEntry("", "Weekly Summary",    "weekly",  () -> { currentView = "weekly";  showWeeklyReport();     refreshSidebar(); }),
                new SidebarBuilder.NavEntry("", "Monthly Summary",   "monthly", () -> { currentView = "monthly"; showMonthlyReport();    refreshSidebar(); }),
                new SidebarBuilder.NavEntry("", "Yearly Wrapped",    "yearly",  () -> { currentView = "yearly";  showYearlyReport();     refreshSidebar(); }),
                new SidebarBuilder.NavEntry("", "Top Songs",         "songs",   () -> { currentView = "songs";   showTopSongs();         refreshSidebar(); }),
                new SidebarBuilder.NavEntry("", "Top Albums",        "albums",  () -> { currentView = "albums";  showTopAlbums();        refreshSidebar(); }),
                new SidebarBuilder.NavEntry("", "Top Artists",       "artists", () -> { currentView = "artists"; showTopArtists();       refreshSidebar(); })
        );

        yearSelector = createYearSelector();
        Label yearLabel = new Label("Select a year to begin");
        yearLabel.getStyleClass().addAll("txt-white-sm", "txt-centre-underline");
        VBox yearBox = new VBox(4, yearLabel, yearSelector);
        yearBox.setPadding(new Insets(8, 0, 4, 0));

        return SidebarBuilder.build(
                List.of("panels", "sidebar"),
                "My Reports",
                true,
                entries,
                currentView,
                true,
                yearBox,
                "Return home",
                "← Dashboard",
                () -> MainController.navigateTo("dashboard")
        );
    }

    private ScrollPane createCenterArea() {
        centerContent = new VBox(20);
        centerContent.setPadding(new Insets(20));
        centerContent.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(centerContent);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private ComboBox<Integer> createYearSelector() {
        ComboBox<Integer> selector = new ComboBox<>();
        selector.setPrefWidth(Double.MAX_VALUE);
        selector.setMaxWidth(Double.MAX_VALUE);
        selector.getStyleClass().addAll("year-selector", "Cursor-Hand");

        dataLoadingService.loadAsync(
                () -> {
                    int current = Year.now().getValue();
                    var years = new java.util.ArrayList<Integer>();
                    for (int i = current; i >= current - 6; i--) years.add(i);
                    return years;
                },
                years -> {
                    selector.getItems().addAll(years);
                    if (!years.isEmpty()) selector.setValue(Year.now().getValue());
                }
        );

        selector.valueProperty().addListener((obs, old, val) -> {
            if (val == null || currentView == null) return;
            switch (currentView) {
                case "weekly"  -> showWeeklyReport();
                case "monthly" -> showMonthlyReport();
                case "yearly"  -> showYearlyReport();
                case "history" -> showListeningHistory();
                case "songs"   -> showTopSongs();
                case "albums"  -> showTopAlbums();
                case "artists" -> showTopArtists();
            }
        });

        return selector;
    }

    private void showWeeklyReport() {
        centerContent.getChildren().clear();

        Integer selectedYear = yearSelector.getValue();
        if (selectedYear == null) return;

        Label title = new Label("Weekly Summary");
        title.getStyleClass().addAll("txt-white-bld-thirty", "txt-centre-underline");

        VBox reportsContainer = new VBox(15);
        reportsContainer.getStyleClass().add("analytics-section-container");

        int currentMonth = java.time.LocalDate.now().getMonthValue();
        showWeeklyCalendar(selectedYear, currentMonth, reportsContainer);
        centerContent.getChildren().addAll(title, reportsContainer);
    }

    private void showWeeklyCalendar(Integer year, Integer month, VBox container) {
        container.getChildren().clear();
        weeklyCalendarController.showCalendar(
                year, month,
                week -> loadWeeklyReportData(year, week, container),
                container.getChildren()::clear
        );
        container.getChildren().add(weeklyCalendarController.getCalendarPane());
    }

    private void showMonthlyReport() {
        centerContent.getChildren().clear();

        Integer selectedYear = yearSelector.getValue();
        if (selectedYear == null) return;

        Label title = new Label("Monthly Summary");
        title.getStyleClass().addAll("txt-white-bld-thirty", "txt-centre-underline");

        VBox reportsContainer = new VBox(15);
        reportsContainer.getStyleClass().add("analytics-section-container");

        HBox monthsRow1 = new HBox(5);
        HBox monthsRow2 = new HBox(5);
        for (int month = 1; month <= 12; month++) {
            Button btn = createMonthButton(month);
            final int m = month;
            btn.setOnAction(e -> {
                reportsContainer.getChildren().clear();
                loadMonthlyReportData(selectedYear, m, reportsContainer);
            });
            (month <= 6 ? monthsRow1 : monthsRow2).getChildren().add(btn);
        }

        VBox monthButtons = new VBox(5, monthsRow1, monthsRow2);
        centerContent.getChildren().addAll(title, monthButtons, reportsContainer);
    }

    private void showYearlyReport() {
        centerContent.getChildren().clear();

        Integer selectedYear = yearSelector.getValue();
        if (selectedYear == null) return;

        VBox wrappedView = wrappedViewController.buildView(selectedYear, centerContent);
        centerContent.getChildren().add(wrappedView);
    }

    private void showListeningHistory() {
        buildSection(
                centerContent,
                "Listening History",
                "View Full History",
                () -> showListeningHistoryModal(),
                preview -> {
                    preview.setSpacing(5);
                    listeningHistoryViewModel.loadHistoryWindow(0, 20, items ->
                            populateRows(
                                    preview, items,
                                    item -> AnalyticsRowFactory.createHistoryRow(
                                            item,
                                            () -> { if (item.song != null) musicPlayerService.playSong(item.song); }
                                    )));
                }
        );
    }

    private void showTopSongs() {
        buildSection(
                centerContent,
                "Top Songs",
                "View All",
                () -> showTopSongsModal(),
                preview -> topSongsViewModel.loadTopSongsWindow(0, 20, items ->
                        populateRows(
                                preview, items,
                                item -> AnalyticsRowFactory.createTopSongRow(
                                        item,
                                        () -> { if (item.song != null) musicPlayerService.playSong(item.song); }
                                )))
        );
    }

    private void showTopAlbums() {
        buildSection(
                centerContent,
                "Top Albums",
                "View All",
                () -> showTopAlbumsModal(),
                preview -> topAlbumsViewModel.loadTopAlbumsWindow(0, 20, items ->
                        populateRows(
                                preview, items,
                                item -> AnalyticsRowFactory.createTopAlbumRow(
                                        item,
                                        () -> { if (item.album != null) myLibraryController.showWithAlbum(item.album); }
                                )))
        );
    }

    private void showTopArtists() {
        buildSection(
                centerContent,
                "Top Artists",
                "View All",
                () -> showTopArtistsModal(),
                preview -> topArtistsViewModel.loadTopArtistsData(items ->
                        populateRows(
                                preview, items, AnalyticsRowFactory::createArtistRow), 20)
        );
    }

    private void showListeningHistoryModal() {
        genericModalLoader.loadAndShow(
                mainPane,
                "Listening History",
                () -> analyticsCacheService.getOrLoad(
                        "history",
                        listeningHistoryService::getListeningHistory
                ),
                h -> new HistoryRowData(
                        h.getSong() != null ? h.getSong().getTitle() : "Unknown",
                        h.getSong() != null && !h.getSong().getArtists().isEmpty()
                                ? h.getSong().getArtists().stream().findFirst()
    .map(Artist::getName).orElse("Unknown Artist")
                                : "Unknown",
                        AppUtils.formatRelativeTime(h.getPlayedAt()),
                        h.getDurationPlayedSeconds() != null ? h.getDurationPlayedSeconds() : 0,
                        h.getSong()
                ),
                item -> AnalyticsRowFactory.createHistoryRow(
                        item,
                        () -> { if (item.song != null) musicPlayerService.playSong(item.song); }
                ),
                25
        );
    }

    private void showTopSongsModal() {
        genericModalLoader.loadAndShow(
                mainPane,
                "Top Songs",
                () -> analyticsCacheService.getOrLoad(
                        "topSongs",
                        paginationService::getAllTopSongs
                ),
                song -> new TopSongRowData(
                        song.getTitle(),
                        song.getArtists() != null && !song.getArtists().isEmpty()
                                ? song.getArtists().stream().findFirst()
    .map(Artist::getName).orElse("Unknown Artist")
                                : "Unknown",
                        song.getDuration() != null ? song.getDuration() : 0,
                        song
                ),
                item -> AnalyticsRowFactory.createTopSongRow(
                        item,
                        () -> { if (item.song != null) musicPlayerService.playSong(item.song); }
                ),
                10
        );
    }

    private void showTopAlbumsModal() {
        genericModalLoader.loadAndShow(
                mainPane,
                "Top Albums",
                () -> analyticsCacheService.getOrLoad(
                        "topAlbums",
                        paginationService::getAllTopAlbums
                ),
                album -> new TopAlbumRowData(
                        album.getTitle(),
                        album.getReleaseYear() != null
                                ? album.getReleaseYear().toString()
                                : "Unknown",
                        album.getSongs() != null ? album.getSongs().size() : 0,
                        album
                ),
                item -> AnalyticsRowFactory.createTopAlbumRow(
                        item,
                        () -> { if (item.album != null) myLibraryController.showWithAlbum(item.album); }
                ),
                10
        );
    }

    private void showTopArtistsModal() {
        genericModalLoader.loadAndShow(
                mainPane,
                "Top Artists",
                () -> analyticsCacheService.getOrLoad(
                        "topArtists",
                        () -> topArtistsViewModel.buildAllArtistsRankedData()
                ),
                rowData -> rowData,
                AnalyticsRowFactory::createArtistRow,
                20
        );
    }

    private void loadWeeklyReportData(Integer year, Integer week, VBox container) {
        weeklyReportViewModel.loadWeeklyReport(year, week, data -> {
            VBox card = ReportDisplayController.createReportCard(
                    "Week " + data.week,
                    data.totalSongs, data.totalMinutes,
                    data.topSongName, data.topArtistName,
                    data.topAlbumName, data.topGenreName
            );
            container.getChildren().add(card);
        });
    }

    private void loadMonthlyReportData(Integer year, Integer month, VBox container) {
        monthlyReportViewModel.loadMonthlyReport(year, month, data -> {
            VBox card = ReportDisplayController.createReportCard(
                    data.monthName,
                    data.totalSongs, data.totalMinutes,
                    data.topSongName, data.topArtistName,
                    data.topAlbumName, data.topGenreName
            );
            container.getChildren().add(card);
        });
    }

    private Button createMonthButton(int month) {
        Button btn = new Button(java.time.Month.of(month).toString().substring(0, 3));
        btn.getStyleClass().add("month-btn");
        btn.setPrefWidth(40);
        return btn;
    }

    private void refreshSidebar() {
        content.setLeft(createLeftMenu());
    }

    private HBox createBottomPlayer() {
        HBox playerContainer = new HBox();
        playerContainer.setAlignment(Pos.CENTER);
        playerContainer.setPrefHeight(AppUtils.APP_HEIGHT * 0.1);
        playerContainer.setMaxHeight(AppUtils.APP_HEIGHT * 0.1);
        playerContainer.setMinHeight(AppUtils.APP_HEIGHT * 0.1);
        playerContainer.getStyleClass().add("analytics-player-container");
        playerContainer.setMaxWidth(Double.MAX_VALUE);

        HBox player = playbackPanelController.createPanel(
                artist -> MainController.navigateTo("library"),
                PlayerConfig.PlayerSize.SMALL
        );
        player.setPrefHeight(AppUtils.APP_HEIGHT * 0.1);
        player.setMaxHeight(AppUtils.APP_HEIGHT * 0.1);
        player.setMinHeight(AppUtils.APP_HEIGHT * 0.1);
        player.setPrefWidth(AppUtils.APP_WIDTH);
        player.setMaxWidth(AppUtils.APP_WIDTH);
        player.setMinWidth(AppUtils.APP_WIDTH);

        playerContainer.getChildren().add(player);
        return playerContainer;
    }
}