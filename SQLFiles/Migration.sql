-- 1. This migration assumes null duration = full song play (natural end)
--    This is safe because:
--    - Old code only set duration to null for natural completions
--    - Skips always had a duration recorded (via recordPlayWithDuration)

-- 2. After this migration, PlaybackConstants.isValidPlay() will count these
--    records if they're >= 20 seconds (most songs are longer)

-- 3. Year-over-year comparisons will shift due to recovered data
--    This is expected and correct

-- ------------------------------------------------------------------------------------  
-- See how many records will be affected
SELECT 
    COUNT(*) AS null_duration_records,
    SUM(s.duration_seconds) AS total_recovered_seconds,
    ROUND(SUM(s.duration_seconds) / 60.0, 2) AS total_recovered_minutes,
    ROUND(SUM(s.duration_seconds) / 3600.0, 2) AS total_recovered_hours
FROM playback_history ph
JOIN songs s ON ph.song_id = s.song_id
WHERE ph.duration_played_seconds IS NULL
  AND ph.completed = false;
-- Sample breakdown by user
SELECT 
    u.username,
    COUNT(*) AS null_records,
    ROUND(SUM(s.duration_seconds) / 60.0, 2) AS recovered_minutes
FROM playback_history ph
JOIN users u ON ph.user_id = u.user_id
JOIN songs s ON ph.song_id = s.song_id
WHERE ph.duration_played_seconds IS NULL
  AND ph.completed = false
GROUP BY u.user_id, u.username
ORDER BY recovered_minutes DESC;
-- ------------------------------------------------------------------------------------  


-- PostgreSQL: Create backup table
CREATE TABLE playback_history_backup_pre_migration AS
SELECT * FROM playback_history
WHERE duration_played_seconds IS NULL
  AND completed = false;

-- Verify backup
SELECT COUNT(*) FROM playback_history_backup_pre_migration;
-- ------------------------------------------------------------------------------------  


-- Update null durations with song duration
UPDATE playback_history
SET duration_played_seconds = s.duration_seconds,
    completed = true
FROM songs s
WHERE playback_history.song_id = s.song_id
  AND playback_history.duration_played_seconds IS NULL
  AND playback_history.completed = false;

-- Log the change
SELECT 'Migration Complete' AS status,
       NOW() AS migration_timestamp,
       (SELECT COUNT(*) FROM playback_history WHERE duration_played_seconds IS NULL) AS remaining_nulls;
-- Check: No more null durations in playback_history
SELECT COUNT(*) AS null_durations_remaining
FROM playback_history
WHERE duration_played_seconds IS NULL;

-- Check: Updated records are marked completed
SELECT COUNT(*) AS marked_completed
FROM playback_history_backup_pre_migration ph_old
JOIN playback_history ph_new ON ph_old.playback_id = ph_new.playback_id
WHERE ph_new.completed = true
  AND ph_new.duration_played_seconds IS NOT NULL;
-- Expected: Equal to backup record count

-- Check: Duration values are reasonable (> 0)
SELECT 
    MIN(duration_played_seconds) AS min_duration,
    AVG(duration_played_seconds) AS avg_duration,
    MAX(duration_played_seconds) AS max_duration
FROM playback_history
WHERE playback_id IN (SELECT playback_id FROM playback_history_backup_pre_migration);
-- ------------------------------------------------------------------------------------------------------------------------------  

-- Drop backup after verification
DROP TABLE playback_history_backup_pre_migration;
-- ------------------------------------------------------------------------------------------------------------------------------  


-- After migration, re-generate YearEndReports to recalculate with recovered data
DELETE FROM year_end_reports WHERE user_id = 8 AND year = 2026;
-- JPA/Hibernate will regenerate on next demand via YearEndReportService

-- RollBack
-- 1. Restore from backup:
-- DELETE FROM playback_history
-- WHERE playback_id IN (SELECT playback_id FROM playback_history_backup_pre_migration);
-- 
-- 2. INSERT INTO playback_history
-- SELECT * FROM playback_history_backup_pre_migration;
-- 
-- 3.
-- TRUNCATE TABLE year_end_reports CASCADE;
