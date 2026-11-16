package com.sebastiandorata.musicdashboard.service.Import;

import com.sebastiandorata.musicdashboard.dto.ExtractedSongMetadata;
import com.sebastiandorata.musicdashboard.dto.MigrationResult;
import com.sebastiandorata.musicdashboard.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source-of-truth entry point for the import pipeline.
 *
 * <p>Genuinely single-pass now: for each audio file, {@link SongMetadataExtractor}
 * reads it once and {@link SongUpdateService} persists the result (create,
 * update-in-place, or relocate). There is no separate "scan for new files"
 * phase followed by a "reconcile metadata" phase — one file, one read, one
 * write, one result.
 *
 * <p>This class deliberately knows nothing about *how* a song is matched or
 * persisted — that all lives in {@link SongUpdateService}. If a future
 * change needs a new matching rule or an extra synced field, this class
 * should not need to change at all (OCP).
 *
 * <p>The web layer tracks progress through {@link ImportJobRegistry}; this class only reports to
 * the {@link ImportCallbacks} it is given.
 *
 * <p>Imports are serialized on a single-threaded executor so concurrent
 * invocations are queued, never interleaved — this also means two imports
 * can never race each other into creating duplicate rows for the same file.
 */
@Service
public class ImportOrchestrator {

    @Autowired private SongMetadataExtractor extractor;
    @Autowired private SongUpdateService upsertService;
    @Autowired private LibraryService libraryService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "import-pipeline");
        t.setDaemon(true);
        return t;
    });

    /**
     * Resolves a mixed list of files and directories into a flat list of
     * supported audio files, then runs the single-pass import.
     *
     * <p>When the inputs contain no audio file, {@link ImportCallbacks#onNoFiles()} is invoked
     * synchronously on the calling thread; otherwise the work is queued on the import thread.
     *
     * <p>Time Complexity: O(f) to walk the inputs for f files and folders found.
     *
     * @param inputs    files or folders uploaded or selected by the user
     * @param callbacks progress notification callbacks (never {@code null})
     */
    public void process(List<File> inputs, ImportCallbacks callbacks) {
        List<File> audioFiles = collectAudioFiles(inputs);

        if (audioFiles.isEmpty()) {
            callbacks.onNoFiles();
            return;
        }

        executor.submit(() -> runPipeline(audioFiles, callbacks));
    }

    /**
     * Runs the pipeline for an already collected list of audio files on the import thread.
     *
     * <p>Time Complexity: O(n) file reads and writes for n files.
     *
     * @param audioFiles the supported audio files to import
     * @param callbacks  progress callbacks
     */
    private void runPipeline(List<File> audioFiles, ImportCallbacks callbacks) {
        try {
            runPipelineUnchecked(audioFiles, callbacks);
        } catch (RuntimeException e) {
            System.err.println("[ImportOrchestrator] Import failed: " + e.getMessage());
            callbacks.onFailed("Import failed: " + e.getMessage());
        }
    }

    private void runPipelineUnchecked(List<File> audioFiles, ImportCallbacks callbacks) {
        int total = audioFiles.size();
        List<MigrationResult> results = new ArrayList<>();

        callbacks.onStart(total);

        for (int i = 0; i < audioFiles.size(); i++) {
            File file = audioFiles.get(i);
            callbacks.onProgress(i + 1, total, file.getName());
            results.add(processOneFile(file));
        }

        // Invalidate the library cache once at the end so subsequent
        // navigation reflects everything imported/updated in this run.
        libraryService.invalidateCache();

        callbacks.onComplete(results);
    }

    private MigrationResult processOneFile(File file) {
        try {
            ExtractedSongMetadata data = extractor.extract(file);
            return upsertService.upsert(data);
        } catch (Exception e) {
            System.err.println("[ImportOrchestrator] Could not read " + file.getName()
                    + ": " + e.getMessage());
            return new MigrationResult(file.getName(),
                    MigrationResult.Status.ERROR, "Could not read file: " + e.getMessage());
        }
    }

    // ── File collection helpers ───────────────────────────────────────────────

    /**
     * Flattens a mixed list of files and directories into supported audio files.
     * Directories are recursively searched.
     *
     * <p>Time Complexity: O(f) for f entries found.
     * <p>Space Complexity: O(a) for a audio files found.
     *
     * @param inputs files or folders to flatten
     * @return every supported audio file found, in directory order
     */
    public List<File> collectAudioFiles(List<File> inputs) {
        List<File> results = new ArrayList<>();
        for (File input : inputs) {
            if (input.isDirectory()) {
                collectFromDirectory(input, results);
            } else if (isSupportedAudioFile(input)) {
                results.add(input);
            }
        }
        return results;
    }

    private void collectFromDirectory(File dir, List<File> results) {
        File[] contents = dir.listFiles();
        if (contents == null) return;
        for (File f : contents) {
            if (f.isDirectory()) {
                collectFromDirectory(f, results);
            } else if (isSupportedAudioFile(f)) {
                results.add(f);
            }
        }
    }

    /**
     * Tells whether a file has a supported audio extension ({@code .mp3} or {@code .m4a}).
     *
     * @param file the file to test (only its name is read)
     * @return {@code true} when the file is a supported audio file
     */
    public boolean isSupportedAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".m4a");
    }

    // ── Types ─────────────────────────────────────────────────────────────────

    /**
     * All UI notification points for a single import run.
     *
     * <p>Methods are called on the import-pipeline thread (except {@link #onNoFiles()}, which
     * runs on the thread that called {@link ImportOrchestrator#process}), so implementations
     * must be thread-safe. No callback depends on the logged-in user.
     */
    public interface ImportCallbacks {

        /** Called when the provided inputs contain no supported audio files. */
        void onNoFiles();

        /**
         * Called once, before the first file is processed.
         *
         * @param totalFiles the number of supported audio files that will be processed
         */
        void onStart(int totalFiles);

        /**
         * Called after each file is processed.
         *
         * @param current  the one-based position of the file that was just processed
         * @param total    the total number of files in this run
         * @param fileName the name of the file that was just processed
         */
        void onProgress(int current, int total, String fileName);

        /**
         * Called once after every file has been processed.
         *
         * @param results the outcome for each processed file
         */
        void onComplete(List<MigrationResult> results);

        /**
         * Called instead of {@link #onComplete} when the pipeline aborts unexpectedly.
         *
         * @param reason a human-readable failure description
         */
        default void onFailed(String reason) { }
    }
}