[← Back to root](../../../../../README.md)

# css — web stylesheets

## Behavior

Web translations of the desktop JavaFX stylesheets. File names and class names match the desktop app, and the JavaFX `-fx-*` properties are rewritten as plain CSS. `templates/fragments/layout.html` loads three sheets on every page, in this order: `globalStyle.css` (reset, typography and shared utility classes), `buttons.css` (every button and combo-box style) and `musicPlayer.css` (the player panel). Each page then adds its own sheets through the `links` parameter of the `head` fragment. Every sheet is mobile-first: base rules suit a phone, `@media (min-width: 768px)` adds tablet changes, and `@media (min-width: 1280px)` restores the original desktop-port layout. Only these two breakpoints are used. The files are:

- `globalStyle.css` — reset, text sizes and colours, panels and shared utility classes (`txt-*`, `dark-table`, `dark-page-bg`, `dropZone`, `touch-only`); loaded everywhere. It also owns the shell below 1280px: the document scrolls instead of a fixed 100vh shell, `--mobile-bar-h` sets the height of the top bar (`.mobile-bar`, `.mobile-menu-btn`), the sidebar (`.sidebar`) is an off-canvas drawer opened by the `sidebar-open` class on `<body>` over the `.sidebar-backdrop`, and dialogs, popup menus and toasts are sized for a phone.
- `buttons.css` — single source for button and combo-box styles (`green-btn`, `btn-red`, `view-All`, `month-btn`, `player-btn`); loaded everywhere. Below 1280px every button a finger has to hit is at least 44px tall, and a "Desktop sizes" block at the end restores the original sizes.
- `musicPlayer.css` — the player panel in its two sizes (`now-playing-bar`, `player-large`, `player-small`); loaded everywhere. On phones the large player stacks its art above the text, and the small player is a compact bar stuck to the bottom of the screen. From 768px the small bar is one row with time labels, and from 1280px both are the original desktop sizes.
- `dashboard.css` — dashboard panels (`panels`) and their layout.
- `graph.css` — the listening chart; the SVG elements drawn by `dashboard.js` carry the desktop class names.
- `analytics.css` — row labels and rows of the My Reports lists.
- `reports.css` — page layout shared by the My Reports sections (`year-selector`).
- `wrapped.css` — the Yearly Wrapped slideshow (`wrapped-view`, `wrapped-slides`).
- `library.css` — My Library: album and artist detail headers, grids, lists and the top bar.
- `playlist.css` — the My Playlist page layout (`playlist-page`, `playlist-content`).
- `import.css` — the Import Music page (`import-page`).
- `login.css` — the sign-in page (`main-background`, `login-page`, `login-card`).

On phones, the page sheets stack their panels in one column and reflow their card grids. In `library.css` the alphabet bar becomes a horizontal strip stuck under the top bar. In `import.css` the results table becomes stacked rows below 768px. In `reports.css` and `analytics.css` report rows wrap onto two lines, and the weekly calendar and month buttons scale to the screen width. The original desktop layout is restored at 1280px.

## Responsibilities

- Owns: how things look — colours, sizes, spacing, layout and the visual states of the page parts.
- Does **not** own: markup (goes in `templates/`), behaviour such as clicks, timers or the chart drawing (goes in `static/js/`), or the data shown (comes from the controllers). A new page gets its own sheet here, and shared classes go in `globalStyle.css` or `buttons.css`, not copied into page sheets.

## S.O.L.I.D. principles used

Not applicable — no logic in this folder. These files are declarative style rules with no behaviour to extend or substitute.

## Code reused

Shared classes are reused across pages. `globalStyle.css` and `buttons.css` apply to every page. `dashboard.css` and `analytics.css` are loaded by both `dashboard.html` and `analytics.html`, `wrapped.css` and `reports.css` by `analytics.html`, and `graph.css` by `dashboard.html`. The sheets are reused by reference through the `<link>` tags in the templates, not by import.

## Table of contents

- [globalStyle.css](globalStyle.css) — reset, text, panels and shared utility classes (every page)
- [buttons.css](buttons.css) — all button and combo-box styles (every page)
- [musicPlayer.css](musicPlayer.css) — player panel in its large and small sizes (every page)
- [dashboard.css](dashboard.css) — dashboard panels and layout
- [graph.css](graph.css) — listening chart layout and series colours
- [analytics.css](analytics.css) — My Reports list rows
- [reports.css](reports.css) — My Reports page layout and year selector
- [wrapped.css](wrapped.css) — Yearly Wrapped slideshow
- [library.css](library.css) — My Library layout, grids and lists
- [playlist.css](playlist.css) — My Playlist layout
- [import.css](import.css) — Import Music layout
- [login.css](login.css) — sign-in page layout
- [← Back to static](../README.md)
- [← Back to root](../../../../../README.md)

## Sequence diagram

```mermaid
sequenceDiagram
    participant Browser
    participant Head
    participant Static
    Browser->>Head: parse head from layout fragment
    Head-->>Browser: link globalStyle, buttons, musicPlayer, then page sheet
    Browser->>Static: GET /css/globalStyle.css
    Static-->>Browser: base rules
    Browser->>Static: GET /css/buttons.css
    Static-->>Browser: button rules
    Browser->>Static: GET /css/musicPlayer.css
    Static-->>Browser: player rules
    Browser->>Static: GET /css/library.css
    Static-->>Browser: page rules
    Note over Browser: later sheets override earlier ones with the same selector
```
