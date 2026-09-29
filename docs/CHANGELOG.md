# Changelog

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
