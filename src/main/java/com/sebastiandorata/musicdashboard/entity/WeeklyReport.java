package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "weekly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "year", "week_of_year"})
})
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "week_of_year", nullable = false)
    private Integer weekOfYear;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    private Integer totalSongsPlayed;

    private Integer totalListeningTimeMinutes;

    @ManyToOne
    @JoinColumn(name = "top_song_id")
    private Song topSong;

    @ManyToOne
    @JoinColumn(name = "top_artist_id")
    private Artist topArtist;

    @ManyToOne
    @JoinColumn(name = "top_album_id")
    private Song topAlbum;

    @ManyToOne
    @JoinColumn(name = "top_genre_id")
    private Genre topGenre;

    public WeeklyReport() {}

}