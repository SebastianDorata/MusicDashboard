# MusicDashboard — MBID & Scrobble Support Schema Change

**Project:** MusicDashboard (JavaFX + Spring Boot)<br>
**Author:** Sebastian Dorata<br>
**Date:** August 8, 2026<br>
**Environment:** macOS, IntelliJ IDEA, Java 26, Spring Boot 4.0.3, Hibernate 7.2.4.Final, H2 2.3.232, Flyway 11.14.1<br>
**Status:** Applied and verified against H2 (embedded profile). Not yet run against Postgres.

---

## Table of Contents

- [1. Summary](#summary)
- [2. Motivation](#motivation)
- [3. Schema Changes](#schema-changes)
    - [3.1 New columns](#new-columns)
    - [3.2 Why `file_path` needed to become nullable](#why-nullable)
    - [3.3 Why the change was split across two migrations](#why-split)
- [4. Entity Changes](#entity-changes)
- [5. Migration Files](#migration-files)
- [6. Database Actions Performed](#db-actions)
- [7. Verification](#verification)
- [8. Design Decisions / Rationale](#design-decisions)
- [9. What This Enables Next](#next-steps)

---

<a id="summary"></a>

## 1. Summary

This change prepares the schema for two related upcoming features:

1. **MusicBrainz ID (MBID) tracking** on `artists`, `albums`, and `songs`, so entities cleaned up via a MusicBrainz
   tagging tool (e.g. Picard on macOS) carry a stable, vendor-neutral identifier instead of relying only on name/title
   string matching.
2. **Placeholder songs and scrobble import support**, so a future Last.fm listening-history importer can create `Song`
   rows for tracks that don't yet have a local audio file, and record playback history from Last.fm without producing
   duplicate rows on repeated imports.

Two Flyway migrations were written (`V2`, `V3`), split across `common`, `h2`, and `postgresql` migration folders to keep
vendor-specific SQL out of the shared migration set — a lesson carried over directly from
the [Schema Migration Incident Report](./MusicDashboard%20—%20Schema%20Migration%20Incident%20Report.md), where a single
reserved-word issue silently broke schema generation for H2 while working fine on Postgres.

---

<a id="motivation"></a>

## 2. Motivation

- **MusicBrainz cleanup workflow:** The project owner uses a MusicBrainz-based tagging tool on macOS (rather than a
  Picard plugin built into the app) to clean up song/artist/album metadata before import. That tool writes MusicBrainz
  IDs into file tags, but the schema previously had no column to persist them — they were read and then discarded.
- **Last.fm import (planned):** A future feature will import a user's Last.fm scrobble history (`user.getRecentTracks`,
  tested against a real export of ~34,000 scrobbles). Last.fm's API returns an MBID per artist/album/track where known.
  Matching incoming scrobbles against the local library by MBID first (exact) and title/artist second (fuzzy) is
  significantly more reliable than string matching alone.
- **Scrobbles for songs not yet in the local library:** A user may have scrobbled a song on Last.fm before ever
  importing the matching audio file (e.g. streamed elsewhere, or the file was later re-organized). Recording that
  history requires a `Song` row to attach it to — even without a file. This is the origin of the **placeholder song**
  concept.
- **Idempotent scrobble import:** Last.fm history should be safely re-importable (e.g. periodic sync) without creating
  duplicate `playback_history` rows for scrobbles already recorded.

---

<a id="schema-changes"></a>

## 3. Schema Changes

<a id="new-columns"></a>

### 3.1 New columns

| Table              | Column                 | Type           | Nullable             | Unique | Purpose                                      |
|--------------------|------------------------|----------------|----------------------|--------|----------------------------------------------|
| `artists`          | `mbid`                 | `VARCHAR(36)`  | Yes                  | Yes    | MusicBrainz artist ID                        |
| `albums`           | `mbid`                 | `VARCHAR(36)`  | Yes                  | Yes    | MusicBrainz release-group ID                 |
| `songs`            | `mbid`                 | `VARCHAR(36)`  | Yes                  | **No** | MusicBrainz recording ID                     |
| `songs`            | `is_placeholder`       | `BOOLEAN`      | No (`DEFAULT FALSE`) | —      | Marks metadata-only songs with no local file |
| `songs`            | `file_path`            | `VARCHAR(500)` | **Changed to Yes**   | Yes    | Now nullable to support placeholders         |
| `playback_history` | `external_scrobble_id` | `VARCHAR(64)`  | Yes                  | Yes    | Dedup key for imported scrobbles             |

**Why `songs.mbid` is not unique, unlike `artists.mbid` / `albums.mbid`:**
Last.fm track-level MBIDs are frequently blank or shared across re-releases and regional variants of the same recording,
unlike artist and album MBIDs which are much more consistently unique per real-world entity. Enforcing uniqueness on
`songs.mbid` would risk constraint violations the first time two legitimately different `Song` rows shared a track MBID
from Last.fm's data. Artist and album MBIDs did not carry this risk in testing and benefit from the extra integrity
guarantee.

**Why `NULL` is safe under a `UNIQUE` constraint:**
Both H2 and PostgreSQL follow the SQL standard treatment of `NULL` as distinct from every other `NULL` under a unique
constraint — meaning any number of rows can have `mbid IS NULL` (or `file_path IS NULL`, or
`external_scrobble_id IS NULL`) simultaneously without violating uniqueness. This is what makes it safe to add these as
nullable+unique on existing tables with many pre-existing rows that will never populate the new column.

<a id="why-nullable"></a>

### 3.2 Why `file_path` needed to become nullable

The `Song` entity previously enforced `file_path` as `NOT NULL` and `UNIQUE` — a deliberate fix from an earlier
incident (documented in the CHANGELOG, 2026-07-04) that prevented two rows from ever sharing a path. That constraint
remains, but the `NOT NULL` half had to be relaxed:

- A placeholder song, by definition, has no file yet — there is nothing to put in `file_path`.
- The `UNIQUE` constraint is retained and remains safe because `NULL <> NULL`, so multiple placeholders never collide
  with each other or with real songs' populated paths.

<a id="why-split"></a>

### 3.3 Why the change was split across two migrations

The first draft of this change placed `ALTER TABLE songs ALTER COLUMN file_path DROP NOT NULL;` directly alongside the
new-column additions in a single script under `db/migration/common`. This was caught before being applied:

- `DROP NOT NULL` is **PostgreSQL syntax**. H2 2.x requires `ALTER COLUMN file_path SET NULL` for the equivalent
  operation.
- Anything placed in `common` is applied to **every** active profile (
  `spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/h2` or
  `...,classpath:db/migration/postgresql` depending on `DB_PROFILE`). A Postgres-only statement in `common` would throw
  a syntax error the moment it ran against H2 — which, per `application.properties`, is this project's *default*
  profile (`DB_PROFILE:embedded`).

This is the same category of failure documented in the prior incident report (H2 reserved words silently breaking schema
generation) — a vendor-specific detail leaking into a shared migration path. The fix follows the same remediation
pattern already established for this project: vendor-neutral SQL stays in `common`; anything that differs between H2 and
PostgreSQL gets its own migration version, duplicated once per vendor folder.

---

<a id="entity-changes"></a>

## 4. Entity Changes

**`Artist.java`**

```java
/** MusicBrainz artist ID, when known. Nullable — most tags/scrobbles won't have it. */
@Column(name = "mbid", length = 36, unique = true)
private String mbid;
```

**`Album.java`**

```java
/** MusicBrainz release-group ID, when known. */
@Column(name = "mbid", length = 36, unique = true)
private String mbid;
```

**`Song.java`**

```java
// file_path — nullable = false removed from the existing @Column annotation

/** MusicBrainz recording ID. Not unique — Last.fm track mbids are frequently blank
 *  or shared across re-releases, unlike artist/album mbids. */
@Column(name = "mbid", length = 36)
private String mbid;

/** True for songs that exist only as metadata (e.g. an unmatched Last.fm scrobble). */
@Column(name = "is_placeholder", nullable = false)
private final Boolean isPlaceholder = false;
```

**`PlaybackHistory.java`**

```java
/**
 * External identifier for imported plays (e.g. Last.fm's scrobble {@code uts}
 * timestamp). Lets a re-import detect and skip already-imported scrobbles.
 * Null for plays recorded natively by this app.
 */
@Column(name = "external_scrobble_id", length = 64, unique = true)
private String externalScrobbleId;
```

No getters/setters were hand-written — all four entities already use Lombok `@Getter`/`@Setter` at the class level, so
the new fields are covered automatically.

No changes were required to `Song.getFilePath()` callers as a result of this migration alone. However, this is a known
follow-up: any code path that assumes `song.getFilePath()` is non-null (e.g. `MusicPlayerService.playSong()`) should be
reviewed once placeholder songs actually start appearing in the library, rather than only existing as a schema-level
possibility. `MusicPlayerService.playSong()` already null/empty-checks `filePath` and logs an error instead of throwing,
so no immediate crash risk — but the message ("Song has no file path") is not yet placeholder-aware.

---

<a id="migration-files"></a>

## 5. Migration Files

| File                                | Location                  | Applies to                              |
|-------------------------------------|---------------------------|-----------------------------------------|
| `V1__init_schema.sql`               | `db/migration/common`     | H2 + Postgres (pre-existing, unchanged) |
| `V2__mbid_and_scrobble_support.sql` | `db/migration/common`     | H2 + Postgres                           |
| `V3__songs_file_path_nullable.sql`  | `db/migration/h2`         | H2 only                                 |
| `V3__songs_file_path_nullable.sql`  | `db/migration/postgresql` | Postgres only                           |

**`V2__mbid_and_scrobble_support.sql`** (vendor-neutral — safe for both databases):

```sql
ALTER TABLE artists
    ADD COLUMN mbid VARCHAR(36);
ALTER TABLE artists
    ADD CONSTRAINT uq_artists_mbid UNIQUE (mbid);

ALTER TABLE albums
    ADD COLUMN mbid VARCHAR(36);
ALTER TABLE albums
    ADD CONSTRAINT uq_albums_mbid UNIQUE (mbid);

ALTER TABLE songs
    ADD COLUMN mbid VARCHAR(36);
ALTER TABLE songs
    ADD COLUMN is_placeholder BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE playback_history
    ADD COLUMN external_scrobble_id VARCHAR(64);
ALTER TABLE playback_history
    ADD CONSTRAINT uq_playback_external_scrobble UNIQUE (external_scrobble_id);
```

**`V3__songs_file_path_nullable.sql`] (H2 variant):**

```sql
ALTER TABLE songs ALTER COLUMN file_path SET NULL;
```

**`V3__songs_file_path_nullable.sql` (PostgreSQL variant):**

```sql
ALTER TABLE songs
    ALTER COLUMN file_path DROP NOT NULL;
```

**Why `V3` and not a second `V2` in the vendor folders:** Flyway tracks applied migration versions per schema,
independent of which physical folder a script came from. Two files both named `V2__...sql` — one in `h2`, one in
`postgresql` — would be a version collision the moment both locations are active for the same profile's classpath scan.
Giving the vendor-specific statement its own version number (`V3`) after the shared `V2` keeps the version sequence
unambiguous regardless of which profile is running.

---

<a id="db-actions"></a>

## 6. Database Actions Performed

Since this change was made **pre-flight** (no production or previously-migrated database depended on the old,
incorrectly-combined `V2`), the following actions were taken directly rather than through a corrective
forward-migration:

1. Deleted the original single-file draft of `V2__mbid_and_scrobble_support.sql` (which had incorrectly included the
   Postgres-only `DROP NOT NULL` statement inside `common`).
2. Replaced it with the vendor-neutral `V2` shown above.
3. Added `V3__songs_file_path_nullable.sql` to both `db/migration/h2` and `db/migration/postgresql`, each with the
   vendor-appropriate syntax.
4. Deleted the local H2 database files to force a clean rebuild and validate the full migration sequence from an empty
   schema:
   ```bash
   rm ~/.musicdashboard/data/musicdashboard.mv.db
   rm ~/.musicdashboard/data/musicdashboard.trace.db
   ```
5. Relaunched the application and confirmed Flyway applied `V1` → `V2` → `V3` in order, followed by a successful
   Hibernate `ddl-auto=validate` pass with no `SchemaManagementException`.

**Note on immutability going forward:** Per the lesson already documented in the prior incident report — *"treat every
applied Flyway migration file as immutable once committed; never delete or edit an already-applied version, only add new
ones"* — the direct edit-and-delete approach used here is only valid because this was pre-flight. Any future correction
to schema logic once this has been applied to a real, in-use database (either developer's local H2 file or a shared
Postgres instance) must be done via a new `V4` migration, not by editing `V2`/`V3` in place.

---

<a id="verification"></a>

## 7. Verification

Application launched from a completely empty H2 database (`embedded` profile). Startup log confirmed:

```
o.f.core.internal.command.DbValidate     : Successfully validated 3 migrations (execution time 00:00.005s)
o.f.core.internal.command.DbMigrate      : Current version of schema "PUBLIC": << Empty Schema >>
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "1 - init schema"
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "2 - mbid and scrobble support"
o.f.core.internal.command.DbMigrate      : Migrating schema "PUBLIC" to version "3 - songs file path nullable"
o.f.core.internal.command.DbMigrate      : Successfully applied 3 migrations to schema "PUBLIC", now at version v3
```

Followed immediately by:

```
org.hibernate.orm.jpa   : HHH008540: Processing PersistenceUnitInfo [name: default]
j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
o.s.boot.SpringApplication : Started application in 2.315 seconds
```

No `SchemaManagementException`, no Hibernate validation warnings, application reached the JavaFX login screen normally.

**Result: PASS (H2 / embedded profile only).**

**Not yet verified:** This change has not been run against the `postgres` profile. The `V3` Postgres variant (
`DROP COLUMN ... DROP NOT NULL`) has not been executed against a live PostgreSQL instance. This should be verified
before switching `DB_PROFILE=postgres` in any environment, per the existing dual-profile support already documented for
this project.

---

<a id="design-decisions"></a>

## 8. Design Decisions / Rationale

| Decision                                                                             | Rationale                                                                                                                                                                                                                                                                                                                                                                                                                  |
|--------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Explicit `is_placeholder` boolean instead of inferring from `file_path IS NULL`      | An implicit signal (checking for null path) would otherwise need to be re-derived in every future view/filter/query that needs to distinguish real songs from placeholders. An explicit column documents the concept once and is queryable/indexable directly.                                                                                                                                                             |
| `artists.mbid` / `albums.mbid` unique, `songs.mbid` not unique                       | Artist and album MBIDs reliably identify one real-world entity. Track-level MBIDs from Last.fm are inconsistent (often blank, sometimes shared across re-releases) — enforcing uniqueness there would risk avoidable constraint violations.                                                                                                                                                                                |
| `external_scrobble_id` stored on `playback_history`, not a separate table            | Keeps the dedup key co-located with the row it protects, consistent with how `source` (e.g. distinguishing native playback from imports) is already modeled as a plain column on the same table rather than a side table.                                                                                                                                                                                                  |
| MBID columns added now, before the Last.fm importer exists                           | The importer's matching strategy (MBID first, normalized title/artist fallback) depends on having somewhere to store MBIDs it resolves — including MBIDs picked up from local files that were already cleaned up via MusicBrainz tooling, independent of Last.fm. Adding the columns first means existing local metadata can start being enriched incrementally, rather than only becoming useful once the importer ships. |
| Migration split by vendor rather than keeping a single script with conditional logic | Flyway migrations are plain SQL per vendor folder — there is no cross-vendor conditional syntax available within a single `.sql` file. Splitting into vendor-specific folders is the standard Flyway pattern for this project (already used for `application-embedded.properties` / `application-postgres.properties` and the existing `db/migration/h2` / `db/migration/postgresql` folder structure).                    |

---

<a id="next-steps"></a>

## 9. What This Enables Next

This schema change is preparatory — no new application feature is functional yet as a result of it alone. It unblocks:

1. **Last.fm scrobble importer** (planned, not yet built): a `LastFmClient` + matching service that resolves each
   scrobble to an `Artist`/`Album`/`Song` by MBID first, then normalized title/artist, creating placeholder `Song` rows
   for unmatched tracks and deduplicating `PlaybackHistory` inserts via `external_scrobble_id`.
2. **Placeholder-to-real upgrade path** (planned, not yet built): when a real audio file is later imported for a song
   that already exists as a placeholder (e.g. scrobbled on Last.fm before ever owning the file),
   `SongUpdateService.upsert()` will need a new matching branch — title+artist match against `is_placeholder = true`
   rows — inserted before its current "create new" fallback, so the existing row is upgraded in place (file path
   attached, flag cleared) instead of creating a duplicate `Song` and orphaning the placeholder's scrobble history.
3. **MusicBrainz-aware local matching** (partially usable now): once `SongMetadataExtractor` / `SongUpdateService` are
   updated to read and persist MBIDs from local tags (not yet done), songs cleaned up via the macOS MusicBrainz tool
   will carry a stable ID usable for future matching, independent of the Last.fm feature.

---
