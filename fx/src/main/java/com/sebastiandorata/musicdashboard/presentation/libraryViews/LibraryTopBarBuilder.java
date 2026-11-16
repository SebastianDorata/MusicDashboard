package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.controller.MainController;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the top bar for the My Library page.
 *
 * <p>Pure UI construction: takes already-resolved data ({@code availableGenres})
 * and callbacks via {@link Config}, and has no dependency on any repository
 * or service. Callers are responsible for fetching genres and wiring the
 * returned {@link Result#tabButtons()} into their own state.</p>
 *
 * <p>SRP: Only responsible for constructing the top bar node.</p>
 */
public class LibraryTopBarBuilder {

    private LibraryTopBarBuilder() {}

    /**
     * Immutable input to {@link #build(Config)}. Construct via {@link #builder()}.
     */
    public static final class Config {
        final List<Genre> availableGenres;
        final String currentView;
        final String currentDisplayMode;
        final Consumer<String> onTabSwitch;
        final Consumer<String> onDisplayMode;
        final Consumer<Genre> onGenreFilter;
        final Consumer<SortStrategy> onSortChange;

        private Config(Builder b) {
            this.availableGenres    = b.availableGenres;
            this.currentView        = b.currentView;
            this.currentDisplayMode = b.currentDisplayMode;
            this.onTabSwitch        = b.onTabSwitch;
            this.onDisplayMode      = b.onDisplayMode;
            this.onGenreFilter      = b.onGenreFilter;
            this.onSortChange       = b.onSortChange;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private List<Genre> availableGenres = List.of();
            private String currentView;
            private String currentDisplayMode;
            private Consumer<String> onTabSwitch;
            private Consumer<String> onDisplayMode;
            private Consumer<Genre> onGenreFilter;
            private Consumer<SortStrategy> onSortChange;

            public Builder availableGenres(List<Genre> v)        { this.availableGenres = v; return this; }
            public Builder currentView(String v)                 { this.currentView = v; return this; }
            public Builder currentDisplayMode(String v)          { this.currentDisplayMode = v; return this; }
            public Builder onTabSwitch(Consumer<String> v)       { this.onTabSwitch = v; return this; }
            public Builder onDisplayMode(Consumer<String> v)     { this.onDisplayMode = v; return this; }
            public Builder onGenreFilter(Consumer<Genre> v)      { this.onGenreFilter = v; return this; }
            public Builder onSortChange(Consumer<SortStrategy> v){ this.onSortChange = v; return this; }

            public Config build() { return new Config(this); }
        }
    }

    /**
     * Carries all built nodes back to the controller, including the tab
     * button map — the builder returns this rather than mutating a
     * caller-supplied map.
     */
    public record Result(
            VBox                    topBar,
            Map<String, Button>     tabButtons,
            ToggleButton            listToggle,
            ToggleButton            gridToggle,
            HBox                    filterControlsBox,
            ComboBox<SortStrategy>  sortComboBox,
            ComboBox<GenreOption>   genreComboBox
    ) {}

    public static Result build(Config config) {
        Map<String, Button> tabButtons = new LinkedHashMap<>();

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
            updateTabStyle(btn, tabKey, config.currentView);
            btn.setOnMouseEntered(e -> {
                if (!tabKey.equals(config.currentView))
                    btn.getStyleClass().add("btn-enter");
            });
            btn.setOnMouseExited(e ->
                    btn.getStyleClass().remove("btn-enter"));
            btn.setOnAction(e -> config.onTabSwitch.accept(tabKey));
            tabButtons.put(tabKey, btn);
            leftGroup.getChildren().add(btn);
        }

        // List/Grid toggles
        ToggleGroup viewGroup = new ToggleGroup();

        ToggleButton listToggle = new ToggleButton("List");
        listToggle.setToggleGroup(viewGroup);
        listToggle.getStyleClass().addAll("nav-btn", "txt-white-md-bld");
        listToggle.setOnAction(e -> config.onDisplayMode.accept("list"));

        ToggleButton gridToggle = new ToggleButton("Grid");
        gridToggle.setToggleGroup(viewGroup);
        gridToggle.setSelected("grid".equals(config.currentDisplayMode));
        gridToggle.getStyleClass().addAll("nav-btn-active", "txt-white-md-bld");
        gridToggle.setOnAction(e -> config.onDisplayMode.accept("grid"));

        HBox centerGroup = new HBox(10, listToggle, gridToggle);
        centerGroup.setAlignment(Pos.CENTER);

        // Filter controls on the right
        FilterControls filterControls = buildFilterControls(
                config.availableGenres, config.onGenreFilter, config.onSortChange);

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
                tabButtons,
                listToggle,
                gridToggle,
                filterControls.box(),
                filterControls.sortComboBox(),
                filterControls.genreComboBox()
        );
    }

    private static FilterControls buildFilterControls(
            List<Genre>             availableGenres,
            Consumer<Genre>        onGenreFilter,
            Consumer<SortStrategy> onSortChange) {

        HBox box = new HBox(15);
        box.getStyleClass().addAll("dropDown-options");

        Label genreLabel = new Label("Genre:");
        genreLabel.getStyleClass().add("txt-white-ttl-bld");

        ComboBox<GenreOption> genreComboBox = new ComboBox<>();
        genreComboBox.setPrefWidth(150);
        genreComboBox.getStyleClass().addAll("combo-box", "txt-white-sm");

        // Populate genres from already-resolved data — no repository access here
        genreComboBox.getItems().add(new GenreOption(null, "All Genres"));
        for (Genre genre : availableGenres) {
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

    /** Internal carrier for the filter controls section. */
    private record FilterControls(
            HBox                    box,
            ComboBox<SortStrategy>  sortComboBox,
            ComboBox<GenreOption>   genreComboBox
    ) {}

    /** Display wrapper for Genre in the ComboBox. */
    public record GenreOption(Genre genre, String display) {
        @Override
        public String toString() { return display; }
    }
}