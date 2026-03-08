package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "year_end_reports")
public class YearEndReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_listening_time_minutes")
    private Integer totalListeningTimeMinutes;

    @Column(name = "total_songs_played")
    private Integer totalSongsPlayed;

    @ManyToOne
    @JoinColumn(name = "top_song_id")
    private Song topSong;

    @ManyToOne
    @JoinColumn(name = "top_artist_id")
    private Artist topArtist;

    @ManyToOne
    @JoinColumn(name = "top_genre_id")
    private Genre topGenre;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public YearEndReport() {
        this.generatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "YearEndReport{" +
                "reportId=" + reportId +
                ", year=" + year +
                ", totalSongsPlayed=" + totalSongsPlayed +
                '}';
    }
}