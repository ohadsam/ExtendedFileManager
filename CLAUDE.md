# Extended File Manager (EFM)

Native Android file manager (Kotlin, min SDK 26). Full roadmap and the
requirement-to-phase mapping live in `docs/PLAN.md` — read that first for
"why are we building X now vs. later."

## Stack

- Kotlin + Jetpack Compose (Material 3), single `:app` module, package-by-feature.
- Hilt (DI), Room (local DB), Coroutines/Flow (async), WorkManager (background jobs: weekly log purge, duplicate scans).
- Coil (images), Media3/ExoPlayer (video/audio), Android `PdfRenderer` (PDF preview).
- `androidx.security.crypto` (Keystore-backed encryption) for sensitive prefs and the encrypted vault.

## Package layout (grows as phases land — do not pre-create empty packages)

```
com.efm.filemanager/
  EfmApplication.kt, MainActivity.kt
  ui/                 top-level Compose root + theme/
  ui/theme/           Color.kt, Theme.kt, Type.kt
  data/                (Phase 1+) repositories, Room entities/DAOs, SAF/MediaStore access
  data/prefs/          (Phase 2+) DataStore-backed preferences repository (appearance, language, defaults) — shared by every settings-adjacent feature, not reinvented per screen
  domain/              (Phase 1+) use cases, models
  ui/feature/<name>/   (Phase 1+) one package per feature screen (browse, settings, favorites, duplicates, logs, audit, ...)
```

## Conventions

- Every mutating file operation goes through the shared confirmation component (from Phase 3 onward) and emits an audit event — no screen should call raw file-system mutations directly.
- Prefer Storage Access Framework / MediaStore over `MANAGE_EXTERNAL_STORAGE`; only request the broad permission where a feature genuinely can't work without it, with an in-app explanation.
- No feature reads/writes outside the user's selected scope without going through the protected-path check (Phase 11).
- Tests live next to the phase that introduces the behavior — don't defer test-writing to a later "testing phase" for new code (Phase 20 is for coverage *gates*, benchmarks, and E2E, not for backfilling missing unit tests).
- **Reuse before duplicating.** Before writing new SAF/DocumentFile plumbing, audit-logging, or dialog-orchestration code, check whether an existing repository or component already does it (e.g. `FileOperationsRepository`'s `requireTreeDocument`/`requireSingleDocument`/`uniqueNameIn`) and promote it to `internal` for cross-package reuse rather than copy-pasting.
- **Keep files small and single-purpose.** Split a screen's top bar, dialogs, and state-holder classes into their own files instead of one large screen file — see `ui/feature/browse/` (`BrowseScreen.kt`, `BrowseTopBar.kt`, `BrowseDialogs.kt`, `ArchiveDialogs.kt`, ...). This isn't just style: detekt's `TooManyFunctions`/`LongMethod`/`CyclomaticComplexMethod` thresholds actively enforce it, and a function's parameter count over ~5 is a signal to bundle related callbacks/state into a small holder class rather than growing the parameter list.
- **Comment the non-obvious *why*, not the *what*.** A platform quirk (`fromTreeUri` vs `fromSingleUri`), a workaround, or a deliberate scope boundary deserves a short comment explaining the reasoning; code whose purpose is already clear from good naming doesn't need one restating it.

### Navigation & toolbar conventions

Established in Phase 1 (the app shell) and followed by every screen from then on, not something
Phase 8's polish pass retrofits:

- **Toolbar actions are grouped into dropdowns, not a flat icon row.** Once a top bar or bottom bar
  would carry more than ~3-4 actions, group them behind a labeled dropdown (Material3
  `DropdownMenu`) rather than growing the row — a flat row of many icons is exactly the mobile
  horizontal-overflow trap `system_diagram`'s own toolbar learned the hard way. Each dropdown has
  **subheadings** separating its groups (e.g. a sort dropdown: "Sort by" section, then "Order"
  section) via `DropdownMenu`'s non-clickable header text, and groups/items are ordered by how
  often they're used, not alphabetically.
- **One hamburger-driven navigation drawer** (`ModalNavigationDrawer`, ☰ icon in the main top bar)
  is the app's primary cross-feature navigation — Browse, Favorites, Duplicates, Storage Advisor,
  Logs, Audit, Settings — introduced in Phase 1 as part of the app shell, with each later phase
  adding its own entry as it lands rather than the drawer being redesigned per phase.
- **Long-press opens contextual actions**, the mobile equivalent of a desktop right-click, on every
  file/folder row/tile wherever one is shown (browse, search, favorites, duplicate-review, storage
  advisor). Long-press enters multi-select mode with a contextual action bar (or a `DropdownMenu`
  anchored at the press point for a single item) exposing whichever actions are relevant to what's
  selected and already built at that point in the roadmap — rename/delete/move/copy from Phase 3,
  extract from Phase 4, tag/favorite/lock/notes from Phase 9, etc. Build the interaction shell in
  Phase 1 even though most actions are still stubs then; wire each real action in as its owning
  phase lands, rather than deferring the whole pattern.
- A short tap always does the primary action for that context (open a folder, preview a file);
  long-press/overflow is exclusively for secondary actions — never make the primary action harder
  to reach than a secondary one.

## Sandbox constraint (important for future sessions)

This repo is developed from a cloud sandbox with JDK 21 + Gradle 8.14 but
**no Android SDK, and network egress to `dl.google.com`/Maven is blocked**.
That means **no Gradle task can run locally in this sandbox** — not
`assembleDebug`, not `test`, not even `ktlintCheck`/`detekt` (they still need
the Android Gradle Plugin to configure the project). GitHub Actions
(`.github/workflows/android-ci.yml`) is the real, and only, build/test gate.
Treat "pushed, CI green" as the actual verification step — read Kotlin/Gradle
changes carefully by eye before pushing, since there's no local compile to
catch typos first.

## Model-tier guidance for maintaining this project

- Routine work (UI tweaks, strings, wiring an existing pattern to a new
  screen, writing tests for already-designed behavior, changelog/docs
  updates) — default/fast model tier is enough.
- Escalate reasoning effort/model tier for: architecture decisions (module
  boundaries, DB schema changes), anything touching security surfaces
  (encryption, permission boundaries, protected-path logic, the audit/log
  tamper-evidence chain), the duplicate-detection hashing algorithm
  (correctness + performance trade-offs), and release reviews (the
  `release-checklist` skill's multi-persona pass).
- When in doubt on a security- or data-loss-adjacent change (anything that
  deletes/overwrites user files), prefer the higher tier — the cost of a
  wrong call there is much higher than the cost of a slower response.

## Release process

Use the repo-local `release-checklist` skill (`.claude/skills/release-checklist/SKILL.md`)
before considering any batch of work "done": version bump, What's New entry,
multi-persona review (system architect / UI expert / UX expert / QA
architect), docs/skills refresh, CI green, then merge.
