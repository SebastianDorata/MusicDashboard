package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.dto.MigrationResult;
import com.sebastiandorata.musicdashboard.presentation.shared.SidebarBuilder;
import com.sebastiandorata.musicdashboard.service.ImportOrchestrator;
import com.sebastiandorata.musicdashboard.service.ImportOrchestrator.ImportCallbacks;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Single UI entry point for all import functionality.
 *
 * <h2>Layout</h2>
 * <pre>
 * ┌─────────────┬────────────────────────────────────────────────┐
 * │             │  Header: "Import Music"                        │
 * │   Sidebar   ├────────────────────────────────────────────────┤
 * │  (Dashboard │  Browse row  [path label]  [Browse…] [Scan]   │
 * │   style)    ├────────────────────────────────────────────────┤
 * │             │  Drag-and-drop zone                            │
 * │             ├────────────────────────────────────────────────┤
 * │             │  Progress bar + phase label                    │
 * │             ├────────────────────────────────────────────────┤
 * │             │  Results table (MigrationResult rows)          │
 * └─────────────┴────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Workflows</h2>
 * <b>Option A — Drag-and-drop</b>: user drops files or folders onto the
 * drop zone → {@link ImportOrchestrator#process} runs the single-pass import.
 *
 * <b>Option B — Browse</b>: user clicks Browse to pick a folder, then clicks
 * Scan → the same {@link ImportOrchestrator#process} is invoked with the
 * chosen folder.
 *
 * <p>Both paths delegate to the same orchestrator, so there is exactly one
 * backend import pipeline, and it is a single pass — every file is read once
 * and reconciled against the library in the same step (see
 * {@code SongUpsertService}), rather than an "import" scan followed by a
 * separate "metadata reconcile" scan.
 *
 * <h2>Threading</h2>
 * All heavy work runs on the import-pipeline thread inside
 * {@link ImportOrchestrator}. UI mutations are always dispatched via
 * {@link Platform#runLater(Runnable)}.
 */
@Component
public class MainImportController {

    @Autowired
    private ImportOrchestrator orchestrator;

    // ── Mutable UI state ──────────────────────────────────────────────────────

    private File                       selectedFolder  = null;
    private Button                     scanBtn         = null;
    private Label                      folderPathLabel = null;
    private VBox                       dropZone        = null;
    private ProgressBar                progressBar     = null;
    private Label                      progressLabel   = null;
    private Label                      summaryLabel    = null;
    private TableView<MigrationResult> resultsTable    = null;

    // ── Spring lifecycle ──────────────────────────────────────────────────────

    @PostConstruct
    public void register() {
        MainController.registerImport(this);
    }

    // ── Navigation entry point ────────────────────────────────────────────────

    public void show() {
        Scene scene = buildScene();
        loadStylesheets(scene);
        MainController.switchViews(scene);
    }

    // ── Scene construction ────────────────────────────────────────────────────

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(buildMainContent());
        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    /**
     * Left sidebar — same style as the Dashboard, with the "Import Files" entry
     * highlighted as the active route.
     */
    private VBox buildSidebar() {
        var entries = List.of(
                new SidebarBuilder.NavEntry("♫", "My Library",  "library",
                        () -> MainController.navigateTo("library")),
                new SidebarBuilder.NavEntry("≡", "My Playlist", "playlist",
                        () -> MainController.navigateTo("playlist")),
                new SidebarBuilder.NavEntry("↓", "Import Files","import",
                        () -> MainController.navigateTo("import")),
                new SidebarBuilder.NavEntry("◫", "My Reports",  "analytics",
                        () -> MainController.navigateTo("analytics"))
        );

        return SidebarBuilder.build(
                List.of("panels", "sidebar"),
                "My Dashboard",
                true,
                entries,
                "import",   // activeRoute — highlights "Import Files"
                true,
                null, null, null, null
        );
    }

    /**
     * Right-hand content area containing the header, browse row, drop zone,
     * progress bar, and results table.
     */
    private VBox buildMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.getStyleClass().add("dark-page-bg");

        // Build stateful widgets first so event handlers can reference them
        resultsTable    = buildResultsTable();
        progressBar     = buildProgressBar();
        progressLabel   = buildProgressLabel();
        summaryLabel    = buildSummaryLabel();
        folderPathLabel = buildFolderPathLabel();
        scanBtn         = buildScanButton();
        dropZone        = buildDropZone();

        content.getChildren().addAll(
                buildHeader(),
                buildBrowseRow(),
                dropZone,
                buildProgressSection(),
                summaryLabel,
                resultsTable
        );
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        return content;
    }

    // ── UI component builders ─────────────────────────────────────────────────

    private StackPane buildHeader() {
        StackPane header = new StackPane();
        header.setMaxWidth(Double.MAX_VALUE);

        Button homeBtn = new Button("← Dashboard");
        homeBtn.getStyleClass().addAll("nav-btn-back", "txt-white-md-bld");
        homeBtn.setOnAction(e -> MainController.navigateTo("dashboard"));
        StackPane.setAlignment(homeBtn, Pos.CENTER_LEFT);

        Label title = new Label("Import Music");
        title.getStyleClass().addAll("txt-white-bld-forty", "txt-centre-underline");
        StackPane.setAlignment(title, Pos.CENTER);

        header.getChildren().addAll(homeBtn, title);
        return header;
    }

    private HBox buildBrowseRow() {
        Label hint = new Label(
                "Browse for a folder, or drag-and-drop files/folders below. "
                        + "Every file is read once and fully imported/updated in a single pass.");
        hint.getStyleClass().add("txt-grey-sm");
        hint.setWrapText(true);

        Button browseBtn = new Button("Browse…");
        browseBtn.getStyleClass().add("nav-btn");
        browseBtn.setOnAction(e -> handleBrowse());

        HBox controls = new HBox(10, folderPathLabel, browseBtn, scanBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(folderPathLabel, Priority.ALWAYS);

        VBox col = new VBox(8, hint, controls);
        col.setFillWidth(true);

        HBox row = new HBox(col);
        HBox.setHgrow(col, Priority.ALWAYS);
        return row;
    }

    private VBox buildDropZone() {
        VBox zone = new VBox(15);
        zone.setAlignment(Pos.CENTER);
        zone.setPrefSize(700, 220);
        zone.getStyleClass().add("dropZone");

        Label dropLabel = new Label("Drag & Drop Music Files or Folders Here");
        dropLabel.getStyleClass().add("txt-white-md");

        Label subLabel = new Label("Supports .mp3 and .m4a  ·  Folders are scanned recursively");
        subLabel.getStyleClass().add("txt-grey-sm");

        zone.getChildren().addAll(dropLabel, subLabel);

        zone.setOnDragOver(event -> {
            if (event.getGestureSource() != zone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        zone.setOnDragExited(Event::consume);
        zone.setOnDragDropped(this::handleDrop);

        return zone;
    }

    private VBox buildProgressSection() {
        VBox section = new VBox(6, progressBar, progressLabel);
        section.setFillWidth(true);
        return section;
    }

    private ProgressBar buildProgressBar() {
        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(Double.MAX_VALUE);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setVisible(false);
        return bar;
    }

    private Label buildProgressLabel() {
        Label label = new Label("");
        label.getStyleClass().add("txt-white-sm");
        return label;
    }

    private Label buildSummaryLabel() {
        Label label = new Label("");
        label.getStyleClass().add("txt-white-sm");
        label.setWrapText(true);
        return label;
    }

    private Label buildFolderPathLabel() {
        Label label = new Label("No folder selected");
        label.getStyleClass().add("txt-grey-sm");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Button buildScanButton() {
        Button btn = new Button("Start Scan");
        btn.getStyleClass().add("login-btn-primary");
        btn.setDisable(true);
        btn.setOnAction(e -> handleBrowseScan());
        return btn;
    }

    @SuppressWarnings("unchecked")
    private TableView<MigrationResult> buildResultsTable() {
        TableView<MigrationResult> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("analytics-section-container");
        table.setPlaceholder(new Label("Results will appear here after importing."));

        TableColumn<MigrationResult, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().fileName()));
        fileCol.setPrefWidth(280);

        TableColumn<MigrationResult, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c ->
                new SimpleStringProperty(formatStatus(c.getValue().status())));
        statusCol.setPrefWidth(160);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                MigrationResult row = getTableView().getItems().get(getIndex());
                setStyle(statusColour(row.status()));
            }
        });

        TableColumn<MigrationResult, String> detailCol = new TableColumn<>("Details");
        detailCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().message()));

        table.getColumns().addAll(fileCol, statusCol, detailCol);
        return table;
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Music Folder");
        File defaultDir = new File(System.getProperty("user.home"), "Music");
        if (defaultDir.exists()) chooser.setInitialDirectory(defaultDir);

        File chosen = chooser.showDialog(MainController.getMainStage());
        if (chosen != null) {
            selectedFolder = chosen;
            folderPathLabel.setText(chosen.getAbsolutePath());
            scanBtn.setDisable(false);
            resetProgress();
        }
    }

    private void handleBrowseScan() {
        if (selectedFolder == null || !selectedFolder.exists()) return;
        scanBtn.setDisable(true);
        startPipeline(List.of(selectedFolder));
    }

    private void handleDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            List<File> collected = orchestrator.collectAudioFiles(db.getFiles());
            if (!collected.isEmpty()) {
                startPipeline(db.getFiles()); // pass raw inputs; orchestrator handles collection
            } else {
                Platform.runLater(() ->
                        progressLabel.setText("No supported audio files found (.mp3, .m4a)"));
            }
            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    // ── Pipeline invocation ───────────────────────────────────────────────────

    /**
     * Common entry point for both workflows. Resets UI then delegates to
     * {@link ImportOrchestrator#process}. There is now only one phase to
     * report progress for — the orchestrator no longer distinguishes an
     * "import" phase from a "reconcile" phase.
     *
     * @param inputs raw files or folders from drag-drop or browse selection
     */
    private void startPipeline(List<File> inputs) {
        resetProgress();
        progressBar.setVisible(true);
        progressLabel.setText("Starting…");

        orchestrator.process(inputs, new ImportCallbacks() {

            @Override
            public void onNoFiles() {
                Platform.runLater(() ->
                        progressLabel.setText("No supported audio files found (.mp3, .m4a)"));
            }

            @Override
            public void onStart(int totalFiles) {
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    progressLabel.setText("Importing — 0 / " + totalFiles);
                });
            }

            @Override
            public void onProgress(int current, int total, String fileName) {
                Platform.runLater(() -> {
                    progressBar.setProgress((double) current / total);
                    progressLabel.setText("Importing " + current + " / " + total + "  —  " + fileName);
                });
            }

            @Override
            public void onComplete(List<MigrationResult> results) {
                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    scanBtn.setDisable(selectedFolder == null);

                    long imported     = count(results, MigrationResult.Status.IMPORTED);
                    long pathUpdated  = count(results, MigrationResult.Status.PATH_UPDATED);
                    long metaUpdated  = count(results, MigrationResult.Status.METADATA_UPDATED);
                    long artRefreshed = count(results, MigrationResult.Status.ART_REFRESHED);
                    long alreadyOk    = count(results, MigrationResult.Status.ALREADY_CURRENT);
                    long skipped      = count(results, MigrationResult.Status.SKIPPED);
                    long errors       = count(results, MigrationResult.Status.ERROR);

                    summaryLabel.setText(String.format(
                            "Done.  %d new  ·  %d relocated  ·  %d metadata refreshed  ·  "
                                    + "%d art refreshed  ·  %d already current  ·  %d skipped  ·  %d error(s)",
                            imported, pathUpdated, metaUpdated, artRefreshed, alreadyOk, skipped, errors));

                    progressLabel.setText("All done — " + results.size() + " file(s) evaluated.");
                    resultsTable.getItems().setAll(results);
                });
            }
        });
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    private void resetProgress() {
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        progressLabel.setText("");
        summaryLabel.setText("");
        resultsTable.getItems().clear();
    }

    private long count(List<MigrationResult> results, MigrationResult.Status status) {
        return results.stream().filter(r -> r.status() == status).count();
    }

    private String formatStatus(MigrationResult.Status status) {
        return switch (status) {
            case IMPORTED         -> "＋  Imported";
            case PATH_UPDATED     -> "✔  Path Updated";
            case METADATA_UPDATED -> "✎  Metadata Updated";
            case ALREADY_CURRENT  -> "·  Already Current";
            case ART_REFRESHED    -> "⊙  Art Refreshed";
            case SKIPPED          -> "—  Skipped";
            case ERROR            -> "✕  Error";
        };
    }

    private String statusColour(MigrationResult.Status status) {
        return switch (status) {
            case IMPORTED         -> "-fx-text-fill: #1db954;";
            case PATH_UPDATED     -> "-fx-text-fill: #1db954;";
            case METADATA_UPDATED -> "-fx-text-fill: #f7931e;";
            case ART_REFRESHED    -> "-fx-text-fill: #61dafb;";
            case ALREADY_CURRENT  -> "-fx-text-fill: #999999;";
            case SKIPPED          -> "-fx-text-fill: #777777;";
            case ERROR            -> "-fx-text-fill: #e94560;";
        };
    }

    private void loadStylesheets(Scene scene) {
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(
                    getClass().getResource("/css/buttons.css").toExternalForm());
            scene.getStylesheets().add(
                    getClass().getResource("/css/dashboard.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }
    }
}