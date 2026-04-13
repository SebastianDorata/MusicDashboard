package com.sebastiandorata.musicdashboard.entity;


import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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
 * <p><b><u>References:</u></b></p>
 *<ul>
 *  <li><a href="https://stackoverflow.com/questions/2990799/difference-between-fetchtype-lazy-and-eager-in-java-persistence-api">
 *      FetchType in Java Persistence API </a> </li>
 *  </ul>
 *
 *
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "songs")
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

    @Column(name = "file_path", nullable = false, length = 500)
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

