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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class ImportController {

    @Autowired
    private ImportService importService;

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

            HBox header = new HBox(20, backBtn, title);
                header.setAlignment(Pos.CENTER_LEFT);

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

        root.getChildren().addAll(header, dropZone, progressBar, countLabel, statusLabel);
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

        // Set up the UI before handing off to the service
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        countLabel.setText("Importing 0 / " + total);
        statusLabel.setText("Starting import...");
        backBtn.setDisable(true);

        // Hand off all logic to the service, passing UI callbacks wrapped in Platform.runLater
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
                    backBtn.setDisable(false);
                })
        );
    }
}