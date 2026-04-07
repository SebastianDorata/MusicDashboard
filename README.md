# MusicDashboard ITEC 4010 Systems Analysis and Design II Final Project. 


[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/sebastian-dorata-5013b6297/)


### A full-stack desktop application built with JavaFX, Spring Boot, and PostgreSQL designed for browsing and managing your personal music library. Features a responsive GUI, with support for library exploration and detailed playback statistics.

---
## Tech Stack

## System Architecture (N-Tier) and Dependencies

| Library                                                  | Version | Purpose                                                 |
|----------------------------------------------------------|---------|---------------------------------------------------------|
| Spring Boot                                              | 4.0.3   | Backend framework & dependency injection                |
| JavaFX                                                   | 25.0.2  | Desktop GUI framework                                   |
| Hibernate JPA                                            | 7.0.6   | ORM for PostgreSQL                                      |
| Spring Security                                          | 7.0.4   | Authentication & authorization                          |
| Lombok                                                   | -       | Reduces boilerplate code                                |
| [jaudiotagger](https://github.com/RouHim/jaudiotagger)   | 2.0.16  | Reading audio file metadata (artist, album, track info) |
| PostgreSQL                                               | 18      | Relational database                                     |

Separating the application into distinct layers ensures each part of the system has a single, well-defined responsibility, making the codebase easier to navigate, debug, and extend without risking breaks to the underlying database logic.

## Academic Foundation

This project integrates concepts from multiple courses across the Information Technology curriculum:

- **ITEC 2610** introduced JavaFX, which powers the entire graphical user interface including dashboards, analytics panels, playlists, and other visual components.
- **ITEC 2620** introduced core data structures including Doubly Linked Lists and Hash Maps, which informed how in-memory collections and lookups are structured throughout the application.
- **ITEC 3030** provided the foundation for SOLID design principles and N-Tier architecture, both of which directly guided the layered structure and class-level design decisions throughout the codebase.
- **ITEC 3220** covered database design and interaction, informing all PostgreSQL schema design, query patterns, indexing strategy, and repository layer implementation.
- **ITEC 3230** introduced Human-Computer Interaction principles, which guided UI decisions around affordance, visibility, consistency, and individual difference throughout the application's design.

---

## System Architecture (N-Tier)

This application was designed using an N-Tier architecture, which separates the system into multiple layers, each with a clear and well-defined responsibility. This makes the codebase easier to navigate, debug, and extend without risking breaking the underlying database logic. This architectural approach was a core learning outcome of ITEC 3030.

**Client Layer (JavaFX):** The outermost layer is built with JavaFX, introduced in ITEC 2610, and is what the user interacts with directly, including dashboards, analytics, playlists, and other visual components.

**Controller Layer:** Implemented using Spring-managed components, this layer handles incoming user actions and routes requests to the appropriate parts of the system.

**Service Layer:** This layer contains all business logic and validation. Some services, such as the import and playback tracking services, run on background threads to keep the UI responsive, with results dispatched back to the JavaFX thread via `Platform.runLater()`. Centralizing logic here avoids scattering rules across the UI or database code, making the system more organized and easier to maintain.

**Dependency Injection:** Spring Boot's dependency injection is used throughout to wire the layers together. Instead of manually creating objects, Spring automatically injects the required dependencies, such as services into controllers or repositories into services. This reduces tight coupling between components and makes the system easier to test, maintain, and extend.

**Data Transfer Objects (DTOs):** DTOs are read-only objects that send only the necessary data to the UI. For example, `TopSongRowData`, `TopArtistRowData`, and `StatCardsData` carry pre-computed analytics data to the dashboard without exposing full database entities, preventing unintended data access and keeping the UI layer clean.

**Model Layer:** Consists of Hibernate JPA entities representing core data structures such as songs, albums, and users.

**Database Layer:** Uses PostgreSQL, where all persistent data is stored. Accessed through repository interfaces that isolate database operations and make the system easier to test and modify. Data flows from the JavaFX client, through the controller and service layers, into the database, and back up to the UI.

---

## Design Philosophy and SOLID Principles

This project was architected from the ground up with the SOLID design principles as a guiding framework, ensuring the codebase remains maintainable, extensible, and testable as the system grows. These principles were a core learning outcome of ITEC 3030.

The **Single Responsibility Principle** is reflected throughout the layer separation: controllers handle only navigation and scene assembly, ViewModels own only data transformation and async loading, and service classes encapsulate only their specific domain logic. For example, `AnalyticsRowFactory` is solely responsible for building row UI components from ViewModel DTOs, while `AnalyticsSectionBuilder` is solely responsible for assembling the repeating section layout pattern; neither class knows anything about the other's domain.

The **Open/Closed Principle** guided decisions like `SortStrategy`, implemented as an enum with abstract methods so new sort behaviours can be added without modifying any existing call sites.

The **Dependency Inversion Principle** is visible in components like `TopArtistsController`, which accepts a `Consumer<Artist>` callback rather than holding a direct reference to `MyLibraryController`. The panel depends on an abstraction, not a concrete navigation target.

The **Interface Segregation Principle** shaped the `LibraryHandler` record, which bundles only the services a view component actually needs rather than injecting the entire Spring context.

**Liskov Substitution** is honoured across the entity layer, where all 12 entity classes follow a consistent contract for equality via explicit `@EqualsAndHashCode` configuration on their ID fields, preventing subtle Hibernate reference equality bugs that would otherwise silently corrupt analytics data.

---

## Database Indexing Strategy

This project's database knowledge, including schema design and query optimization, was developed through ITEC 3220.

As the user base grows, the playback history table will be the fastest growing table in the system; every single song played by every user gets logged here. Without optimization, querying this table would mean scanning millions of rows every time a user opens their dashboard or listening history. Three indexes were added to prevent this:

1. **Index on `user_id`:** Ensures that when a user opens the app, only their data is retrieved instantly, not every other user's playback history.
2. **Index on `played_at`:** Ensures that sorting and filtering by date is fast, which is critical for features like "recently played" and monthly reports.
3. **Composite index on `user_id` and `played_at` together:** The most powerful of the three, this handles the most common query pattern ("get this user's history sorted by most recent") in a single optimized lookup.

The primary key index is automatically managed by PostgreSQL. These indexes are what allow the application to feel instant to the user today, and stay fast as the data scales to millions of records in production.

---

## HCI Design Principles

The application's user interface was designed with Human-Computer Interaction principles introduced in ITEC 3230, specifically:

- **Affordance:** UI elements are designed to clearly communicate their purpose and how they should be used.
- **Visibility:** System state and available actions are always visible to the user, reducing the need for memorization.
- **Consistency:** Visual language, interaction patterns, and terminology remain uniform across all screens and panels.
- **Individual Difference:** The interface accommodates varying levels of user experience and familiarity, ensuring accessibility for a broad range of users.

---

## Privacy Compliance and Regulatory Research

Before any implementation decisions were made around authentication and data storage, a structured review of the applicable Ontario and federal regulatory landscape was conducted to ensure the system was compliant by design rather than retrofitted. The primary frameworks examined were PIPEDA (the Personal Information Protection and Electronic Documents Act), Ontario's Bill 194 (Strengthening Cyber Security and Building Trust in the Public Sector Act, 2024), FIPPA (Freedom of Information and Protection of Privacy Act, referencing the July 2025 modernization), and the Consumer Protection Act, 2023, which has been identified as not yet in force but was included in the roadmap for future portability and transparency obligations.

This research directly produced concrete implementation decisions. PIPEDA Principles 4.4 and 4.5, which mandate data minimization and limiting use, informed the decision to store only the data strictly necessary for personal analytics: no third-party SDKs, no advertising frameworks, and no outbound network calls of any kind exist in the codebase. Bill 194's cyber resilience mandate drove the design of `LoginAttemptService`, which implements escalating brute force lockouts across four tiers (5, 10, and 30 minute windows followed by a permanent lock), per-failure delays running on a background thread to preserve UI responsiveness, and timing-safe BCrypt execution regardless of whether a submitted username exists, directly preventing username enumeration via response-time side-channel analysis. PIPEDA Principle 4.9, the right to erasure, is enforced at the database level through CASCADE DELETE constraints, ensuring that a user deletion physically removes all associated records rather than orphaning them.

---

## Known Limitations and Prototype Scope

The session model is held entirely in memory via `UserSessionService` with no JWT, no token expiry, and no session timeout, a known limitation acknowledged against Bill 194 and PIPEDA session management expectations. Lockout state in `LoginAttemptService` is also in-memory only, meaning an application restart clears all lockout records. Local configuration and cached data are stored unencrypted on the user's file system. These limitations are captured in the compliance roadmap, with JWT authentication, local encryption, and stronger password policy enforcement identified as the next implementation priorities before any production or public-sector deployment.

