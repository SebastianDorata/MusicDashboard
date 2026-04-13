package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Modal dialog for editing album metadata.
 *
 * <p>Allows the user to update the album title, release year, and genre.
 * Because {@link Album} has no direct genre field, the entered genre is
 * returned as part of {@link Result} and the caller is responsible for
 * applying it to all songs in the album via the appropriate service.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * AlbumEditDialog.show(album).ifPresent(result -> {
 *     album.setTitle(result.title());
 *     album.setReleaseYear(result.releaseYear());
 *     // apply result.genre() to all songs via LibraryService
 * });
 * }</pre>
 */
public class AlbumEditDialog {

    /**
     * Immutable result returned when the user confirms the edit dialog.
     *
     * @param title       the updated album title
     * @param releaseYear the updated release year, or {@code null} if blank
     * @param genre       the genre to apply to all songs in the album,
     *                    or {@code null} if left blank
     */
    public record Result(String title, Integer releaseYear, String genre) {}

    /**
     * Displays a modal edit dialog pre-populated with the given album's
     * current metadata.
     *
     * @param album the album to edit; must not be {@code null}
     * @return an {@link Optional} containing the {@link Result} if the user
     *         confirmed, or empty if the user cancelled
     */
    public static Optional<Result> show(Album album) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Edit Album");
        dialog.setHeaderText(null);
        AppUtils.styleDialog(dialog);

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = AppUtils.buildDialogGrid();

        // Title field
        TextField titleField = new TextField(album.getTitle() != null ? album.getTitle() : "");
        titleField.setPromptText("Album title");
        titleField.getStyleClass().add("dialog-text-field");

        // Year field digits only, max 4 characters
        TextField yearField = new TextField(
                album.getReleaseYear() != null ? String.valueOf(album.getReleaseYear()) : ""
        );
        yearField.setPromptText("e.g. 2024");
        yearField.getStyleClass().add("dialog-text-field");

        // Genre field  applied to all songs in the album on save
        String currentGenre = resolveCurrentGenre(album);
        TextField genreField = new TextField(currentGenre != null ? currentGenre : "");
        genreField.setPromptText("e.g. Hip-Hop");
        genreField.getStyleClass().add("dialog-text-field");

        Label genreNote = new Label("Applied to all songs in this album");
        genreNote.getStyleClass().add("txt-grey-md");
        genreNote.setStyle("-fx-font-size: 10px;");

        grid.add(new Label("Title"),  0, 0);
        grid.add(titleField,          1, 0);
        grid.add(new Label("Year"),   0, 1);
        grid.add(yearField,           1, 1);
        grid.add(new Label("Genre"),  0, 2);
        grid.add(genreField,          1, 2);
        grid.add(genreNote,           1, 3);

        GridPane.setMargin(genreNote, new Insets(0, 0, 0, 2));

        dialog.getDialogPane().setContent(grid);

        // Disable Save if title is blank
        Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.setDisable(titleField.getText().isBlank());
        titleField.textProperty().addListener((obs, oldVal, newVal) ->
                saveButton.setDisable(newVal.isBlank())
        );

        // Only allow digits in year field
        yearField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d{0,4}")) yearField.setText(oldVal);
        });

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            String title    = titleField.getText().trim();
            String yearText = yearField.getText().trim();
            String genre    = genreField.getText().trim();

            Integer year = null;
            if (!yearText.isBlank()) {
                try { year = Integer.parseInt(yearText); }
                catch (NumberFormatException ignored) {}
            }

            return new Result(title, year, genre.isBlank() ? null : genre);
        });

        return dialog.showAndWait();
    }

    /**
     * Resolves the current genre for the album by inspecting the first song's
     * first genre, since {@link Album} has no direct genre field.
     *
     * @param album the album to inspect
     * @return the genre name, or {@code null} if none found
     */
    private static String resolveCurrentGenre(Album album) {
        if (album.getSongs() == null || album.getSongs().isEmpty()) return null;
        return album.getSongs().stream()
                .filter(s -> s.getGenres() != null && !s.getGenres().isEmpty())
                .findFirst()
                .map(s -> s.getGenres().iterator().next().getName())
                .orElse(null);
    }
}