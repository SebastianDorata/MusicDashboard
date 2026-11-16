package com.sebastiandorata.musicdashboard.service.Import;

import com.sebastiandorata.musicdashboard.dto.MigrationResult;
import com.sebastiandorata.musicdashboard.service.Import.ImportOrchestrator.ImportCallbacks;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of import jobs so the browser can poll the progress of a running import.
 *
 * <p>Each scan or upload creates a {@link Job} with a random id and the id of the user who
 * started it. The job doubles as the {@link ImportCallbacks} handed to
 * {@link ImportOrchestrator}: the import-pipeline thread updates it while HTTP request threads
 * read it through {@link #snapshot(String, Long)}. A job is only ever returned to its owner, and
 * finished jobs are dropped after {@link #RETENTION} so the map cannot grow forever.
 *
 * <p>SRP: only tracks job state; it never touches files or the database.
 *
 * <p>Space Complexity: O(J + R) for J live jobs and R result rows they hold.
 */
@Component
public class ImportJobRegistry {

    /** How long a finished job stays pollable before it is purged. */
    public static final Duration RETENTION = Duration.ofMinutes(30);

    /** Jobs that never finish (should not happen) are purged after this long. */
    private static final Duration MAX_AGE = Duration.ofHours(12);

    /** Lifecycle of a job. */
    public enum State {
        /** Created, working or waiting in the pipeline queue. */
        RUNNING,
        /** Every file was processed; results are available. */
        DONE,
        /** The inputs contained no supported audio files. */
        NO_FILES,
        /** The job failed before it could finish. */
        FAILED
    }

    /**
     * Read-only view of a job returned to the browser.
     *
     * @param jobId       the job id
     * @param state       the job state
     * @param current     1-based index of the file being processed (0 before the first file)
     * @param total       total number of files (0 until known)
     * @param fileName    short name of the file being processed
     * @param message     failure text when the state is {@code FAILED}, otherwise {@code null}
     * @param results     one entry per evaluated file (empty until the job is done)
     * @param startedAt   when the job was created
     */
    public record JobSnapshot(String jobId, State state, int current, int total, String fileName,
                              String message, List<MigrationResult> results, Instant startedAt) { }

    /**
     * Mutable job state shared between the pipeline thread (writer) and request threads (readers).
     *
     * <p>Doubles as the callback object passed to {@link ImportOrchestrator#process}.
     */
    public static final class Job implements ImportCallbacks {
        private final String id = UUID.randomUUID().toString();
        private final Long ownerId;
        private final Instant startedAt = Instant.now();
        private volatile State state = State.RUNNING;
        private volatile int current;
        private volatile int total;
        private volatile String fileName = "";
        private volatile String message;
        private volatile List<MigrationResult> results = List.of();
        private volatile Instant finishedAt;

        private Job(Long ownerId) {
            this.ownerId = ownerId;
        }

        /**
         * Returns the job id.
         *
         * @return the random job id
         */
        public String getId() {
            return id;
        }

        @Override
        public void onNoFiles() {
            state = State.NO_FILES;
            finishedAt = Instant.now();
        }

        @Override
        public void onStart(int totalFiles) {
            total = totalFiles;
            current = 0;
        }

        @Override
        public void onProgress(int current, int total, String fileName) {
            this.total = total;
            this.fileName = fileName;
            this.current = current;
        }

        @Override
        public void onComplete(List<MigrationResult> results) {
            this.results = List.copyOf(results);
            state = State.DONE;
            finishedAt = Instant.now();
        }

        @Override
        public void onFailed(String reason) {
            fail(reason);
        }

        /**
         * Marks the job as failed with a human-readable reason.
         *
         * @param reason the failure text shown to the user
         */
        public void fail(String reason) {
            message = reason;
            state = State.FAILED;
            finishedAt = Instant.now();
        }

        private JobSnapshot snapshot() {
            return new JobSnapshot(id, state, current, total, fileName, message, results, startedAt);
        }
    }

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new running job for a user, purging expired jobs first.
     *
     * @param ownerId the id of the user who started the import (captured on the request thread)
     * @return the new job
     */
    public Job create(Long ownerId) {
        purgeExpired();
        Job job = new Job(ownerId);
        jobs.put(job.id, job);
        return job;
    }

    /**
     * Returns a snapshot of a job, but only to the user who started it.
     *
     * @param jobId   the job id
     * @param ownerId the id of the requesting user
     * @return the snapshot, or {@code null} when the job is unknown, expired or owned by someone else
     */
    public JobSnapshot snapshot(String jobId, Long ownerId) {
        Job job = jobs.get(jobId);
        if (job == null || job.ownerId == null || !job.ownerId.equals(ownerId)) return null;
        return job.snapshot();
    }

    /** Removes finished jobs older than {@link #RETENTION} and unfinished ones older than the max age. */
    private void purgeExpired() {
        Instant now = Instant.now();
        jobs.values().removeIf(j ->
                (j.finishedAt != null && Duration.between(j.finishedAt, now).compareTo(RETENTION) > 0)
                        || Duration.between(j.startedAt, now).compareTo(MAX_AGE) > 0);
    }
}
