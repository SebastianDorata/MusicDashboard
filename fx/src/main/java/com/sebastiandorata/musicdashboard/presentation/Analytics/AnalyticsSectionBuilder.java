package com.sebastiandorata.musicdashboard.presentation.Analytics;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Static builder for standard analytics center-content sections.
 *
 * Every analytics section follows the same pattern:
 *   Title label + "View All" button + preview VBox (populated by a callback)
 *
 * SRP: only responsible for assembling this repeating layout pattern.
 * OCP: new sections are added by calling buildSection() with different args, not by modifying this class.
 *
 * Time Complexity: O(1) per call, layout construction is constant work.
 * Space Complexity: O(1), no data stored; nodes added to the caller's VBox.
 */
public class AnalyticsSectionBuilder {

    private AnalyticsSectionBuilder() {}

    /**
     * Clears {@code centerContent}, then adds a title, an action button,
     * and a styled preview container. The {@code previewPopulator} callback
     * receives the empty preview VBox and is responsible for loading data
     * into it asynchronously.
     *
     * @param centerContent The page's main content VBox (cleared on entry)
     * @param titleText Section heading shown at the top
     * @param actionButtonText Label for the "View All / View Full" button
     * @param onActionClicked Runnable invoked when the action button is pressed
     * @param previewPopulator Callback that receives the preview VBox so callers can load rows into it after an async data fetch
     */
    public static void buildSection(
            VBox centerContent,
            String titleText,
            String actionButtonText,
            Runnable onActionClicked,
            Consumer<VBox> previewPopulator) {

        centerContent.getChildren().clear();

        Label title = new Label(titleText);
        title.getStyleClass().addAll("txt-white-bld-thirty", "txt-centre-underline");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        Button actionBtn = new Button(actionButtonText);
        actionBtn.getStyleClass().add("view-All");
        actionBtn.setOnAction(e -> onActionClicked.run());

        VBox preview = new VBox(10);
        preview.getStyleClass().addAll("txt-white-bld-thirty", "txt-centre-underline");
        VBox.setVgrow(preview, Priority.ALWAYS);

        centerContent.getChildren().addAll(title, actionBtn, preview);

        previewPopulator.accept(preview);
    }

    /**
     * Clears {@code container} then adds one rendered row per item.
     * Designed to be called from inside a ViewModel's async callback.
     *
     * Example usage inside a loadAsync callback:
     * <pre>
     *   topSongsViewModel.loadTopSongsWindow(0, 5, items ->
     *       AnalyticsSectionBuilder.populateRows(
     *           preview, items, AnalyticsRowFactory::createTopSongRow));
     * </pre>
     *
     * @param container The VBox to populate
     * @param items Data items returned from the ViewModel
     * @param rowRenderer Function that converts one data item to a JavaFX HBox row
     * @param <T> The data type
     *
     * Time Complexity: O(n) where n = items.size()
     * Space Complexity: O(n) nodes added to the scene graph
     */
    public static <T> void populateRows(
            VBox container,
            List<T> items,
            Function<T, javafx.scene.Node> rowRenderer) {

        container.getChildren().clear();
        items.forEach(item -> container.getChildren().add(rowRenderer.apply(item)));
    }
}