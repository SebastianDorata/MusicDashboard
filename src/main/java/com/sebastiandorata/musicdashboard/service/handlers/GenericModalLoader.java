package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.presentation.Analytics.AnalyticsPaginatedModal;
import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Combines asynchronous data loading with paginated modal display.
 *
 * <p>SRP: Only responsible for the async-load and modal-show wiring.</p>
 * <p>DIP: Entity-type agnostic; callers supply mappers and renderers.</p>
 *
 * <p>Time Complexity: O(1) per call, O(n) on the background thread during load.</p>
 * <p>Space Complexity: O(n) for the in-memory list.</p>
 */
@Service
public class GenericModalLoader {

    @Autowired
    private DataLoadingService dataLoadingService;

    /**
     * Loads a full dataset asynchronously, maps each raw item to a display DTO,
     * then opens a paginated modal backed entirely by the in-memory list.
     *
     * @param parentPane StackPane that hosts the modal overlay
     * @param title Modal window title
     * @param listLoader Supplier returning the full {@link DoublyLinkedList}
     * @param mapper Converts a raw entity {@code E} to display DTO {@code D}
     * @param rowRenderer Converts a display DTO to a JavaFX {@link Node} row
     * @param pageSize Number of items shown per modal page
     * @param <E> Source element type stored in the DoublyLinkedList
     * @param <D> Display DTO type rendered by rowRenderer
     */
    public <E, D> void loadAndShow(
            StackPane parentPane,
            String title,
            Supplier<DoublyLinkedList<E>> listLoader,
            Function<E, D> mapper,
            Function<D, Node> rowRenderer,
            int pageSize) {

        dataLoadingService.loadAsync(
                () -> {
                    // Background thread: load full list and map to display DTOs
                    DoublyLinkedList<E> full = listLoader.get();
                    return full.toList().stream()
                            .map(mapper)
                            .toList();
                },
                (List<D> allItems) -> {
                    // JavaFX thread: open the modal with the first page
                    AnalyticsPaginatedModal modal = new AnalyticsPaginatedModal(parentPane);

                    List<D> firstPage = allItems.stream()
                            .limit(pageSize)
                            .toList();

                    modal.show(
                            title,
                            firstPage,
                            allItems.size(),
                            pageSize,
                            rowRenderer,
                            (offset, limit) -> {
                                // In-memory page navigation — no DB call needed
                                List<D> page = allItems.stream()
                                        .skip(offset)
                                        .limit(limit)
                                        .toList();
                                modal.updateItems(page, rowRenderer);
                            }
                    );
                }
        );
    }
}
// =============================================================================
// Bug fx:
// The original code called new AnalyticsPaginatedModal(parentPane)
// and then modal.show(...) / modal.updateItems(...). Those instance methods did
// not exist. The class only exposed a single static show() with a different
// signature. This was a compile error that prevented the app from starting at all.
