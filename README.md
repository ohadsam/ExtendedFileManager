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
notification infrastructure), and Phase 11 (security hardening) are done — CI is green
end-to-end, including a successful `assembleDebug`. A protected-path guard now
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
you choose) or remove it for good. In response to the 68 alerts that Dependabot flagged, most
AndroidX/3rd-party dependencies in `gradle/libs.versions.toml` were audited and bumped to their
current stable release (`security-crypto` chief among them, off a years-old alpha onto `1.1.0`);
a few (`media3`, `core-ktx`, `lifecycle`, `navigation-compose`, the Compose BOM, and `hilt`) hit a
wall at their very latest -- needing either a newer Kotlin than this project compiles with, or a
`compileSdk` that needs AGP 9 -- so each stopped one notch short, or stayed put outright, grouped
with Kotlin and the AGP 9 jump itself for a separate follow-up slice, since a jump that size can't
be pre-verified without a local build; that slice is tracked on its own and doesn't block Phase 11
itself, which closes out with ProGuard reviewed and found to need nothing beyond its existing
crash-readability baseline rule -- CI's real `assembleRelease` shrinks and obfuscates clean with
zero missing-class warnings. Phase 12 (logs) is also done: a new Logs screen (its own
drawer entry, also reachable from Settings) shows the app's diagnostic log, newest first --
`Timber` now backs onto a Room-backed store instead of just Logcat, starting with every failed
file operation -- entries older than 7 days are purged automatically in the background, a
"Clear logs" action empties it on demand, a filter menu narrows it to just warnings or
errors when that's all you need, and an Export action writes the currently-filtered log out as
plain text via the system "Save As" picker. Phase 13 (audit trail) is also done: every
file operation this app makes was already being recorded (since Phase 3); that record is now
hash-chained for tamper-evidence, and a new Audit log screen (its own drawer entry, also reachable
from Settings) lists every entry newest-first, warning if the chain is ever broken, with a filter
to show only failed operations, a search box to find entries by file name, and an Export action
to write the currently filtered/searched entries out as plain text. Phase 14 (localization &
responsiveness) is now in progress: a code-level audit found the app's RTL handling already
consistent across every screen (mirrored icons, start/end-only padding, locale-aware formatting),
with one small fix landed (a breadcrumb bar's separator); adaptive foldable/tablet layouts have
since landed too (a tablet/unfolded-foldable-width screen now gets an always-visible nav drawer
instead of a hamburger, at Material's own 840dp breakpoint) -- an actual visual RTL/font-scale/
tablet-layout pass is still open, needing a real device this sandbox doesn't have. Phase 15 (versioning & updates) has also
started: a "What's New" sheet now shows once after an upgrade (never on a fresh install),
catching this release up on the Logs and Audit screens; the GitHub-Releases update checker is
deferred to a later slice. Phase 16 (cloud upload & sharing) is done too: a new "Share"
action in Browse's selection menu opens the system share sheet for selected files, and a new
"Upload to…" action reuses the same destination picker as Move/Copy to upload to any folder --
including a Google Drive folder -- in the background, with a status banner (queued/uploading/
failed, with Retry/Dismiss) above the file list. Rolling Share out to every other screen is still
a follow-up. Phase 17 (daily insights & smart notifications) is now done: what used to be the
"Storage Advisor" screen is "Insights" everywhere it shows to the user, and now also flags files
with an unrecognized or suspicious double extension (like "invoice.pdf.exe") as worth a look --
a hygiene nudge, not a security verdict -- alongside a new "Duplicates" category folding in
Phase 6's duplicate-scan results, all with the same multi-select delete/dismiss actions. A daily
background job re-runs the cheap part of that scan once a day, adds in whatever duplicates are
already cached, and posts one summary notification ("Worth a look -- N item(s) found") that
opens straight into the Insights screen when tapped. A Settings "Insights" section controls both
pieces -- "Run daily insights" and "Notify me" (which triggers the real notification-permission
request on Android 13+). Having that daily job also trigger its own fresh, incrementally-throttled
duplicate rescan (rather than only reading whatever Phase 6's own scan last cached) is the one
piece left for later. Phase 18 (statistics dashboard) has now started too: a new "Statistics"
screen (own nav-drawer entry) shows six basic widgets -- storage used, a by-file-type size
breakdown, largest files, duplicate-group count + reclaimable space, how many files Insights has
flagged, and a total operations-recorded count -- all reusing data that already exists elsewhere
in the app wherever possible. Every widget now taps through somewhere: Duplicates/Insights/
Operations jump straight to their owning screen, and the by-type and largest-files widgets open a
new global, size-sorted file list (optionally filtered to one file type) that didn't exist in this
app before. The by-type widget also has its first chart -- a horizontal stacked bar (not a donut,
since a donut's every-slice-at-once display can't guarantee all seven category colors stay
distinguishable from each other, checked with the `dataviz` skill's own color validator) with a
legend swatch per category, and the storage-used widget now grows a trend sparkline once it has
at least two days of history behind it -- a new table quietly records one snapshot every time
that widget's own scan runs, building the trend forward from today rather than pretending to
know the past. The dashboard's own full-tree walk is now also skipped entirely when nothing's
changed since the last time it ran, reusing Phase 17's existing "has the file index changed"
signal rather than re-walking just to get the same numbers back, and that same daily snapshot now
records itself automatically (piggybacking on Phase 17's own once-a-day job) even on a day the
user never opens Statistics. See `docs/PLAN.md` for the full phase list.

## Building

This project is built and tested via GitHub Actions (`.github/workflows/android-ci.yml`). To build
locally you'll need the Android SDK (compileSdk 35) and JDK 17:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
