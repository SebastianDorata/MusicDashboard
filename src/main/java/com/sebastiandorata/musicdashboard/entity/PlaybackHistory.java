package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity recording a single playback event.
 *
 * <p>Stores the {@link User}, the {@link Song} played, the wall-clock
 * timestamp ({@code played_at}), the duration played in seconds, and
 * whether the song was played to completion. Composite indexes on
 * (user_id, played_at) and (song_id, played_at) accelerate the analytics
 * queries that filter by user and date range.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "playback_history", indexes = {
        @Index(name = "idx_user_played_at", columnList = "user_id, played_at"),
        @Index(name = "idx_song_played_at", columnList = "song_id, played_at")
})
public class PlaybackHistory {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playback_id")
    private Long playbackId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    @Column(name = "duration_played_seconds")
    private Integer durationPlayedSeconds;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "source", length = 50)
    private String source;

    public PlaybackHistory() {
        this.playedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "PlaybackHistory{" +
                "playbackId=" + playbackId +
                ", playedAt=" + playedAt +
                ", completed=" + completed +
                '}';
    }
}