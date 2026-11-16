package com.sebastiandorata.musicdashboard.controller;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.presentation.libraryViews.LibraryState;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import com.sebastiandorata.musicdashboard.service.FavouriteService;
import com.sebastiandorata.musicdashboard.service.GenreFilterService;
import com.sebastiandorata.musicdashboard.service.Import.SongImportService;
import com.sebastiandorata.musicdashboard.service.LibraryService;
import com.sebastiandorata.musicdashboard.utils.SortStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Serves the My Library page ({@code GET /library}) and its JSON endpoints under
 * {@code /api/library/}.
 *
 * <p>The desktop controller was a singleton that held a {@code LibraryState} and rebuilt JavaFX
 * nodes on every click. On the web the state is the URL query string (see {@link LibraryState});
 * every request builds a fresh state, loads the data for exactly one pane (Songs, Albums, Artists,
 * Favourites, album detail or artist detail), converts it to small immutable view records and hands
 * those to Thymeleaf. Nothing per-user or per-page is kept in a field.</p>
 *
 * <p>JSON endpoints: favourite toggle, song read / edit / delete and album edit / delete. Every
 * endpoint that changes library data invalidates the {@link LibraryService} cache, both immediately
 * and again after the surrounding transaction commits so a concurrent reader cannot re-cache stale
 * rows.</p>
 *
 * <p>Detached-entity safety: the cached lists of {@link LibraryService} are rendered only through
 * associations their repository queries fetch-join (song artists and genres, album artists, songs,
 * song artists and genres, artist songs and albums); the song's album is an eager {@code ManyToOne}.
 * Everything else (album detail, favourites, artist albums, edit endpoints) is loaded fresh during
 * the current request.</p>
 *
 * <p>SRP: translates HTTP requests into library service calls and view models.</p>
 * <p>Time Complexity: O(n log n) per page render for n items (sorting and grouping).</p>
 * <p>Space Complexity: O(n) for the view records of the rendered pane.</p>
 */
@Controller
public class MyLibraryController {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** Letters of the alphabet bar, in display order. */
    private static final List<String> LETTERS = Arrays.stream("ABCDEFGHIJKLMNOPQRSTUVWXYZ#".split(""))
            .collect(Collectors.toUnmodifiableList());

    @Autowired private LibraryService libraryService;
    @Autowired private SongImportService songImportService;
    @Autowired private FavouriteService favouriteService;
    @Autowired private GenreFilterService genreFilterService;
    @Autowired private SongRepository songRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private GenreRepository genreRepository;

    // ───────────────────────── View records ─────────────────────────

    /**
     * An album as shown in cards, rows and the edit dialog.
     *
     * @param id        album id
     * @param title     album title (never {@code null})
     * @param year      release year, or {@code null}
     * @param songCount number of songs in the album
     * @param genre     the first genre of the first song that has one, or {@code null}
     * @param hasArt    whether the album has cover art
     */
    public record AlbumItem(Long id, String title, Integer year, int songCount, String genre, boolean hasArt) {
        /**
         * Year text for the grid-card subtitle.
         *
         * @return the year, or an em dash when unknown
         */
        public String cardYear() { return year != null ? String.valueOf(year) : "—"; }

        /**
         * Info line for list rows.
         *
         * @return "N songs • year" with "Unknown" for a missing year
         */
        public String info() { return songCount + " songs • " + (year != null ? year : "Unknown"); }

        /**
         * Year text for the edit dialog.
         *
         * @return the year, or an empty string
         */
        public String yearText() { return year != null ? String.valueOf(year) : ""; }

        /**
         * Genre text for the edit dialog.
         *
         * @return the genre name, or an empty string
         */
        public String genreText() { return genre != null ? genre : ""; }
    }

    /**
     * A song as shown in list rows, grid cards and album detail rows.
     *
     * @param id       song id
     * @param title    song title with the desktop fallback ("Unknown Title")
     * @param artist   first artist name or "Unknown Artist"
     * @param albumId  album id, or {@code null}
     * @param hasArt   whether the song's album has cover art
     * @param track    track label ("—" when absent), used by the album detail rows
     * @param plays    play-count label such as "3 plays"
     * @param date     date first listened formatted "MMM d, yyyy", or "—"
     */
    public record SongItem(Long id, String title, String artist, Long albumId, boolean hasArt,
                           String track, String plays, String date) { }

    /**
     * An artist row of the Artists tab.
     *
     * @param id         artist id
     * @param name       artist name
     * @param songCount  number of songs
     * @param albumCount number of directly linked albums
     */
    public record ArtistItem(Long id, String name, int songCount, int albumCount) {
        /**
         * Info line of the artist row.
         *
         * @return "N songs • M albums"
         */
        public String info() { return songCount + " songs • " + albumCount + " albums"; }
    }

    /**
     * A section of an alphabetically grouped list.
     *
     * @param letter the divider letter ("#" for non-letters)
     * @param items  the rows under that divider
     * @param <T>    the row type
     */
    public record Group<T>(String letter, List<T> items) { }

    /**
     * The album detail page model.
     *
     * @param id            album id
     * @param title         album title
     * @param hasArt        whether the album has cover art
     * @param artistNames   comma-joined artist names, or {@code null} when there are none
     * @param firstArtistId id of the first artist (target of the artist link), or {@code null}
     * @param songs         the songs sorted by track number
     */
    public record AlbumDetail(Long id, String title, boolean hasArt, String artistNames,
                              Long firstArtistId, List<SongItem> songs) { }

    /**
     * Details of one song as needed by the edit dialog and the context menu.
     *
     * @param id          song id
     * @param title       title
     * @param album       album title or empty text
     * @param artists     comma-separated artist names
     * @param genres      comma-separated genre names
     * @param trackNumber track number text or empty text
     * @param favourited  whether the current user has favourited the song
     */
    public record SongDetails(Long id, String title, String album, String artists, String genres,
                              String trackNumber, boolean favourited) { }

    /**
     * Request body of {@code PUT /api/library/songs/{id}} (the {@code SongEditDialog} fields).
     *
     * @param title       new title (blank keeps the old one)
     * @param album       new album title (blank keeps the old one)
     * @param artists     comma-separated artist names (blank keeps the old ones)
     * @param genres      comma-separated genre names (blank keeps the old ones)
     * @param trackNumber new track number text (blank or unparsable keeps the old one)
     */
    public record SongEditRequest(String title, String album, String artists, String genres, String trackNumber) { }

    /**
     * Request body of {@code PUT /api/library/albums/{id}} (the {@code AlbumEditDialog.Result}).
     *
     * @param title       new title, required
     * @param releaseYear new release year, or {@code null} to clear it
     * @param genre       genre applied to every song of the album, or blank for none
     */
    public record AlbumEditRequest(String title, Integer releaseYear, String genre) { }

    // ───────────────────────── Page ─────────────────────────

    /**
     * Renders the My Library page for the view state encoded in the query string.
     *
     * <p>Model attributes: {@code state} ({@link LibraryState}), {@code pane}
     * ({@code songs|albums|artists|favourites|albumDetail|artistDetail}), {@code genres} (all
     * genres), {@code sorts} (all sort strategies), {@code header} (the view header text), plus
     * pane-specific data: {@code groups} and {@code activeLetters} (alphabetical lists),
     * {@code albums} (album cards), {@code songs} (favourites grid), {@code total},
     * {@code albumDetail}, {@code artistName}, {@code artistAlbums} and {@code queueIds} (the sorted
     * song ids a double-click queues, comma-separated).</p>
     *
     * @param view   tab key
     * @param mode   display mode
     * @param sort   sort strategy name
     * @param genre  genre id filter
     * @param album  album id to drill into
     * @param artist artist id to drill into
     * @param model  the MVC model
     * @return the {@code library} template, or a redirect to the default view for an unknown id
     */
    @GetMapping("/library")
    public String library(@RequestParam(required = false) String view,
                          @RequestParam(required = false) String mode,
                          @RequestParam(required = false) String sort,
                          @RequestParam(required = false) Long genre,
                          @RequestParam(required = false) Long album,
                          @RequestParam(required = false) Long artist,
                          Model model) {
        LibraryState state = LibraryState.fromParams(view, mode, sort, genre, album, artist);
        model.addAttribute("state", state);
        model.addAttribute("genres", genreRepository.findAll());
        model.addAttribute("sorts", SortStrategy.values());
        model.addAttribute("letters", LETTERS);

        if (state.isAlbumDetail()) {
            AlbumDetail detail = loadAlbumDetail(state.getAlbumId());
            if (detail == null) return "redirect:/library";
            model.addAttribute("pane", "albumDetail");
            model.addAttribute("albumDetail", detail);
            return "library";
        }
        if (state.isArtistDetail()) {
            Artist found = artistRepository.findById(state.getArtistId()).orElse(null);
            if (found == null) return "redirect:/library";
            model.addAttribute("pane", "artistDetail");
            model.addAttribute("artistName", found.getName());
            List<AlbumItem> albums = libraryService.resolveAlbumsForArtist(found).stream()
                    .map(this::toAlbumItem).toList();
            model.addAttribute("artistAlbums", albums);
            return "library";
        }

        Genre genreFilter = state.getGenreId() == null ? null
                : genreRepository.findById(state.getGenreId()).orElse(null);
        switch (state.getView()) {
            case LibraryState.SONGS -> loadSongs(state, genreFilter, model);
            case LibraryState.ARTISTS -> loadArtists(model);
            case LibraryState.FAVOURITES -> loadFavourites(state, genreFilter, model);
            default -> loadAlbums(state, genreFilter, model);
        }
        return "library";
    }

    private void loadSongs(LibraryState state, Genre genre, Model model) {
        List<Song> songs = genreFilterService.filterSongsByGenre(songImportService.getAllSongs(), genre);
        List<Song> sorted = songs.stream().sorted(state.getSort().getSongComparator()).toList();
        model.addAttribute("pane", "songs");
        model.addAttribute("header", "All Songs (" + songs.size() + ")");
        addSongGroups(sorted, model);
    }

    private void loadFavourites(LibraryState state, Genre genre, Model model) {
        List<Song> favourites;
        try {
            favourites = favouriteService.getUserFavouritesSortedByDate();
        } catch (Exception e) {
            favourites = List.of();
        }
        List<Song> sorted = genreFilterService.filterSongsByGenre(favourites, genre).stream()
                .sorted(state.getSort().getSongComparator()).toList();
        model.addAttribute("pane", "favourites");
        model.addAttribute("header", "Favourites (" + sorted.size() + ")");
        model.addAttribute("total", sorted.size());
        if ("grid".equals(state.getMode())) {
            model.addAttribute("songs", sorted.stream().map(s -> toSongItem(s)).toList());
        } else {
            addSongGroups(sorted, model);
        }
    }

    private void loadAlbums(LibraryState state, Genre genre, Model model) {
        List<Album> albums = genreFilterService.filterAlbumsByGenre(libraryService.getAllAlbums(), genre);
        List<AlbumItem> sorted = albums.stream()
                .sorted(state.getSort().getAlbumComparator())
                .map(this::toAlbumItem).toList();
        model.addAttribute("pane", "albums");
        model.addAttribute("header", "All Albums (" + albums.size() + ")");
        model.addAttribute("albums", sorted);
        if ("list".equals(state.getMode())) {
            Map<String, List<AlbumItem>> grouped = new LinkedHashMap<>();
            for (AlbumItem item : sorted) {
                grouped.computeIfAbsent(letterKey(item.title()), k -> new ArrayList<>()).add(item);
            }
            addGroups(grouped, model);
        }
    }

    private void loadArtists(Model model) {
        List<Artist> artists = libraryService.getAllArtists();
        List<Artist> sorted = artists.stream()
                .filter(a -> a.getName() != null && !a.getName().isBlank())
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())).toList();
        Map<String, List<ArtistItem>> grouped = new LinkedHashMap<>();
        for (Artist a : sorted) {
            String letter = a.getName().substring(0, 1).toUpperCase();
            if (!Character.isLetter(letter.charAt(0))) letter = "#";
            grouped.computeIfAbsent(letter, k -> new ArrayList<>()).add(new ArtistItem(
                    a.getArtistId(), a.getName(),
                    a.getSongs() != null ? a.getSongs().size() : 0,
                    a.getAlbums() != null ? a.getAlbums().size() : 0));
        }
        model.addAttribute("pane", "artists");
        model.addAttribute("header", "Artists (" + artists.size() + ")");
        addGroups(grouped, model);
    }

    private void addSongGroups(List<Song> sorted, Model model) {
        model.addAttribute("queueIds", sorted.stream().map(s -> String.valueOf(s.getSongID()))
                .collect(Collectors.joining(",")));
        Map<String, List<SongItem>> grouped = new TreeMap<>();
        for (Song s : sorted) {
            grouped.computeIfAbsent(letterKey(s.getTitle()), k -> new ArrayList<>()).add(toSongItem(s));
        }
        addGroups(grouped, model);
    }

    private <T> void addGroups(Map<String, List<T>> grouped, Model model) {
        List<Group<T>> groups = grouped.entrySet().stream()
                .map(e -> new Group<>(e.getKey(), e.getValue())).toList();
        model.addAttribute("groups", groups);
        model.addAttribute("activeLetters", new ArrayList<>(grouped.keySet()));
    }

    /**
     * Returns the uppercase first letter of a title, or "#" for blank or non-letter titles.
     */
    private String letterKey(String title) {
        if (title == null || title.isBlank()) return "#";
        char c = Character.toUpperCase(title.trim().charAt(0));
        return Character.isLetter(c) ? String.valueOf(c) : "#";
    }

    private AlbumItem toAlbumItem(Album a) {
        String genre = null;
        if (a.getSongs() != null) {
            genre = a.getSongs().stream()
                    .filter(s -> s.getGenres() != null && !s.getGenres().isEmpty())
                    .findFirst()
                    .map(s -> s.getGenres().iterator().next().getName())
                    .orElse(null);
        }
        return new AlbumItem(a.getAlbumId(), a.getTitle() != null ? a.getTitle() : "",
                a.getReleaseYear(), a.getSongs() != null ? a.getSongs().size() : 0,
                genre, hasArt(a));
    }

    private SongItem toSongItem(Song s) {
        String artist = "Unknown Artist";
        if (s.getArtists() != null && !s.getArtists().isEmpty()) {
            artist = s.getArtists().stream().findFirst().map(Artist::getName).orElse("Unknown Artist");
            if (artist == null) artist = "Unknown Artist";
        }
        Integer tn = s.getTrackNum();
        int pc = s.getListenCount() != null ? s.getListenCount() : 0;
        String date = s.getDateFirstListened() != null ? s.getDateFirstListened().format(DATE_FORMAT) : "—";
        Album album = s.getAlbum();
        return new SongItem(s.getSongID(), s.getTitle() != null ? s.getTitle() : "Unknown Title", artist,
                album != null ? album.getAlbumId() : null, album != null && hasArt(album),
                (tn != null && tn > 0) ? String.valueOf(tn) : "—", pc + " plays", date);
    }

    private boolean hasArt(Album a) {
        return a.getAlbumArtPath() != null && !a.getAlbumArtPath().isBlank();
    }

    /**
     * Loads one album with its songs and artists fresh from the database (never from the cache).
     */
    private AlbumDetail loadAlbumDetail(Long albumId) {
        Album album;
        try {
            album = libraryService.getAlbumWithFullDetails(albumId);
        } catch (IllegalStateException e) {
            return null;
        }
        List<Artist> artists;
        if (album.getArtists() != null && !album.getArtists().isEmpty()) {
            artists = new ArrayList<>(album.getArtists());
        } else if (album.getSongs() == null) {
            artists = List.of();
        } else {
            artists = album.getSongs().stream()
                    .filter(s -> s.getArtists() != null)
                    .flatMap(s -> s.getArtists().stream())
                    .filter(a -> a != null && a.getName() != null)
                    .distinct().collect(Collectors.toList());
        }
        List<SongItem> songs = album.getSongs() == null ? List.of() : album.getSongs().stream()
                .sorted(Comparator.<Song>comparingInt(s -> s.getTrackNum() != null && s.getTrackNum() > 0
                                ? s.getTrackNum() : Integer.MAX_VALUE)
                        .thenComparing(s -> s.getTitle() != null ? s.getTitle() : "", String.CASE_INSENSITIVE_ORDER))
                .map(s -> toSongItem(s)).toList();
        return new AlbumDetail(album.getAlbumId(), album.getTitle(), hasArt(album),
                artists.isEmpty() ? null : artists.stream().map(Artist::getName).collect(Collectors.joining(", ")),
                artists.isEmpty() ? null : artists.get(0).getArtistId(), songs);
    }

    // ───────────────────────── JSON: favourites & songs ─────────────────────────

    /**
     * Toggles the current user's favourite flag on a song.
     *
     * @param songId the song id
     * @return {@code {"favourited": true|false}} with the new state
     * @throws ResponseStatusException {@code 404} when the song does not exist
     */
    @PostMapping("/api/library/songs/{songId}/favourite")
    @ResponseBody
    public Map<String, Boolean> toggleFavourite(@PathVariable Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found."));
        try {
            return Map.of("favourited", favouriteService.toggleFavourite(song));
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Returns the fields the Edit Song dialog pre-fills, plus the favourite flag.
     *
     * @param songId the song id
     * @return the song details
     * @throws ResponseStatusException {@code 404} when the song does not exist
     */
    @GetMapping("/api/library/songs/{songId}")
    @ResponseBody
    public SongDetails getSong(@PathVariable Long songId) {
        return toDetails(loadSong(songId));
    }

    /**
     * Applies a song edit with the {@code SongEditDialog} save logic.
     *
     * <p>Only non-blank fields are applied. The album is looked up by title or created, artists and
     * genres are comma-separated names looked up or created by name, and the track number is parsed
     * leniently (an unparsable value is ignored).</p>
     *
     * @param songId  the song id
     * @param request the edited fields
     * @return the song details after saving
     * @throws ResponseStatusException {@code 404} when the song does not exist
     */
    @PutMapping("/api/library/songs/{songId}")
    @ResponseBody
    @Transactional
    public SongDetails editSong(@PathVariable Long songId, @RequestBody SongEditRequest request) {
        Song song = loadSong(songId);
        String newTitle = trim(request.title());
        String newAlbum = trim(request.album());
        String newArtists = trim(request.artists());
        String newGenres = trim(request.genres());
        String newTrack = trim(request.trackNumber());

        if (!newTitle.isBlank()) song.setTitle(newTitle);

        if (!newAlbum.isBlank()) {
            Album album = albumRepository.findByTitle(newAlbum).orElseGet(() -> {
                Album a = new Album();
                a.setTitle(newAlbum);
                return albumRepository.save(a);
            });
            song.setAlbum(album);
        }

        if (!newArtists.isBlank()) {
            Set<Artist> artists = Arrays.stream(newArtists.split(","))
                    .map(String::trim).filter(s -> !s.isBlank())
                    .map(name -> artistRepository.findByName(name).orElseGet(() -> {
                        Artist a = new Artist();
                        a.setName(name);
                        return artistRepository.save(a);
                    }))
                    .collect(Collectors.toSet());
            song.setArtists(artists);
        }

        if (!newGenres.isBlank()) {
            Set<Genre> genres = Arrays.stream(newGenres.split(","))
                    .map(String::trim).filter(s -> !s.isBlank())
                    .map(this::findOrCreateGenre)
                    .collect(Collectors.toSet());
            song.setGenres(genres);
        }

        if (!newTrack.isBlank()) {
            try {
                song.setTrackNum(Integer.parseInt(newTrack));
            } catch (NumberFormatException ignored) {
                // desktop behaviour: an invalid track number is silently ignored
            }
        }

        songRepository.save(song);
        invalidateCacheAfterCommit();
        return toDetails(song);
    }

    /**
     * Permanently deletes a song with its playback history, favourites and playlist entries.
     *
     * @param songId the song id
     * @return {@code {"deleted": true}}
     * @throws ResponseStatusException {@code 404} when the song does not exist
     */
    @DeleteMapping("/api/library/songs/{songId}")
    @ResponseBody
    public Map<String, Boolean> deleteSong(@PathVariable Long songId) {
        if (!songRepository.existsById(songId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found.");
        }
        libraryService.deleteSong(songId);
        libraryService.invalidateCache();
        return Map.of("deleted", true);
    }

    // ───────────────────────── JSON: albums ─────────────────────────

    /**
     * Applies the Edit Album dialog result: title, release year and, when given, one genre replacing
     * the genres of every song in the album.
     *
     * @param albumId the album id
     * @param request the edited fields
     * @return {@code {"updated": true}}
     * @throws ResponseStatusException {@code 400} for a blank title or invalid year, {@code 404}
     *                                 when the album does not exist
     */
    @PutMapping("/api/library/albums/{albumId}")
    @ResponseBody
    @Transactional
    public Map<String, Boolean> editAlbum(@PathVariable Long albumId, @RequestBody AlbumEditRequest request) {
        Album album = albumRepository.findByIdWithSongsAndArtists(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found."));
        String title = trim(request.title());
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Album title is required.");
        }
        Integer year = request.releaseYear();
        if (year != null && (year < 0 || year > 9999)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year must be up to 4 digits.");
        }
        album.setTitle(title);
        album.setReleaseYear(year);
        albumRepository.save(album);

        String genreName = trim(request.genre());
        if (!genreName.isBlank()) {
            Genre genre = findOrCreateGenre(genreName);
            for (Song song : album.getSongs()) {
                song.getGenres().clear();
                song.getGenres().add(genre);
                songRepository.save(song);
            }
        }
        invalidateCacheAfterCommit();
        return Map.of("updated", true);
    }

    /**
     * Permanently deletes an album and every song it contains (desktop "Delete Album").
     *
     * @param albumId the album id
     * @return {@code {"deleted": true}}
     * @throws ResponseStatusException {@code 404} when the album does not exist
     */
    @DeleteMapping("/api/library/albums/{albumId}")
    @ResponseBody
    public Map<String, Boolean> deleteAlbum(@PathVariable Long albumId) {
        if (!albumRepository.existsById(albumId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found.");
        }
        libraryService.deleteAlbum(albumId);
        libraryService.invalidateCache();
        return Map.of("deleted", true);
    }

    // ───────────────────────── Helpers ─────────────────────────

    private Song loadSong(Long songId) {
        return songRepository.findByIdWithDetails(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found."));
    }

    private SongDetails toDetails(Song song) {
        boolean fav;
        try {
            fav = favouriteService.isFavourited(song);
        } catch (Exception e) {
            fav = false;
        }
        return new SongDetails(song.getSongID(),
                song.getTitle() != null ? song.getTitle() : "",
                song.getAlbum() != null && song.getAlbum().getTitle() != null ? song.getAlbum().getTitle() : "",
                song.getArtists() == null ? "" : song.getArtists().stream()
                        .map(Artist::getName).collect(Collectors.joining(", ")),
                song.getGenres() == null ? "" : song.getGenres().stream()
                        .map(Genre::getName).collect(Collectors.joining(", ")),
                song.getTrackNum() != null ? song.getTrackNum().toString() : "", fav);
    }

    private Genre findOrCreateGenre(String name) {
        return genreRepository.findByName(name).orElseGet(() -> {
            Genre g = new Genre();
            g.setName(name);
            return genreRepository.save(g);
        });
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Invalidates the library cache now and again once the current transaction commits.
     */
    private void invalidateCacheAfterCommit() {
        libraryService.invalidateCache();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    libraryService.invalidateCache();
                }
            });
        }
    }
}
