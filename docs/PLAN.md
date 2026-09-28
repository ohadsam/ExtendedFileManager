# Extended File Manager (EFM) — Master Plan

**Platform:** Android (native), mobile only, min SDK 26 (Android 8.0), target/compile latest stable.
**Language / UI:** Kotlin, Jetpack Compose + Material 3.
**Architecture:** Clean Architecture (data / domain / ui) inside a single `:app` module for now; split into Gradle modules later only if build time actually requires it (avoid premature multi-module complexity).
**DI:** Hilt. **Async:** Kotlin Coroutines + Flow. **DB:** Room. **DI for background jobs:** WorkManager.
**Distribution:** GitHub Releases (signed APK) to start; Play Store optional later. Update-check mechanism must work either way.

This document is the living roadmap. Each phase is a merged, working increment — the app must build, boot, and pass its tests in CI before the next phase starts. Update this file's checkboxes as phases land.

## Why these choices

- **Kotlin native, not Flutter/Electron** — per explicit decision: real native Android, deepest access to Storage Access Framework / MediaStore, Android Keystore, WorkManager, and the smallest attack surface (no bundled JS runtime).
- **Single module, package-by-feature** — 19 requirement areas is a lot of surface, but Gradle multi-module mainly pays off for build parallelism/team-scaling, not for a project this size at day one. Package boundaries (`data/`, `domain/`, `ui/feature/x`) give the same separation without the build-config overhead. Revisit if `:app` compile times become painful.
- **Room + content hashing, not a cloud index** — everything must work fully offline; the device is the source of truth.
- **Scoped storage / SAF, not `MANAGE_EXTERNAL_STORAGE` by default** — Google increasingly restricts broad "all files" access; EFM should request the narrowest permission that satisfies each feature, and only fall back to `MANAGE_EXTERNAL_STORAGE` (with a clear in-app justification screen, needed for a full file manager to browse arbitrary folders) where SAF genuinely can't do the job.
- **A Settings screen scaffolded early (Phase 2), filled in incrementally** — display/permissions/logs/theme/language settings each depend on a feature landing first (view modes, log store, localization), but the screen *container* and navigation entry don't need to wait. Building the shell early means every later phase just adds a section to an existing, working screen instead of bolting on a whole new screen at the end.

## Requirement map (user's numbered list → where it lands)

| # | Requirement | Phase |
|---|---|---|
| 1 | Core CRUD + compress/extract | 3, 4 |
| 2 | Filter / group-by / sort (type, date, source app, ...) + global free-text search | 5 |
| 3 | Simple, comfortable UI/UX | ongoing, 8 |
| 4 | Duplicate/identical file finder (hash-based, cross-extension) | 6 |
| 5 | Native, simple mobile app | Phase 0 decision (Kotlin native, Android only) |
| 6 | Performance + security critical | ongoing, 10, 15 |
| 7 | Detailed logs + weekly auto-cleanup + manual view/download/clear | 11 (view entry point in Phase 2's Settings shell) |
| 8 | Full audit mechanism with viewing | 12 |
| 9 | Confirm/warn before file operations | 3 (built into every mutating action) |
| 10 | System-file protection | 10 |
| 11 | Preview: video/audio/image | 7 |
| 12 | Multiple view sizes / detail levels | 8 (settings control in Phase 2's shell) |
| 13 | Favorites with editable internal hierarchy | 9 |
| 14 | Responsive to resolutions + Hebrew/English (RTL) | 13 (theme/language toggle lands in Phase 2's shell; full RTL polish in 13) |
| 15 | Extra capabilities — see below | woven throughout |
| 16 | Version management, seamless upgrade, What's New | 14 |
| 17 | release-checklist skill, multi-persona review, CI build+test on every push | 0, 15, ongoing |
| 18 | Skills/CLAUDE.md for token efficiency + model-tier guidance | 0 |
| 19 | Full test coverage: unit, security, performance, e2e/emulator | woven throughout + 15 |
| 20 | Settings screen (display, permission status, logs, color/theme, language) | 2 (shell), then wired up by 8, 10, 11, 13 |

## Suggested additional capabilities (item 15)

- **Batch/queue operations** with progress, pause/cancel, and a single confirmation for multi-file actions instead of one dialog per file.
- **Undo** for the last destructive action where technically possible (e.g., move-to-trash instead of hard delete, with a "Recently Deleted" area purged after N days).
- **Storage analyzer** — treemap/sunburst view of what's eating space, drill into large files/folders.
- **Smart "Clean up" suggestions** — cache junk, empty folders, old screenshots, large old downloads — always opt-in, never auto-deletes without confirmation.
- **Tagging** (free-form labels) in addition to folder-based favorites, so a file can belong to multiple logical groups.
- **Quick actions / share sheet integration** (Android share target) so other apps can send files to EFM, and EFM can share out.
- **Cloud/network locations** (SAF-based access to Drive/Dropbox/SMB providers already exposed via Android's document provider framework) — read-only browse first, later full ops.
- **Saved searches** — a filter+sort+group+search-text combination saved as a named smart folder (the search bar itself is core, in Phase 5 — this is the "save it for later" extra on top).
- **File integrity / checksum tools** exposed directly to the user (compute & compare SHA-256/MD5 of a file, verify against a known hash).
- **Encrypted vault** — a password/biometric-gated folder for sensitive files (encrypted at rest via Keystore-wrapped key), separate from the audit-log encryption.
- **Widgets & shortcuts** — home-screen widget for a favorite folder, app shortcuts for "Scan duplicates", "Open Downloads".
- **Accessibility** — TalkBack labels, minimum touch targets, dynamic font scaling support, high-contrast theme.
- **Biometric app-lock** (optional) for the whole app or just the vault/audit views.

## Phase plan

Each phase = one PR into `main` (via the working branch), green CI, before the next starts.

- **Phase 0 — Foundation** ✅ *(done)*
  Gradle/Kotlin/Compose/Hilt/Room skeleton, package structure, `.gitignore`, GitHub Actions CI (build + unit tests + lint/detekt/ktlint on every push), `CLAUDE.md`, this plan, repo-local skills stub. App builds and boots to an empty scaffold screen.

- **Phase 1 — Core browsing (read-only)** *(in progress)*
  SAF/MediaStore-backed directory listing, permission request flow, breadcrumb navigation, Room-backed file index cache for performance. No mutations yet.
  The index entity captures **source-app metadata** at index time, not bolted on later, since every later filter/group/sort feature (Phase 5) depends on it existing per-file:
  - **Exact**, from `MediaStore.MediaColumns.OWNER_PACKAGE_NAME` (API 29+, populated for media/downloads MediaStore indexes automatically by the OS) — this is the only source that's actually authoritative.
  - **Heuristic fallback** for files MediaStore doesn't tag (older API levels, non-media files, manually-placed files): a maintained table of well-known folder-name → package patterns (`WhatsApp Images`/`WhatsApp Video` → `com.whatsapp`, `Telegram` → `org.telegram.messenger`, `Camera` → device camera, `Screenshots` → system, etc.), stored as data, not hardcoded logic, so it's easy to extend. Every heuristic match is flagged `confidence = HEURISTIC` in the model and shown differently in the UI (e.g. "likely WhatsApp" vs. a plain "WhatsApp" for exact matches) — never presented as certain when it isn't.
  - Resolved to a human label + icon via `PackageManager` when the owning app is still installed; falls back to the raw package name (or "Unknown source") when it's been uninstalled or no match exists.
  - **Known platform limit, not an app bug**: Android's scoped storage blocks any app — including this one — from browsing another app's private directory (`Android/data/<package>/…`, `Android/obb/<package>/…`) since API 30. That's Android protecting other apps' data, not something EFM can (or should try to) bypass, so "source app" is necessarily best-effort outside MediaStore's own tagging + folder heuristics — worth being upfront about rather than promising 100% attribution.
  - **Per-chat/group attribution (e.g. "which WhatsApp conversation this photo came from") is explicitly out of scope, and not a future-phase item either — it's not technically achievable without root.** WhatsApp (and similar messaging apps) never write that association to the filesystem: every media file for every chat lands in the same shared bucket (`WhatsApp Images`, `WhatsApp Video`, etc.) with a date-encoded filename, not a per-contact/group folder. The actual chat↔media mapping lives only inside WhatsApp's own sandboxed, encrypted database (`Android/data/com.whatsapp/…`), which scoped storage blocks any other app from reading, and which is encrypted in a way a non-rooted app can't decrypt even if it could reach it. What's genuinely available and worth building instead: WhatsApp does preserve a **sent-vs-received** split (a `Sent/` subfolder) and date/time (from the filename or file metadata), so EFM can filter/group/sort a WhatsApp media bucket by that — just not by conversation.

- **Phase 2 — Settings screen shell**
  A real navigable Settings screen (reached from the main app bar), with sections that light up as later phases land rather than placeholders that never get wired up:
  - **Display** — default view mode (list/grid), density; real control added in Phase 8, this phase creates the section and a no-op/default-only version.
  - **Permissions** — shows current storage-access grant status (from Phase 1's permission flow) with a re-request/open-system-settings action; expands with the protected-path explanation in Phase 10.
  - **Logs** — entry point to the log viewer; the viewer itself is built in Phase 11, this phase reserves the menu item and wires navigation once it exists.
  - **Appearance** — light/dark/system + Material You dynamic color toggle (this is small enough to implement fully now, on top of the theme already in `ui/theme/Theme.kt`), persisted via a DataStore-backed preferences repository.
  - **Language** — Hebrew/English in-app override using `AppCompatDelegate`'s per-app language API (works standalone now; Phase 13 adds full RTL layout verification across the rest of the app on top of it).
  This phase also introduces the shared preferences repository (DataStore) later phases reuse instead of each inventing their own persistence.

- **Phase 3 — Core CRUD + confirmation framework**
  Create/rename/delete/move/copy for files & folders. Every mutating action routes through a shared `ConfirmDangerousAction` component (item 9) and writes an audit entry (groundwork for Phase 12). Undo/trash for delete.

- **Phase 4 — Compress / extract**
  Zip create/extract (java.util.zip baseline; evaluate Apache Commons Compress for broader format read-support: tar, gz, 7z-read). Progress + cancel for large archives.

- **Phase 5 — Filter, sort, group-by engine, and global search**
  A single reusable query spec (type, extension, date range, size range, source app/package, favorite status, tag, free-text) applied consistently across browse/duplicates/favorites — search is not a bolted-on separate screen, it's the same query spec with its text field filled in, so every filter/sort/group control works identically whether the user got there by browsing or by searching.
  - **Global free-text search bar**, reachable from anywhere in the app (persistent search icon in the top bar), matching against filename and path substrings from the Room file index — backed by SQLite FTS (Room's `@Fts4`/`Fts5` entity) rather than a `LIKE` scan, so it stays fast as the index grows into the tens of thousands of files. Results respect whatever filters are currently active (e.g. search "invoice" within "PDFs from the last month").
  - Source app is a first-class filter/group dimension, not an afterthought: filter to "files from WhatsApp," group the current results by owning app (with a distinct "Unknown source" bucket), sort within a group like any other. The confidence flag from Phase 1's index (exact `OWNER_PACKAGE_NAME` vs. heuristic folder match) carries through to these views so a heuristic grouping is visibly marked as such, not presented with the same certainty as an exact one.

- **Phase 6 — Duplicate & identical-file finder**
  Two-stage: cheap pre-filter (file size, then partial/head hash) → full SHA-256 streaming hash only on remaining candidates, to stay fast on large volumes. Cross-extension identical-content detection (hash content, ignore name/extension). Background via WorkManager with progress + cancel; results grouped for bulk review/delete.

- **Phase 7 — Preview**
  Images (Coil), video/audio (Media3/ExoPlayer), PDF (PdfRenderer). Inline preview pane + full-screen viewer, works from browse, search, and duplicate-review screens.

- **Phase 8 — View modes & UI polish**
  List / grid / compact / detailed density options, remembered per-folder or globally, and this is where the Settings screen's Display section gets its real control (Phase 2 built the shell). Also where general UI/UX passes happen continuously (item 3).

- **Phase 9 — Favorites with hierarchy**
  Self-referencing Room tree (nested favorite "collections"), add/rename/delete/reorder/move-between-collections, drag-and-drop where practical.

- **Phase 10 — Security hardening**
  Protected-path blocklist (Android/data, Android/obb, app-internal dirs, other apps' private storage — inherently inaccessible under scoped storage, but explicit guard + clear error rather than a silent failure), path-traversal checks, Keystore-backed encryption for sensitive prefs and the vault, R8/ProGuard release config, dependency vulnerability scanning wired into CI. Settings screen's Permissions section gets the protected-path explanation.

- **Phase 11 — Logs**
  Structured logging (Timber → Room-backed log store), weekly auto-purge via WorkManager `PeriodicWorkRequest`, in-app log viewer with filter, export-to-file, and manual "clear now" — reachable from the Settings screen's Logs entry (Phase 2).

- **Phase 12 — Audit trail**
  Every mutating action (already emitting from Phase 3 onward) surfaced in a dedicated, filterable/searchable audit viewer; exportable; tamper-evident (hash-chained entries) since this is a security-sensitive log distinct from debug logs.

- **Phase 13 — Localization & responsiveness**
  Hebrew + English resource sets, full RTL verification (Compose `LayoutDirection`, mirrored icons/gestures) across every screen built so far, `WindowSizeClass`-driven adaptive layouts for phone/foldable, dynamic font-scale support. Builds on the Settings screen's Language toggle from Phase 2.

- **Phase 14 — Versioning & updates**
  In-app update checker against GitHub Releases (APK signature verification before install prompt) or Play Core In-App Update API if/when published to Play; versioned "What's New" sheet shown once per upgrade, sourced from a changelog file.

- **Phase 15 — Test & release hardening**
  Coverage gates (Jacoco) on domain/data layers, security test suite (encryption round-trip, protected-path denial, path traversal), performance benchmarks (hash throughput, duplicate-scan on large trees, cold-start time via Macrobenchmark), Compose UI + Espresso E2E suite on an emulator matrix in CI, first tagged release via the `release-checklist` skill (multi-persona review: system architect / UI expert / UX expert / QA architect, version bump, What's New, docs/skills refresh).

Phases 3–14 can reorder slightly as real constraints surface, but the dependency chain (0→1→2→3 first; hashing needs the file index from 1; audit viewer needs the audit events from 3; etc.) stays fixed.

## CI/CD (GitHub Actions), from Phase 0 onward

- `android-ci.yml`: on every push/PR — `assembleDebug`, unit tests (JVM, Robolectric where needed), `ktlint`, `detekt`, Android Lint.
- `instrumented-tests.yml` (from Phase 15, introduced earlier if a phase needs device-level verification sooner): Compose/Espresso tests on `reactivecircus/android-emulator-runner`.
- `release.yml`: on version tag — signed release build (AAB/APK), changelog extraction, GitHub Release publish.
- `security.yml`: CodeQL + dependency review, scheduled + on PR.

## Skills & docs (item 18)

- `CLAUDE.md` at repo root: architecture map, module/package conventions, how to run what's runnable locally vs. what needs CI (no Android SDK in this sandbox), and a **model-tier guide**: routine UI/text/test-scaffolding work → default/fast tier; architecture decisions, security-sensitive code (encryption, permission boundaries, hashing correctness), and release reviews → escalate reasoning effort/model tier.
- Repo-local skill `release-checklist` (mirrors the one in `system_diagram`): version bump, What's New entry, 4-persona review (system architect / UI expert / UX expert / QA architect), docs+skills refresh, CI-green gate, merge.
- Repo-local skill `add-feature`: conventions for adding a new feature package (folder layout, ViewModel/UseCase/Repository wiring, where tests go) — added once Phase 3 or 4 establishes the pattern concretely, not speculatively now.

## Known sandbox constraint

This dev container has JDK 21 and Gradle 8.14 but **no Android SDK**, so `./gradlew assembleDebug`/instrumented tests cannot run locally here — GitHub Actions CI is the real build/test gate for every phase. Kotlin source will be written carefully and cross-checked, but "CI green" is the actual verification signal, not a local build.
