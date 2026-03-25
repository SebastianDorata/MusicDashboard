package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.SyncQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueueEntry, Long> {

    List<SyncQueueEntry> findByUserIdAndStatus(Long userId, String status);
}
