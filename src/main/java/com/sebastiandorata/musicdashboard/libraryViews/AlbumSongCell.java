package com.sebastiandorata.musicdashboard.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.format.DateTimeFormatter;

public class AlbumSongCell extends ListCell<Song> {

    private final HBox          row;
    private final Label         trackNum;
    private final Label         title;
    private final Label         playCount;
    private final Label         dateAdded;
    private final Button        menuBtn;
    private final LibraryContext ctx;

    public AlbumSongCell(LibraryContext ctx) {
        this.ctx = ctx;

        trackNum = new Label();
        trackNum.setPrefWidth(40);
        trackNum.getStyleClass().add("album-song-track");

        title = new Label();
        title.setPrefWidth(300);
        HBox.setHgrow(title, Priority.ALWAYS);
        title.getStyleClass().add("album-song-title");

        playCount = new Label();
        playCount.setPrefWidth(100);
        playCount.getStyleClass().add("album-song-meta");

        dateAdded = new Label();
        dateAdded.setPrefWidth(150);
        dateAdded.getStyleClass().add("album-song-meta");

        menuBtn = new Button("⋯");
        menuBtn.getStyleClass().add("song-row-menu-btn");
        menuBtn.setOnMousePressed(javafx.event.Event::consume);

        row = new HBox(20, trackNum, title, playCount, dateAdded, menuBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("album-song-row");
    }

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);
        if (empty || song == null) {
            setGraphic(null);
            return;
        }

        Integer tn = song.getTrackNum();
        trackNum.setText((tn != null && tn > 0) ? String.valueOf(tn) : "—");
        title.setText(song.getTitle() != null ? song.getTitle() : "Unknown");

        int pc = song.getListenCount() != null ? song.getListenCount() : 0;
        playCount.setText(pc + " plays");

        if (song.getDateFirstListened() != null) {
            try {
                dateAdded.setText(song.getDateFirstListened()
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
            } catch (Exception ignored) {
                dateAdded.setText("—");
            }
        } else {
            dateAdded.setText("—");
        }

        menuBtn.setOnAction(e -> ctx.onSongMenu.accept(song, menuBtn));
        setGraphic(row);
    }
}