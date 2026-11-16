package com.sebastiandorata.musicdashboard.service.handlers;

import com.sebastiandorata.musicdashboard.utils.DoublyLinkedList;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * In-memory cache for analytics {@link DoublyLinkedList} datasets, shared by all
 * users of the web application and scoped per user.
 *
 * <p>The web server handles many users at once, so a single shared map would
 * leak one user's listening data into another user's response. Entries are
 * therefore keyed by the composite key {@code userId + ":" + key}, and
 * {@link #invalidateUser(Long)} drops every entry that belongs to one user.</p>
 *
 * <p>The server also runs for days, so entries expire. Each cached value stores
 * the time it was loaded, and an entry older than {@link #TTL_MILLIS} is reloaded
 * on the next request. Expired entries are swept from the map on each cache miss
 * so memory stays bounded by the live entries.</p>
 *
 * <p>The map is a {@link ConcurrentHashMap} because request threads read and
 * write it concurrently. Two requests that miss at the same moment may both run
 * the loader; the last write wins, which is harmless because both loaders return
 * the same data.</p>
 *
 * <p><b><u>Cache invalidation:</u></b></p>
 * <ul>
 *   <li>Call {@link #invalidateUser(Long)} after that user's playback history
 *       changes (for example after a song is played).</li>
 *   <li>Call {@link #invalidate(String)} to drop one unscoped key, or
 *       {@link #invalidateAll()} to clear everything, for example after a bulk import.</li>
 * </ul>
 *
 * <p>Time Complexity: O(1) average per get/put (ConcurrentHashMap). A miss also
 * sweeps expired entries, which is O(n) where n = cached entries.</p>
 * <p>Space Complexity: O(n) where n = live cached entries across all users.</p>
 */
@Service
public class AnalyticsCacheService {

    /** How long a loaded dataset stays valid, in milliseconds (60 seconds). */
    private static final long TTL_MILLIS = 60_000L;

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    /**
     * A cached dataset and the time it was loaded.
     *
     * @param value        the cached list (may be {@code null} if the loader returned {@code null})
     * @param loadedAtMillis the {@link System#currentTimeMillis()} value when the entry was stored
     * @param <T>          the element type of the list
     */
    private record CacheEntry<T>(DoublyLinkedList<T> value, long loadedAtMillis) {

        /**
         * Tells whether this entry is still within its time-to-live.
         *
         * @param nowMillis the current time in milliseconds
         * @return {@code true} if the entry has not yet expired
         */
        boolean isFresh(long nowMillis) {
            return nowMillis - loadedAtMillis < TTL_MILLIS;
        }
    }

    /**
     * Returns the cached list for one user and key, or loads it via {@code loader}
     * when there is no entry or the entry is older than 60 seconds.
     *
     * <p>The entry is stored under the composite key {@code userId + ":" + key},
     * so two users never share a cached value even when they use the same key.</p>
     *
     * <p>Time Complexity: O(1) on a fresh hit; O(n) on a miss (the loader plus the expiry sweep)</p>
     * <p>Space Complexity: O(n) on a miss, where n = size of the loaded list</p>
     *
     * @param userId the owner of the cached data
     * @param key    cache key within the user's scope (e.g. "history", "topSongs", "topAlbums", "topArtists")
     * @param loader supplier that builds the full list; only called on a miss or after expiry
     * @param <T>    element type stored in the list
     * @return the cached or freshly loaded list
     */
    public <T> DoublyLinkedList<T> getOrLoad(Long userId, String key, Supplier<DoublyLinkedList<T>> loader) {
        return getOrLoad(userId + ":" + key, loader);
    }

    /**
     * Returns the cached list for an unscoped key, or loads it via {@code loader}
     * when there is no entry or the entry is older than 60 seconds.
     *
     * <p>Prefer {@link #getOrLoad(Long, String, Supplier)} for user data, so that
     * entries are scoped per user.</p>
     *
     * <p>Time Complexity: O(1) on a fresh hit; O(n) on a miss (the loader plus the expiry sweep)</p>
     * <p>Space Complexity: O(n) on a miss, where n = size of the loaded list</p>
     *
     * @param key    cache key (used as-is, without a user prefix)
     * @param loader supplier that builds the full list; only called on a miss or after expiry
     * @param <T>    element type stored in the list
     * @return the cached or freshly loaded list
     */
    @SuppressWarnings("unchecked")
    public <T> DoublyLinkedList<T> getOrLoad(String key, Supplier<DoublyLinkedList<T>> loader) {
        long now = System.currentTimeMillis();
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && entry.isFresh(now)) {
            return (DoublyLinkedList<T>) entry.value();
        }

        DoublyLinkedList<T> loaded = loader.get();
        cache.put(key, new CacheEntry<>(loaded, now));
        cache.values().removeIf(e -> !e.isFresh(now));
        return loaded;
    }

    /**
     * Removes every cached entry that belongs to one user.
     * The next {@link #getOrLoad(Long, String, Supplier)} call for that user reloads from the database.
     *
     * <p>Time Complexity: O(n) where n = cached entries across all users</p>
     * <p>Space Complexity: O(1)</p>
     *
     * @param userId the user whose entries should be dropped
     */
    public void invalidateUser(Long userId) {
        String prefix = userId + ":";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Removes a single unscoped key from the cache.
     * The next call to {@link #getOrLoad(String, Supplier)} for this key reloads from the database.
     *
     * <p>Time Complexity: O(1) average</p>
     * <p>Space Complexity: O(1)</p>
     *
     * @param key cache key to evict
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /**
     * Clears the entire cache, for every user.
     * Call this after a bulk import.
     *
     * <p>Time Complexity: O(n) where n = cached entries</p>
     * <p>Space Complexity: O(1)</p>
     */
    public void invalidateAll() {
        cache.clear();
    }
}
