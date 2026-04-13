package com.sebastiandorata.musicdashboard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for orchestrating the music file import process.
 *
 * <p>Runs imports on a dedicated single-threaded {@link java.util.concurrent.ExecutorService}
 * to ensure imports are sequential and never overlap, preventing duplicate
 * import attempts on the same file.
 *
 * <p>Delegates the actual metadata extraction and persistence of each file
 * to {@link SongImportService}. This class is responsible only for
 * iterating the file list, invoking progress and completion callbacks,
 * and classifying results as imported, skipped, or failed.
 *
 * <p>Skipped files are those that throw {@link IllegalStateException},
 * the convention used by {@link SongImportService#importSong(java.io.File)}
 * when a song with the same file path already exists in the database.
 */
@Service
public class ImportService {

    @Lazy @Autowired private SongImportService songService;
    @Lazy @Autowired private LibraryService libraryService;

    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();

    /**
     * @param files the list of music files to import
     * @param onProgress callback invoked per file with (currentIndex, total, fileName)
     * @param onComplete callback invoked once with (imported, skipped, failed) counts
    */
    public void startImport(List<File> files,
                            TriConsumer<Integer, Integer, String> onProgress,
                            TriConsumer<Integer, Integer, Integer> onComplete) {
        int total = files.size();
        importExecutor.submit(() -> {
            int imported = 0, skipped = 0, failed = 0;
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                final int current = i + 1;
                onProgress.accept(current, total, file.getName());
                try {
                    songService.importSong(file);
                    imported++;
                } catch (IllegalStateException e) {
                    skipped++;
                } catch (Exception e) {
                    failed++;
                    System.err.println("Failed to import: " + file.getName()
                            + " — " + e.getMessage());
                }
            }
            // Invalidate so library reloads fresh data after import
            libraryService.invalidateCache();
            onComplete.accept(imported, skipped, failed);
        });
    }

    public void collectMusicFiles(File folder, List<File> results) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectMusicFiles(file, results);
            } else if (isMusicFile(file)) {
                results.add(file);
            }
        }
    }

    public boolean isMusicFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".m4a");
    }


    /**
     * A functional interface for callbacks that accept three arguments.
     *
     * <p>Used by {@link #startImport} to pass progress and completion data
     * from the background thread to the caller without requiring a custom
     * class or a {@link java.util.function.BiConsumer} workaround.
     *
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <C> the type of the third argument
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}