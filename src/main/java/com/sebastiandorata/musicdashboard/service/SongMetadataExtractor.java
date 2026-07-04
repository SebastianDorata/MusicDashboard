package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.dto.ExtractedSongMetadata;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Reads an audio file's tag and header data exactly once and returns a
 * fully-populated {@link ExtractedSongMetadata}.
 *
 * <p>This replaces the metadata-reading logic that used to be duplicated
 * across {@code SongImportService.importSong} (first pass, partial data)
 * and {@code LibraryMigrationService.processFile} / {@code syncMetadata}
 * (second pass, everything else). Both used to open the same file with
 * jAudioTagger separately; this class does it once, so the import pipeline
 * only ever needs a single pass per file.
 *
 * <p>Also computes {@link ExtractedSongMetadata#contentFingerprint()} — a
 * hash of the song's title, primary artist, and duration — so
 * {@code SongUpsertService} can recognise the same logical song later even
 * if its file path changes (moved folder, re-imported on another
 * machine/session), without needing to write an ID into the user's files.
 *
 * <p>SRP: this class only extracts. It never touches the database.
 */
@Service
public class SongMetadataExtractor {

    public ExtractedSongMetadata extract(File file) throws Exception {
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();

        String title = stripExtension(getTagValue(tag, FieldKey.TITLE, file.getName()));

        String rawAlbum = getTagValue(tag, FieldKey.ALBUM, null);
        String albumTitle = (rawAlbum == null || rawAlbum.isBlank()
                || rawAlbum.equalsIgnoreCase("Unknown Album"))
                ? title : rawAlbum;

        String artistRaw = getTagValue(tag, FieldKey.ARTIST, "Unknown Artist");
        String genreRaw  = getTagValue(tag, FieldKey.GENRE, "Unknown");

        Integer year = parseYear(getTagValue(tag, FieldKey.YEAR, null));
        int trackNum = defaultTrackNumber(parseTrackNumber(getTagValue(tag, FieldKey.TRACK, null)));

        int duration   = audioFile.getAudioHeader().getTrackLength();
        int bitRate    = (int) audioFile.getAudioHeader().getBitRateAsNumber();
        int sampleRate = audioFile.getAudioHeader().getSampleRateAsNumber();
        int channels   = parseChannels(audioFile.getAudioHeader().getChannels());
        String codec   = audioFile.getAudioHeader().getEncodingType();
        String format  = getFileExtension(file);
        long size      = file.length();

        byte[] artwork = extractArtworkBytes(tag);

        String primaryArtist = artistRaw.split("[,;&]")[0].trim();
        String fingerprint = fingerprint(title, primaryArtist, duration);

        return new ExtractedSongMetadata(
                file, title, albumTitle, artistRaw, genreRaw, year, trackNum,
                duration, bitRate, sampleRate, channels, codec, format, size,
                artwork, fingerprint);
    }

    /**
     * Builds a stable identity hash from title + primary artist + duration.
     * Deliberately file-path independent so the same song can be matched
     * across machines/sessions for future multi-device sync, without
     * embedding an ID inside the user's audio files (which risks corrupting
     * them and requires a write-back step on every import).
     */
    private String fingerprint(String title, String artist, int durationSeconds) {
        String normalized = (title == null ? "" : title.trim().toLowerCase())
                + "|" + (artist == null ? "" : artist.trim().toLowerCase())
                + "|" + durationSeconds;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 is always available on the JVM; this is just a paranoia
            // fallback so a missing algorithm can never fail an import.
            return Integer.toHexString(normalized.hashCode());
        }
    }

    private byte[] extractArtworkBytes(Tag tag) {
        try {
            if (tag == null) return null;
            Artwork artwork = tag.getFirstArtwork();
            return artwork != null ? artwork.getBinaryData() : null;
        } catch (Exception e) {
            return null;
        }
    }

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
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private Integer parseYear(String yearStr) {
        if (yearStr == null || yearStr.isBlank()) return null;
        try { return Integer.parseInt(yearStr.trim().substring(0, 4)); }
        catch (Exception e) { return null; }
    }

    private Integer parseTrackNumber(String trackStr) {
        if (trackStr == null || trackStr.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(trackStr.split("/")[0].trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int defaultTrackNumber(Integer parsed) {
        return (parsed != null && parsed > 0) ? parsed : 1;
    }

    private int parseChannels(String channelStr) {
        if (channelStr == null) return 2;
        try { return Integer.parseInt(channelStr.trim()); }
        catch (NumberFormatException ignored) {}
        return switch (channelStr.trim().toLowerCase()) {
            case "mono" -> 1;
            case "stereo", "joint stereo", "dual channel", "joint_stereo" -> 2;
            case "5.1", "surround" -> 6;
            default -> 2;
        };
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toUpperCase() : "UNKNOWN";
    }
}