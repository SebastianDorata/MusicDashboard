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

### Refactored artist navigation into a centralized ArtistDiscographyNavigation hub
- All cross-page artist drill-in calls (Dashboard Top Artists panel, Playback Panel now-playing bar) now route through this single Spring bean via a Consumer<Artist> callback, removing direct dependencies on MyLibraryController from Dashboard components. 
- Fixed a LazyInitializationException by re-fetching artists within a @Transactional context using a JOIN FETCH query before accessing lazy-loaded album collections. 
- Used @Lazy injection to resolve a circular Spring dependency between ArtistDiscographyNavigation and MyLibraryController.