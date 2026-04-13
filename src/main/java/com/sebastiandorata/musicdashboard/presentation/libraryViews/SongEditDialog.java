package com.sebastiandorata.musicdashboard.presentation.libraryViews;


import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A modal dialog that allows the user to edit the metadata of a {@link Song}.
 *
 * <p>Editable fields include title, album, artist(s), genre(s), and track number.
 * Multiple artists and genres can be entered as a comma-separated list. When the
 * user confirms:</p>
 * <ul>
 *   <li>Existing albums, artists, and genres are reused by name lookup to avoid
 *       duplicates; new entries are created and persisted automatically.</li>
 *   <li>The updated song is saved via {@link SongRepository}.</li>
 *   <li>The supplied {@code onSaved} callback is invoked so the calling view
 *       can refresh its list.</li>
 * </ul>
 *
 * <p>The dialog is application-modal and blocks the parent window until dismissed.</p>
 */
public class SongEditDialog {

    private final SongRepository   songRepository;
    private final AlbumRepository  albumRepository;
    private final ArtistRepository artistRepository;
    private final GenreRepository  genreRepository;

    /**
     * Constructs a {@code SongEditDialog} with the repositories needed to
     * look up and persist metadata entities.
     *
     * @param songRepository   repository for persisting the edited song
     * @param albumRepository  repository for looking up or creating albums by title
     * @param artistRepository repository for looking up or creating artists by name
     * @param genreRepository  repository for looking up or creating genres by name
     */
    public SongEditDialog(SongRepository songRepository,
                          AlbumRepository albumRepository,
                          ArtistRepository artistRepository,
                          GenreRepository genreRepository) {
        this.songRepository   = songRepository;
        this.albumRepository  = albumRepository;
        this.artistRepository = artistRepository;
        this.genreRepository  = genreRepository;
    }

    /**
     * Opens the edit dialog for the given song and saves changes on confirmation.
     *
     * <p>Pre-populates all fields from the song's current state. If the user
     * presses Cancel or closes the dialog, no changes are persisted.</p>
     *
     * @param song    the {@link Song} to edit; mutated in-place on save
     * @param onSaved a {@link Runnable} invoked after a successful save, intended
     *                for triggering a UI refresh; may be {@code null}
     */
    public void show(Song song, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Song");
        dialog.setHeaderText("Editing: " + song.getTitle());
        dialog.initModality(Modality.APPLICATION_MODAL);

        TextField titleField  = new TextField(song.getTitle());
        TextField albumField  = new TextField(
                song.getAlbum() != null ? song.getAlbum().getTitle() : "");
        TextField artistField = new TextField(
                song.getArtists() != null
                        ? song.getArtists().stream().map(Artist::getName).collect(Collectors.joining(", "))
                        : "");
        TextField genreField  = new TextField(
                song.getGenres() != null
                        ? song.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "))
                        : "");
        TextField trackField  = new TextField(
                song.getTrackNum() != null ? song.getTrackNum().toString() : "");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, label("Title"),     titleField);
        grid.addRow(1, label("Album"),     albumField);
        grid.addRow(2, label("Artist(s)"), artistField);
        grid.addRow(3, label("Genre(s)"),  genreField);
        grid.addRow(4, label("Track #"),   trackField);

        Label hint = new Label("Separate multiple artists/genres with a comma");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        grid.add(hint, 1, 5);

        ColumnConstraints col1 = new ColumnConstraints(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Rename OK to "Save" for clarity
        ButtonType saveBtn   = new ButtonType("Save",   ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);

        dialog.showAndWait().ifPresent(result -> {
            if (result != saveBtn) return;

            //Collect proposed changes
            String newTitle   = titleField.getText().trim();
            String newAlbum   = albumField.getText().trim();
            String newArtists = artistField.getText().trim();
            String newGenres  = genreField.getText().trim();
            String newTrack   = trackField.getText().trim();

            // Build a summary of what changed
            StringBuilder changes = new StringBuilder();

            String oldTitle = song.getTitle() != null ? song.getTitle() : "";
            if (!newTitle.isBlank() && !newTitle.equals(oldTitle))
                changes.append("Title:   ").append(oldTitle).append("  →  ").append(newTitle).append("\n");

            String oldAlbum = song.getAlbum() != null ? song.getAlbum().getTitle() : "";
            if (!newAlbum.isBlank() && !newAlbum.equals(oldAlbum))
                changes.append("Album:   ").append(oldAlbum).append("  →  ").append(newAlbum).append("\n");

            String oldArtists = song.getArtists() != null
                    ? song.getArtists().stream().map(Artist::getName).collect(Collectors.joining(", ")) : "";
            if (!newArtists.isBlank() && !newArtists.equals(oldArtists))
                changes.append("Artists: ").append(oldArtists).append("  →  ").append(newArtists).append("\n");

            String oldGenres = song.getGenres() != null
                    ? song.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", ")) : "";
            if (!newGenres.isBlank() && !newGenres.equals(oldGenres))
                changes.append("Genres:  ").append(oldGenres).append("  →  ").append(newGenres).append("\n");

            String oldTrack = song.getTrackNum() != null ? song.getTrackNum().toString() : "";
            if (!newTrack.isBlank() && !newTrack.equals(oldTrack))
                changes.append("Track #: ").append(oldTrack).append("  →  ").append(newTrack).append("\n");

            if (changes.isEmpty()) return;

            //Confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Changes");
            confirm.setHeaderText("Save the following changes?");
            confirm.setContentText(changes.toString());
            confirm.initModality(Modality.APPLICATION_MODAL);

            ((Button) confirm.getDialogPane().lookupButton(ButtonType.OK))
                    .setText("Confirm");
            ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL))
                    .setText("Go Back");

            confirm.showAndWait().ifPresent(confirmResult -> {
                if (confirmResult != ButtonType.OK) return;

                //Persist
                if (!newTitle.isBlank()) song.setTitle(newTitle);

                if (!newAlbum.isBlank()) {
                    Album album = albumRepository.findByTitle(newAlbum)
                            .orElseGet(() -> {
                                Album a = new Album();
                                a.setTitle(newAlbum);
                                return albumRepository.save(a);
                            });
                    song.setAlbum(album);
                }

                if (!newArtists.isBlank()) {
                    Set<Artist> artists = Arrays.stream(newArtists.split(","))
                            .map(String::trim).filter(s -> !s.isBlank())
                            .map(name -> artistRepository.findByName(name)
                                    .orElseGet(() -> {
                                        Artist a = new Artist();
                                        a.setName(name);
                                        return artistRepository.save(a);
                                    }))
                            .collect(Collectors.toSet());
                    song.setArtists(artists);
                }

                if (!newGenres.isBlank()) {
                    Set<Genre> genres = Arrays.stream(newGenres.split(","))
                            .map(String::trim).filter(s -> !s.isBlank())
                            .map(name -> genreRepository.findByName(name)
                                    .orElseGet(() -> {
                                        Genre g = new Genre();
                                        g.setName(name);
                                        return genreRepository.save(g);
                                    }))
                            .collect(Collectors.toSet());
                    song.setGenres(genres);
                }

                if (!newTrack.isBlank()) {
                    try { song.setTrackNum(Integer.parseInt(newTrack)); }
                    catch (NumberFormatException ignored) {}
                }

                songRepository.save(song);
                System.out.println("Saved: " + song.getTitle());
                if (onSaved != null) onSaved.run();
            });
        });
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }
}
