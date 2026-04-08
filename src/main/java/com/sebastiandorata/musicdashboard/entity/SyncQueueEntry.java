package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity for the offline action queue.
 *
 * <p>Stores a pending operation (action type and JSON-serialised payload),
 * its current status ({@code pending}, {@code synced}, {@code failed}),
 * and the timestamps for creation and last sync. Intended for future
 * multi-device sync support.</p>
 */
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "sync_queue")
public class SyncQueueEntry {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_id")
    private Long syncId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data;

    @Column(name = "status", length = 20)
    private String status = "pending";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    public SyncQueueEntry() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SyncQueue{" +
                "syncId=" + syncId +
                ", actionType='" + actionType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
