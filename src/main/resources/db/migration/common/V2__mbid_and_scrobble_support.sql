ALTER TABLE artists ADD COLUMN mbid VARCHAR(36);
ALTER TABLE artists ADD CONSTRAINT uq_artists_mbid UNIQUE (mbid);

ALTER TABLE albums ADD COLUMN mbid VARCHAR(36);
ALTER TABLE albums ADD CONSTRAINT uq_albums_mbid UNIQUE (mbid);

ALTER TABLE songs ADD COLUMN mbid VARCHAR(36);
ALTER TABLE songs ADD COLUMN is_placeholder BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE playback_history ADD COLUMN external_scrobble_id VARCHAR(64);
ALTER TABLE playback_history ADD CONSTRAINT uq_playback_external_scrobble UNIQUE (external_scrobble_id);