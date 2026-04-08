package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Session-scoped in-memory cache for analytics {@link DoublyLinkedList} datasets.
 *
 * <p>Loads the full list once per session key, stores it here, and returns the
 * cached version on subsequent calls. The cache lives for the lifetime of the
 * Spring application context (one user session), so it never goes stale between
 * page navigations within the same login.</p>
 *
 * <p><b><u>Cache invalidation:</u></b></p>
 * <ul>
 *   <li>Call {@link #invalidate(String)} or {@link #invalidateAll()} after any
 *       operation that changes playback history (after a new song is played).</li>
 * </ul>
 *
 * <p>Time Complexity: O(1) amortized per get/put (HashMap).</p>
 * <p>Space Complexity: O(n) where n = total cached items across all keys.</p>
 */
@Service
public class AnalyticsCacheService {// Purpose: Every time a user opens a paginated modal (e.g. "Top Songs"),
                                    // the current code re-queries the database and rebuilds the full sorted list from scratch.
                                    // For a user with thousands of plays this is expensive and blocks the UI thread.

    private final Map<String, DoublyLinkedList<?>> cache = new HashMap<>();

    /**
     * Returns the cached list for {@code key}, or loads it via {@code loader}
     * on a cache miss, stores it, and returns the result.
     *
     * @param key    Cache key (e.g. "history", "topSongs", "topAlbums", "topArtists")
     * @param loader Supplier that builds the full list — only called on a miss
     * @param <T>    Element type stored in the list
     * @return       The cached (or freshly loaded) DoublyLinkedList
     *
     * Time Complexity : O(1) on hit; O(n) on miss (delegated to loader)
     * Space Complexity: O(n) on miss
     */
    @SuppressWarnings("unchecked")
    public <T> DoublyLinkedList<T> getOrLoad(String key, Supplier<DoublyLinkedList<T>> loader) {
        if (!cache.containsKey(key)) {
            cache.put(key, loader.get());
        }
        return (DoublyLinkedList<T>) cache.get(key);
    }

    /**
     * Removes a single key from the cache.
     * The next call to {@link #getOrLoad} for this key will re-fetch from the DB.
     *
     * @param key Cache key to evict
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /**
     * Clears the entire cache.
     * Call this after a bulk import or after the user logs out.
     */
    public void invalidateAll() {
        cache.clear();
    }
}




