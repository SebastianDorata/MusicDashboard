package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "albums")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "album_art_path", length = 500)
    private String albumArtPath;

    @OneToMany(mappedBy = "album", fetch = FetchType.EAGER)
    private List<Song> songs;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "album_artists",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists;

    @Override
    public String toString() {
        return "Album{" +
                "albumId=" + albumId +
                ", title='" + title + '\'' +
                ", releaseDate=" + releaseDate +
                '}';
    }
}