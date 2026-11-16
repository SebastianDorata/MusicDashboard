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

    /**
     * Returns one user's sync queue entries that have the given status.
     *
     * @param userId the ID of the owning user
     * @param status the status to match, such as {@code pending}, {@code synced} or {@code failed}
     * @return the matching sync entries; an empty list if there are none
     */
    List<SyncQueueEntry> findByUserIdAndStatus(Long userId, String status);
}
