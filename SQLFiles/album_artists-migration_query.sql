INSERT INTO album_artists (album_id, artist_id)
SELECT DISTINCT s.album_id, sa.artist_id
FROM songs s
JOIN song_artists sa ON s.song_id = sa.song_id
WHERE s.album_id IS NOT NULL
ON CONFLICT DO NOTHING;