# Extended File Manager

A native Android file manager (Kotlin + Jetpack Compose) with advanced filtering/grouping,
duplicate-file detection, previews, favorites, a full audit trail, and Hebrew/English support.

- Full requirements-to-phase roadmap: [`docs/PLAN.md`](docs/PLAN.md)
- Architecture, conventions, and this repo's sandbox constraints: [`CLAUDE.md`](CLAUDE.md)
- Release process: [`.claude/skills/release-checklist/SKILL.md`](.claude/skills/release-checklist/SKILL.md)

## Status

Phase 0 (project foundation), Phase 1 (core read-only browsing), Phase 2 (Settings screen),
Phase 3 (core file operations: create/rename/move/copy/delete with confirmation, undo-able trash,
audit logging), Phase 4 (compress/extract, with a conflict policy and Extract & Replace), Phase 5
(a shared filter/sort/group-by spec plus global full-text search), Phase 6 (a background,
WorkManager-driven duplicate finder with grouped results and bulk delete), and Phase 7 (image/
video/audio/PDF preview, with inline thumbnails and a swipeable full-screen viewer) are done — CI
is green end-to-end, including a successful `assembleDebug`. Phase 8 (view modes & UI polish) is
next. See `docs/PLAN.md` for the full phase list.

## Building

This project is built and tested via GitHub Actions (`.github/workflows/android-ci.yml`). To build
locally you'll need the Android SDK (compileSdk 35) and JDK 17:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
