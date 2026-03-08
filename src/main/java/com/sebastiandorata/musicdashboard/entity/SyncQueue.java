package com.sebastiandorata.musicdashboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "sync_queue")
public class SyncQueue {

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

    public SyncQueue() {
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
