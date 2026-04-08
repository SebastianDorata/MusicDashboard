package com.sebastiandorata.musicdashboard.utils;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;

import java.util.Comparator;

/**
 * Defines sorting strategies for songs and albums used across the library views.
 *
 * <p>Each constant provides two comparators, one for {@link Song}
 * and one for {@link Album}.The same strategy is applied consistently across both entity types.
 *
 * <p>Time Complexity: O(n log n) for the sorting operation that uses these comparators.
 * Space Complexity: O(1). All comparators are stateless.
 */
public enum SortStrategy {

    ALPHABETICAL("Alphabetical") {
        @Override
        public Comparator<Song> getSongComparator() {
            return (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle());
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle());
        }
    },

    REVERSE_ALPHABETICAL("Reverse Alphabetical") {
        @Override
        public Comparator<Song> getSongComparator() {
            return (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle());
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle());
        }
    },

    RECENTLY_ADDED("Recently Added") {
        @Override
        public Comparator<Song> getSongComparator() {
            return (a, b) -> {
                if (a.getDateFirstListened() == null) return 1;
                if (b.getDateFirstListened() == null) return -1;
                return b.getDateFirstListened().compareTo(a.getDateFirstListened());
            };
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return (a, b) -> {
                java.time.LocalDate dateA = getNewestSongDate(a);
                java.time.LocalDate dateB = getNewestSongDate(b);
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            };
        }
    },

    MOST_PLAYED("Most Played") {
        @Override
        public Comparator<Song> getSongComparator() {
            return (a, b) -> Integer.compare(
                    b.getListenCount() != null ? b.getListenCount() : 0,
                    a.getListenCount() != null ? a.getListenCount() : 0
            );
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return (a, b) -> {
                int countA = getTotalPlayCount(a);
                int countB = getTotalPlayCount(b);
                return Integer.compare(countB, countA);
            };
        }
    };

    private final String displayName;

    SortStrategy(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a readable display name for this strategy, used in the sort dropdown in the library view.
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a {@link Comparator} for sorting {@link Song}
     * entities according to this strategy.
     * @return a stateless song comparator
     */
    public abstract Comparator<Song> getSongComparator();

    /**
     * Returns a {@link Comparator} for sorting {@link Album}
     * entities according to this strategy.
     * @return a stateless album comparator
     */
    public abstract Comparator<Album> getAlbumComparator();

    /**
     * Gets the newest song date in an album.
     */
    private static java.time.LocalDate getNewestSongDate(Album album) {
        if (album.getSongs() == null || album.getSongs().isEmpty()) return null;
        return album.getSongs().stream()
                .map(Song::getDateFirstListened)
                .filter(d -> d != null)
                .max(java.time.LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Calculates total play count for an album.
     */
    private static int getTotalPlayCount(Album album) {
        if (album.getSongs() == null) return 0;
        return album.getSongs().stream()
                .mapToInt(s -> s.getListenCount() != null ? s.getListenCount() : 0)
                .sum();
    }
}