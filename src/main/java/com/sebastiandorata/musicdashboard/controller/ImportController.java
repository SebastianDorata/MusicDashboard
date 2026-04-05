package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.service.ImportService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Import page.
 *
 * <p>Provides a drag-and-drop zone for importing music files into the library.
 * Folders are supported and recursively scanned for supported audio files.
 *
 * <p>The import process runs on a background thread via {@link ImportService},
 * with progress and completion callbacks dispatched back to the JavaFX thread
 * via {@link javafx.application.Platform#runLater(Runnable)}.
 *
 * <p>UI state during import:
 * <ul>
 *   <li>A {@link ProgressBar} shows per-file progress.</li>
 *   <li>A count label shows "Importing X / Y".</li>
 *   <li>A status label shows the current file name.</li>
 *   <li>On completion, a summary of imported, skipped, and failed counts is shown.</li>
 * </ul>
 */
@Component
public class ImportController {

    @Autowired
    private ImportService importService;

    private Label statusLabel;
    private Label countLabel;
    private ProgressBar progressBar;
    private VBox dropZone;

    @PostConstruct
    public void register() {
        MainController.registerImport(this);
    }

    public void show() {
        Scene scene = this.createScene();

        try {
            scene.getStylesheets().add(getClass().getResource("/css/globalStyle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/buttons.css").toExternalForm());

        } catch (Exception e) {
            System.out.println("CSS not found: " + e.getMessage());
        }

        MainController.switchViews(scene);
    }

    private Scene createScene() {
        VBox root = new VBox(25);
            root.setAlignment(Pos.TOP_CENTER);
            root.setPadding(new Insets(30));
            root.getStyleClass().add("header-background");
        StackPane header = new StackPane();
        header.setMaxWidth(Double.MAX_VALUE);

        Button homeBtn = new Button("← Dashboard");
        homeBtn.getStyleClass().addAll("nav-btn-back", "txt-white-md-bld");
        homeBtn.setOnAction(e -> MainController.navigateTo("dashboard"));
        StackPane.setAlignment(homeBtn, Pos.CENTER_LEFT);

        Label title = new Label("My Library");
        title.getStyleClass().addAll("txt-white-bld-forty", "txt-centre-underline");
        StackPane.setAlignment(title, Pos.CENTER);

        header.getChildren().addAll(homeBtn, title);
        root.getChildren().add(header);

                dropZone = new VBox(15);
                dropZone.setAlignment(Pos.CENTER);
                dropZone.setPrefSize(700, 300);
                dropZone.getStyleClass().add("dropZone");

            Label dropLabel = new Label("Drag & Drop a Music Folder or Files Here");
                dropLabel.getStyleClass().add("txt-white-md");

            Label subLabel = new Label("Supports .mp3 and .m4a");
                subLabel.getStyleClass().add("txt-grey-sm");

                dropZone.getChildren().addAll(dropLabel, subLabel);
                setupDragAndDrop();

                statusLabel = new Label("");
                statusLabel.getStyleClass().add("txt-grey-sm");

                countLabel = new Label("");
                countLabel.getStyleClass().add("txt-white-md");

                progressBar = new ProgressBar(0);
                progressBar.setPrefWidth(700);
                progressBar.setVisible(false);

        root.getChildren().addAll(dropZone, progressBar, countLabel, statusLabel);
        return new Scene(root, AppUtils.APP_WIDTH, AppUtils.APP_HEIGHT);
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> event.consume());

        dropZone.setOnDragDropped(this::handleDrop);
    }

    private void handleDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            List<File> musicFiles = new ArrayList<>();

            for (File file : db.getFiles()) {
                if (file.isDirectory()) {
                    importService.collectMusicFiles(file, musicFiles);
                } else if (importService.isMusicFile(file)) {
                    musicFiles.add(file);
                }
            }

            if (!musicFiles.isEmpty()) {
                beginImport(musicFiles);
            } else {
                statusLabel.setText("No supported music files found (.mp3, .m4a)");
            }

            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void beginImport(List<File> files) {
        int total = files.size();
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        countLabel.setText("Importing 0 / " + total);
        statusLabel.setText("Starting import...");

        importService.startImport(
                files,

                // onProgress: called from background thread for each file
                (current, tot, fileName) -> Platform.runLater(() -> {
                    progressBar.setProgress((double) current / tot);
                    countLabel.setText("Importing " + current + " / " + tot);
                    statusLabel.setText(fileName);
                }),

                // onComplete: called from background thread when finished
                (imported, skipped, failed) -> Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    countLabel.setText("Done! " + imported + " imported, "
                            + skipped + " skipped, "
                            + failed + " failed");
                    statusLabel.setText("Import complete.");
                    //backBtn.setDisable(false);
                })
        );
    }


}