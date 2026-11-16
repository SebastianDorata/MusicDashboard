package com.sebastiandorata.musicdashboard.dto;

/**
 * Carries the outcome of a single file evaluated during import.
 *
 * <p>Previously this only described outcomes from the second-pass
 * "reconcile" scan. Now that import is a single pass owned by
 * {@code SongUpsertService}, it also covers the "brand new song" outcome
 * ({@link Status#IMPORTED}), so one result list describes the whole import.
 *
 * @param fileName  the short file name (not the full path) for display
 * @param status    the outcome for this file
 * @param message   a human-readable description of what changed or why it was skipped
 */
public record MigrationResult(String fileName, Status status, String message) {

    /**
     * The possible outcomes for a single file during import.
     */
    public enum Status {

        /** Brand new song, not previously in the library. */
        IMPORTED,

        /** The file's path was updated in the database to a new location. */
        PATH_UPDATED,

        /**
         * The file's path was already correct but one or more metadata fields
         * (track number, genre, release year, artists, etc.) were out of date
         * and have been refreshed from the audio tags.
         */
        METADATA_UPDATED,

        /**
         * The file already exists in the database with the same absolute path
         * and all metadata is current. No changes were made.
         */
        ALREADY_CURRENT,

        /**
         * Cover art was re-extracted or re-linked for an existing song whose
         * album had no art recorded.
         */
        ART_REFRESHED,

        /**
         * The file was not recognised as an audio file, or jAudioTagger could
         * not read its metadata. No changes were made.
         */
        SKIPPED,

        /**
         * An unexpected error occurred while processing this file.
         * The stack trace is printed to stderr; the import continues.
         */
        ERROR
    }
}