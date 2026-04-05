package com.sebastiandorata.musicdashboard.presentation.helpers;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Builds the shared sidebar structure used by DashboardController
 * and AnalyticsController.
 *
 * <p><b><u>Both sidebars share:</u></b></p>
 * <ul>
 *   <li>App title (via AppUtils.SideBarTitle()).</li>
 *   <li>Nav section with label and nav buttons.</li>
 *   <li>Optional extra content slot (e.g. year selector in Analytics).</li>
 *   <li>Spacer and optional back button pinned to the bottom.</li>
 * </ul>
 *
 * <p>SRP: Only responsible for sidebar layout assembly.</p>
 * <p>OCP: Callers supply their own NavEntry lists and optional extras,
 * no modification needed to add new pages.</p>
 *
 * <p>Time Complexity: O(n) where n = number of nav entries.</p>
 * <p>Space Complexity: O(n).</p>
 */
public class SidebarBuilder {

    /**
     * A single navigation entry in the sidebar.
     *
     * @param icon      emoji or symbol shown before the label
     * @param label     display text
     * @param activeKey the route key used to determine if this item is active
     * @param action    what happens when the button is clicked
     */
    public record NavEntry(String icon, String label, String activeKey, Runnable action) {}

    /**
     * Builds a complete sidebar VBox.
     *
     * @param styleClasses CSS classes to apply to the sidebar root (e.g. "panels", "sidebar")
     * @param sectionTitle label shown above the nav entries (e.g. "Reports", null to omit)
     * @param entries list of nav entries
     * @param activeKey the currently active route key, matching entry gets nav-btn-active
     * @param backLabel label shown above the back button section (e.g. "Return home"). Pass null to omit the back section entirely.
     * @param extraContent optional VBox inserted below the nav entries (e.g. year selector). Pass null to omit.
     * @param backAction what happens when the back button is clicked. Pass null if no back button.
     * @return the fully assembled sidebar VBox
     */
    public static VBox build(
                             List<String>    styleClasses,
                             String          sectionTitle,
                             boolean         separatorBefore,
                             List<NavEntry>  entries,
                             String          activeKey,
                             boolean         separatorAfter,
                             VBox            extraContent,
                             String          backLabel,
                             String          backButtonText,
                             Runnable        backAction) {

        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.getStyleClass().addAll(styleClasses);
        sidebar.setMinWidth(220);
        sidebar.setPrefWidth(250);
        sidebar.setMaxWidth(300);
        sidebar.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(sidebar, Priority.ALWAYS);


        sidebar.getChildren().add(SideBarTitle());

        if (sectionTitle != null) {
            Label sectionLabel = new Label(sectionTitle);
            sectionLabel.getStyleClass().add("sidebar-section-label");
            sidebar.getChildren().add(sectionLabel);
        }
        if (separatorBefore) {
            Separator sep = new Separator();
            sep.getStyleClass().add("sidebar-sep");
            sidebar.getChildren().add(sep);
        }


        //Nav entries
        for (NavEntry entry : entries) {
            Button btn = buildNavButton(entry.icon(), entry.label());
            if (entry.activeKey() != null && entry.activeKey().equals(activeKey)) {
                btn.getStyleClass().add("nav-btn-active");
            }
            btn.setOnAction(e -> entry.action().run());
            sidebar.getChildren().add(btn);
        }

        //  Optional extra content
        if (extraContent != null) {
            if (separatorAfter) {
                Separator sep = new Separator();
                sep.getStyleClass().add("sidebar-sep");
                sidebar.getChildren().add(sep);
            }
            sidebar.getChildren().add(extraContent);
        }

        //Optional back section
            if (backLabel != null) {
                Label label = new Label(backLabel);
                label.getStyleClass().addAll("sidebar-section-label");
                sidebar.getChildren().add(label);

            Button backBtn = new Button(backButtonText);
            backBtn.getStyleClass().addAll("nav-btn-back","txt-white-sm-bld");
            backBtn.setMaxWidth(Double.MAX_VALUE);
            backBtn.setOnAction(e -> backAction.run());
            sidebar.getChildren().add(backBtn);
        }

        return sidebar;
    }

    /**
     * Builds a single nav button with icon + label.
     * Caller adds active/inactive style classes after this returns.
     */
    public static Button buildNavButton(String icon, String text) {
        Button btn = new Button(icon + "   " + text);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }
    public static VBox SideBarTitle() {
        VBox box = new VBox(10);
        Label music  = new Label("Music");
        music.getStyleClass().add("txt-white-bld-thirty");

        Label dash = new Label("Dashboard");
        dash.getStyleClass().add("sidebar-title-accent");

        HBox nameRow = new HBox(0, music, dash);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().add(nameRow);
        return box;
    }


}