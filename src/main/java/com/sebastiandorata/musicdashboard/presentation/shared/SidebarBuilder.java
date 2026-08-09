package com.sebastiandorata.musicdashboard.presentation.shared;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Generic, reusable sidebar builder.
 *
 * <p>Knows nothing about the application's specific pages — only how to lay
 * out a title, one or two groups of nav buttons, an optional extra-content
 * slot, and an optional back-navigation section.
 *
 * <p>Any controller that needs its own sidebar (the standard app nav, or a
 * page-specific one like Analytics' "My Reports" panel) builds a
 * {@link SidebarConfig} and calls {@link #build(SidebarConfig)}. For the
 * app's standard Dashboard/Library/Playlist/... tabs, use {@link AppSidebar}
 * instead of redefining that entry list at another call site.
 */
public class SidebarBuilder {

    private SidebarBuilder() {}

    /** A single navigation entry in the sidebar. */
    public record NavEntry(String icon, String label, String activeKey, Runnable action) {}

    /** Optional bottom-of-sidebar back-navigation section. */
    public record BackSection(String sectionLabel, String buttonText, Runnable action) {}

    /** Immutable sidebar configuration. Construct via {@link #builder()}. */
    public static final class SidebarConfig {
        final List<String> styleClasses;
        final String activeKey;
        final List<NavEntry> primaryEntries;
        final List<NavEntry> secondaryEntries;
        final boolean separatorAfterPrimary;
        final Node extraContent;
        final BackSection backSection;

        private SidebarConfig(Builder b) {
            this.styleClasses = b.styleClasses;
            this.activeKey = b.activeKey;
            this.primaryEntries = b.primaryEntries;
            this.secondaryEntries = b.secondaryEntries;
            this.separatorAfterPrimary = b.separatorAfterPrimary;
            this.extraContent = b.extraContent;
            this.backSection = b.backSection;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private List<String> styleClasses = List.of("panels", "sidebar");
            private String activeKey;
            private List<NavEntry> primaryEntries = List.of();
            private List<NavEntry> secondaryEntries;
            private boolean separatorAfterPrimary = true;
            private Node extraContent;
            private BackSection backSection;

            public Builder styleClasses(List<String> v)          { this.styleClasses = v; return this; }
            public Builder activeKey(String v)                   { this.activeKey = v; return this; }
            public Builder primaryEntries(List<NavEntry> v)       { this.primaryEntries = v; return this; }
            public Builder secondaryEntries(List<NavEntry> v)     { this.secondaryEntries = v; return this; }
            public Builder separatorAfterPrimary(boolean v)       { this.separatorAfterPrimary = v; return this; }
            public Builder extraContent(Node v)                  { this.extraContent = v; return this; }
            public Builder backSection(BackSection v)             { this.backSection = v; return this; }

            public SidebarConfig build() { return new SidebarConfig(this); }
        }
    }

    public static VBox build(SidebarConfig config) {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20, 12, 5, 12));
        sidebar.getStyleClass().addAll(config.styleClasses);
        sidebar.setMinWidth(220);
        sidebar.setPrefWidth(250);
        sidebar.setMaxWidth(300);
        sidebar.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(sidebar, Priority.ALWAYS);

        sidebar.getChildren().add(sidebarTitle());
        sidebar.getChildren().add(separator());

        addEntries(sidebar, config.primaryEntries, config.activeKey);

        if (config.separatorAfterPrimary) {
            sidebar.getChildren().add(separator());
        }

        if (config.secondaryEntries != null) {
            addEntries(sidebar, config.secondaryEntries, config.activeKey);
        }

        if (config.extraContent != null) {
            sidebar.getChildren().add(config.extraContent);
        }

        if (config.backSection != null) {
            Label label = new Label(config.backSection.sectionLabel());
            label.getStyleClass().add("sidebar-section-label");
            sidebar.getChildren().add(label);

            Button backBtn = new Button(config.backSection.buttonText());
            backBtn.getStyleClass().addAll("nav-btn-back", "txt-white-sm-bld");
            backBtn.setMaxWidth(Double.MAX_VALUE);
            backBtn.setOnAction(e -> config.backSection.action().run());
            sidebar.getChildren().add(backBtn);
        }

        return sidebar;
    }

    private static void addEntries(VBox sidebar, List<NavEntry> entries, String activeKey) {
        for (NavEntry entry : entries) {
            Button btn = buildNavButton(entry.icon(), entry.label());
            if (entry.activeKey() != null && entry.activeKey().equals(activeKey)) {
                btn.getStyleClass().add("nav-btn-active");
            }
            btn.setOnAction(e -> entry.action().run());
            sidebar.getChildren().add(btn);
        }
    }

    private static Separator separator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("sidebar-sep");
        return sep;
    }

    public static Button buildNavButton(String icon, String text) {
        Button btn = new Button(icon + "   " + text);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    public static VBox sidebarTitle() {
        VBox box = new VBox(10);
        Label music = new Label("Music");
        music.getStyleClass().add("txt-white-bld-thirty");

        Label dash = new Label("Dashboard");
        dash.getStyleClass().add("sidebar-title-accent");

        HBox nameRow = new HBox(0, music, dash);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().add(nameRow);
        return box;
    }
}