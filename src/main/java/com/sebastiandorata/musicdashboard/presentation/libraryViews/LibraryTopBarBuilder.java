package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the top bar for the My Library page.
 *
 * <p>Extracted from MyLibraryController to separate UI
 * construction from control logic. All interactive elements
 * receive callbacks from the controller so this class has
 * no dependency on controller state.</p>
 *
 * <p>SRP: Only responsible for constructing the top bar node.</p>
 */
public class LibraryTopBarBuilder {

    /**
     * Builds the complete top bar and returns a Result record
     * carrying the built node and all references the controller
     * needs for later style updates.
     */
    public static Result build(
            Map<String, Button>    tabButtons,
            GenreRepository        genreRepository,
            String                 currentView,
            String                 currentDisplayMode,
            Consumer<String>       onTabSwitch,
            Consumer<String>       onDisplayMode,
            Consumer<Genre>        onGenreFilter,
            Consumer<SortStrategy> onSortChange) {

        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(20));
        topBar.getStyleClass().add("header-background");
        topBar.setAlignment(Pos.CENTER);

        // Header row
        StackPane header = new StackPane();
        header.setMaxWidth(Double.MAX_VALUE);

        Button homeBtn = new Button("← Dashboard");
        homeBtn.getStyleClass().addAll("nav-btn-back", "txt-white-md-bld");
        homeBtn.setOnAction(e -> MainController.navigateTo("dashboard"));
        StackPane.setAlignment(homeBtn, Pos.CENTER_LEFT);

        Label title = new Label("My Library");
        title.getStyleClass().addAll("txt-white-bld-forty", "txt-centre-underline");
        StackPane.setAlignment(title, Pos.CENTER);
        header.getChildren().addAll(homeBtn, title);

        // Tab buttons on the left
        HBox leftGroup = new HBox(10);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        for (String[] pair : new String[][]{
                {"Songs", "songs"}, {"Albums", "albums"},
                {"Artists", "artists"}, {"Favourites", "favourites"}}) {
            String tabKey = pair[1];
            Button btn = new Button(pair[0]);
            updateTabStyle(btn, tabKey, currentView);
            btn.setOnMouseEntered(e -> {
                if (!tabKey.equals(currentView))
                    btn.getStyleClass().add("btn-enter");
            });
            btn.setOnMouseExited(e ->
                    btn.getStyleClass().remove("btn-enter"));
            btn.setOnAction(e -> onTabSwitch.accept(tabKey));
            tabButtons.put(tabKey, btn);
            leftGroup.getChildren().add(btn);
        }

        // List/Grid toggles
        ToggleGroup viewGroup = new ToggleGroup();

        ToggleButton listToggle = new ToggleButton("List");
        listToggle.setToggleGroup(viewGroup);
        listToggle.getStyleClass().addAll("nav-btn", "txt-white-md-bld");
        listToggle.setOnAction(e -> onDisplayMode.accept("list"));

        ToggleButton gridToggle = new ToggleButton("Grid");
        gridToggle.setToggleGroup(viewGroup);
        gridToggle.setSelected("grid".equals(currentDisplayMode));
        gridToggle.getStyleClass().addAll("nav-btn-active", "txt-white-md-bld");
        gridToggle.setOnAction(e -> onDisplayMode.accept("grid"));

        HBox centerGroup = new HBox(10, listToggle, gridToggle);
        centerGroup.setAlignment(Pos.CENTER);

        // Filter controls on the right
        FilterControls filterControls = buildFilterControls(
                genreRepository, onGenreFilter, onSortChange);

        HBox rightGroup = new HBox(filterControls.box());
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        StackPane controlsRow = new StackPane();
        controlsRow.setMaxWidth(Double.MAX_VALUE);
        StackPane.setAlignment(leftGroup,   Pos.CENTER_LEFT);
        StackPane.setAlignment(centerGroup, Pos.CENTER);
        StackPane.setAlignment(rightGroup,  Pos.CENTER_RIGHT);
        leftGroup.setPickOnBounds(false);
        centerGroup.setPickOnBounds(false);
        rightGroup.setPickOnBounds(false);
        controlsRow.getChildren().addAll(leftGroup, centerGroup, rightGroup);

        topBar.getChildren().addAll(header, controlsRow);

        return new Result(
                topBar,
                listToggle,
                gridToggle,
                filterControls.box(),
                filterControls.sortComboBox(),
                filterControls.genreComboBox()
        );
    }

    private static FilterControls buildFilterControls(
            GenreRepository        genreRepository,
            Consumer<Genre>        onGenreFilter,
            Consumer<SortStrategy> onSortChange) {

        HBox box = new HBox(15);
        box.getStyleClass().addAll("dropDown-options");

        Label genreLabel = new Label("Genre:");
        genreLabel.getStyleClass().add("txt-white-ttl-bld");

        ComboBox<GenreOption> genreComboBox = new ComboBox<>();
        genreComboBox.setPrefWidth(150);
        genreComboBox.getStyleClass().addAll("combo-box", "txt-white-sm");

        // Populate genres
        genreComboBox.getItems().add(new GenreOption(null, "All Genres"));
        List<Genre> allGenres = genreRepository.findAll();
        for (Genre genre : allGenres) {
            genreComboBox.getItems().add(new GenreOption(genre, genre.getName()));
        }
        genreComboBox.setValue(genreComboBox.getItems().get(0));

        genreComboBox.setOnAction(e -> {
            GenreOption selected = genreComboBox.getValue();
            onGenreFilter.accept(selected != null ? selected.genre() : null);
        });

        Label sortLabel = new Label("Sort by:");
        sortLabel.getStyleClass().add("txt-white-ttl-bld");

        ComboBox<SortStrategy> sortComboBox = new ComboBox<>();
        sortComboBox.setPrefWidth(150);
        sortComboBox.getStyleClass().addAll("combo-box", "txt-white-sm");
        sortComboBox.getItems().addAll(SortStrategy.values());
        sortComboBox.setValue(SortStrategy.ALPHABETICAL);
        sortComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SortStrategy s) {
                return s != null ? s.getDisplayName() : "";
            }
            @Override
            public SortStrategy fromString(String s) {
                return SortStrategy.ALPHABETICAL;
            }
        });
        sortComboBox.setOnAction(e -> {
            if (sortComboBox.getValue() != null)
                onSortChange.accept(sortComboBox.getValue());
        });

        box.getChildren().addAll(
                genreLabel, genreComboBox, sortLabel, sortComboBox);

        return new FilterControls(box, sortComboBox, genreComboBox);
    }

    /**
     * Updates a tab button's style based on whether it is the active tab.
     * Called both during construction and when the active tab changes.
     */
    public static void updateTabStyle(Button btn,
                                      String tabKey,
                                      String currentView) {
        btn.getStyleClass().removeAll("nav-btn", "nav-btn-active");
        btn.getStyleClass().add(
                tabKey.equals(currentView) ? "nav-btn-active" : "nav-btn");
    }

    // Result records

    /**
     * Carries all built nodes back to the controller so it can
     * hold references for later style updates without traversing
     * the scene graph.
     */
    public record Result(
            VBox                    topBar,
            ToggleButton            listToggle,
            ToggleButton            gridToggle,
            HBox                    filterControlsBox,
            ComboBox<SortStrategy>  sortComboBox,
            ComboBox<GenreOption>   genreComboBox
    ) {}

    /**
     * Internal carrier for the filter controls section.
     */
    private record FilterControls(
            HBox                    box,
            ComboBox<SortStrategy>  sortComboBox,
            ComboBox<GenreOption>   genreComboBox
    ) {}

    /**
     * Display wrapper for Genre in the ComboBox.
     * Record replaces the old private static inner class.
     */
    public record GenreOption(Genre genre, String display) {
        @Override
        public String toString() { return display; }
    }
}