package com.sebastiandorata.musicdashboard.repository;

import com.sebastiandorata.musicdashboard.entity.SyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncQueueRepository extends JpaRepository<SyncQueue, Long> {

    List<SyncQueue> findByUserIdAndStatus(Long userId, String status);
}
