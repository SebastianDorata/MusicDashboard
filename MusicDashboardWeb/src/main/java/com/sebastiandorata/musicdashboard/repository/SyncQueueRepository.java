package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.SyncQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 * Spring Data JPA repository for {@link SyncQueueEntry} entities.
 *
 * <p>Provides lookup of pending sync entries by user and status string,
 * supporting future multi-device sync processing.</p>
 */
@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueueEntry, Long> {

    List<SyncQueueEntry> findByUserIdAndStatus(Long userId, String status);
}
