package com.sebastiandorata.musicdashboard.presentation.Analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Standalone modal controller for displaying paginated lists.
 *
 * <p><b><u>This component is reusable across the application:</u></b></p>
 * <ul>
 *   <li>Displays any list with DoublyLinkedList pagination.</li>
 *   <li>No knowledge of entity types (uses callbacks for rendering).</li>
 * </ul>
 *
 * <p>DIP: Takes callbacks for rendering rather than depending on concrete types.</p>
 * <p>SRP: Only responsible for modal UI and pagination logic.</p>
 *
 *
 * <p>Time Complexity: O(1) per page navigation.</p>
 * <p>Space Complexity: O(limit) for displayed items.</p>
 */
public class ModalListController {

    @Getter
    private StackPane overlay;
    private VBox modalContent;
    private VBox itemsList;
    private Label pageLabel;
    private int currentOffset = 0;
    private int pageSize = 10;
    private int totalItems;
    @Setter private Runnable onClose;
    @Setter private BiConsumer<Integer, Integer> onPageChange;

    /**
     * Creates and shows a modal list with pagination.
     *
     * @param title Modal title
     * @param items First page of items to display
     * @param totalCount Total number of items across all pages
     * @param pageSize Items per page
     * @param itemRenderer Callback to render each item as a Node
     * @param onCloseCallback Called when modal is closed.
     */
    public <T> void showModal(String title, List<T> items, int totalCount, int pageSize, Function<T, Node> itemRenderer, Runnable onCloseCallback) {
        this.pageSize    = pageSize;
        this.totalItems  = totalCount;
        this.currentOffset = 0;
        if (onCloseCallback != null) this.onClose = onCloseCallback;

        createOverlay();
        createModalContent(title);   // sets this.itemsList and this.pageLabel
        populateItems(items, itemRenderer);
        updatePaginationControls();
    }

    private void createOverlay() {
        overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        overlay.setPrefSize(1280, 800);

        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeModal();
            }
        });
    }

    private void createModalContent(String title) {
        modalContent = new VBox(15);
        modalContent.setPrefWidth(1280 * 0.8);
        modalContent.setPrefHeight(800 * 0.9);
        modalContent.setStyle("-fx-background-color: #1e1e1e; -fx-border-radius: 10; -fx-padding: 20;");
        modalContent.getStyleClass().add("modal-content");

        HBox header = createHeader(title);

        // Store as a field so populateItems/updateItems never need lookup()
        itemsList = new VBox(8);
        itemsList.setStyle("-fx-padding: 10;");

        // Wrap in ScrollPane so the header and footer are never pushed out of view
        ScrollPane scrollPane = new ScrollPane(itemsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-background-color: #1e1e1e;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox footer = createFooter();   // also sets this.pageLabel

        modalContent.getChildren().addAll(header, scrollPane, footer);

        StackPane.setAlignment(modalContent, Pos.CENTER);
        overlay.getChildren().add(modalContent);
    }

    private HBox createHeader(String title) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("wt-smmd-bld");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("← Go Back");
        closeBtn.getStyleClass().add("return-btn");
        closeBtn.setOnAction(e -> closeModal());

        header.getChildren().addAll(titleLabel, spacer, closeBtn);
        return header;
    }

    private HBox createFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 0, 0, 0));

        Button firstBtn = new Button("⟨⟨ First");
        firstBtn.getStyleClass().add("btn-blue");
        firstBtn.setOnAction(e -> goToFirst());

        Button prevBtn = new Button("⟨ Previous");
        prevBtn.getStyleClass().add("btn-blue");
        prevBtn.setOnAction(e -> goToPrevious());

        // Store as a field so updatePaginationControls() never needs lookup()
        pageLabel = new Label("Page 1 / 1");
        pageLabel.getStyleClass().add("wt-smmd-bld");
        pageLabel.setMinWidth(150);
        pageLabel.setAlignment(Pos.CENTER);

        Button nextBtn = new Button("Next ⟩");
        nextBtn.getStyleClass().add("btn-blue");
        nextBtn.setOnAction(e -> goToNext());

        Button lastBtn = new Button("Last ⟩⟩");
        lastBtn.getStyleClass().add("btn-blue");
        lastBtn.setOnAction(e -> goToLast());

        footer.getChildren().addAll(firstBtn, prevBtn, pageLabel, nextBtn, lastBtn);
        return footer;
    }

    private <T> void populateItems(List<T> items, java.util.function.Function<T, Node> itemRenderer) {
        itemsList.getChildren().clear();
        for (T item : items) {
            itemsList.getChildren().add(itemRenderer.apply(item));
        }
    }

    private void updatePaginationControls() {
        int totalPages  = Math.max(1, (totalItems + pageSize - 1) / pageSize);
        int currentPage = (currentOffset / pageSize) + 1;
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
    }


    private void goToFirst() {
        currentOffset = 0;
        updatePaginationControls();
        if (onPageChange != null) onPageChange.accept(currentOffset, pageSize);
    }

    private void goToPrevious() {
        if (currentOffset > 0) {
            currentOffset = Math.max(0, currentOffset - pageSize);
            updatePaginationControls();
            if (onPageChange != null) onPageChange.accept(currentOffset, pageSize);
        }
    }

    private void goToNext() {
        int totalPages  = Math.max(1, (totalItems + pageSize - 1) / pageSize);
        int currentPage = (currentOffset / pageSize) + 1;
        if (currentPage < totalPages) {
            currentOffset += pageSize;
            updatePaginationControls();
            if (onPageChange != null) onPageChange.accept(currentOffset, pageSize);
        }
    }

    private void goToLast() {
        int totalPages = Math.max(1, (totalItems + pageSize - 1) / pageSize);
        currentOffset  = (totalPages - 1) * pageSize;
        updatePaginationControls();
        if (onPageChange != null) onPageChange.accept(currentOffset, pageSize);
    }

    private void closeModal() {
        if (onClose != null) onClose.run();
    }

    public <T> void updateItems(List<T> newItems, java.util.function.Function<T, Node> itemRenderer) {
        itemsList.getChildren().clear();
        for (T item : newItems) {
            itemsList.getChildren().add(itemRenderer.apply(item));
        }
    }
}