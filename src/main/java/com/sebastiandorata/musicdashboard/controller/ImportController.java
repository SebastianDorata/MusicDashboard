package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.Utils.Utils;
import com.sebastiandorata.musicdashboard.service.SongService;
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
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ImportController {

    @Lazy
    @Autowired
    private SongService songService;

    // Single background thread for imports to keep UI responsive
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();

    // UI elements held as fields so background thread can update them via Platform.runLater()
    private Label statusLabel;
    private Label countLabel;
    private ProgressBar progressBar;
    private Button backBtn;
    private VBox dropZone;

    @PostConstruct
    public void register() {
        MainController.registerImport(this);
    }


    public void show() {
        Scene scene = createScene();
        try {
            scene.getStylesheets().add(getClass().getResource("/globalStyle.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS not found");
        }
        MainController.switchViews(scene);
    }

    private Scene createScene() {
        VBox root = new VBox(25);
            root.setAlignment(Pos.TOP_CENTER);
            root.setPadding(new Insets(30));
            root.getStyleClass().add("main-bkColour");


            backBtn = new Button("Home");
            backBtn.getStyleClass().add("btn-blue");
            backBtn.setOnAction(e -> MainController.navigateTo("dashboard"));

        Label title = new Label("Import Music");
            title.getStyleClass().add("section-title");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(20, backBtn, title);
            header.setAlignment(Pos.CENTER_LEFT);

        // --- Drop zone ---
            dropZone = new VBox(15);
            dropZone.setAlignment(Pos.CENTER);
            dropZone.setPrefSize(700, 300);
            dropZone.getStyleClass().add("dropZone");

        //TODO: replace with png
            //Label dropIcon  = new Label();
            //dropIcon.setStyle();

        Label dropLabel = new Label("Drag & Drop a Music Folder or Files Here");
            dropLabel.getStyleClass().add("txt-white-md");

        Label subLabel  = new Label("Supports .mp3 and .m4a");
        subLabel.getStyleClass().add("txt-grey-sm");

            dropZone.getChildren().addAll( dropLabel, subLabel);//TODO: Add dropIcon
            setupDragAndDrop();

        //Status area
            statusLabel = new Label("");
            statusLabel.getStyleClass().add("txt-grey-sm");

            countLabel = new Label("");
            countLabel.getStyleClass().add("txt-white-md");

            progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(700);
            progressBar.setVisible(false);

        root.getChildren().addAll(header, dropZone, progressBar, countLabel, statusLabel);
        return new Scene(root, Utils.APP_WIDTH, Utils.APP_HEIGHT);
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                dropZone.getStyleClass().add("dropZone");
            }
            event.consume();
        });
        dropZone.setOnDragExited(event -> {
            dropZone.getStyleClass().add("dropZone");
        });

        dropZone.setOnDragDropped(this::handleDrop);
    }

    private void handleDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            List<File> droppedFiles = db.getFiles();

            // Collect all music files first
            List<File> musicFiles = new ArrayList<>();
            for (File file : droppedFiles) {
                if (file.isDirectory()) {
                    collectMusicFiles(file, musicFiles);
                } else if (isMusicFile(file)) {
                    musicFiles.add(file);
                }
            }

            if (!musicFiles.isEmpty()) {
                startImport(musicFiles);
            } else {
                statusLabel.setText("No supported music files found (.mp3, .m4a)");
            }

            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }


    private void startImport(List<File> files) {
        int total = files.size();

        // Update UI on JavaFX thread before starting background work
        Platform.runLater(() -> {
            progressBar.setVisible(true);
            progressBar.setProgress(0);
            countLabel.setText("Importing 0 / " + total);
            statusLabel.setText("Starting import...");
            backBtn.setDisable(true); // prevent navigating away mid-import
        });

        // Run the actual import on a background thread. Without this importing freezes the entire UI
        importExecutor.submit(() -> {
            int imported = 0;
            int skipped  = 0;
            int failed   = 0;

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                final int current = i + 1;

                // Update progress on JavaFX thread
                Platform.runLater(() -> {
                    progressBar.setProgress((double) current / total);
                    countLabel.setText("Importing " + current + " / " + total);
                    statusLabel.setText(file.getName());
                });

                try {
                    songService.importSong(file);
                    imported++;
                } catch (IllegalStateException e) {
                    // importSong throws this if already imported
                    skipped++;
                } catch (Exception e) {
                    failed++;
                    System.err.println("Failed to import: " + file.getName() + " — " + e.getMessage());
                }
            }

            // Final status update back on JavaFX thread
            final int finalImported = imported;
            final int finalSkipped  = skipped;
            final int finalFailed   = failed;

            Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                countLabel.setText("Done! " + finalImported + " imported, "
                        + finalSkipped + " skipped, "
                        + finalFailed + " failed");
                statusLabel.setText("Import complete.");
                backBtn.setDisable(false);

                // Reset drop zone style
                dropZone.getStyleClass().add("dropZone");
            });
        });
    }




      //Recursively collects all music files from a folder and its subfolders. Replaces the old scanFolder() which called processFile() directly  now we collect first so we know the total count for the progress bar.
    private void collectMusicFiles(File folder, List<File> results) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectMusicFiles(file, results);
            } else if (isMusicFile(file)) {
                results.add(file);
            }
        }
    }

    private boolean isMusicFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".m4a");
    }
}