package com.sebastiandorata.musicdashboard.service.handlers;

import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Asynchronous data loader backed by a fixed thread pool.
 *
 * <p>Time Complexity: O(1) per submission (task queued immediately).</p>
 * <p>Space Complexity: O(1), pool size is bounded.</p>
 */
@Service
public class DataLoadingService {

    /**
     * Bounded cached pool: at most 4 concurrent background loads.
     * Covers the worst case (all stat cards, graph, top artists, recently played
     * loading simultaneously on Dashboard open) without over-threading.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "data-loader");
        t.setDaemon(true); // don't prevent JVM shutdown
        return t;
    });

    public <T> void loadAsync(Supplier<T> loader,
                              Consumer<T> onSuccess,
                              Consumer<Exception> onError) {
        executor.submit(() -> {
            try {
                T result = loader.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    public <T> void loadAsync(Supplier<T> loader, Consumer<T> onSuccess) {
        loadAsync(
                loader,
                onSuccess,
                e -> System.err.println("[DataLoadingService] load failed: " + e.getMessage())
        );
    }
}
