package com.sebastiandorata.musicdashboard.Controller;

import com.sebastiandorata.musicdashboard.service.SongService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class ImportController {

    @Autowired
    private SongService songService;

    private VBox dropZone = new VBox();
    private Label label = new Label("Drag & Drop Music Folder Here");

    public void show() {

        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefSize(600, 400);
        dropZone.setStyle("-fx-border-color: white; -fx-border-width: 3;");

        label.setStyle("-fx-font-size: 24px;");

        dropZone.getChildren().add(label);

        setupDragAndDrop();

        Scene scene = new Scene(dropZone, 800, 600);

        MainController.switchViews(scene);
    }

    private void setupDragAndDrop() {

        dropZone.setOnDragOver(event -> {

            if (event.getGestureSource() != dropZone &&
                    event.getDragboard().hasFiles()) {

                event.acceptTransferModes(TransferMode.COPY);
            }

            event.consume();
        });

        dropZone.setOnDragDropped(this::handleDrop);
    }

    private void handleDrop(DragEvent event) {

        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {

            List<File> files = db.getFiles();

            for (File file : files) {

                if (file.isDirectory()) {
                    scanFolder(file);
                } else {
                    processFile(file);
                }
            }

            success = true;
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void scanFolder(File folder) {

        File[] files = folder.listFiles();

        if (files == null) return;

        for (File file : files) {

            if (file.isDirectory()) {
                scanFolder(file);
            } else {
                processFile(file);
            }
        }
    }

    private void processFile(File file) {

        String name = file.getName().toLowerCase();

        if (!(name.endsWith(".mp3") || name.endsWith(".m4a"))) {
            return;
        }

        try {
            songService.importSong(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}