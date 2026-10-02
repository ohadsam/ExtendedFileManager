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
WorkManager-driven duplicate finder with grouped results and bulk delete), Phase 7 (image/
video/audio/PDF preview, with inline thumbnails and a swipeable full-screen viewer), and Phase 8
(list/compact/detailed/grid view modes, chosen from Settings or a Browse toolbar quick-toggle), and
Phase 9 (favorites with nested collections, a reusable colored tag catalog with a Manage Tags
screen, lock-against-deletion, free-text notes, and the same multi-select quick-actions reachable
from Browse/Search/Duplicates/Preview alike), and Phase 10 (storage optimization advisor: a
Storage Advisor screen scans for large-unused/junk/temporary-file recommendations -- reviewed,
staged for later, dismissed, or deleted, with Settings-tunable thresholds -- everything from the
original spec has landed except the periodic staged-review reminder, deferred to Phase 17's proper
notification infrastructure) are done — CI is green end-to-end, including a successful
`assembleDebug`. Phase 11 (security hardening) is in progress -- a protected-path guard now
refuses to create/rename/move/copy/delete/extract into Android/data, Android/obb, or another
app's private storage, and rejects any traversal-unsafe name, across every mutating repository;
CI also now builds a real, minified `assembleRelease` on every push (with its R8/ProGuard mapping
uploaded as an artifact), catching a shrink/obfuscation failure immediately instead of only at an
actual release, and CI submits the project's dependencies to GitHub's Dependency Graph on every
push, turning on Dependabot vulnerability alerts (once enabled for this repo under Settings → Code
security -- a one-time manual step, same kind of gap as Phase 10's Play Console declaration). A new
password-and-biometric-gated, Keystore-encrypted Vault (its own drawer entry) is also live --
"Add to Vault" is reachable from every screen that shows files (Browse, Duplicates, Favorites,
Search, Preview), moving a file in, encrypted at rest, until you export it back out (to any folder
you choose) or remove it for good. In response to the 68 alerts that Dependabot flagged, every
AndroidX/3rd-party dependency in `gradle/libs.versions.toml` was audited and bumped to its current
stable release (`security-crypto` chief among them, off a years-old alpha onto `1.1.0`); Kotlin and
AGP are left for a smaller follow-up since a large jump on either can't be pre-verified without a
local build. See `docs/PLAN.md` for the full phase list.

## Building

This project is built and tested via GitHub Actions (`.github/workflows/android-ci.yml`). To build
locally you'll need the Android SDK (compileSdk 35) and JDK 17:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
