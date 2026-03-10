package com.sebastiandorata.musicdashboard.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "songs")
public class Song {

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



    @ManyToMany
    @JoinTable(
            name = "song_artists",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
            )
    private List<Artist> artists;

    @ManyToMany
    @JoinTable(
            name = "song_genres",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;


    @Override
    public String toString() {
        String artistName = "Unknown Artist";
        if (artists != null && !artists.isEmpty()) {
            artistName = artists.get(0).getName();
        }

        return title + " - " + artistName;
    }
}

