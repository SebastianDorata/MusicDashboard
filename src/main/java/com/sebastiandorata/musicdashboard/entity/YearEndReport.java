package com.sebastiandorata.musicdashboard.entity;

import com.sebastiandorata.musicdashboard.service.handlers.YearEndReportService;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity that persists an annual listening summary used by the Wrapped slideshow.
 *
 * <p>Stores total listening minutes, total songs played, and the top
 * {@link Song}, {@link Artist}, album {@link Song}, and {@link Genre}
 * for the year. A unique constraint on (user_id, year) prevents duplicates;
 * race conditions are handled by
 * {@link YearEndReportService}
 * using {@code SERIALIZABLE} transaction isolation.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "year_end_reports", uniqueConstraints = {//Prevent duplicates from ever being created again
        @UniqueConstraint(columnNames = {"user_id", "year"})
})
public class YearEndReport {

    @EqualsAndHashCode.Include
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
    @JoinColumn(name = "top_album_id")
    private Song topAlbum;

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