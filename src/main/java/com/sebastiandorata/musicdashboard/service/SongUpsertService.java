package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.dto.ExtractedSongMetadata;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The single class responsible for turning {@link ExtractedSongMetadata}
 * into database state.
 *
 * <p>This is the one place the import feature revolves around (SRP): given
 * metadata for one file, it decides whether that song is brand new, already
 * known at the same path, or known but relocated — and persists accordingly,
 * in one pass, in one transaction. Any future change to "what happens when a
 * song is imported" (a new field to sync, a new matching rule, etc.) only
 * ever touches this class. {@link ImportOrchestrator} and the UI never need
 * to change for that kind of update.
 *
 * <p>Replaces {@code SongImportService.importSong},
 * {@code SongImportService.importSongWithOverrides}, {@code ImportService},
 * and {@code LibraryMigrationService} entirely — there is no more
 * "import now, reconcile metadata later" split, and therefore no window in
 * which two different code paths can each decide a song doesn't exist yet.
 *
 * <h2>Matching strategy (in order)</h2>
 * <ol>
 *   <li><b>Exact path match</b> — the file's canonical path is already in the DB.</li>
 *   <li><b>Content fingerprint match</b> — same title+artist+duration exists
 *       under a different path (file was moved, or the same library is being
 *       imported again on another machine/session). The path is updated on
 *       the existing row rather than creating a second one.</li>
 *   <li><b>No match</b> — a new {@link Song} is created.</li>
 * </ol>
 */
@Service
public class SongUpsertService {

    @Autowired private SongRepository   songRepository;
    @Autowired private AlbumRepository  albumRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private GenreRepository  genreRepository;

    @Transactional
    public MigrationResult upsert(ExtractedSongMetadata data) {
        try {
            String canonicalPath = canonicalPath(data.file());

            Optional<Song> byPath = songRepository.findFirstByFilePath(canonicalPath);
            if (byPath.isPresent()) {
                return applyToExisting(byPath.get(), data, canonicalPath, false);
            }

            Optional<Song> byFingerprint =
                    songRepository.findFirstByContentFingerprint(data.contentFingerprint());
            if (byFingerprint.isPresent()) {
                return applyToExisting(byFingerprint.get(), data, canonicalPath, true);
            }

            return createNew(data, canonicalPath);

        } catch (Exception e) {
            System.err.println("[SongUpsertService] Failed on " + data.file().getName()
                    + ": " + e.getMessage());
            return new MigrationResult(data.file().getName(), Status.ERROR,
                    "Error: " + e.getMessage());
        }
    }

    // ── Existing song: update in place, or relocate ─────────────────────────

    private MigrationResult applyToExisting(Song song, ExtractedSongMetadata data,
                                            String canonicalPath, boolean pathChanged) {
        List<String> changed = new ArrayList<>();

        if (pathChanged && !canonicalPath.equals(song.getFilePath())) {
            song.setFilePath(canonicalPath);
            changed.add("path");
        }

        applyField(changed, "duration",   song.getDuration(),   data.durationSeconds(), song::setDuration);
        applyField(changed, "bitRate",    song.getBitRate(),    data.bitRate(),         song::setBitRate);
        applyField(changed, "sampleRate", song.getSampleRate(), data.sampleRate(),      song::setSampleRate);
        applyField(changed, "channels",   song.getChannels(),   data.channels(),        song::setChannels);

        if (data.codec() != null && !data.codec().equals(song.getCodec())) {
            song.setCodec(data.codec()); changed.add("codec");
        }
        if (data.fileFormat() != null && !data.fileFormat().equals(song.getFileFormat())) {
            song.setFileFormat(data.fileFormat()); changed.add("format");
        }
        if (!Long.valueOf(data.fileSizeBytes()).equals(song.getFileSizeBytes())) {
            song.setFileSizeBytes(data.fileSizeBytes()); changed.add("fileSize");
        }
        if (data.title() != null && !data.title().equalsIgnoreCase(song.getTitle())) {
            song.setTitle(data.title()); changed.add("title");
        }
        if (data.trackNum() > 0 && !Integer.valueOf(data.trackNum()).equals(song.getTrackNum())) {
            song.setTrackNum(data.trackNum()); changed.add("trackNum");
        }
        // Backfill the fingerprint for rows that predate this column.
        if (song.getContentFingerprint() == null) {
            song.setContentFingerprint(data.contentFingerprint());
        }

        if (data.artistRaw() != null && !data.artistRaw().isBlank()) {
            song.setArtists(resolveArtists(data.artistRaw()));
            changed.add("artists");
        }
        if (data.genreRaw() != null && !data.genreRaw().isBlank()) {
            song.setGenres(resolveGenres(data.genreRaw()));
            changed.add("genres");
        }

        Album album = resolveAlbumFor(song, data);
        if (maybeRefreshArt(album, data)) changed.add("coverArt");

        songRepository.save(song);

        if (pathChanged) {
            String summary = changed.isEmpty() ? "path only" : "path + " + String.join(", ", changed);
            return new MigrationResult(data.file().getName(), Status.PATH_UPDATED,
                    "Song relocated (" + summary + ")");
        }
        if (changed.isEmpty()) {
            return new MigrationResult(data.file().getName(), Status.ALREADY_CURRENT,
                    "Already up to date.");
        }
        return new MigrationResult(data.file().getName(), Status.METADATA_UPDATED,
                "Metadata refreshed: " + String.join(", ", changed));
    }

    private <T> void applyField(List<String> changed, String name, T current, T incoming,
                                Consumer<T> setter) {
        if (!incoming.equals(current)) {
            setter.accept(incoming);
            changed.add(name);
        }
    }

    // ── New song ─────────────────────────────────────────────────────────

    private MigrationResult createNew(ExtractedSongMetadata data, String canonicalPath) {
        Song song = new Song();
        song.setFilePath(canonicalPath);
        song.setTitle(data.title());
        song.setDuration(data.durationSeconds());
        song.setFileFormat(data.fileFormat());
        song.setFileSizeBytes(data.fileSizeBytes());
        song.setDateFirstListened(LocalDate.now());
        song.setListenCount(0);
        song.setBitRate(data.bitRate());
        song.setSampleRate(data.sampleRate());
        song.setChannels(data.channels());
        song.setCodec(data.codec());
        song.setTrackNum(data.trackNum());
        song.setContentFingerprint(data.contentFingerprint());

        Album album = albumRepository.findByTitle(data.albumTitle())
                .orElseGet(() -> {
                    Album a = new Album();
                    a.setTitle(data.albumTitle());
                    a.setReleaseYear(data.releaseYear());
                    return albumRepository.save(a);
                });
        song.setAlbum(album);

        Set<Artist> artists = resolveArtists(data.artistRaw());
        song.setArtists(artists);
        for (Artist artist : artists) {
            if (!album.getArtists().contains(artist)) album.getArtists().add(artist);
        }
        albumRepository.save(album);

        song.setGenres(resolveGenres(data.genreRaw()));

        maybeRefreshArt(album, data);

        songRepository.save(song);
        return new MigrationResult(data.file().getName(), Status.IMPORTED, "Imported new song.");
    }

    // ── Shared helpers ───────────────────────────────────────────────────

    private Album resolveAlbumFor(Song song, ExtractedSongMetadata data) {
        Album album = song.getAlbum();
        if (album == null) {
            album = albumRepository.findByTitle(data.albumTitle())
                    .orElseGet(() -> {
                        Album a = new Album();
                        a.setTitle(data.albumTitle());
                        a.setReleaseYear(data.releaseYear());
                        return albumRepository.save(a);
                    });
            song.setAlbum(album);
        } else if (data.releaseYear() != null && !data.releaseYear().equals(album.getReleaseYear())) {
            album.setReleaseYear(data.releaseYear());
            albumRepository.save(album);
        }
        return album;
    }

    private boolean maybeRefreshArt(Album album, ExtractedSongMetadata data) {
        if (album == null || data.artworkBytes() == null || data.artworkBytes().length == 0) return false;

        boolean artMissing = album.getAlbumArtPath() == null
                || album.getAlbumArtPath().isBlank()
                || !new File(album.getAlbumArtPath()).exists();
        if (!artMissing) return false;

        try {
            String folder = data.file().getParent();
            String artPath = folder + File.separator + "cover.jpg";
            Files.write(Paths.get(artPath), data.artworkBytes());
            album.setAlbumArtPath(artPath);
            albumRepository.save(album);
            return true;
        } catch (IOException e) {
            System.err.println("[SongUpsertService] Could not write cover art: " + e.getMessage());
            return false;
        }
    }

    private Set<Artist> resolveArtists(String raw) {
        Set<Artist> set = new HashSet<>();
        for (String name : raw.split("[,;&]")) {
            String t = name.trim();
            if (t.isBlank()) continue;
            set.add(artistRepository.findByName(t).orElseGet(() -> {
                Artist a = new Artist(); a.setName(t); return artistRepository.save(a);
            }));
        }
        return set;
    }

    private Set<Genre> resolveGenres(String raw) {
        Set<Genre> set = new HashSet<>();
        for (String name : raw.split("[,;]")) {
            String t = name.trim();
            if (t.isBlank()) continue;
            set.add(genreRepository.findByName(t).orElseGet(() -> {
                Genre g = new Genre(); g.setName(t); return genreRepository.save(g);
            }));
        }
        return set;
    }

    /**
     * Resolves symlinks and relative-path segments (./ ../) so the same
     * physical file always produces the same stored path string. This is
     * part of the duplicate-row fix: previously {@code getAbsolutePath()}
     * could produce two different strings for the same file (e.g. accessed
     * via a symlink vs. directly), which defeated the exact-path lookup and
     * let a second row slip in for what was really the same song.
     */
    private String canonicalPath(File file) throws IOException {
        return file.getCanonicalFile().getAbsolutePath();
    }
}