package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Song;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.format.DateTimeFormatter;

/**
 * A {@link ListCell} used inside the album detail view to display a single
 * {@link Song} as a tabular row containing the track number, title, play
 * count, date first listened, and a context-menu trigger button.
 *
 * <p>Column widths are fixed to align rows across the list. The menu button
 * delegates to {@link LibraryHandler#onSongMenu()} so that song actions
 * (add to playlist, favourite, etc.) are handled centrally.</p>
 */
public class AlbumSongCell extends ListCell<Song> {

    private final HBox          row;
    private final Label         trackNum;
    private final Label         title;
    private final Label         playCount;
    private final Label         dateAdded;
    private final Button        menuBtn;
    private final LibraryHandler ctx;

    /**
     * Constructs an {@code AlbumSongCell} and builds the fixed-width column
     * layout for track number, title, play count, date, and the menu button.
     *
     * @param ctx the {@link LibraryHandler} providing the song-menu callback
     */
    public AlbumSongCell(LibraryHandler ctx) {
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
        menuBtn.setOnMousePressed(Event::consume);

        row = new HBox(50, trackNum, title, playCount, dateAdded, menuBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("album-song-row");
    }

    /**
     * Updates the cell's content for the given {@link Song}.
     *
     * <p>Populates the track number (displaying "—" when absent), title,
     * play count, and the date first listened formatted as "MMM d, yyyy".
     * The menu button's action is rebound to the current song on each update.</p>
     *
     * @param song  the song to display, or {@code null} if the cell is empty
     * @param empty {@code true} if this cell represents an empty row
     */
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

        menuBtn.setOnAction(e -> ctx.onSongMenu().accept(song, menuBtn));
        setGraphic(row);
    }
}