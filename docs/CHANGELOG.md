# Changelog

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
