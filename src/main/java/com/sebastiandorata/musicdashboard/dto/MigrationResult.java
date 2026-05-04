package com.sebastiandorata.musicdashboard.dto;

/**
 * Carries the outcome of a single file evaluated during a library migration scan.
 *
 * <p>Each {@code MigrationResult} describes what happened to one audio file
 * found in the scanned folder. The {@link Status} enum drives which summary
 * counter is incremented in the UI and what message is shown per row.</p>
 *
 * @param fileName  the short file name (not the full path) for display
 * @param status    the outcome for this file
 * @param message   a human-readable description of what changed or why it was skipped
 */
public record MigrationResult(String fileName, Status status, String message) {

    /**
     * The possible outcomes for a single file during migration.
     */
    public enum Status {

        /** The file's path was updated in the database to the new location. */
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
         * The stack trace is printed to stderr; the migration continues.
         */
        ERROR
    }
}