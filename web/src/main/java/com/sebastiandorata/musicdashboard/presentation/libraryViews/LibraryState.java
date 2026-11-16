package com.sebastiandorata.musicdashboard.presentation.libraryViews;

import com.sebastiandorata.musicdashboard.utils.SortStrategy;

/**
 * Immutable per-request view state of the My Library page, built from the query string.
 *
 * <p>The desktop kept this state in a mutable object inside a singleton controller. On the web the
 * state lives in the URL, so one instance is created for every {@code GET /library} request by
 * {@link #fromParams(String, String, String, Long, Long, Long)} and handed to the template. The
 * recognised parameters are {@code view} ({@code songs|albums|artists|favourites}), {@code mode}
 * ({@code grid|list}), {@code sort} (a {@link SortStrategy} constant name), {@code genre} (a genre
 * id) and the drill-ins {@code album} and {@code artist} (ids). Missing or invalid values fall back
 * to the desktop defaults: Albums view, grid mode, alphabetical sort, no genre filter.</p>
 *
 * <p>The URL builder methods ({@link #tabUrl(String)}, {@link #modeUrl(String)}, ...) reproduce the
 * way the desktop changed one piece of state and kept or reset the rest: switching tabs resets sort,
 * genre and mode, switching the display mode keeps sort and genre.</p>
 *
 * <p>SRP: only resolves and describes view state; it never touches a repository or service.</p>
 */
public final class LibraryState {

    /** Songs tab key. */
    public static final String SONGS = "songs";
    /** Albums tab key (the default view). */
    public static final String ALBUMS = "albums";
    /** Artists tab key. */
    public static final String ARTISTS = "artists";
    /** Favourites tab key. */
    public static final String FAVOURITES = "favourites";

    private final String view;
    private final String mode;
    private final SortStrategy sort;
    private final Long genreId;
    private final Long albumId;
    private final Long artistId;

    private LibraryState(String view, String mode, SortStrategy sort,
                         Long genreId, Long albumId, Long artistId) {
        this.view = view;
        this.mode = mode;
        this.sort = sort;
        this.genreId = genreId;
        this.albumId = albumId;
        this.artistId = artistId;
    }

    /**
     * Resolves the view state from raw query parameters, applying the desktop defaults.
     *
     * <p>An {@code album} drill-in wins over an {@code artist} drill-in. The artist drill-in forces
     * the Artists tab and defaults to grid mode (desktop {@code drillIntoArtist}); the album drill-in
     * forces list mode (desktop {@code drillIntoAlbum}). Top-level Songs and Artists views are always
     * lists, Albums defaults to grid and Favourites defaults to list, as in the desktop
     * {@code switchView}.</p>
     *
     * @param view   requested tab, may be {@code null} or invalid
     * @param mode   requested display mode, may be {@code null} or invalid
     * @param sort   requested sort strategy name, may be {@code null} or invalid
     * @param genre  requested genre id filter, may be {@code null}
     * @param album  album id to drill into, may be {@code null}
     * @param artist artist id to drill into, may be {@code null}
     * @return the resolved state
     */
    public static LibraryState fromParams(String view, String mode, String sort,
                                          Long genre, Long album, Long artist) {
        String v = isView(view) ? view : ALBUMS;
        SortStrategy s = parseSort(sort);
        boolean gridRequested = "grid".equals(mode);
        boolean listRequested = "list".equals(mode);

        if (album != null) {
            return new LibraryState(v, "list", SortStrategy.ALPHABETICAL, null, album, null);
        }
        if (artist != null) {
            String m = listRequested ? "list" : "grid";
            return new LibraryState(ARTISTS, m, SortStrategy.ALPHABETICAL, null, null, artist);
        }
        String m = switch (v) {
            case ALBUMS -> listRequested ? "list" : "grid";
            case FAVOURITES -> gridRequested ? "grid" : "list";
            default -> "list";
        };
        boolean filters = !ARTISTS.equals(v);
        return new LibraryState(v, m, filters ? s : SortStrategy.ALPHABETICAL,
                filters ? genre : null, null, null);
    }

    private static boolean isView(String v) {
        return SONGS.equals(v) || ALBUMS.equals(v) || ARTISTS.equals(v) || FAVOURITES.equals(v);
    }

    private static SortStrategy parseSort(String name) {
        if (name == null) return SortStrategy.ALPHABETICAL;
        try {
            return SortStrategy.valueOf(name);
        } catch (IllegalArgumentException e) {
            return SortStrategy.ALPHABETICAL;
        }
    }

    /**
     * Returns the active tab key.
     *
     * @return {@code songs}, {@code albums}, {@code artists} or {@code favourites}
     */
    public String getView() { return view; }

    /**
     * Returns the display mode.
     *
     * @return {@code grid} or {@code list}
     */
    public String getMode() { return mode; }

    /**
     * Returns the sort strategy.
     *
     * @return the strategy, never {@code null}
     */
    public SortStrategy getSort() { return sort; }

    /**
     * Returns the genre filter.
     *
     * @return the genre id, or {@code null} for "All Genres"
     */
    public Long getGenreId() { return genreId; }

    /**
     * Returns the album drill-in.
     *
     * @return the album id, or {@code null} when no album is open
     */
    public Long getAlbumId() { return albumId; }

    /**
     * Returns the artist drill-in.
     *
     * @return the artist id, or {@code null} when no artist is open
     */
    public Long getArtistId() { return artistId; }

    /**
     * Tells whether an album detail page is requested.
     *
     * @return {@code true} when {@code album} was supplied
     */
    public boolean isAlbumDetail() { return albumId != null; }

    /**
     * Tells whether an artist detail page is requested.
     *
     * @return {@code true} when {@code artist} was supplied (and no album)
     */
    public boolean isArtistDetail() { return artistId != null; }

    /**
     * Tells whether the "List" toggle is shown (everywhere except album detail).
     *
     * @return {@code true} when the toggle is visible
     */
    public boolean isListToggleVisible() { return !isAlbumDetail(); }

    /**
     * Tells whether the "Grid" toggle is shown (Albums, Favourites and artist detail).
     *
     * @return {@code true} when the toggle is visible
     */
    public boolean isGridToggleVisible() {
        if (isAlbumDetail()) return false;
        return isArtistDetail() || ALBUMS.equals(view) || FAVOURITES.equals(view);
    }

    /**
     * Tells whether the Genre / Sort controls are shown (Songs, Albums and Favourites).
     *
     * @return {@code true} when the filter controls are visible
     */
    public boolean isFilterVisible() {
        return !isAlbumDetail() && !isArtistDetail() && !ARTISTS.equals(view);
    }

    /**
     * Builds the URL of a tab: a fresh state for that view (sort, genre and mode reset).
     *
     * @param tab the tab key
     * @return the relative URL
     */
    public String tabUrl(String tab) {
        return "/library?view=" + tab;
    }

    /**
     * Builds the URL that switches the display mode, keeping view, sort and genre
     * (or the artist drill-in when an artist is open).
     *
     * @param newMode {@code grid} or {@code list}
     * @return the relative URL
     */
    public String modeUrl(String newMode) {
        if (isArtistDetail()) {
            return "/library?artist=" + artistId + "&mode=" + newMode;
        }
        StringBuilder sb = new StringBuilder("/library?view=").append(view).append("&mode=").append(newMode);
        if (sort != SortStrategy.ALPHABETICAL) sb.append("&sort=").append(sort.name());
        if (genreId != null) sb.append("&genre=").append(genreId);
        return sb.toString();
    }

    /**
     * Builds the deep link that opens an album (desktop {@code drillIntoAlbum}).
     *
     * <p>The current tab is carried along unless it is Albums so the tab highlight survives.</p>
     *
     * @param id the album id
     * @return the relative URL
     */
    public String albumUrl(Long id) {
        String url = "/library?album=" + id;
        return ALBUMS.equals(view) ? url : url + "&view=" + view;
    }

    /**
     * Builds the deep link that opens an artist (desktop {@code drillIntoArtist}).
     *
     * @param id the artist id
     * @return the relative URL
     */
    public String artistUrl(Long id) {
        return "/library?artist=" + id;
    }
}
