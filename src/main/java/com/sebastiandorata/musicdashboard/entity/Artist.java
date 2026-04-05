package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * JPA entity representing a music artist.
 *
 * <p>Stores the artist name and an optional biography. Participates in
 * bidirectional many-to-many relationships with both {@link Song} (via
 * {@code song_artists}) and {@link Album} (via {@code album_artists}).</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "artists")
public class Artist {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id")
    private Long artistId;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @ManyToMany(mappedBy = "artists", fetch = FetchType.EAGER)
    private List<Song> songs;

    @ManyToMany(mappedBy = "artists", fetch = FetchType.EAGER)
    private List<Album> albums;

    @Override
    public String toString() {
        return "Artist{" +
                "artistId=" + artistId +
                ", name='" + name + '\'' +
                '}';
    }
}