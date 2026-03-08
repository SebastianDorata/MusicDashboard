package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "favourites")  // ← Changed to Canadian spelling
public class Favourite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favourite_id")  // ← Changed to Canadian spelling
    private Long favouriteId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "favourited_at", nullable = false)  // ← Changed to Canadian spelling
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