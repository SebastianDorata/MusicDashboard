package com.sebastiandorata.musicdashboard.utils;

import com.sebastiandorata.musicdashboard.entity.Album;
import com.sebastiandorata.musicdashboard.entity.Song;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Defines sorting strategies for songs and albums used across
 * the library views.
 *
 * <p>Each constant provides two comparators — one for Song and
 * one for Album. RECENTLY_ADDED and MOST_PLAYED both include
 * alphabetical tiebreakers so the secondary order is always
 * deterministic rather than falling through to database order.</p>
 */
public enum SortStrategy {

    ALPHABETICAL("Alphabetical") {
        @Override
        public Comparator<Song> getSongComparator() {
            return (a, b) -> {
                String ta = a.getTitle() != null ? a.getTitle() : "";
                String tb = b.getTitle() != null ? b.getTitle() : "";
                return ta.compareToIgnoreCase(tb);
            };
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return (a, b) -> {
                String ta = a.getTitle() != null ? a.getTitle() : "";
                String tb = b.getTitle() != null ? b.getTitle() : "";
                return ta.compareToIgnoreCase(tb);
            };
        }
    },

    REVERSE_ALPHABETICAL("Reverse Alphabetical") {
        @Override
        public Comparator<Song> getSongComparator() {
            return (a, b) -> {
                String ta = a.getTitle() != null ? a.getTitle() : "";
                String tb = b.getTitle() != null ? b.getTitle() : "";
                return tb.compareToIgnoreCase(ta);
            };
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return (a, b) -> {
                String ta = a.getTitle() != null ? a.getTitle() : "";
                String tb = b.getTitle() != null ? b.getTitle() : "";
                return tb.compareToIgnoreCase(ta);
            };
        }
    },

    RECENTLY_ADDED("Recently Added") {
        @Override
        public Comparator<Song> getSongComparator() {
            return Comparator
                    // Primary: most recent date first, nulls pushed to end
                    .<Song, LocalDate>comparing(
                            s -> s.getDateFirstListened(),
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
                    // Tiebreaker: alphabetical within the same date
                    .thenComparing(s -> s.getTitle() != null ? s.getTitle() : "",
                            String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return Comparator
                    .<Album, LocalDate>comparing(
                            a -> getNewestSongDate(a),
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
                    .thenComparing(a -> a.getTitle() != null ? a.getTitle() : "",
                            String.CASE_INSENSITIVE_ORDER);
        }
    },

    MOST_PLAYED("Most Played") {
        @Override
        public Comparator<Song> getSongComparator() {
            return Comparator
                    // Primary: highest play count first
                    .<Song, Integer>comparing(
                            s -> s.getListenCount() != null ? s.getListenCount() : 0,
                            Comparator.reverseOrder()
                    )
                    // Tiebreaker: alphabetical within the same count
                    .thenComparing(s -> s.getTitle() != null ? s.getTitle() : "",
                            String.CASE_INSENSITIVE_ORDER);
        }

        @Override
        public Comparator<Album> getAlbumComparator() {
            return Comparator
                    .<Album, Integer>comparing(
                            a -> getTotalPlayCount(a),
                            Comparator.reverseOrder()
                    )
                    .thenComparing(a -> a.getTitle() != null ? a.getTitle() : "",
                            String.CASE_INSENSITIVE_ORDER);
        }
    };

    private final String displayName;

    SortStrategy(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public abstract Comparator<Song>  getSongComparator();
    public abstract Comparator<Album> getAlbumComparator();

    private static LocalDate getNewestSongDate(Album album) {
        if (album.getSongs() == null || album.getSongs().isEmpty())
            return null;
        return album.getSongs().stream()
                .map(Song::getDateFirstListened)
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    private static int getTotalPlayCount(Album album) {
        if (album.getSongs() == null) return 0;
        return album.getSongs().stream()
                .mapToInt(s -> s.getListenCount() != null
                        ? s.getListenCount() : 0)
                .sum();
    }
}