# Changelog

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
