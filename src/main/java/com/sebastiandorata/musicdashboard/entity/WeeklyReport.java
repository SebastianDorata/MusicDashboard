package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity that persists a pre-computed weekly listening summary for one user.
 *
 * <p>Stores the total songs played, total listening minutes, and the top
 * {@link Song}, {@link Artist}, album {@link Song}, and {@link Genre} for
 * the requested ISO week. A unique constraint on
 * (user_id, year, week_of_year) prevents duplicate reports.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "weekly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "year", "week_of_year"})
})
public class WeeklyReport {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
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