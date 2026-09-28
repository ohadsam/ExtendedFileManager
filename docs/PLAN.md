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
| 1 | Core CRUD + compress/extract (incl. "Extract & Replace") | 3, 4 |
| 2 | Filter / group-by / sort (type, date, source app, ...) + global free-text search | 5 |
| 3 | Simple, comfortable UI/UX (grouped/sorted toolbar dropdowns with subheadings, hamburger nav drawer, long-press context actions) | 1 (shell), ongoing, 8 (polish) |
| 4 | Duplicate/identical file finder (hash-based, cross-extension) | 6 |
| 5 | Native, simple mobile app | Phase 0 decision (Kotlin native, Android only) |
| 6 | Performance + security critical | ongoing, 11, 18 |
| 7 | Detailed logs + weekly auto-cleanup + manual view/download/clear | 12 (view entry point in Phase 2's Settings shell) |
| 8 | Full audit mechanism with viewing | 13 |
| 9 | Confirm/warn before file operations | 3 (built into every mutating action) |
| 10 | System-file protection | 11 |
| 11 | Preview: video/audio/image | 7 |
| 12 | Multiple view sizes / detail levels | 8 (settings control in Phase 2's shell) |
| 13 | Favorites with editable internal hierarchy, tags, lock-against-delete, notes | 9 |
| 14 | Responsive to resolutions + Hebrew/English (RTL) | 14 (theme/language toggle lands in Phase 2's shell; full RTL polish in 14) |
| 15 | Extra capabilities — see below | woven throughout |
| 16 | Version management, seamless upgrade, What's New | 15 |
| 17 | release-checklist skill, multi-persona review, CI build+test on every push | 0, 18, ongoing |
| 18 | Skills/CLAUDE.md for token efficiency + model-tier guidance | 0 |
| 19 | Full test coverage: unit, security, performance, e2e/emulator | woven throughout + 18 |
| 20 | Settings screen (display, permission status, logs, color/theme, language) | 2 (shell), then wired up by 8, 11, 12, 14 |
| 21 | Storage optimization advisor: unused-large-file, junk, and temp-file recommendations, multi-select bulk actions, staged-for-deletion (review after 30 days) | 10 |
| 22 | In-app HTML user guide + per-screen info button, kept in sync every batch | 2 (shell + baseline content), grown every phase, sync enforced by 18's release-checklist |
| 23 | Google Drive upload (incl. whole-subtree), upload-status markers, bulk upload of the current selection, and Share/Send (email, WhatsApp, etc.) for one or more files | 16 |
| 24 | Daily "insights" digest (cleanup recommendations, duplicates, oversized files, unclear extensions), summary notification deep-linking into the screen, always-reachable drawer entry, actions performable on the results, smart incremental scanning (no full rescan every run), Settings toggles to disable the job / the notification | 17 |

## Suggested additional capabilities (item 15)

- **Batch/queue operations** with progress, pause/cancel, and a single confirmation for multi-file actions instead of one dialog per file.
- **Undo** for the last destructive action where technically possible (e.g., move-to-trash instead of hard delete, with a "Recently Deleted" area purged after N days).
- **Storage analyzer** — treemap/sunburst view of what's eating space, drill into large files/folders (complements Phase 10's recommendations with an exploratory view).
- *(Share-sheet integration and cloud/Drive access, originally suggested here, were concrete enough to become their own phase — see Phase 16 below.)*
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

- **Phase 1 — Core browsing (read-only)** ✅ *(done)*
  SAF/MediaStore-backed directory listing, permission request flow, breadcrumb navigation, Room-backed file index cache for performance. No mutations yet.
  Also establishes the app's navigation shell, since retrofitting navigation onto every screen later is far more work than building it once now: a hamburger-driven `ModalNavigationDrawer` (Browse/Favorites/Duplicates/Storage Advisor/Logs/Audit/Settings, each entry added by its owning phase), grouped toolbar dropdowns with subheadings instead of flat icon rows, and a long-press-to-select contextual-action pattern on file/folder rows (most of its actions are stubs until their owning phase lands — rename/delete in Phase 3, extract in Phase 4, tag/favorite/lock/notes in Phase 9, etc.). See `CLAUDE.md`'s "Navigation & toolbar conventions" for the full rules every later phase follows.
  The index entity captures **source-app metadata** at index time, not bolted on later, since every later filter/group/sort feature (Phase 5) depends on it existing per-file:
  - **Exact**, from `MediaStore.MediaColumns.OWNER_PACKAGE_NAME` (API 29+, populated for media/downloads MediaStore indexes automatically by the OS) — this is the only source that's actually authoritative.
  - **Heuristic fallback** for files MediaStore doesn't tag (older API levels, non-media files, manually-placed files): a maintained table of well-known folder-name → package patterns (`WhatsApp Images`/`WhatsApp Video` → `com.whatsapp`, `Telegram` → `org.telegram.messenger`, `Camera` → device camera, `Screenshots` → system, etc.), stored as data, not hardcoded logic, so it's easy to extend. Every heuristic match is flagged `confidence = HEURISTIC` in the model and shown differently in the UI (e.g. "likely WhatsApp" vs. a plain "WhatsApp" for exact matches) — never presented as certain when it isn't.
  - Resolved to a human label + icon via `PackageManager` when the owning app is still installed; falls back to the raw package name (or "Unknown source") when it's been uninstalled or no match exists.
  - **Known platform limit, not an app bug**: Android's scoped storage blocks any app — including this one — from browsing another app's private directory (`Android/data/<package>/…`, `Android/obb/<package>/…`) since API 30. That's Android protecting other apps' data, not something EFM can (or should try to) bypass, so "source app" is necessarily best-effort outside MediaStore's own tagging + folder heuristics — worth being upfront about rather than promising 100% attribution.
  - **Per-chat/group attribution (e.g. "which WhatsApp conversation this photo came from") is explicitly out of scope, and not a future-phase item either — it's not technically achievable without root.** WhatsApp (and similar messaging apps) never write that association to the filesystem: every media file for every chat lands in the same shared bucket (`WhatsApp Images`, `WhatsApp Video`, etc.) with a date-encoded filename, not a per-contact/group folder. The actual chat↔media mapping lives only inside WhatsApp's own sandboxed, encrypted database (`Android/data/com.whatsapp/…`), which scoped storage blocks any other app from reading, and which is encrypted in a way a non-rooted app can't decrypt even if it could reach it. What's genuinely available and worth building instead: WhatsApp does preserve a **sent-vs-received** split (a `Sent/` subfolder) and date/time (from the filename or file metadata), so EFM can filter/group/sort a WhatsApp media bucket by that — just not by conversation.

- **Phase 2 — Settings screen shell** ✅ *(done)*
  A real navigable Settings screen (reached from the main app bar), with sections that light up as later phases land rather than placeholders that never get wired up:
  - **Display** — default view mode (list/grid), density; real control added in Phase 8, this phase creates the section and a no-op/default-only version.
  - **Permissions** — shows current storage-access grant status (from Phase 1's permission flow) with a re-request/open-system-settings action; expands with the protected-path explanation in Phase 11.
  - **Logs** — entry point to the log viewer; the viewer itself is built in Phase 12, this phase reserves the menu item and wires navigation once it exists.
  - **Appearance** — light/dark/system + Material You dynamic color toggle (this is small enough to implement fully now, on top of the theme already in `ui/theme/Theme.kt`), persisted via a DataStore-backed preferences repository.
  - **Language** — Hebrew/English in-app override using `AppCompatDelegate`'s per-app language API (works standalone now; Phase 14 adds full RTL layout verification across the rest of the app on top of it).
  - **Help / User Guide** — entry point to a bundled, in-app HTML user guide (`assets/help.html`, opened in a `WebView` or Custom Tab) covering every capability the app has at any given point; this phase ships it with real Phase 0-2 content (not a placeholder), and every later phase is responsible for adding its own section when it lands (enforced by the `release-checklist` skill, see Phase 18). This phase also introduces a small reusable `InfoButton` composable (an ⓘ icon that opens a short contextual explanation, with a link into the relevant guide section for more detail) — used from Phase 3 onward on any screen non-obvious enough to warrant one, not on every screen reflexively.
  This phase also introduces the shared preferences repository (DataStore) later phases reuse instead of each inventing their own persistence.

- **Phase 3 — Core CRUD + confirmation framework** ✅ *(done)*
  Create/rename/delete/move/copy for files & folders. Every mutating action routes through a shared `ConfirmDangerousAction` component (item 9) and writes an audit entry (groundwork for Phase 13). Undo/trash for delete.

- **Phase 4 — Compress / extract**
  Zip create/extract (java.util.zip baseline; evaluate Apache Commons Compress for broader format read-support: tar, gz, 7z-read). Progress + cancel for large archives.
  Two distinct extract actions, both offered wherever an archive can be extracted:
  - **Extract** — the plain operation; archive is left in place.
  - **Extract & Replace** — extracts, and once extraction has fully succeeded (every entry written and verified, not just "started"), deletes the original archive. Never deletes on a partial/failed/cancelled extraction. The delete step reuses Phase 3's confirmation framework and goes to trash rather than a hard delete — same as any other destructive action in the app, so it's undoable like everything else, and it emits the same audit event a manual delete would.
  Extraction also needs a conflict policy (skip / overwrite / keep-both-rename) for when target files already exist — surfaced once per operation, not per-file, unless the user asks to decide per-file.

- **Phase 5 — Filter, sort, group-by engine, and global search**
  A single reusable query spec (type, extension, date range, size range, source app/package, favorite status, tag, locked status, free-text) applied consistently across browse/duplicates/favorites — search is not a bolted-on separate screen, it's the same query spec with its text field filled in, so every filter/sort/group control works identically whether the user got there by browsing or by searching. Tag, locked, and favorite are defined here as dimensions even though the data behind them lands in Phase 9 — the query spec is built to accommodate them from the start rather than retrofitted later.
  - **Global free-text search bar**, reachable from anywhere in the app (persistent search icon in the top bar), matching against filename and path substrings from the Room file index — backed by SQLite FTS (Room's `@Fts4`/`Fts5` entity) rather than a `LIKE` scan, so it stays fast as the index grows into the tens of thousands of files. Results respect whatever filters are currently active (e.g. search "invoice" within "PDFs from the last month").
  - Source app is a first-class filter/group dimension, not an afterthought: filter to "files from WhatsApp," group the current results by owning app (with a distinct "Unknown source" bucket), sort within a group like any other. The confidence flag from Phase 1's index (exact `OWNER_PACKAGE_NAME` vs. heuristic folder match) carries through to these views so a heuristic grouping is visibly marked as such, not presented with the same certainty as an exact one.

- **Phase 6 — Duplicate & identical-file finder**
  Two-stage: cheap pre-filter (file size, then partial/head hash) → full SHA-256 streaming hash only on remaining candidates, to stay fast on large volumes. Cross-extension identical-content detection (hash content, ignore name/extension). Background via WorkManager with progress + cancel; results grouped for bulk review/delete.

- **Phase 7 — Preview**
  Images (Coil), video/audio (Media3/ExoPlayer), PDF (PdfRenderer). Inline preview pane + full-screen viewer, works from browse, search, and duplicate-review screens.

- **Phase 8 — View modes & UI polish**
  List / grid / compact / detailed density options, remembered per-folder or globally, and this is where the Settings screen's Display section gets its real control (Phase 2 built the shell). Also where general UI/UX passes happen continuously (item 3) — including auditing every screen built so far against the navigation drawer / toolbar-dropdown / long-press conventions from Phase 1, since a real-world screen doesn't always end up following its own pattern perfectly on the first pass.

- **Phase 9 — Per-file metadata: favorites, tags, lock, notes**
  One Room feature, not four, since all of these are the same shape (metadata attached to a file/folder, independent of its content) and share one "file details" UI surface:
  - **Favorites**, with hierarchy: self-referencing Room tree (nested favorite "collections"), add/rename/delete/reorder/move-between-collections, drag-and-drop where practical. Marking a file/folder as a favorite (with an optional collection) is the base action; the hierarchy is what organizes it.
  - **Tags** — free-form, many-to-many labels a file/folder can carry (a file can have several), created ad-hoc from the same UI that applies them, with rename/delete/merge management for the tag list itself.
  - **Lock against deletion** — a per-file/folder flag that blocks deletion (and Phase 4's Extract & Replace auto-delete) until explicitly unlocked; this phase retrofits Phase 3's delete-confirmation path and Phase 4's Extract & Replace to check it, refusing (not just warning) a locked item and explaining why, with an "unlock" action offered right there.
  - **Notes** — a free-text note per file/folder, separate from tags, shown in the same file-details view and editable there.
  - **Filter/group by all of it**: tag, locked status, and favorite status are already part of Phase 5's query spec, so this phase's job is to make sure the UI actually exposes filtering/grouping by tag (including multi-tag), by "locked," and by "favorite" — not just storing the data. A file/folder's details panel becomes the one place to see and edit all four at once.

- **Phase 10 — Storage optimization advisor**
  Recommendations only — this phase never auto-deletes anything; everything lands in a reviewable, multi-select list the user acts on explicitly, same as Phase 6's duplicate-review UI (and reuses its WorkManager background-scan + progress/cancel pattern). Three recommendation categories, plus a staged-deletion workflow:
  - **Large files unused for a long time.** Honesty matters here: Android doesn't give any app a reliable system-wide "last opened" timestamp (`atime` is effectively unavailable — most Android filesystems mount `noatime`/`relatime`, and MediaStore has no genuine last-accessed column). So "unused" is scored from what's actually available — `lastModified` (and MediaStore's `DATE_ADDED`) as the primary signal, plus an **EFM-tracked "last opened via this app"** timestamp recorded whenever the user previews/opens a file through EFM itself (Phase 7) as a secondary, admittedly partial signal. The recommendation is explicitly framed as "not modified in N months" / "not opened via this app in N months," never as a false claim of true system-wide usage tracking.
  - **Junk file detection**, scoped honestly to what EFM can actually see: scoped storage (since API 30) blocks any app — including this one — from reading another app's private cache (`Android/data/<package>/…`), so this can't be a general "clean other apps' caches" tool the way pre-scoped-storage cleaners were. What's real and buildable: orphaned files/folders left behind by now-*uninstalled* apps (an `Android/media/<package>` folder whose package no longer resolves via `PackageManager`), empty folders, and EFM's own app-private cache.
  - **Temporary file detection** — extension/pattern heuristics within accessible shared storage (`.tmp`, `.temp`, `.log`, `.bak`, `.cache`, `.crdownload`, `.part`, leftover `.trashed-*`), each flagged with why it matched, never silently bundled in with "junk."
  - **Staged-for-deletion review** ("mark now, decide later"): a distinct action from delete or trash — marks a file/folder with a `stagedAt` timestamp (an extension of Phase 9's per-file metadata, not a new table) without touching it at all; a WorkManager periodic job checks for items past their review window (default 30 days, configurable in Settings) and surfaces them in a review list — never auto-deletes, only prompts. A locked file (Phase 9) can't be staged.
  - All three recommendation categories and the staged-review list share one multi-select bulk-action UI (select several → delete-with-confirmation, or dismiss the recommendation), consistent with Phase 6's duplicate-review pattern rather than a new interaction model.

- **Phase 11 — Security hardening**
  Protected-path blocklist (Android/data, Android/obb, app-internal dirs, other apps' private storage — inherently inaccessible under scoped storage, but explicit guard + clear error rather than a silent failure), path-traversal checks, Keystore-backed encryption for sensitive prefs and the vault, R8/ProGuard release config, dependency vulnerability scanning wired into CI. Settings screen's Permissions section gets the protected-path explanation.

- **Phase 12 — Logs**
  Structured logging (Timber → Room-backed log store), weekly auto-purge via WorkManager `PeriodicWorkRequest`, in-app log viewer with filter, export-to-file, and manual "clear now" — reachable from the Settings screen's Logs entry (Phase 2).

- **Phase 13 — Audit trail**
  Every mutating action (already emitting from Phase 3 onward) surfaced in a dedicated, filterable/searchable audit viewer; exportable; tamper-evident (hash-chained entries) since this is a security-sensitive log distinct from debug logs.

- **Phase 14 — Localization & responsiveness**
  Hebrew + English resource sets, full RTL verification (Compose `LayoutDirection`, mirrored icons/gestures) across every screen built so far, `WindowSizeClass`-driven adaptive layouts for phone/foldable, dynamic font-scale support. Builds on the Settings screen's Language toggle from Phase 2.

- **Phase 15 — Versioning & updates**
  In-app update checker against GitHub Releases (APK signature verification before install prompt) or Play Core In-App Update API if/when published to Play; versioned "What's New" sheet shown once per upgrade, sourced from a changelog file.

- **Phase 16 — Cloud upload & sharing**
  Builds directly on Phase 3's file-operation plumbing rather than inventing a parallel upload pipeline:
  - **Google Drive upload** — Google Drive already exposes itself as a Storage Access Framework `DocumentsProvider`, so "upload to Drive" is the user granting a Drive folder the same way they grant any local folder (Phase 1's tree picker), after which it's just a destination for Phase 3's existing `copy()` — no OAuth flow, no Google Drive REST API client, no separate credential storage needed to get real uploads working. **Whole-subtree upload** falls out of this for free: Phase 3's `copyRecursively` already walks folders, so uploading a folder uploads everything under it, preserving structure.
  - **Upload-status markers** — a small badge on the file row (and in the selection action bar while a transfer is active) showing queued/uploading/done/failed, backed by WorkManager for large transfers so they survive the app leaving the foreground (the same background-job pattern Phase 6 establishes for duplicate scanning, reused rather than reinvented). Never silently fails: a failed upload stays visibly marked until dismissed or retried.
  - **Bulk upload of the current selection** — not a new selection mechanism: Phase 3's multi-select + destination-picker flow already generalizes to "pick N items, pick a Drive-backed folder, copy them all." This phase's job is surfacing "Upload to Drive" as an explicit action alongside Move/Copy in the selection action bar.
  - **Share / Send** (email, WhatsApp, or any other installed app) for one or more selected files via Android's native share sheet (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`), wired to the same multi-select action bar. SAF document URIs are already `content://` and directly shareable with a read-permission grant on the share `Intent` — no separate `FileProvider` needed for files already under a tree the user granted EFM.

- **Phase 17 — Daily insights & smart notifications**
  Turns Phase 10's Storage Advisor from something the user has to remember to open into a proactive daily habit, and folds Phase 6's duplicate results into the same hub — one screen, one notification, not two separate systems:
  - **Smart, incremental scanning — never a full rescan on every run.** A new `insights_cache` Room table stores each category's last result plus a `lastComputedAt` timestamp and the file-index `changeVersion` it was computed against (a lightweight counter, bumped by Phase 1's `refresh()` / Phase 3's mutating operations, since every write already touches the Room-cached index). On each daily run:
    - Large/junk/temp-file and staged-for-deletion recommendations (Phase 10) are cheap to recompute since they only read the already-current file index — recomputed every run.
    - Duplicate detection (Phase 6's hashing engine) is the expensive one — only reruns in full if the change-version has moved past a configurable threshold since the last full duplicate scan; otherwise the cached duplicate groups are re-surfaced as-is, filtered to drop any entries whose files no longer exist.
    - **Unclear file extensions** — a new lightweight category: files with an unrecognized or unusual extension (not in a maintained known-extensions table), or a suspicious double extension (e.g. `.pdf.exe`), flagged as "worth a look," never as a security verdict — a hygiene nudge, not a malware scanner.
  - **Daily WorkManager job** (`PeriodicWorkRequest`, ~once/day, respecting battery/charging constraints like Phase 6's scan) computes the above and, if anything's found, posts **one summary notification** ("N things worth a look") via a dedicated notification channel — never one notification per category. Tapping it deep-links straight into the Insights screen (the same screen Phase 10 already built, now also showing duplicates and unclear extensions).
  - **Always reachable, notification or not** — Insights lives as its own entry in the nav drawer (already slated for Phase 10, as "Storage Advisor"; it becomes "Insights" once this phase lands), so dismissing or ignoring the notification never loses access to it.
  - **Directly actionable** — the Insights screen keeps Phase 10's multi-select bulk-action pattern (select → delete/move-with-confirmation, or dismiss) across every category shown, duplicates included — never a read-only report the user has to act on from a different screen.
  - **Settings controls** (a new Insights section in the Settings screen) — two independent switches: **"Run daily insights"** (stops the WorkManager job entirely) and **"Notify me"** (keeps the job running so the screen stays fresh, just suppresses the notification) — a user may want the data ready without being pinged. On API 33+, the notification toggle is backed by the real `POST_NOTIFICATIONS` runtime permission, requested with an in-app rationale the same way Phase 1 explains the storage-access request.

- **Phase 18 — Test & release hardening**
  Coverage gates (Jacoco) on domain/data layers, security test suite (encryption round-trip, protected-path denial, path traversal), performance benchmarks (hash throughput, duplicate-scan on large trees, cold-start time via Macrobenchmark), Compose UI + Espresso E2E suite on an emulator matrix in CI, first tagged release via the `release-checklist` skill (multi-persona review: system architect / UI expert / UX expert / QA architect, version bump, What's New, docs/skills refresh, user guide + info-button sync).

Phases 3–17 can reorder slightly as real constraints surface, but the dependency chain (0→1→2→3 first; hashing needs the file index from 1; audit viewer needs the audit events from 3; Storage advisor (10) needs the duplicate-review UI pattern from 6 and the per-file metadata from 9; cloud upload (16) needs Phase 3's copy/move plumbing and Phase 6's WorkManager pattern; daily insights (17) needs Phase 10's recommendation categories and Phase 6's duplicate engine; etc.) stays fixed.

## CI/CD (GitHub Actions), from Phase 0 onward

- `android-ci.yml`: on every push/PR — `assembleDebug`, unit tests (JVM, Robolectric where needed), `ktlint`, `detekt`, Android Lint.
- `instrumented-tests.yml` (from Phase 18, introduced earlier if a phase needs device-level verification sooner): Compose/Espresso tests on `reactivecircus/android-emulator-runner`.
- `release.yml`: on version tag — signed release build (AAB/APK), changelog extraction, GitHub Release publish.
- `security.yml`: CodeQL + dependency review, scheduled + on PR.

## Skills & docs (item 18)

- `CLAUDE.md` at repo root: architecture map, module/package conventions, how to run what's runnable locally vs. what needs CI (no Android SDK in this sandbox), and a **model-tier guide**: routine UI/text/test-scaffolding work → default/fast tier; architecture decisions, security-sensitive code (encryption, permission boundaries, hashing correctness), and release reviews → escalate reasoning effort/model tier.
- Repo-local skill `release-checklist` (mirrors the one in `system_diagram`): version bump, What's New entry, 4-persona review (system architect / UI expert / UX expert / QA architect), docs+skills refresh, **user guide (`assets/help.html`) + every `InfoButton`'s copy checked against this batch's actual behavior**, CI-green gate, merge.
- Repo-local skill `add-feature`: conventions for adding a new feature package (folder layout, ViewModel/UseCase/Repository wiring, where tests go) — added once Phase 3 or 4 establishes the pattern concretely, not speculatively now.

## Known sandbox constraint

This dev container has JDK 21 and Gradle 8.14 but **no Android SDK**, so `./gradlew assembleDebug`/instrumented tests cannot run locally here — GitHub Actions CI is the real build/test gate for every phase. Kotlin source will be written carefully and cross-checked, but "CI green" is the actual verification signal, not a local build.
