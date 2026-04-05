package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity recording a song that a user has marked as a favourite.
 *
 * <p>Each row links a {@link User} to a {@link Song} and stores the
 * timestamp at which the song was favourited. A unique constraint on
 * (user_id, song_id) prevents duplicate favourites.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "favourites")
public class Favourite {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favourite_id")
    private Long favouriteId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "song_id", nullable = false)
    private Song songId;

    @Column(name = "favourited_at", nullable = false)
    private LocalDateTime favouritedAt;

    public Favourite() {
        this.favouritedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Favourite{" +
                "favouriteId=" + favouriteId +
                ", favouritedAt=" + favouritedAt +
                '}';
    }
}