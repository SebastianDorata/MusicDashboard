package com.sebastiandorata.musicdashboard.presentation.playlist;

import com.sebastiandorata.musicdashboard.controller.PlaylistController;
import com.sebastiandorata.musicdashboard.entity.Playlist;
import com.sebastiandorata.musicdashboard.service.PlaylistService;
import com.sebastiandorata.musicdashboard.utils.AppUtils;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Handles all playlist-related dialogs (create, rename, delete).
 *
 * <p>All methods are static. PlaylistViewBuilder and PlaylistController
 * call them directly without needing to hold a handler instance.
 * The PlaylistController is passed as a parameter so callbacks can
 * route state changes back</p>
 *
 * <p>SRP: Dialog presentation and result dispatch only.</p>
 * <p>DIP: Depends on PlaylistController's narrow callback interface,
 * not its internals.</p>
 *
 * <p>Time Complexity: O(1) per dialog, all operations are single service calls.</p>
 * <p>Space Complexity: O(1).</p>
 */
public class PlaylistDialogHandler {

    private PlaylistDialogHandler() {}

    /** Shared form data returned from the name/description dialog. */
    private record PlaylistFormResult(String name, String description) {}


    /** Opens the "New Playlist" dialog and creates the playlist on confirm. */
    public static void showCreateDialog(PlaylistService playlistService, PlaylistController controller) {
        showForm("New Playlist", "Create a new playlist", null, null)
                .ifPresent(result -> {
                    try {
                        Playlist created = playlistService.createPlaylist(
                                result.name(), result.description());
                        controller.selectPlaylist(created);
                    } catch (Exception e) {
                        AppUtils.showError("Could not create playlist: " + e.getMessage());
                    }
                });
    }

    /** Opens the rename dialog for an existing playlist. */
    public static void showRenameDialog(Playlist playlist, PlaylistService playlistService, PlaylistController controller) {
        showForm(
                "Rename Playlist",
                "Rename \"" + playlist.getName() + "\"",
                playlist.getName(),
                playlist.getDescription()
        ).ifPresent(result -> {
            try {
                playlistService.updatePlaylist(
                        playlist.getPlaylistId(),
                        result.name(), result.description(), null);
                controller.reloadSelected(playlist.getPlaylistId());
            } catch (Exception e) {
                AppUtils.showError("Could not rename playlist: " + e.getMessage());
            }
        });
    }

    /** Opens the delete confirmation dialog. */
    public static void showDeleteDialog(Playlist playlist, PlaylistService playlistService, PlaylistController controller) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Playlist");
        alert.setHeaderText("Delete \"" + playlist.getName() + "\"?");
        alert.setContentText("This permanently deletes the playlist. Songs remain in your library.");

        alert.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .ifPresent(r -> {
                    try {
                        playlistService.deletePlaylist(playlist.getPlaylistId());
                        controller.clearSelection();
                    } catch (Exception e) {
                        AppUtils.showError("Could not delete playlist: " + e.getMessage());
                    }
                });
    }

    /**
     * Builds and shows the name/description form used by create and rename.
     *
     * @param dialogTitle  window title
     * @param headerText   text shown at the top of the dialog
     * @param initialName  pre-populated name (null = empty)
     * @param initialDesc  pre-populated description (null = empty)
     * @return             Optional containing the trimmed result, empty if cancelled
     */
    private static Optional<PlaylistFormResult> showForm(String dialogTitle, String headerText, String initialName, String initialDesc) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(dialogTitle);
        dialog.setHeaderText(headerText);
        AppUtils.styleDialog(dialog);

        GridPane grid = AppUtils.buildDialogGrid();

        TextField nameField = new TextField(initialName != null ? initialName : "");
        nameField.setPromptText("Playlist name");
        nameField.setPrefWidth(260);

        TextField descField = new TextField(initialDesc != null ? initialDesc : "");
        descField.setPromptText("Description (optional)");
        descField.setPrefWidth(260);

        grid.add(new Label("Name:"),        0, 0);
        grid.add(nameField,                 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField,                 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((o, old, val) ->
                okBtn.setDisable(val.trim().isEmpty()));
        Platform.runLater(nameField::requestFocus);

        return dialog.showAndWait()
                .filter(r -> r == ButtonType.OK)
                .map(r -> new PlaylistFormResult(
                        nameField.getText().trim(),
                        descField.getText().trim()));
    }
}