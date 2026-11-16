package com.sebastiandorata.musicdashboard.utils;

import com.sebastiandorata.musicdashboard.entity.Artist;
import com.sebastiandorata.musicdashboard.entity.Song;

/**
 * Utility class for resolving artist display names from {@link Song} entities.
 *
 * <p>Centralizes the artist name resolution pattern that was previously
 * duplicated across at least 8 classes:
 * {@code ListeningHistoryViewModel}, {@code TopSongsViewModel},
 * {@code AnalyticsController}, {@code TopArtistsViewModel},
 * {@code YearEndReportViewModel}, {@code PlaylistViewBuilder},
 * {@code SongCell}, and {@code CardFactory}.</p>
 *
 * <p>All methods are static. This class is not instantiable.</p>
 *
 * <p>Time Complexity: O(1) — operates on the first element of the artist set.<br>
 * Space Complexity: O(1)</p>
 */
public final class ArtistUtils {

    /** Default string returned when no artist can be resolved. */
    public static final String UNKNOWN_ARTIST = "Unknown Artist";

    private ArtistUtils() {}

    /**
     * Returns the name of the first artist on the given song, or
     * {@value #UNKNOWN_ARTIST} if the song has no artists or is {@code null}.
     *
     * <p>This replaces the repeated inline pattern:
     * <pre>
     *   song.getArtists().stream()
     *       .findFirst()
     *       .map(Artist::getName)
     *       .orElse("Unknown Artist")
     * </pre>
     *
     * @param song the song to resolve the artist name from; may be {@code null}
     * @return the primary artist name, or {@value #UNKNOWN_ARTIST}
     */
    public static String getPrimaryArtistName(Song song) {
        if (song == null || song.getArtists() == null || song.getArtists().isEmpty()) {
            return UNKNOWN_ARTIST;
        }
        return song.getArtists().stream()
                .findFirst()
                .map(Artist::getName)
                .orElse(UNKNOWN_ARTIST);
    }

    /**
     * Returns the name of the given artist, or {@value #UNKNOWN_ARTIST}
     * if the artist or its name is {@code null}.
     *
     * <p>Useful when you already have an {@link Artist} reference but
     * want a guaranteed non-null display string.
     *
     * @param artist the artist to resolve the name from; may be {@code null}
     * @return the artist name, or {@value #UNKNOWN_ARTIST}
     */
    public static String getArtistName(Artist artist) {
        if (artist == null || artist.getName() == null) {
            return UNKNOWN_ARTIST;
        }
        return artist.getName();
    }
}
