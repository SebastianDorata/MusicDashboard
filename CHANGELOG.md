# Changelog

---

## 2026-05-06 v1.0.7
### 1. DoublyLinkedList / "View All" Pagination Bug

**Goal:** Enable the "View All" modal overlays to open correctly when triggered from the analytics page.

**Bug:** Two instance fields in `AnalyticsController` were never assigned in `createScene()`, causing null reference crashes.

- `mainPane` (the `StackPane` passed to every modal call) stayed `null` because a local variable of the same name shadowed the instance field — the overlay had nowhere to attach and was silently dropped.
- `content` (the `BorderPane` used by `refreshSidebar()`) also stayed `null`, causing an NPE on every navigation click.

**Solution:** Two one-line fixes in `createScene()`:

- `Line 110`: Changed `BorderPane root = new BorderPane()` → `content = new BorderPane()` to assign to the instance field.
- `Line 118`: Dropped the local `StackPane` type declaration, changing `StackPane mainPane = new StackPane(root)` → `mainPane = new StackPane(content)` to assign to the instance field.

The root cause of both bugs was the same pattern: declaring a new local variable with the same name as an instance field, which shadows it instead of assigning to it.

### 2. Session Average Inaccuracy

**Goal:** The "Average Listening Period" stat card should measure how long the user has had the app open in the current session, starting from when the app launches.

**Bug:** `sessionStart` was set inside `setCurrentUser()`, which is only called after a successful login. Any time spent on the login screen was excluded from the duration, making the stat inaccurate.

**Solution:** Two files changed.

- `UserSessionService` — added an `appStart` field and a `recordAppStart()` method that stamps the time once on first call. A null-check guard prevents subsequent calls (e.g. from re-showing the login screen) from resetting the clock. `getSessionDurationSeconds()` now uses `appStart` as its reference point, falling back to `sessionStart` only if `recordAppStart()` was never called.
- `JavaFxApplication` — added a single call to `recordAppStart()` at the very top of `start()`, before the stage is configured or the auth screen is shown. This is the earliest point after the Spring context exists and `UserSessionService` can be resolved as a bean.

### 3. Player Controls Position

**Goal:** Move the playback buttons (previous, play/pause, next) so they sit just above the duration bar rather than at the top of the info section.

**Solution:** Modified `createNowPlayingInfoSection()` in `PlaybackPanelController`. Previously, `controls` was added to `infoSection` alongside `songTitle` and `artistName`, before `buildProgressSection()` inserted the spacer and slider. The spacer pushed everything above it (including the controls) to the top of the panel.

The fix removes `controls` from the initial `addAll` call and instead inserts it at `size() - 1` after `buildProgressSection()` runs, placing it after the spacer but before the `timeBox` (slider + time labels) that `buildProgressSection` appends last.

### 4. UI Change: Album Art Oversizing, Player Layout, and Gap Fix

**Goal:** Control the playback panel and stat card heights so album artwork fills the player naturally without overflowing, and eliminate empty gaps when resizing the window.

**Bug:** `albumArtView.prefWidthProperty()` was bound to `nowPlaying.heightProperty()`. Since `nowPlaying` had `setMaxHeight(Double.MAX_VALUE)`, JavaFX let it grow indefinitely, making `prefWidth` very large and crowding out the info section.

**Attempts:** Capping art size helped but didn't control vertical space. Adjusting grow priorities and adding `setMaxHeight` to the player stopped it from stretching, but cards then grew unbounded.

**Final Fixed Layout:** Locked both player and cards with `setPrefHeight` + `setMaxHeight` and `VBox.setVgrow(Priority.NEVER)` on both. Set `top` to `Priority.NEVER` so fixed values were respected without interference.

**Gap Problem:** The `top` region was bound to 60% of scene height, but the center `VBox` had a fixed 380px player and cards at `Priority.NEVER`. Extra height became empty space. The right panel stretched because it had `Priority.ALWAYS` and `setMaxHeight(Double.MAX_VALUE)`.

**Gap Fix:** Changed cards to `Priority.ALWAYS` with `setMaxHeight(Double.MAX_VALUE)` so they absorb extra height. Added `setMaxHeight(Double.MAX_VALUE)` to the center `VBox` so it doesn't cap its children. The player remained locked at 380px with `Priority.NEVER`.

**Why Resizing Broke It:** `top` was set to 60% of scene height using a hardcoded multiplier, not a dynamic binding. On window resize, the value didn't update, so `top` kept its original height and the gap returned. A reactive solution would require binding `top.prefHeightProperty()` to `scene.heightProperty().multiply(0.6)`.






---
## 2026-05-01 v1.0.6
## Library Migration Tool

### What we wanted to build

Music libraries move. Folder structures get reorganised, drives get remounted at different paths, files get renamed, and cover art files get deleted or never extracted in the first place. When any of these things happen, the Music Dashboard database still holds the old absolute file path, so the song becomes unplayable even though the file physically exists on disk.

The goal was a dedicated migration page that lets you point the app at a folder — or an updated copy of your library — and have it automatically reconcile every audio file it finds against the songs already stored in the database. Critically, **no existing data should ever be deleted**: playback history, play counts, favourites, playlist memberships, artist bios, and album associations all survive the scan unconditionally. Files that don't match anything already in the library are ignored; the normal Import page handles new additions.

---

### Version 1 — Path and cover art only

**Files added:**
- `MigrationResult.java` — DTO with statuses: `PATH_UPDATED`, `ALREADY_CURRENT`, `ART_REFRESHED`, `SKIPPED`, `ERROR`
- `LibraryMigrationService.java` — core scan logic
- `LibraryMigrationController.java` — JavaFX UI: folder browser, progress bar, results table
- `MainController.java` — added `"migration"` route and `registerMigration()` hook

**How it worked:**

`scanFolder()` recursively collected every `.mp3` and `.m4a` file under the chosen root folder, then called `processFile()` for each one. `processFile` used two matching strategies in order:

1. **Exact path** — `songRepository.findByFilePath(absolutePath)`. If the file's current absolute path was already in the database, no path update was needed. `maybeRefreshArt()` was called to write `cover.jpg` if the album's art path was missing or stale.
2. **Metadata fingerprint** — if no exact path match, the title and primary artist were read from the audio tag. `findByTitleAndArtist()` searched the full song list in memory with a case-insensitive, lenient prefix match (handling minor tag variations like "The Beatles" vs "Beatles"). On a match, the stored path was updated via a dedicated `updateSongPath()` transactional helper and cover art was refreshed.

**The problem:**

After a rescan, songs were playable again but their metadata was stale. Track numbers, genres, release year, bit rate, and other fields were only ever written during the original import and never touched again by the migration. `processFile` called `updateSongPath()` (which set `filePath` and saved) and `maybeRefreshArt()` (which wrote cover art if missing), but it never re-read any tag fields or wrote them back to the database. The data associated with each song in the database simply reflected whatever the tags said at import time.

---

### Version 2 — Full metadata sync (and the lazy-load crash)

**What changed:**

`updateSongPath()` was replaced by a comprehensive `syncMetadata()` method. For every matched song — regardless of whether the path changed — `syncMetadata` re-reads the audio header and all relevant tag fields and writes back anything that has drifted:

- **Song fields synced:** `filePath`, `title`, `duration`, `trackNumber`, `bitRate`, `sampleRate`, `channels`, `codec`, `fileFormat`, `fileSizeBytes`, `artists`, `genres`
- **Album fields synced:** `releaseYear`, `albumArtPath`

Artist and genre resolution used `resolveArtists()` and `resolveGenres()` — the same create-or-reuse pattern as `SongImportService`. Names already in the database are reused by lookup; new names get a new row; no artist or genre row is ever deleted. `parseTrackNumber()` handled both the `"5"` and `"5/12"` TRACK tag formats. The year tag was truncated to four characters before parsing to handle tags like `"2003-01-01"`.

A new `METADATA_UPDATED` status was added to `MigrationResult` (and wired into the UI table and summary counter) so that songs whose path was already correct but whose metadata was refreshed showed as orange "✎ Metadata Updated" rather than grey "Already Current".

**The problem — `LazyInitializationException`:**

Every file threw the following error at runtime:

```
Cannot lazily initialize collection of role
'com.sebastiandorata.musicdashboard.entity.Song.artists'
with key '4982' (no session)
org.hibernate.LazyInitializationException
    at LibraryMigrationService.syncMetadata(LibraryMigrationService.java:335)
    at LibraryMigrationService.processFile(LibraryMigrationService.java:164)
    at LibraryMigrationService.scanFolder(LibraryMigrationService.java:119)
```

**Root cause:**

`Song.artists` and `Song.genres` are both mapped with `FetchType.LAZY`. Hibernate only loads them from the database when something actually accesses the collection, and it can only do that while a Hibernate session is open.

`syncMetadata` was annotated `@Transactional`, which normally opens a session. However, `processFile` — the method that called `syncMetadata` — was not transactional, and neither was `scanFolder`. In Spring, `@Transactional` works through a proxy: when an external caller invokes a `@Transactional` method, Spring intercepts the call, opens a session, and closes it when the method returns. But when a method calls another method **on the same bean**, the call goes directly to the object — the proxy is bypassed entirely. Because `processFile` called `syncMetadata` as an internal `this.syncMetadata(...)` call, Spring's proxy never ran, no session was opened, and Hibernate had no way to resolve the lazy `artists` collection when `syncMetadata` attempted `song.getArtists().equals(newArtists)` on line 335.

There was a second, related problem: even if the transaction boundary had been correct, comparing the lazy sets with `.equals()` was fragile. `AbstractSet.equals()` calls `.size()` on both sides, which forces Hibernate to load the existing collection from the database. Calling `.size()` on a `PersistentSet` triggers a lazy load — meaning the session must still be open at that moment.

---

### Version 3 — Correct transaction scope (final, working version)

**What changed:**

Two targeted fixes resolved the crash:

**Fix 1 — Move the transaction boundary to `processFile`.**

`processFile` was made `@Transactional` (and `protected` so Spring's proxy can intercept it). The `@Transactional` was removed from `syncMetadata` and `maybeRefreshArt` — they no longer need their own boundary because they now run inside the session that `processFile` opened. This means the entire unit of work for one file (load song, read tags, write all fields, save) happens inside a single Hibernate session. Lazy collections like `artists` and `genres` can be loaded at any point during that method without a "no session" error, because the session stays open for the duration of the call.

`scanFolder` deliberately remains non-transactional. Each file commits independently, so a failure on file 400 does not roll back the path updates already applied to files 1–399.

**Fix 2 — Remove `.equals()` comparisons on lazy collections.**

The artist and genre comparison blocks that called `!newArtists.equals(song.getArtists())` were replaced with unconditional assignments: if the tag has a non-blank value, resolve it and set it on the entity. This avoids loading the existing persistent set purely to compare it. The `songRepository.save()` at the end of `syncMetadata` is idempotent — saving with the same values produces no change in the database — so writing unconditionally is safe and simpler.

**Why the final version works:**

The `LazyInitializationException` was fundamentally a transaction-scope problem. `@Transactional` on an internal method call does nothing because Spring's proxy is not involved. Placing `@Transactional` on the method that is called from outside the bean (`processFile`, invoked from `scanFolder`) ensures Spring's proxy intercepts the call, opens a session before the method body runs, and keeps it open until the method returns. Every lazy collection access, every repository query, and every `save()` inside that method share the same session. Removing the `.equals()` checks eliminated the secondary risk of accidentally triggering a lazy load through collection comparison before the session was guaranteed to be open.

---

### Files modified across all versions

| File                              | Change                                                                                                                                           |
|:----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `MigrationResult.java`            | Added `METADATA_UPDATED` status in v2                                                                                                            |
| `LibraryMigrationService.java`    | v1: path + art only. v2: full `syncMetadata`, wrong transaction scope. v3: `@Transactional` moved to `processFile`, lazy-set comparisons removed |
| `LibraryMigrationController.java` | v2: added `METADATA_UPDATED` to summary counter, `formatStatus()`, and `statusColour()`                                                          |
| `MainController.java`             | v1: added `migrationController` field, `registerMigration()`, and `"migration"` case in `navigateTo()`                                           |


---

## 2026-04-14 v1.0.5

### Refactored
- Extracted `ArtistUtils` utility class (`com.sebastiandorata.musicdashboard.utils`) to
  centralize artist name resolution. The inline stream pattern
  `song.getArtists().stream().findFirst().map(Artist::getName).orElse("Unknown Artist")`
  was duplicated across 8 classes: `ListeningHistoryViewModel`, `TopSongsViewModel`,
  `AnalyticsController`, `TopArtistsViewModel`, `YearEndReportViewModel`,
  `PlaylistViewBuilder`, `SongCell`, and `CardFactory`. All call sites now use
  `ArtistUtils.getPrimaryArtistName(Song)` or `ArtistUtils.getArtistName(Artist)`.
  The private `firstArtist()` helper in `YearWrappedViewController` was deleted and
  replaced with the shared utility method.

- Extracted `PlaybackAggregator` utility class (`com.sebastiandorata.musicdashboard.utils`)
  to centralize playback history aggregation. Five stream pipelines for computing
  valid play counts, total listening minutes, top song, top artist, top album song,
  and top genre were copy-pasted verbatim across `WeeklyReportService`,
  `MonthlyReportService`, and `YearEndReportService`. The private `generate` methods
  in all three services now delegate to `PlaybackAggregator.countValidPlays()`,
  `PlaybackAggregator.sumValidMinutes()`, `PlaybackAggregator.findTopSong()`,
  `PlaybackAggregator.findTopArtist()`, `PlaybackAggregator.findTopAlbumSong()`,
  and `PlaybackAggregator.findTopGenre()`.

---

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

---

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
