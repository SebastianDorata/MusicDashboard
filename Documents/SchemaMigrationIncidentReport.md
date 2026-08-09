# MusicDashboard — Schema Migration Incident Report

**Project:** MusicDashboard (JavaFX + Spring Boot)<br>
**Author:** Sebastian Dorata<br>
**Date:** August 6, 2026<br>
**Environment:** macOS, IntelliJ IDEA, Java 26, Spring Boot 4.0.3, Hibernate 7.2.4.Final, H2 2.3.232, Flyway 11.14.1

---

## Table of Contents

- [1. Summary](#summary)
- [2. Background / Context](#background)
- [3. Timeline of Issues](#timeline)
  - [3.1 Issue 1 — H2 Reserved Word Collision](#issue-1)
  - [3.2 Issue 2 — Silent Schema Failure (ddl-auto=update)](#issue-2)
  - [3.3 Issue 3 — Missing Flyway Migration File](#issue-3)
  - [3.4 Issue 4 — Incomplete Entity Column Renames](#issue-4)
  - [3.5 Issue 5 — Incomplete V1 Schema Script](#issue-5)
- [4. Root Cause Analysis](#root-cause)
- [5. Solution](#solution)
  - [5.1 Entity Changes](#solution-entities)
  - [5.2 Flyway Migration Script](#solution-flyway)
  - [5.3 Configuration Changes](#solution-config)
- [6. Verification](#verification)
- [7. Lessons Learned / Preventive Measures](#lessons-learned)
- [8. Reference Commands](#reference-commands)

---

<a id="summary"></a>
## 1. Summary

The application failed to start against the H2 embedded database profile due to a schema generation failure in three report-related tables (`monthly_reports`, `weekly_reports`, `year_end_reports`). The root cause was **`year` and `month` being reserved keywords in H2 2.x**, which caused Hibernate's auto-generated DDL to fail silently under `ddl-auto=update`. This was compounded by a **missing/orphaned Flyway migration file** once the project was migrated to Flyway-managed schema control. The issue was resolved by renaming the offending columns, writing a complete Flyway baseline migration (`V1__init_schema.sql`), and switching Hibernate to `ddl-auto=validate` so future mismatches fail loudly instead of silently.

---

<a id="background"></a>
## 2. Background / Context

- The project originally used `spring.jpa.hibernate.ddl-auto=update`, letting Hibernate auto-generate and alter the schema from `@Entity` classes.
- The project supports dual database profiles (`embedded` H2 / `postgres`) via a `DB_PROFILE` environment variable, using separate `application-embedded.properties` / `application-postgres.properties` files.
- Flyway was later introduced to make schema changes explicit, versioned, and vendor-tested — replacing Hibernate's implicit schema generation.
- Migration locations were split per-profile:
```properties
  spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/h2
```

---

<a id="timeline"></a>
## 3. Timeline of Issues

<a id="issue-1"></a>
### 3.1 Issue 1 — H2 Reserved Word Collision

**Symptom:**

Syntax error in SQL statement "...month integer not null,..."; expected "identifier"

Repeated for `year` in `weekly_reports` and `year_end_reports`.

**Cause:** `MonthlyReport`, `WeeklyReport`, and `YearEndReport` entities declared bare `year` / `month` fields, which Hibernate emitted as unquoted column names. Both `year` and `month` are **reserved keywords in H2 2.x**. Postgres does not reserve these the same way, so the bug was invisible until the project switched from Postgres to the H2 embedded profile.

**Immediate effect:** Logged only as `WARN` (`GenerationTarget encountered exception accepting command`). The application **still booted**, because `ddl-auto=update` treats failed DDL as non-fatal.

---

<a id="issue-2"></a>
### 3.2 Issue 2 — Silent Schema Failure (`ddl-auto=update`)

**Symptom:** App appeared to start successfully ("Started application in X seconds"), but `monthly_reports`, `weekly_reports`, and `year_end_reports` tables were **never actually created**.

**Cause:** `ddl-auto=update` is forgiving by design — it applies whatever DDL it can and logs failures as warnings rather than stopping startup. This let a broken schema persist undetected until a service tried to query one of the missing tables at runtime.

**Diagnostic action taken:** Compared `update` vs `validate` mode behavior; decided to migrate toward Flyway + `validate` for loud, fail-fast schema mismatches going forward.

---

<a id="issue-3"></a>
### 3.3 Issue 3 — Missing Flyway Migration File

**Symptom:**

org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing table [monthly_reports]

This is a **hard failure** — application context refused to start (`Application run failed`, exit code 1).

**Investigation:**
- `spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/h2` was configured.
- `find` search across the entire project (`src`, `target`, home directory) turned up **no `V1__*.sql` file anywhere** on disk.
- Despite this, Flyway logged `Successfully validated 1 migration` and `Current version of schema "PUBLIC": 1` — meaning Flyway's history table (`flyway_schema_history`), stored inside the H2 database file itself, remembered a `V1` migration that no longer existed as a file.

**Cause:** At some prior point, a `V1` migration file existed, was applied to the database, and was subsequently deleted or never committed to version control — leaving the database's internal migration ledger out of sync with the project's actual source files.

---

<a id="issue-4"></a>
### 3.4 Issue 4 — Incomplete Entity Column Renames

**Symptom:** During remediation, only `MonthlyReport.java` had been updated with renamed columns (`report_year`, `report_month`). `WeeklyReport.java` and `YearEndReport.java` still declared bare `year` columns and `@UniqueConstraint(columnNames = {"user_id", "year", ...})`, which would have reintroduced Issue 1 if left unfixed.

**Cause:** Partial application of the fix — the column rename was correctly identified but not consistently applied across all three affected entities in the same pass.

---

<a id="issue-5"></a>
### 3.5 Issue 5 — Incomplete V1 Schema Script

**Symptom:** An initial draft of `V1__init_schema.sql` only contained `CREATE TABLE` statements for the three broken report tables, omitting the other ~13 tables (`users`, `songs`, `albums`, `artists`, `genres`, `playlists`, `favourites`, `playback_history`, `sync_queue`, and four join tables) as well as foreign key constraints and unique constraints declared in the corresponding entities.

**Cause:** Since the plan was to delete the entire H2 database file and start Flyway from an empty schema, a partial `V1` would have caused the *same class* of failure to reappear for a different table (e.g., `missing table [users]`) on the very next run.

---

<a id="root-cause"></a>
## 4. Root Cause Analysis

| Layer | Root Cause |
|---|---|
| **Immediate** | `year` and `month` are reserved words in H2 2.x; Hibernate emitted them unquoted in generated DDL. |
| **Contributing** | `ddl-auto=update` swallows DDL failures as warnings instead of stopping startup, allowing the broken schema state to persist silently for multiple sessions. |
| **Systemic** | No process existed to guarantee that Flyway migration *files* and the Flyway *history table* stayed in sync — a file was applied once and later lost without corresponding recovery/documentation. |
| **Process gap** | Schema changes were driven by editing `@Entity` classes directly rather than through a single source of truth (versioned SQL), making cross-database (H2 → Postgres) portability unverified until failure occurred. |

**Five-Whys summary:**
1. App failed to start → missing table `monthly_reports`.
2. Table missing → Flyway's `V1` never actually created it on this database instance.
3. V1 never ran correctly → the migration file referenced by Flyway's history didn't exist on disk / was incomplete.
4. File missing/incomplete → schema management transitioned from Hibernate `update` to Flyway mid-project without validating the migration content end-to-end.
5. Underlying trigger → H2's reserved-word conflict on `year`/`month`, undetected for multiple sessions because `ddl-auto=update` doesn't fail loudly.

---

<a id="solution"></a>
## 5. Solution

<a id="solution-entities"></a>
### 5.1 Entity Changes

Renamed the reserved-word-colliding columns via `@Column(name = ...)`, without changing Java field names (so all existing service/getter/setter call sites remain unaffected):

**`MonthlyReport.java`**
```java
@Table(name = "monthly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "report_year", "report_month"})
})
public class MonthlyReport {
    ...
    @Column(name = "report_year", nullable = false)
    private Integer year;

    @Column(name = "report_month", nullable = false)
    private Integer month;
    ...
}
```

**`WeeklyReport.java`**
```java
@Table(name = "weekly_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "report_year", "week_of_year"})
})
public class WeeklyReport {
    ...
    @Column(name = "report_year", nullable = false)
    private Integer year;
    ...
}
```

**`YearEndReport.java`**
```java
@Table(name = "year_end_reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "report_year"})
})
public class YearEndReport {
    ...
    @Column(name = "report_year", nullable = false)
    private Integer year;
    ...
}
```

<a id="solution-flyway"></a>
### 5.2 Flyway Migration Script

Created a complete baseline migration covering **all** entities (not just the three broken tables), placed at:

src/main/resources/db/migration/common/V1__init_schema.sql


Table creation order was arranged so no foreign key references a table that doesn't yet exist:

1. `users`, `artists`, `genres`, `albums` (no dependencies)
2. `songs` (depends on `albums`)
3. Join tables: `song_artists`, `song_genres`, `album_artists`
4. `playlists`, `playlist_songs`, `favourites`, `playback_history`, `sync_queue` (depend on `users`/`songs`)
5. `monthly_reports`, `weekly_reports`, `year_end_reports` (depend on `users`, `songs`, `artists`, `genres`)

Each report table includes:
- Renamed `report_year` / `report_month` columns
- Matching `UNIQUE` constraints (`uq_monthly_report`, `uq_weekly_report`, `uq_year_report`)
- Explicit foreign keys to `users`, `songs`, `artists`, `genres`

*(Full script omitted here for brevity — see `src/main/resources/db/migration/common/V1__init_schema.sql` in the repository.)*

<a id="solution-config"></a>
### 5.3 Configuration Changes

- Deleted the stale/orphaned local H2 database files to clear the mismatched Flyway history:
```bash
  rm ~/.musicdashboard/data/musicdashboard.mv.db
  rm ~/.musicdashboard/data/musicdashboard.trace.db
```
- Confirmed `spring.jpa.hibernate.ddl-auto` is set to `validate` so future schema drift fails loudly at startup instead of silently warning:
```properties
  spring.jpa.hibernate.ddl-auto=validate
```
- Confirmed Flyway location split remains:
```properties
  spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/h2
```

---

<a id="verification"></a>
## 6. Verification

Application restarted from a completely empty H2 database. Startup log confirmed the full expected sequence:

JdbcTableSchemaHistory : Schema history table "PUBLIC"."flyway_schema_history" does not exist yet
DbMigrate : Current version of schema "PUBLIC": << Empty Schema >>
DbMigrate : Migrating schema "PUBLIC" to version "1 - init schema"
DbMigrate : Successfully applied 1 migration to schema "PUBLIC", now at version v1
LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
SpringApplication : Started application in 2.347 seconds


- Flyway created its own history table and applied `V1` cleanly against an empty schema.
- Hibernate's `validate` step passed silently — every `@Entity` class matched the newly created schema, including the three previously-broken report tables.
- Application reached the JavaFX login screen, accepted input, and loaded UI assets (icons, playback panel) without error.
- Application shut down cleanly (HikariCP shutdown completed, exit code `0`).

**Result: PASS.**

---

<a id="lessons-learned"></a>
## 7. Lessons Learned / Preventive Measures

| Lesson | Preventive Measure |
|---|---|
| `ddl-auto=update` hides real schema failures as warnings. | Stay on `ddl-auto=validate` going forward; all schema changes go through Flyway. |
| Reserved words differ per database vendor and can silently break portability. | Avoid bare `year`, `month`, `order`, `group`, etc. as column names; prefer descriptive prefixes (`report_year`). |
| Flyway's history table can drift from the actual files on disk if a migration is deleted without care. | Treat every applied Flyway migration file as **immutable** once committed; never delete or edit an already-applied version — only add new ones. |
| Partial fixes (renaming one entity but not all three) can reintroduce the same bug. | When a fix pattern applies to multiple files, verify **all** affected files before considering the fix complete. |
| An incomplete baseline migration only defers the same class of failure to the next missing table. | When authoring a Flyway baseline against an empty schema, include every entity, not just the ones that were broken. |

---

<a id="reference-commands"></a>
## 8. Reference Commands

```bash
# Locate the H2 database file
find ~ -name "musicdashboard.mv.db" 2>/dev/null

# Search project for Flyway migration files
find ~/IdeaProjects/MusicDashboard -name "V*.sql" 2>/dev/null

# Delete local H2 database to reset Flyway history (dev only — destroys local data)
rm ~/.musicdashboard/data/musicdashboard.mv.db
rm ~/.musicdashboard/data/musicdashboard.trace.db

# Run with debug logging enabled (Spring Boot condition evaluation report)
java -jar target/musicdashboard.jar --debug
```