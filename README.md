# Extended File Manager

A native Android file manager (Kotlin + Jetpack Compose) with advanced filtering/grouping,
duplicate-file detection, previews, favorites, a full audit trail, and Hebrew/English support.

- Full requirements-to-phase roadmap: [`docs/PLAN.md`](docs/PLAN.md)
- Architecture, conventions, and this repo's sandbox constraints: [`CLAUDE.md`](CLAUDE.md)
- Release process: [`.claude/skills/release-checklist/SKILL.md`](.claude/skills/release-checklist/SKILL.md)

## Status

Phase 0 (project foundation), Phase 1 (core read-only browsing), Phase 2 (Settings screen), and
Phase 3 (core file operations: create/rename/move/copy/delete with confirmation, undo-able trash,
audit logging) are done — CI is green end-to-end, including a successful `assembleDebug`. Phase 4
(compress/extract) is next. See `docs/PLAN.md` for the full phase list.

## Building

This project is built and tested via GitHub Actions (`.github/workflows/android-ci.yml`). To build
locally you'll need the Android SDK (compileSdk 35) and JDK 17:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
