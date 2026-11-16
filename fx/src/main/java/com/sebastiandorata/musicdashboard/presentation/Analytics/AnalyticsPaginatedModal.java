package com.sebastiandorata.musicdashboard.presentation.Analytics;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.function.Function;

/**
 * Instantiable modal helper that opens a {@link ModalListController} overlay on a
 * given {@link StackPane} and serves pages from an in-memory {@link DoublyLinkedList}.
 * Time Complexity: O(n) to load allItems into the linked list on show(); O(offset+limit) per page turn.
 * Space Complexity: O(n) for the in-memory linked list.
 */
public class AnalyticsPaginatedModal {

    private final StackPane parentPane;
    private ModalListController modal;

    public AnalyticsPaginatedModal(StackPane parentPane) {
        this.parentPane = parentPane;
    }

    /**
     * Loads {@code allItems} into an in-memory list, shows the first page,
     * and wires subsequent page-change requests to in-memory lookups.
     *
     * @param <T> item type
     * @param title modal heading
     * @param firstPage pre-sliced first page (avoids repeating the skip(0).limit())
     * @param totalCount total number of items across all pages
     * @param pageSize number of items per page
     * @param rowRenderer converts one item to a JavaFX {@link Node} row
     * @param onPageChange callback invoked by the modal footer buttons with (offset, limit)
     */
    public <T> void show(String title,
                         List<T> firstPage,
                         int totalCount,
                         int pageSize,
                         Function<T, Node> rowRenderer,
                         java.util.function.BiConsumer<Integer, Integer> onPageChange) {

        modal = new ModalListController();

        // Capture the overlay as a local final variable — avoids field-capture issues
        modal.showModal(
                title,
                firstPage,
                totalCount,
                pageSize,
                rowRenderer::apply,
                null  // wire close below
        );

        final StackPane overlay = modal.getOverlay();


        // Wire the close button now that we have the overlay reference
        modal.setOnClose(() -> parentPane.getChildren().remove(overlay));

        parentPane.getChildren().add(overlay);
        modal.setOnPageChange(onPageChange::accept);
    }

    /**
     * Replaces the currently visible page with {@code newItems}.
     * Must be called after {@link #show}.
     */
    public <T> void updateItems(List<T> newItems, Function<T, Node> rowRenderer) {
        if (modal != null) {
            modal.updateItems(newItems, rowRenderer::apply);
        }
    }
}

/*
//Bug fix:The original class declared all methods as static and its
  constructor private, but GenericModalLoader called new AnalyticsPaginatedModal(parentPane) and then
  invoked instance methods modal.show(), modal.updateItems().  This was a compile error.
  The class could never be instantiated.
  The fix converts this to a proper instantiable class:
  Constructor accepts the {@code StackPane} host.
  show() and updateItems() are instance methods.
  The internal ModalListController is held as an instance field so
  updateItems() can reach it after  show() returns.
*/
