package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.dto.MigrationResult;
import com.sebastiandorata.musicdashboard.dto.MigrationResult.Status;
import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Genre;
import com.sebastiandorata.musicdashboard.entity.Song;
import com.sebastiandorata.musicdashboard.repository.AlbumRepository;
import com.sebastiandorata.musicdashboard.repository.ArtistRepository;
import com.sebastiandorata.musicdashboard.repository.GenreRepository;
import com.sebastiandorata.musicdashboard.repository.SongRepository;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * Scans a user-selected folder and reconciles the audio files it contains
 * against the songs already stored in the database.
 *
 * <h2>What this service does</h2>
 * <ol>
 *   <li>Recursively collects every supported audio file ({@code .mp3}, {@code .m4a})
 *       from the chosen folder.</li>
 *   <li>For each file, attempts to find a matching {@link Song} record using two
 *       strategies in order:
 *       <ol>
 *         <li><b>Exact path</b> — the file's absolute path already matches a DB row →
 *             no path change needed, but cover art may still be refreshed.</li>
 *         <li><b>Metadata fingerprint</b> — the file's title + primary artist (read
 *             from the audio tag) match an existing song → the stored path is updated
 *             to the new location.</li>
 *       </ol>
 *   </li>
 *   <li>If cover art is missing for the matched album, it is re-extracted from the
 *       audio file's embedded artwork and saved as {@code cover.jpg} in the file's
 *       parent folder.</li>
 *   <li><b>No data is ever deleted.</b> Artist, album, genre, playback history,
 *       favourites, and playlist associations are all preserved unconditionally.</li>
 *   <li>Files that cannot be matched to any existing song are skipped — they are
 *       <em>not</em> imported as new songs. Use the normal Import page for that.</li>
 * </ol>
 *
 * <h2>Thread safety</h2>
 * The public entry point {@link #scanFolder} is designed to be called from a
 * background thread (via {@link com.sebastiandorata.musicdashboard.service.handlers.DataLoadingService}).
 * All database writes are wrapped in a {@code @Transactional} helper so each
 * file is committed independently; a failure on one file does not roll back
 * updates already applied to others.
 *
 * <p>Time Complexity: O(f * s) where f = files in folder, s = songs in DB.<br>
 * Space Complexity: O(f) for the result list.
 */
@Service
public class LibraryMigrationService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private LibraryService libraryService;

    private static final String[] SUPPORTED_EXTENSIONS = {".mp3", ".m4a"};

    /**
     * Recursively scans {@code rootFolder}, matches each audio file to an
     * existing DB song, updates stale paths, and refreshes missing cover art.
     *
     * <p>This method is blocking and is intended to run on a background thread.
     * Progress callbacks are fired on the <em>calling thread</em>; the caller
     * is responsible for dispatching UI updates to the JavaFX thread via
     * {@link javafx.application.Platform#runLater(Runnable)} if needed.
     *
     * @param rootFolder       the folder to scan (searched recursively)
     * @param onFileProcessed  callback fired after each file is evaluated;
     *                         receives {@code (filesProcessedSoFar, totalFiles, currentFileName)}
     * @return a list of {@link MigrationResult} — one entry per audio file found
     */
    public List<MigrationResult> scanFolder(
            File rootFolder,
            TriConsumer<Integer, Integer, String> onFileProcessed) {

        List<File> audioFiles = new ArrayList<>();
        collectAudioFiles(rootFolder, audioFiles);

        int total = audioFiles.size();
        List<MigrationResult> results = new ArrayList<>();

        for (int i = 0; i < audioFiles.size(); i++) {
            File file = audioFiles.get(i);
            onFileProcessed.accept(i + 1, total, file.getName());

            MigrationResult result = processFile(file);
            results.add(result);
        }

        // Invalidate the library cache so the next navigation reflects updated paths
        libraryService.invalidateCache();

        return results;
    }

    /**
     * Evaluates a single audio file and returns its migration outcome.
     * Each call is its own transaction so a failure here does not affect other files.
     *
     * <p>For every matched song — whether the path was already correct or has
     * just been updated — the full tag metadata is re-read from the file and
     * written back to the database. Fields synced:</p>
     * <ul>
     *   <li>Song: title, duration, track number, bit rate, sample rate, channels,
     *       file size, codec, file format, artists, genres</li>
     *   <li>Album: release year, cover art (if missing or stale)</li>
     * </ul>
     * <p>Playback history, favourites, and playlist associations are never touched.</p>
     */
    @Transactional
    private MigrationResult processFile(File file) {
        if (!isSupportedAudioFile(file)) {
            return new MigrationResult(file.getName(), Status.SKIPPED,
                    "Not a supported audio format (.mp3 / .m4a)");
        }

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();

            String title      = getTagValue(tag, FieldKey.TITLE,  stripExtension(file.getName()));
            String artistName = getTagValue(tag, FieldKey.ARTIST, "Unknown Artist");
            // Normalize multi-artist separator by taking the first name only for matching
            String matchArtist = artistName.split("[,;&]")[0].trim();

            // --- Strategy 1: exact path already correct ---
            Optional<Song> byPath = songRepository.findByFilePath(file.getAbsolutePath());
            if (byPath.isPresent()) {
                Song song = byPath.get();
                List<String> changes = syncMetadata(song, audioFile, tag, file, false);
                if (changes.isEmpty()) {
                    return new MigrationResult(file.getName(), Status.ALREADY_CURRENT,
                            "Path and metadata are already up to date.");
                }
                return new MigrationResult(file.getName(), Status.METADATA_UPDATED,
                        "Metadata refreshed: " + String.join(", ", changes));
            }

            // --- Strategy 2: title + artist metadata fingerprint ---
            Optional<Song> byMeta = findByTitleAndArtist(title, matchArtist);
            if (byMeta.isPresent()) {
                Song song = byMeta.get();
                String oldPath = song.getFilePath();
                List<String> changes = syncMetadata(song, audioFile, tag, file, true);
                String changesStr = changes.isEmpty() ? "path only"
                        : "path + " + String.join(", ", changes);
                return new MigrationResult(file.getName(), Status.PATH_UPDATED,
                        "Updated (" + changesStr + ")  ←  …" + abbreviate(oldPath));
            }

            // No match found — skip without importing
            return new MigrationResult(file.getName(), Status.SKIPPED,
                    "No matching song found in library. Use Import to add new files.");

        } catch (Exception e) {
            System.err.println("[LibraryMigrationService] Error processing " + file.getName()
                    + ": " + e.getMessage());
            e.printStackTrace();
            return new MigrationResult(file.getName(), Status.ERROR,
                    "Error: " + e.getMessage());
        }
    }


    /**
     * Searches for a song whose title matches (case-insensitive) and whose
     * artist set contains a name that starts with {@code artistName} or vice versa.
     * A lenient prefix match handles minor tag variations (e.g. "The Beatles"
     * vs. "Beatles").
     *
     * <p>Time Complexity: O(s * a) where s = songs in DB, a = artists per song.
     */
    private Optional<Song> findByTitleAndArtist(String title, String artistName) {
        List<Song> allSongs = songRepository.findAllWithArtistsAndGenres();
        String normTitle  = title.trim().toLowerCase();
        String normArtist = artistName.trim().toLowerCase();

        return allSongs.stream()
                .filter(s -> s.getTitle() != null
                        && s.getTitle().trim().equalsIgnoreCase(normTitle))
                .filter(s -> s.getArtists() != null && s.getArtists().stream()
                        .map(Artist::getName)
                        .anyMatch(n -> n != null && (
                                n.trim().toLowerCase().startsWith(normArtist)
                                        || normArtist.startsWith(n.trim().toLowerCase())
                        )))
                .findFirst();
    }

    /**
     * Reads every relevant tag field from {@code audioFile} and writes any
     * changed values back to the {@link Song} (and its {@link Album}) in the
     * database. The file path is always updated when {@code updatePath} is
     * {@code true}.
     *
     * <h3>Fields synced on the Song entity</h3>
     * <ul>
     *   <li>filePath (when {@code updatePath} is true)</li>
     *   <li>title — only if the tag has a non-blank value different from what is stored</li>
     *   <li>duration — re-read from the audio header (always authoritative)</li>
     *   <li>trackNumber — parsed from the TRACK tag ({@code "5"} or {@code "5/12"})</li>
     *   <li>bitRate, sampleRate, channels, codec, fileFormat, fileSizeBytes — from audio header</li>
     *   <li>artists — resolved via {@link ArtistRepository}; new names are created,
     *       removed names are unlinked (the Artist row itself is never deleted)</li>
     *   <li>genres  — resolved via {@link GenreRepository}; same create-or-reuse rule</li>
     * </ul>
     *
     * <h3>Fields synced on the Album entity</h3>
     * <ul>
     *   <li>releaseYear — from YEAR tag; only written if currently null or different</li>
     *   <li>albumArtPath — written if missing or the file on disk no longer exists</li>
     * </ul>
     *
     * <p>Playback history, favourites, and playlist associations are never modified.
     *
     * @param song        the managed {@link Song} entity to update
     * @param audioFile   the jAudioTagger handle for the physical file
     * @param tag         the tag read from {@code audioFile} (may be null for untagged files)
     * @param sourceFile  the physical audio file on disk
     * @param updatePath  when {@code true} the song's filePath is set to
     *                    {@code sourceFile.getAbsolutePath()}
     * @return a human-readable list of field names that were actually changed,
     *         empty when everything was already up to date
     */
    @Transactional
    protected List<String> syncMetadata(Song song, AudioFile audioFile,
                                        Tag tag, File sourceFile,
                                        boolean updatePath) {
        List<String> changed = new ArrayList<>();

        if (updatePath) {
            song.setFilePath(sourceFile.getAbsolutePath());
            changed.add("path");
        }

        //  Audio header fields (always authoritative from the file)
        int newDuration = audioFile.getAudioHeader().getTrackLength();
        if (!Integer.valueOf(newDuration).equals(song.getDuration())) {
            song.setDuration(newDuration);
            changed.add("duration");
        }

        int newBitRate = (int) audioFile.getAudioHeader().getBitRateAsNumber();
        if (!Integer.valueOf(newBitRate).equals(song.getBitRate())) {
            song.setBitRate(newBitRate);
            changed.add("bitRate");
        }

        int newSampleRate = audioFile.getAudioHeader().getSampleRateAsNumber();
        if (!Integer.valueOf(newSampleRate).equals(song.getSampleRate())) {
            song.setSampleRate(newSampleRate);
            changed.add("sampleRate");
        }

        int newChannels = parseChannels(audioFile.getAudioHeader().getChannels());
        if (!Integer.valueOf(newChannels).equals(song.getChannels())) {
            song.setChannels(newChannels);
            changed.add("channels");
        }

        String newCodec = audioFile.getAudioHeader().getEncodingType();
        if (newCodec != null && !newCodec.equals(song.getCodec())) {
            song.setCodec(newCodec);
            changed.add("codec");
        }

        String newFormat = getFileExtension(sourceFile);
        if (newFormat != null && !newFormat.equals(song.getFileFormat())) {
            song.setFileFormat(newFormat);
            changed.add("format");
        }

        long newSize = sourceFile.length();
        if (!Long.valueOf(newSize).equals(song.getFileSizeBytes())) {
            song.setFileSizeBytes(newSize);
            changed.add("fileSize");
        }


        // Title  only overwrite if the tag carries a non-blank value that differs
        String tagTitle = getTagValue(tag, FieldKey.TITLE, null);
        if (tagTitle != null && !tagTitle.equalsIgnoreCase(song.getTitle())) {
            song.setTitle(tagTitle);
            changed.add("title");
        }

        // Track number — "5" or "5/12" format
        String rawTrack = getTagValue(tag, FieldKey.TRACK, null);
        Integer newTrack = parseTrackNumber(rawTrack);
        if (newTrack != null && !newTrack.equals(song.getTrackNum())) {
            song.setTrackNum(newTrack);
            changed.add("trackNum");
        }

        // Artists — resolve or create, then replace the song's set unconditionally.
        // Avoid .equals() on the lazy PersistentSet — loading both sides outside a
        // session triggers LazyInitializationException. If the tag has data, just write it.
        String rawArtists = getTagValue(tag, FieldKey.ARTIST, null);
        if (rawArtists != null && !rawArtists.isBlank()) {
            Set<Artist> newArtists = resolveArtists(rawArtists);
            song.setArtists(newArtists);
            changed.add("artists");
        }

        // Genres — resolve or create, then replace the song's set unconditionally.
        // Same reasoning as artists above: no .equals() on a lazy PersistentSet.
        String rawGenres = getTagValue(tag, FieldKey.GENRE, null);
        if (rawGenres != null && !rawGenres.isBlank()) {
            Set<Genre> newGenres = resolveGenres(rawGenres);
            song.setGenres(newGenres);
            changed.add("genres");
        }

        // ── Album fields ───────────────────────────────────────────────────
        Album album = song.getAlbum();
        if (album != null) {
            String rawYear = getTagValue(tag, FieldKey.YEAR, null);
            if (rawYear != null && !rawYear.isBlank()) {
                try {
                    Integer newYear = Integer.parseInt(rawYear.trim().substring(0, 4));
                    if (!newYear.equals(album.getReleaseYear())) {
                        album.setReleaseYear(newYear);
                        changed.add("releaseYear");
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Cover art — refresh if missing or the file has been moved/deleted
            boolean artRefreshed = maybeRefreshArt(song, audioFile, sourceFile);
            if (artRefreshed) changed.add("coverArt");

            albumRepository.save(album);
        }

        songRepository.save(song);
        return changed;
    }

    /**
     * Re-extracts embedded artwork from {@code audioFile} and saves it as
     * {@code cover.jpg} if the song's album has no art path recorded yet.
     *
     * @return {@code true} if art was written to disk and the album was updated
     */
    @Transactional
    protected boolean maybeRefreshArt(Song song, AudioFile audioFile, File sourceFile) {
        Album album = song.getAlbum();
        if (album == null) return false;

        // Only refresh if the art path is missing or the file no longer exists
        boolean artMissing = album.getAlbumArtPath() == null
                || album.getAlbumArtPath().isBlank()
                || !new File(album.getAlbumArtPath()).exists();

        if (!artMissing) return false;

        try {
            Tag tag = audioFile.getTag();
            if (tag == null) return false;

            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) return false;

            byte[] imageData = artwork.getBinaryData();
            if (imageData == null || imageData.length == 0) return false;

            String folder  = sourceFile.getParent();
            String artPath = folder + File.separator + "cover.jpg";

            Files.write(Paths.get(artPath), imageData);
            album.setAlbumArtPath(artPath);
            albumRepository.save(album);
            System.out.println("[Migration] Cover art saved: " + artPath);
            return true;

        } catch (Exception e) {
            System.err.println("[Migration] Could not refresh art for "
                    + sourceFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // ── File system helpers ─────────────────────────────────────────────────

    private void collectAudioFiles(File folder, List<File> results) {
        File[] contents = folder.listFiles();
        if (contents == null) return;

        for (File file : contents) {
            if (file.isDirectory()) {
                collectAudioFiles(file, results);
            } else if (isSupportedAudioFile(file)) {
                results.add(file);
            }
        }
    }

    private boolean isSupportedAudioFile(File file) {
        String lower = file.getName().toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    // ── String helpers ──────────────────────────────────────────────────────

    private String getTagValue(Tag tag, FieldKey key, String defaultValue) {
        if (tag == null) return defaultValue;
        try {
            String v = tag.getFirst(key);
            return (v == null || v.isBlank()) ? defaultValue : v;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    /** Returns the last ~40 characters of a path for display in the results table. */
    private String abbreviate(String path) {
        if (path == null) return "";
        return path.length() > 40 ? "…" + path.substring(path.length() - 40) : path;
    }

    // ── Entity resolution helpers ───────────────────────────────────────────

    /**
     * Splits a raw artist tag on {@code , ; &} and returns a set of managed
     * {@link Artist} entities, creating new rows for names not yet in the DB.
     * Existing artist rows (with their song/album relationships) are never deleted.
     */
    private Set<Artist> resolveArtists(String raw) {
        Set<Artist> set = new HashSet<>();
        for (String name : raw.split("[,;&]")) {
            String t = name.trim();
            if (t.isBlank()) continue;
            set.add(artistRepository.findByName(t).orElseGet(() -> {
                Artist a = new Artist();
                a.setName(t);
                return artistRepository.save(a);
            }));
        }
        return set;
    }

    /**
     * Splits a raw genre tag on {@code , ;} and returns a set of managed
     * {@link Genre} entities, creating new rows for names not yet in the DB.
     */
    private Set<Genre> resolveGenres(String raw) {
        Set<Genre> set = new HashSet<>();
        for (String name : raw.split("[,;]")) {
            String t = name.trim();
            if (t.isBlank()) continue;
            set.add(genreRepository.findByName(t).orElseGet(() -> {
                Genre g = new Genre();
                g.setName(t);
                return genreRepository.save(g);
            }));
        }
        return set;
    }

    /**
     * Parses a TRACK tag value that may be in {@code "5"} or {@code "5/12"} format.
     *
     * @return the track number, or {@code null} if the string is blank or unparseable
     */
    private Integer parseTrackNumber(String trackStr) {
        if (trackStr == null || trackStr.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(trackStr.split("/")[0].trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts the audio-header channel descriptor to an integer.
     * Mirrors the same method in {@code SongImportService} so the stored
     * value is always consistent between import and migration.
     */
    private int parseChannels(String channelStr) {
        if (channelStr == null) return 2;
        try {
            return Integer.parseInt(channelStr.trim());
        } catch (NumberFormatException ignored) {}
        return switch (channelStr.trim().toLowerCase()) {
            case "mono"                                               -> 1;
            case "stereo", "joint stereo", "dual channel",
                 "joint_stereo"                                       -> 2;
            case "5.1", "surround"                                    -> 6;
            default                                                   -> 2;
        };
    }

    /** Returns the uppercase file extension without the leading dot, e.g. {@code "MP3"}. */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toUpperCase() : "UNKNOWN";
    }


    /**
     * Three-argument callback used to report per-file progress to the caller.
     *
     * @param <A> current file index (1-based)
     * @param <B> total file count
     * @param <C> current file name
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}