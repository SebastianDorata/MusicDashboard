package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * A vertical A-Z (plus {@code #}) alphabet bar that sits on the right edge
 * of a list view.
 *
 * <p>Each letter is rendered as a clickable {@link Label}. Clicking it scrolls
 * the supplied {@link ScrollPane} so that the matching section-divider node
 * appears at the top of the visible area. Letters that have no matching section
 * in the content are dimmed and non-interactive.</p>
 *
 * <p>Typical usage:</p>
 * <pre>
 *   Map&lt;String, Node&gt; anchors = new LinkedHashMap&lt;&gt;();
 *   // populate: letter to the divider Label in the scroll content
 *   AlphabetBar bar = new AlphabetBar(scrollPane, anchors);
 *   borderPane.setRight(bar);
 * </pre>
 */
public class AlphabetBar extends VBox {

    /** All letters shown in the bar, plus {@code #} for non-alpha titles. */
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";

    /**
     * Constructs an {@code AlphabetBar}.
     *
     * @param scrollPane the pane whose content will be scrolled on letter click
     * @param anchors map of uppercase letter → the divider node for that letter in the scroll content; letters absent from the
     *                map are rendered as inactive (dimmed, non-clickable)
     */
    public AlphabetBar(ScrollPane scrollPane, Map<String, Node> anchors) {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(0);
        setPadding(new Insets(8, 6, 8, 6));
        getStyleClass().add("alphabet-bar");

        for (char c : LETTERS.toCharArray()) {
            String letter = String.valueOf(c);
            Label lbl = new Label(letter);

            // Each label grows equally so letters are distributed across the full bar height
            lbl.setMaxHeight(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            VBox.setVgrow(lbl, Priority.ALWAYS);

            if (anchors.containsKey(letter)) {
                // Active, green, clickable
                lbl.getStyleClass().add("alphabet-letter-active");
                lbl.setOnMouseClicked(e -> scrollToAnchor(scrollPane, anchors.get(letter)));
            } else {
                // Inactive, dimmed, no handler
                lbl.getStyleClass().add("alphabet-letter-inactive");
            }

            getChildren().add(lbl);
        }
    }

    /**
     * Scrolls {@code scrollPane} so that {@code target} appears at the top of
     * the visible area.
     *
     * <p>JavaFX's {@link ScrollPane#setVvalue(double)} works on a 0.0–1.0 scale,
     * so the node's Y position is converted to a fraction of the total content
     * height. Any top padding on the content region is subtracted from the
     * calculation so the divider label lands flush with the viewport edge.</p>
     *
     * @param scrollPane the pane to scroll
     * @param target the node to scroll to
     */
    private void scrollToAnchor(ScrollPane scrollPane, Node target) {
        // Ensure layout is up-to-date so bounds are accurate
        target.getParent().layout();

        double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
        double node         = target.getBoundsInParent().getMinY();

        // Subtract content padding so the divider isn't hidden behind the top edge
        Node content = scrollPane.getContent();
        if (content instanceof Region r) {
            node += r.getPadding().getTop();
        }

        double fraction = node / contentHeight;
        scrollPane.setVvalue(Math.min(fraction, 1.0));
    }
}
/*
 * AlphabetBar not visible or clickable bug fix summary.
 *
 * Root cause: nested ScrollPanes. createContentArea() wrapped contentArea in
 * an outer ScrollPane (fitToHeight=false), which let the inner ScrollPane
 * added by buildListView() expand to its full content height — sometimes
 * thousands of pixels. The AlphabetBar was stretched to match that height,
 * and with setAlignment(CENTER) all 27 letters clustered in the middle of
 * that huge bar, far off-screen.
 *
 * Fix: in list mode, bypass the outer ScrollPane entirely. loadSongsView()
 * and loadAlbumsView() now set sceneRoot.setCenter() directly with a wrapper
 * VBox (header + BorderPane), so the inner ScrollPane + AlphabetBar fill the
 * real viewport. Grid views still use the outer ScrollPane as normal.
 */