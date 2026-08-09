package com.sebastiandorata.musicdashboard.presentation.shared;

import com.sebastiandorata.musicdashboard.controller.MainController;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The application's standard sidebar — the Dashboard/Library/Playlist/
 * Import/Reports tabs plus the Settings/Export/Link Account group — shown
 * on every top-level page except Analytics, which defines its own
 * page-specific sidebar directly via {@link SidebarBuilder}.
 *
 * <p>This is the single place that entry list is defined. Callers pass
 * their own route key so the active tab highlights correctly — previously
 * this was tracked via a static field that was never actually wired to the
 * highlighting logic.
 */
public class AppSidebar {

    private AppSidebar() {}

    public static VBox build(String activeKey) {
        List<SidebarBuilder.NavEntry> primary = List.of(
                new SidebarBuilder.NavEntry("⌂", "My Dashboard", "dashboard", () -> MainController.navigateTo("dashboard")),
                new SidebarBuilder.NavEntry("♫", "My Library",   "library",   () -> MainController.navigateTo("library")),
                new SidebarBuilder.NavEntry("≡", "My Playlist",  "playlist",  () -> MainController.navigateTo("playlist")),
                new SidebarBuilder.NavEntry("↓", "Import Files", "import",    () -> MainController.navigateTo("import")),
                new SidebarBuilder.NavEntry("◫", "My Reports",   "analytics", () -> MainController.navigateTo("analytics"))
        );

        List<SidebarBuilder.NavEntry> secondary = List.of(
                new SidebarBuilder.NavEntry("⚙", "Settings",     "settings",    () -> MainController.navigateTo("settings")),
                new SidebarBuilder.NavEntry("↑", "Export Data",  "ExportData",  () -> MainController.navigateTo("ExportData")),
                new SidebarBuilder.NavEntry("⊕", "Link Account", "LinkAccount", () -> MainController.navigateTo("LinkAccount"))
        );

        SidebarBuilder.SidebarConfig config = SidebarBuilder.SidebarConfig.builder()
                .activeKey(activeKey)
                .primaryEntries(primary)
                .secondaryEntries(secondary)
                .build();

        return SidebarBuilder.build(config);
    }
}