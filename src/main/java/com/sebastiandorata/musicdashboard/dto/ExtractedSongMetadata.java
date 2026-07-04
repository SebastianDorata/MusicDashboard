package com.sebastiandorata.musicdashboard.dto;

import java.io.File;

/**
 * Everything read from one audio file in a single pass by
 * {@code SongMetadataExtractor}.
 *
 * <p>This record is the reason the import pipeline no longer needs a
 * separate "reconcile metadata" phase: it carries both the fields the old
 * {@code SongImportService.importSong} used to read AND the fields the old
 * {@code LibraryMigrationService.syncMetadata} used to read, because both
 * were reading the same file anyway.
 *
 * @param file               the source audio file on disk
 * @param title              track title (tag value, falling back to the file name)
 * @param albumTitle         album title (falls back to the track title for singles)
 * @param artistRaw          raw artist tag value, possibly multiple names separated by , ; &amp;
 * @param genreRaw           raw genre tag value, possibly multiple genres separated by , ;
 * @param releaseYear        parsed release year, or {@code null} if absent/unparseable
 * @param trackNum           track number, defaulted to 1 when absent or invalid
 * @param durationSeconds    track length from the audio header
 * @param bitRate            bit rate from the audio header
 * @param sampleRate         sample rate from the audio header
 * @param channels           channel count, normalized to an integer
 * @param codec              encoding type from the audio header
 * @param fileFormat         uppercase file extension, e.g. "MP3"
 * @param fileSizeBytes      file size in bytes
 * @param artworkBytes       embedded cover art bytes, or {@code null} if none present
 * @param contentFingerprint SHA-256 hash of normalized title+artist+duration —
 *                           a path-independent identity for this song, used to
 *                           recognise it again if the file moves or is imported
 *                           again from a different session/machine
 */
public record ExtractedSongMetadata(
        File file,
        String title,
        String albumTitle,
        String artistRaw,
        String genreRaw,
        Integer releaseYear,
        int trackNum,
        int durationSeconds,
        int bitRate,
        int sampleRate,
        int channels,
        String codec,
        String fileFormat,
        long fileSizeBytes,
        byte[] artworkBytes,
        String contentFingerprint
) {}