# Changelog

### Performance
- Switched Song, Album, Artist, Playlist relationships from
  FetchType.EAGER to FetchType.LAZY to eliminate N+1 query
  cascade on library load
- Added JOIN FETCH repository methods to load songs with artists
  and genres in a single query instead of N+1 queries
- Added DB-side filtering for monthly/yearly playback history
  queries instead of loading all history into memory
- Added in-memory cache to LibraryService for songs, albums,
  and artists with cache invalidation on import
- Added join table indexes on song_artists, song_genres,
  album_artists for faster JOIN FETCH execution
- Fixed PlaybackTrackingService.getRecentlyPlayed() to limit
  results to 50 rows instead of loading full history
