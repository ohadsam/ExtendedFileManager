# Changelog

## v0.14.6 (2026-10-02)

- Phase 11 (fourth slice) — responded to the 68 Dependabot alerts GitHub
  flagged for this repo (no tool in this sandbox can list them directly, so
  this was a version audit instead): `androidx.security:security-crypto`
  moved off a years-old alpha onto the now-stable `1.1.0` -- the library
  guarding the Vault itself -- alongside `room`, `work`,
  `datastore-preferences`, `appcompat`, and `mockk`. Three more turned out
  to each need a newer Kotlin than this project's 2.0.21 compiler can read
  (their binaries carry 2.2.x metadata) and stayed one notch below their
  very latest: `media3` at `1.10.1` rather than `1.11.1`, which upgraded
  its internal Kotlin to 2.2.0; `hilt` stayed on its original `2.52`
  outright, since 2.57+ bundles the same kind of Kotlin-2.2 upgrade in its
  `kotlinx-metadata-jvm` dependency and 2.59+ needs AGP 9 besides;
  `core-ktx`, `lifecycle`, `navigation-compose`, and the Compose BOM hit a
  different wall at their latest -- wanting `compileSdk` 37, which needs
  AGP 9.1+ -- so those stayed down too. All of it (Kotlin, Hilt 2.57+,
  Media3 1.11+, the Compose-ecosystem four, and AGP 9 itself) is grouped
  into one follow-up slice, since bumping any of them for real means
  bumping Kotlin first.

## v0.14.5 (2026-10-02)

- "Add to Vault" has reached every screen that shows files: it's now
  in Preview's selection "⋮" menu too, same confirm dialog and
  locked-file guard as everywhere else. Browse, Duplicates, Favorites,
  Search, and Preview all offer it now -- the full rollout Phase 9's
  tag/favorite/lock actions had.

## v0.14.4 (2026-10-02)

- "Add to Vault" is now in Search too, from the selection bar's "⋮"
  menu once you've selected one or more results -- same confirm
  dialog, locked-file guard, and blocked-by-lock snackbar as Browse
  and Duplicates. Preview is the only screen left to reach.

## v0.14.3 (2026-10-02)

- "Add to Vault" is now in Favorites too, from each entry's "⋮" menu
  (Favorites has no multi-select, so this adds one file at a time,
  same as every other per-entry action there). A locked file still
  can't be added -- the snackbar that tells you so now offers
  "Details" as its action, opening the same sheet where you'd unlock
  it. Search and Preview are the only screens left to reach.

## v0.14.2 (2026-10-02)

- "Add to Vault" is no longer Browse-only: it's now in the Duplicates
  screen's "⋮" selection menu too, with the same locked-file guard and
  confirmation as everywhere else. (Internally, `VaultRepository.
  addToVault` now resolves the file's parent folder itself instead of
  requiring the caller to track a "current folder" -- groundwork for
  reaching Search, Favorites, and Preview next.)

## v0.14.1 (2026-09-30)

- Vault: you can now get a file back out. Select entries in the
  Vault and choose Export to decrypt them into any folder you still
  have access to, picked from the same kind of folder browser Move/
  Copy already use. Also new: biometric unlock -- once a device with
  enrolled biometrics has a vault password set, turn it on from the
  Vault's "⋮" menu (confirms with a biometric prompt before turning
  on) and a "Use biometric unlock" option appears right on the lock
  screen from then on, alongside the password field.

## v0.14.0 (2026-09-30)

- New: Encrypted Vault. A password-gated Vault screen (new drawer
  entry) holds files moved out of normal storage and encrypted at
  rest -- set a password the first time you open it, then unlock with
  that password each session. "Add to Vault" (Browse's "⋮" selection
  menu) moves the selected file(s) in: they're encrypted and removed
  from where they were, reappearing only inside the Vault. Multi-select
  inside the Vault to permanently remove entries (with confirmation --
  this isn't the same as the regular trash-backed delete elsewhere in
  the app, since the vault exists to keep this content out of ordinary,
  recoverable storage). A locked file can't be added to the vault, same
  as it can't be deleted. Biometric unlock and exporting a file back
  out of the vault are still to come.

## v0.13.2 (2026-09-30)

- Phase 11 (third slice) — CI now submits this project's resolved
  dependencies to GitHub's Dependency Graph on every push, which is
  what turns on Dependabot vulnerability alerts -- no new tool or
  secret needed. Requires Dependabot alerts to actually be turned on
  for this repo under Settings → Code security, a one-time manual
  step outside what a workflow file alone can do.

## v0.13.1 (2026-09-30)

- Phase 11 (second slice) — CI now builds a real, minified release
  APK (`assembleRelease`) on every push, not just the unminified
  debug build, so an R8/ProGuard shrink or obfuscation failure gets
  caught immediately instead of only at an actual release. The
  mapping file is uploaded as a CI artifact so a release crash stays
  symbolicate-able. `proguard-rules.pro` gained its first real rule:
  keep line numbers (not full source paths) in obfuscated stack
  traces.

## v0.13.0 (2026-09-30)

- Phase 11 (first slice) — a protected-path guard now refuses to
  create, rename, move, copy, delete, or extract into `Android/data`,
  `Android/obb`, or another app's private storage, and rejects any
  name that would traverse out of its folder (blank, `.`, `..`, or
  containing a path separator) -- across every mutating operation in
  the app, archive extraction included. Settings' Permissions section
  explains it via its info button.

## v0.12.1 (2026-09-29)

- Storage Advisor now flags Extended File Manager's own cache as a
  junk recommendation too, alongside orphaned folders and empty ones
  -- review it and clear it with the same Dismiss/Stage/Delete actions
  as everything else.

## v0.12.0 (2026-09-29)

- Storage Advisor gained a "Stage for later" option: select any
  recommendations you're not ready to decide on and mark them instead
  of dismissing or deleting them right away. They show up in a new
  Staged tab on the same screen, where you can unstage them or delete
  them with confirmation whenever you're ready -- nothing is ever
  deleted automatically.
- Orphaned-folder detection (leftover `Android/media` folders from
  apps you've since uninstalled) now reliably sees every installed
  app, not just the ones EFM happens to already be visible to.

## v0.11.1 (2026-09-29)

- Storage Advisor's "large and unused" thresholds are now yours to
  tune: a new Storage Advisor section in Settings lets you pick the
  minimum file size (50/100/250/500MB) and how long before a file
  counts as unused (3/6/12 months). Every scan reads your current
  choice, defaulting to 100MB / 6 months if you haven't changed it.

## v0.11.0 (2026-09-29)

- Phase 10 (first slice) — Storage Advisor: a new screen (reachable
  from the drawer) scans every granted folder for three kinds of
  cleanup candidates -- large files you haven't opened or touched in
  months, junk (orphaned folders left behind by uninstalled apps,
  empty folders), and temporary files (.tmp/.log/.bak and the like).
  Recommendations are grouped by category with the specific reason
  shown for each; select any number and dismiss them or delete them
  with confirmation, same review-list pattern as Duplicates. Nothing
  is ever deleted automatically. Still to come: a staged-for-deletion
  "review later" workflow and its own reminder, and Settings-tunable
  thresholds.

## v0.10.2 (2026-09-29)

- Favorites: each favorited file now has its own "⋮" menu with a
  "Details" item, opening the same favorite/tags/lock/note sheet
  available elsewhere in the app -- Favorites was the one screen it
  hadn't reached yet, since it has no multi-select bar to hang it off
  of (long-press there is already "remove from favorites").

## v0.10.1 (2026-09-29)

- Phase 9 follow-up, closing gaps from the initial v0.10.0 ship:
  favorites collections now have a real drag handle for reordering,
  alongside move-up/move-down; group-by-tag joins filter-by-tag (a
  file with two tags now shows up under both); bulk-favoriting a
  selection offers a collection picker instead of always landing in
  the uncategorized one; Preview's selection mode is now reachable by
  long-press on the content itself, not just the top-bar toggle; the
  full file-details sheet (favorite/tags/lock/note together) now opens
  from Search and Duplicates too, not just Browse; and Duplicates rows
  now reflect live tag/lock/favorite data instead of the scan snapshot.

## v0.10.0 (2026-09-29)

- Phase 9 — favorites, tags, lock & notes: mark any file or folder as a
  favorite and organize favorites into nested collections (a new
  Favorites screen, reachable from the drawer). Create your own colored
  tags — or use the four built-in ones (Important, Work, To sort,
  Archive) — and apply them to any number of files; a new Manage Tags
  screen handles renaming, recoloring, merging, deleting, and pinning
  a tag for quick access. Lock a file against deletion (blocks Delete
  and Extract & Replace alike, with an "unlock" action right in the
  message explaining why). Add a free-text note to any file. All of it
  lives in one new file-details view (opened from Browse for now), and
  the same tag/favorite/lock quick-actions and multi-select are now
  available from Browse, Search, Duplicates, and Preview — selecting
  files in Preview never disturbs a selection you already made
  elsewhere, and swiping to the next file while selecting adds it to
  your selection. The filter menu gained Favorites-only, Locked-only,
  and tag filters.

## v0.9.0 (2026-09-29)

- Phase 8 — view modes & UI polish: choose how file lists look, from
  Settings' Display section or a quick-toggle button right on Browse's
  toolbar. List (as before), Compact (smaller rows to scan more files at
  once), Detailed (bigger thumbnails plus the source app), and a new
  Grid view with thumbnail-forward tiles. The choice applies everywhere
  you browse or search.

## v0.8.0 (2026-09-29)

- Phase 7 — preview: tapping a previewable file in Browse, Search, or
  Duplicates now opens it instead of doing nothing. Images get a
  pinch-to-zoom/pan viewer, video and audio play inline, and PDFs render
  page-by-page. Swipe left/right to move to the next or previous
  previewable file in the same list you tapped from, gallery-style. File
  rows in Browse and Search now show a real thumbnail for images instead
  of a generic icon.
- Roadmap: expanded the "Widgets & shortcuts" idea and Phase 19's landing
  dashboard into a fuller description of real Android home-screen widgets
  — several distinct, user-selectable types added via the OS's own widget
  picker, rather than one fixed widget.

## v0.7.0 (2026-09-29)

- Phase 6 — duplicate & identical-file finder: a new Duplicates screen scans every
  granted folder for files with identical content (regardless of name or extension),
  in three stages so it stays fast even with many files — same size, then a quick
  partial check, then a full verification hash only on genuine candidates. The scan
  runs in the background (so leaving the screen doesn't cancel it), shows live
  progress, and can be cancelled. Results are grouped by content, with a "Keep one"
  shortcut that selects every extra copy for you, and bulk delete goes through the
  same confirm-and-trash flow as every other delete in the app.

## v0.6.0 (2026-09-28)

- Phase 5 — filter, sort, group-by, and global search: a single reusable
  filter/sort/group spec now drives both browsing and search, so every
  control works the same way wherever you got to a file list. Browse's sort
  menu is fully wired (name/size/date, ascending/descending), with a new
  group-by (type/source app/date modified) and a filter menu (files vs.
  folders, category, size range, date range) alongside it. A new search
  icon opens a dedicated search screen backed by a local full-text index,
  so repeated searches stay fast; tapping a result jumps straight to it in
  Browse with its breadcrumb trail rebuilt automatically.
- Roadmap: added Phase 18 (a Statistics screen with storage/file/operations
  widgets, each one drilling down into the underlying files), pushing
  release hardening to Phase 19.

## v0.5.0 (2026-09-28)

- Phase 4 — compress / extract: zip a selection of files and/or whole folders
  (walked recursively, paths preserved) into a new archive in the current
  folder, streamed directly against storage without a temp-file copy. Two
  distinct actions on any selected `.zip`: **Extract** (archive left in place)
  and **Extract & Replace** (deletes the original archive — to trash, not a
  hard delete — only once extraction has fully succeeded). A conflict policy
  (skip / overwrite / keep-both) is chosen once per operation, and a progress
  dialog with Cancel tracks large archives. Move/Copy/Compress/Extract now
  live in a grouped "more actions" dropdown on the selection bar, keeping
  Rename/Delete as direct icons.
- Roadmap: added Phase 17 (a daily "insights" digest — cleanup
  recommendations, duplicates, oversized files, unclear file extensions —
  with a summary notification, an always-reachable screen, actions performable
  directly on the results, smart incremental scanning that avoids a full
  rescan every run, and Settings toggles to disable the job and/or the
  notification), pushing release hardening to Phase 18.
- Conventions: `CLAUDE.md` and the release-checklist skill now explicitly
  call for reusing existing repository/component logic instead of
  duplicating it, keeping files small and single-purpose, and commenting the
  non-obvious *why* rather than restating self-explanatory code.

## v0.4.0 (2026-09-28)

- Phase 3 — core file operations: create folder/file, rename, move, and copy, all
  reachable from long-press selection's action bar and a "＋" button for new items.
  Every mutating action confirms first for anything destructive. Delete is
  undoable — deleted items move to a hidden per-folder trash instead of being
  removed outright, and a snackbar offers Undo right after. Moving or copying
  opens a destination picker that browses the same granted storage tree. Every
  operation (successful or not) is now logged to an internal audit trail —
  groundwork for a future audit viewer, not yet user-visible.
- Roadmap: added a new Phase 16 (Google Drive upload including whole folders,
  upload-status indicators, bulk upload of a selection, and Share/Send to email,
  WhatsApp, etc. for one or more files), moving final release hardening to
  Phase 17.

## v0.3.0 (2026-09-28)

- Phase 2 — Settings screen: appearance (light/dark/system + Material You dynamic
  color, on Android 12+), in-app language override (Hebrew/English, independent of
  the device's system language), a permissions section showing how many folders
  are currently granted plus a shortcut to system settings, and reserved sections
  for Display and Logs that will get their real controls in later phases. Also
  ships the app's first in-app user guide (`assets/help_en.html` /
  `help_he.html`, opened from Settings) and a reusable info-button component for
  later screens to use. Introduces the shared DataStore-backed preferences
  repository other phases will reuse.

## v0.2.0 (2026-09-28)

- Phase 1 — core browsing: pick a folder and browse it (Storage Access Framework), with
  breadcrumb navigation and a fast local cache so re-visiting a folder is instant. Files show
  which app they most likely came from where that can be known (exact when Android tells us,
  clearly marked "likely" otherwise). New app shell: side navigation menu, grouped sort menu,
  long-press to select.

## v0.1.0 (2026-09-28)

- Project foundation: Kotlin/Compose/Hilt/Room/WorkManager skeleton, GitHub Actions CI
  (build, unit tests, ktlint, detekt, Android Lint), Hebrew/English app name, `CLAUDE.md`,
  full roadmap (`docs/PLAN.md`), and the `release-checklist` skill.
