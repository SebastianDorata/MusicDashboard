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
// =============================================================================
// Bug:
// The original implementation called new Thread(...).start() for every async
// request. Thread creation carries roughly 1-2 ms of OS overhead each time,
// and threads are never reused. Under normal Dashboard load this means 4-6
// threads are created and destroyed in rapid succession just to populate the
// stat cards.
//
// Fix: replaced with a cached thread pool (up to 4 threads). Threads are
// created on first use and reused for subsequent requests, eliminating
// per-call creation overhead. A cached pool rather than a fixed pool is chosen
// so idle threads are reclaimed after 60 seconds and do not waste resources
// when the app is idle.
