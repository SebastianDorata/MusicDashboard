# Changelog

## 2026-04-13

### Fixed
- LazyInitializationException on bulk import in SongImportService.
  When importing multiple songs from the same album, the second and subsequent
  songs would fail because the album entity was loaded in one Hibernate session
  via albumRepository.findByTitle(), but the import executor runs on a background
  thread. By the time the code reached album.getArtists().contains(artist), the
  original session was closed and Hibernate could not lazily load the artists
  collection from the detached entity. The first song in each album always
  succeeded because it created a new album within the same session.
  Fixed by adding @Transactional to SongImportService.importSong(), keeping a
  single Hibernate session open for the entire duration of the method.

- Album cards now correctly separate left-click (drill into album) and
  right-click (open edit context menu). Previously, right-clicking would
  trigger both actions simultaneously.

- PlaybackTrackingService.getRecentlyPlayed() now limits results to 50 rows
  instead of loading the full history on every dashboard refresh.

## 2026-04-12

### Fixed
- LazyInitializationException in artist navigation fixed by re-fetching artists
  within a @Transactional context using a JOIN FETCH query before accessing
  lazy-loaded album collections.

### Performance
- Switched Song, Album, Artist, and Playlist relationships from FetchType.EAGER
  to FetchType.LAZY to eliminate N+1 query cascade on library load.
- Added JOIN FETCH repository methods to load songs with artists and genres in a
  single query instead of N+1 queries.
- Added DB-side filtering for monthly and yearly playback history queries instead
  of loading all history into memory before filtering.
- Added in-memory cache to LibraryService for songs, albums, and artists with
  cache invalidation on import.
- Added join table indexes on song_artists, song_genres, and album_artists for
  faster JOIN FETCH execution.

### Refactored
- Artist navigation centralized into ArtistDiscographyNavigation hub. All
  cross-page artist drill-in calls (Dashboard Top Artists panel, Playback Panel
  now-playing bar) now route through this single Spring bean via a
  Consumer<Artist> callback, removing direct dependencies on MyLibraryController
  from Dashboard components.
- Used @Lazy injection to resolve a circular Spring dependency between
  ArtistDiscographyNavigation and MyLibraryController.