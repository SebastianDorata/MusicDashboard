package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.dto.MigrationResult;
import com.sebastiandorata.musicdashboard.service.LibraryMigrationService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Controller for the Library Migration page.
 *
 * <p>Provides a folder browser that lets the user point to a directory (or
 * an updated copy of their music library). The controller then delegates
 * the heavy lifting to {@link LibraryMigrationService} on a background thread
 * while displaying real-time progress in the UI.</p>
 *
 * <h2>Thread model</h2>
 * The scan runs on a plain daemon {@link Thread}. Per-file progress callbacks
 * dispatch to the JavaFX thread via {@link Platform#runLater(Runnable)}. The
 * Start button is disabled for the duration of the scan to prevent re-entry.
 */
@Component
public class LibraryMigrationController {

    @Autowired
    private LibraryMigrationService migrationService;
    private File selectedFolder     = null;
    private Button startBtn         = null;
    private Label folderPathLabel   = null;
    private ProgressBar progressBar = null;
    private Label progressLabel     = null;
    private Label summaryLabel      = null;
    private TableView<MigrationResult> resultsTable = null;


    @PostConstruct
    public void register() {
        MainController.registerMigration(this);
    }

    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }
        MainController.switchViews(scene);
    }


    private Scene createScene() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("dark-page-bg");

        root.getChildren().addAll(
                buildHeader(),
                buildFolderRow(),
                buildProgressSection(),
                buildSummaryLabel(),
                buildResultsTable()
        );

        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }


    private StackPane buildHeader() {
        StackPane header = new StackPane();
        header.setMaxWidth(Double.MAX_VALUE);

        Button homeBtn = new Button("← Dashboard");
        homeBtn.getStyleClass().addAll("nav-btn-back", "txt-white-md-bld");
        homeBtn.setOnAction(e -> MainController.navigateTo("dashboard"));
        StackPane.setAlignment(homeBtn, Pos.CENTER_LEFT);

        Label title = new Label("Library Migration");
        title.getStyleClass().addAll("txt-white-bld-forty", "txt-centre-underline");
        StackPane.setAlignment(title, Pos.CENTER);

        header.getChildren().addAll(homeBtn, title);
        return header;
    }


    private HBox buildFolderRow() {
        folderPathLabel = new Label("No folder selected");
        folderPathLabel.getStyleClass().add("txt-grey-sm");
        folderPathLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(folderPathLabel, Priority.ALWAYS);

        Button browseBtn = new Button("Browse…");
        browseBtn.getStyleClass().add("nav-btn");
        browseBtn.setOnAction(e -> handleBrowse());

        startBtn = new Button("Start Scan");
        startBtn.getStyleClass().add("login-btn-primary");
        startBtn.setDisable(true);
        startBtn.setOnAction(e -> handleStartScan());

        Label hint = new Label(
                "Select the folder that contains your music files. "
                        + "Existing songs will be re-linked if they have moved; no data will be deleted.");
        hint.getStyleClass().add("txt-grey-sm");
        hint.setWrapText(true);

        VBox col = new VBox(8, hint, new HBox(10, folderPathLabel, browseBtn, startBtn));
        col.setFillWidth(true);
        ((HBox) col.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(col);
        HBox.setHgrow(col, Priority.ALWAYS);
        return row;
    }

    private VBox buildProgressSection() {
        progressLabel = new Label("");
        progressLabel.getStyleClass().add("txt-white-sm");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        VBox section = new VBox(6, progressBar, progressLabel);
        section.setFillWidth(true);
        return section;
    }

    private Label buildSummaryLabel() {
        summaryLabel = new Label("");
        summaryLabel.getStyleClass().add("txt-white-sm");
        summaryLabel.setWrapText(true);
        return summaryLabel;
    }

    @SuppressWarnings("unchecked")
    private TableView<MigrationResult> buildResultsTable() {
        resultsTable = new TableView<>();
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultsTable.getStyleClass().add("analytics-section-container");
        resultsTable.setPlaceholder(new Label("Results will appear here after scanning."));

        TableColumn<MigrationResult, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().fileName()));
        fileCol.setPrefWidth(280);

        TableColumn<MigrationResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        formatStatus(c.getValue().status())));
        statusCol.setPrefWidth(140);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                // Colour-code the status column
                MigrationResult row = getTableView().getItems().get(getIndex());
                setStyle(statusColour(row.status()));
            }
        });

        TableColumn<MigrationResult, String> detailCol = new TableColumn<>("Details");
        detailCol.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().message()));

        resultsTable.getColumns().addAll(fileCol, statusCol, detailCol);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        return resultsTable;
    }

    // Event handlers ──────────────────────────────────────────────────────

    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");

        // Default to the user's home music directory if it exists
        File defaultDir = new File(System.getProperty("user.home"), "Music");
        if (defaultDir.exists()) chooser.setInitialDirectory(defaultDir);

        File chosen = chooser.showDialog(MainController.getMainStage());
        if (chosen != null) {
            selectedFolder = chosen;
            folderPathLabel.setText(chosen.getAbsolutePath());
            startBtn.setDisable(false);
            summaryLabel.setText("");
            resultsTable.getItems().clear();
            progressBar.setProgress(0);
            progressBar.setVisible(false);
            progressLabel.setText("");
        }
    }

    /**
     * Starts the migration scan on a daemon background thread.
     * All UI mutations are dispatched back via {@link Platform#runLater}.
     */
    private void handleStartScan() {
        if (selectedFolder == null || !selectedFolder.exists()) return;

        startBtn.setDisable(true);
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        progressLabel.setText("Starting scan…");
        summaryLabel.setText("");
        resultsTable.getItems().clear();

        Thread scanThread = new Thread(() -> {
            List<MigrationResult> results = migrationService.scanFolder(
                    selectedFolder,
                    (current, total, fileName) -> Platform.runLater(() -> {
                        double pct = total > 0 ? (double) current / total : 0;
                        progressBar.setProgress(pct);
                        progressLabel.setText("Scanning " + current + " / " + total
                                + "  —  " + fileName);
                    })
            );

            Platform.runLater(() -> displayResults(results));
        });
        scanThread.setDaemon(true);
        scanThread.setName("library-migration-scan");
        scanThread.start();
    }

    //  Results display ─────────────────────────────────────────────────────

    private void displayResults(List<MigrationResult> results) {
        resultsTable.getItems().setAll(results);

        long updated      = results.stream().filter(r -> r.status() == MigrationResult.Status.PATH_UPDATED).count();
        long metaUpdated  = results.stream().filter(r -> r.status() == MigrationResult.Status.METADATA_UPDATED).count();
        long artRefresh   = results.stream().filter(r -> r.status() == MigrationResult.Status.ART_REFRESHED).count();
        long alreadyOk    = results.stream().filter(r -> r.status() == MigrationResult.Status.ALREADY_CURRENT).count();
        long skipped      = results.stream().filter(r -> r.status() == MigrationResult.Status.SKIPPED).count();
        long errors       = results.stream().filter(r -> r.status() == MigrationResult.Status.ERROR).count();

        summaryLabel.setText(String.format(
                "Done.  %d path(s) updated  ·  %d metadata refreshed  ·  %d cover art refreshed  "
                        + "·  %d already current  ·  %d skipped  ·  %d error(s)",
                updated, metaUpdated, artRefresh, alreadyOk, skipped, errors));

        progressBar.setProgress(1.0);
        progressLabel.setText("Scan complete — " + results.size() + " file(s) evaluated.");
        startBtn.setDisable(false);
    }

    // Display helpers ─────────────────────────────────────────────────────

    private String formatStatus(MigrationResult.Status status) {
        return switch (status) {
            case PATH_UPDATED     -> "✔  Path Updated";
            case METADATA_UPDATED -> "✎  Metadata Updated";
            case ALREADY_CURRENT  -> "·  Already Current";
            case ART_REFRESHED    -> " Art Refreshed";
            case SKIPPED          -> "—  Skipped";
            case ERROR            -> "✕  Error";
        };
    }

    private String statusColour(MigrationResult.Status status) {
        return switch (status) {
            case PATH_UPDATED     -> "-fx-text-fill: #1db954;";   // green
            case METADATA_UPDATED -> "-fx-text-fill: #f7931e;";   // orange
            case ART_REFRESHED    -> "-fx-text-fill: #61dafb;";   // blue
            case ALREADY_CURRENT  -> "-fx-text-fill: #999999;";   // grey
            case SKIPPED          -> "-fx-text-fill: #777777;";   // darker grey
            case ERROR            -> "-fx-text-fill: #e94560;";   // red
        };
    }
}