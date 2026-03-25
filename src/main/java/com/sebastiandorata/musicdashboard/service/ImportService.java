package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.utils.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class ImportService{

    @Lazy
    @Autowired
    private SongService songService;

    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();

    public void startImport(List<File> files, TriConsumer<Integer, Integer, String> onProgress, TriConsumer<Integer, Integer, Integer> onComplete) {
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
                    System.err.println("Failed to import: " + file.getName() + " — " + e.getMessage());
                }
            }

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
}