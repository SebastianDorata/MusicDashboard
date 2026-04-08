package com.sebastiandorata.musicdashboard.entity;

import com.sebastiandorata.musicdashboard.service.MonthlyReportService;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity that persists a pre-computed monthly listening summary for one user.
 *
 * <p>Stores the total songs played, total listening minutes, and the top
 * {@link Song}, {@link Artist}, album {@link Song}, and {@link Genre} for
 * the requested month. A unique constraint on (user_id, year, month)
 * prevents duplicate reports; race conditions are handled by
 * {@link MonthlyReportService}
 * using {@code SERIALIZABLE} transaction isolation.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "monthly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "year", "month"})
})
public class MonthlyReport {

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

    @Column(nullable = false)
    private Integer month;

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

    public MonthlyReport() {}

}