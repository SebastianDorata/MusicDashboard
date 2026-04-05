package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


/**
 * JPA entity representing a music album.
 *
 * <p>Holds the title, optional release year, and album art file path.
 * Owns a one-to-many relationship to its {@link Song songs} and a
 * many-to-many relationship to its {@link Artist artists} via the
 * {@code album_artists} join table.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "albums")
public class Album {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "release_year")
    private Integer releaseYear;

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
                ", releaseYear=" + releaseYear +
                '}';
    }
}