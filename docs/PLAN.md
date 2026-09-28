# Extended File Manager (EFM) — Master Plan

**Platform:** Android (native), min SDK 26 (Android 8.0), target/compile latest stable.
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

## Requirement map (user's numbered list → where it lands)

| # | Requirement | Phase |
|---|---|---|
| 1 | Core CRUD + compress/extract | 2, 3 |
| 2 | Filter / group-by / sort (type, date, source app, ...) | 4 |
| 3 | Simple, comfortable UI/UX | ongoing, 7 |
| 4 | Duplicate/identical file finder (hash-based, cross-extension) | 5 |
| 5 | Native, simple app | Phase 0 decision (Kotlin native) |
| 6 | Performance + security critical | ongoing, 9, 14 |
| 7 | Detailed logs + weekly auto-cleanup + manual view/download/clear | 10 |
| 8 | Full audit mechanism with viewing | 11 |
| 9 | Confirm/warn before file operations | 2 (built into every mutating action) |
| 10 | System-file protection | 9 |
| 11 | Preview: video/audio/image | 6 |
| 12 | Multiple view sizes / detail levels | 7 |
| 13 | Favorites with editable internal hierarchy | 8 |
| 14 | Responsive to resolutions + Hebrew/English (RTL) | 12 |
| 15 | Extra capabilities — see below | woven throughout |
| 16 | Version management, seamless upgrade, What's New | 13 |
| 17 | release-checklist skill, multi-persona review, CI build+test on every push | 0, 14, ongoing |
| 18 | Skills/CLAUDE.md for token efficiency + model-tier guidance | 0 |
| 19 | Full test coverage: unit, security, performance, e2e/emulator | woven throughout + 14 |

## Suggested additional capabilities (item 15)

- **Batch/queue operations** with progress, pause/cancel, and a single confirmation for multi-file actions instead of one dialog per file.
- **Undo** for the last destructive action where technically possible (e.g., move-to-trash instead of hard delete, with a "Recently Deleted" area purged after N days).
- **Storage analyzer** — treemap/sunburst view of what's eating space, drill into large files/folders.
- **Smart "Clean up" suggestions** — cache junk, empty folders, old screenshots, large old downloads — always opt-in, never auto-deletes without confirmation.
- **Tagging** (free-form labels) in addition to folder-based favorites, so a file can belong to multiple logical groups.
- **Quick actions / share sheet integration** (Android share target) so other apps can send files to EFM, and EFM can share out.
- **Cloud/network locations** (SAF-based access to Drive/Dropbox/SMB providers already exposed via Android's document provider framework) — read-only browse first, later full ops.
- **Search** with saved searches (a filter+sort+group combination saved as a named smart folder).
- **File integrity / checksum tools** exposed directly to the user (compute & compare SHA-256/MD5 of a file, verify against a known hash).
- **Encrypted vault** — a password/biometric-gated folder for sensitive files (encrypted at rest via Keystore-wrapped key), separate from the audit-log encryption.
- **Widgets & shortcuts** — home-screen widget for a favorite folder, app shortcuts for "Scan duplicates", "Open Downloads".
- **Accessibility** — TalkBack labels, minimum touch targets, dynamic font scaling support, high-contrast theme.
- **Biometric app-lock** (optional) for the whole app or just the vault/audit views.

## Phase plan

Each phase = one PR into `main` (via the working branch), green CI, before the next starts.

- **Phase 0 — Foundation** ✅ *(this session)*
  Gradle/Kotlin/Compose/Hilt/Room skeleton, package structure, `.gitignore`, GitHub Actions CI (build + unit tests + lint/detekt/ktlint on every push), `CLAUDE.md`, this plan, repo-local skills stub. App builds and boots to an empty scaffold screen.

- **Phase 1 — Core browsing (read-only)**
  SAF/MediaStore-backed directory listing, permission request flow, breadcrumb navigation, Room-backed file index cache for performance. No mutations yet.

- **Phase 2 — Core CRUD + confirmation framework**
  Create/rename/delete/move/copy for files & folders. Every mutating action routes through a shared `ConfirmDangerousAction` component (item 9) and writes an audit entry (groundwork for Phase 11). Undo/trash for delete.

- **Phase 3 — Compress / extract**
  Zip create/extract (java.util.zip baseline; evaluate Apache Commons Compress for broader format read-support: tar, gz, 7z-read). Progress + cancel for large archives.

- **Phase 4 — Filter, sort, group-by engine**
  A single reusable query spec (type, extension, date range, size range, source app/package, favorite status, tag) applied consistently across browse/search/duplicates.

- **Phase 5 — Duplicate & identical-file finder**
  Two-stage: cheap pre-filter (file size, then partial/head hash) → full SHA-256 streaming hash only on remaining candidates, to stay fast on large volumes. Cross-extension identical-content detection (hash content, ignore name/extension). Background via WorkManager with progress + cancel; results grouped for bulk review/delete.

- **Phase 6 — Preview**
  Images (Coil), video/audio (Media3/ExoPlayer), PDF (PdfRenderer). Inline preview pane + full-screen viewer, works from browse, search, and duplicate-review screens.

- **Phase 7 — View modes & UI polish**
  List / grid / compact / detailed density options, remembered per-folder or globally; this is also where general UI/UX passes happen continuously (item 3).

- **Phase 8 — Favorites with hierarchy**
  Self-referencing Room tree (nested favorite "collections"), add/rename/delete/reorder/move-between-collections, drag-and-drop where practical.

- **Phase 9 — Security hardening**
  Protected-path blocklist (Android/data, Android/obb, app-internal dirs, other apps' private storage — inherently inaccessible under scoped storage, but explicit guard + clear error rather than a silent failure), path-traversal checks, Keystore-backed encryption for sensitive prefs and the vault, R8/ProGuard release config, dependency vulnerability scanning wired into CI.

- **Phase 10 — Logs**
  Structured logging (Timber → Room-backed log store), weekly auto-purge via WorkManager `PeriodicWorkRequest`, in-app log viewer with filter, export-to-file, and manual "clear now".

- **Phase 11 — Audit trail**
  Every mutating action (already emitting from Phase 2 onward) surfaced in a dedicated, filterable/searchable audit viewer; exportable; tamper-evident (hash-chained entries) since this is a security-sensitive log distinct from debug logs.

- **Phase 12 — Localization & responsiveness**
  Hebrew + English resource sets, full RTL verification (Compose `LayoutDirection`, mirrored icons/gestures), `WindowSizeClass`-driven adaptive layouts for phone/tablet/foldable, dynamic font-scale support.

- **Phase 13 — Versioning & updates**
  In-app update checker against GitHub Releases (APK signature verification before install prompt) or Play Core In-App Update API if/when published to Play; versioned "What's New" sheet shown once per upgrade, sourced from a changelog file.

- **Phase 14 — Test & release hardening**
  Coverage gates (Jacoco) on domain/data layers, security test suite (encryption round-trip, protected-path denial, path traversal), performance benchmarks (hash throughput, duplicate-scan on large trees, cold-start time via Macrobenchmark), Compose UI + Espresso E2E suite on an emulator matrix in CI, first tagged release via the `release-checklist` skill (multi-persona review: system architect / UI expert / UX expert / QA architect, version bump, What's New, docs/skills refresh).

Phases 3–13 can reorder slightly as real constraints surface, but the dependency chain (0→1→2 first; hashing needs the file index from 1; audit viewer needs the audit events from 2; etc.) stays fixed.

## CI/CD (GitHub Actions), from Phase 0 onward

- `android-ci.yml`: on every push/PR — `assembleDebug`, unit tests (JVM, Robolectric where needed), `ktlint`, `detekt`, Android Lint.
- `instrumented-tests.yml` (from Phase 14, introduced earlier if a phase needs device-level verification sooner): Compose/Espresso tests on `reactivecircus/android-emulator-runner`.
- `release.yml`: on version tag — signed release build (AAB/APK), changelog extraction, GitHub Release publish.
- `security.yml`: CodeQL + dependency review, scheduled + on PR.

## Skills & docs (item 18)

- `CLAUDE.md` at repo root: architecture map, module/package conventions, how to run what's runnable locally vs. what needs CI (no Android SDK in this sandbox), and a **model-tier guide**: routine UI/text/test-scaffolding work → default/fast tier; architecture decisions, security-sensitive code (encryption, permission boundaries, hashing correctness), and release reviews → escalate reasoning effort/model tier.
- Repo-local skill `release-checklist` (mirrors the one in `system_diagram`): version bump, What's New entry, 3-pass review (system architect / UI / UX / QA architect personas), docs+skills refresh, CI-green gate, merge.
- Repo-local skill `add-feature`: conventions for adding a new feature package (folder layout, ViewModel/UseCase/Repository wiring, where tests go) — added once Phase 2 or 3 establishes the pattern concretely, not speculatively now.

## Known sandbox constraint

This dev container has JDK 21 and Gradle 8.14 but **no Android SDK**, so `./gradlew assembleDebug`/instrumented tests cannot run locally here — GitHub Actions CI is the real build/test gate for every phase. Kotlin source will be written carefully and cross-checked, but "CI green" is the actual verification signal, not a local build.
