package com.sebastiandorata.musicdashboard.entity;


import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a music track.
 *
 * <p>Stores the title, absolute file path, and full audio metadata
 * (duration, format, codec, bit rate, sample rate, channels, file size,
 * track number). Also maintains playback statistics: listen count and
 * date first listened. Participates in many-to-many relationships with
 * {@link Artist artists} and {@link Genre genres}, and a many-to-one
 * relationship with its {@link Album}.</p>
 *
 * <h2>Identity</h2>
 * <p>{@code filePath} is now enforced unique at the database level. This
 * is the fix for the "Query did not return a unique result: 2 results were
 * returned" crash — previously nothing stopped two rows from sharing a path.
 *
 * <p>{@code contentFingerprint} is a SHA-256 hash of the song's normalized
 * title + primary artist + duration, computed by
 * {@code SongMetadataExtractor}. It gives the song a stable identity that
 * survives the file being moved, re-imported under a different path, or
 * imported again on a different machine/session — the basis for future
 * multi-session sync — without needing to write an ID into the audio file
 * itself.
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "songs", indexes = {
        @Index(name = "idx_songs_content_fingerprint", columnList = "content_fingerprint")
})
public class Song {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_id")
    private Long songID;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "date_first_listened")
    private LocalDate dateFirstListened;

    @Column(name = "listen_count")
    private Integer listenCount = 0;

    @Column(name = "duration_seconds")
    private Integer duration;

    /**
     * Enforced unique so two rows can never point at the same physical file.
     * NOTE: if you already have duplicate rows in your database, run
     * sql/01_dedupe_songs_and_albums.sql BEFORE this constraint is applied,
     * or Hibernate's schema update / your migration tool will fail to add it.
     */
    @Column(name = "file_path", nullable = false, length = 500, unique = true)
    private String filePath;

    @Column(name = "file_format", length = 10)
    private String fileFormat;

    @Column(name = "codec", length = 50)
    private String codec;

    @Column(name = "bit_rate")
    private Integer bitRate;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column(name = "channels")
    private Integer channels;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "track_number")
    private Integer trackNum;

    /**
     * Path-independent identity hash (title + primary artist + duration).
     * Indexed (not uniquely constrained — see class javadoc) so
     * {@code SongUpsertService} can find "this same song under a new path"
     * quickly instead of scanning the whole table.
     */
    @Column(name = "content_fingerprint", length = 64)
    private String contentFingerprint;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_artists",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id"),
            indexes = {
                    @Index(name = "idx_song_artists_song_id", columnList = "song_id"),
                    @Index(name = "idx_song_artists_artist_id", columnList = "artist_id")
            }
    )
    private Set<Artist> artists = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_genres",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"),
            indexes = {
                    @Index(name = "idx_song_genres_song_id", columnList = "song_id"),
                    @Index(name = "idx_song_genres_genre_id", columnList = "genre_id")
            }
    )
    private Set<Genre> genres = new HashSet<>();


    @Override
    public String toString() {
        String artistName = "Unknown Artist";
        if (artists != null && !artists.isEmpty()) {
            artistName = artists.iterator().next().getName();
        }
        return title + " - " + artistName;
    }
}