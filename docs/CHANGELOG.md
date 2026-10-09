# Changelog

## v0.23.1 (2026-10-09)

- Phase 10 backlog item closed: the periodic staged-review nudge
  notification, deferred back when Phase 10 landed since this app had
  no notification infrastructure yet. A new "Staged review window"
  Settings control (7/14/30/60 days, default 30) and a pure
  `isOverdueForReview()` check feed straight into the existing daily
  insights notification's combined count -- no new channel or screen,
  same single-summary-notification shape every other category already
  uses. The Staged tab is still where you act on them; this closes
  the "get notified" half of the original spec.

## v0.23.0 (2026-10-08)

- Phase 20 starts: a Jacoco *coverage report* (not yet a gate) for the
  debug variant's unit tests. `enableUnitTestCoverage = true` plus
  `android.testCoverage.jacocoVersion` (AGP's own built-in mechanism)
  produce `app/build/reports/coverage/test/debug/index.html` via a
  new `createDebugUnitTestCoverageReport` CI step, uploaded alongside
  the other test-reports artifacts. No enforced threshold yet --
  there's no way to run Gradle in this sandbox to see real coverage
  numbers first, so a blind `minimumCoverage` guess is deferred to a
  follow-up slice. Also recorded: Phase 20's "security test suite"
  item turns out to be partly already done (protected-path denial and
  path traversal are thoroughly covered by Phase 11's own
  `PathGuardTest.kt`) and partly not unit-testable from this sandbox
  at all (Vault's `encrypt`/`decrypt` are thin wrappers directly over
  `EncryptedFile`/`AndroidKeyStore`, with no pure logic inside them to
  extract and test, and `AndroidKeyStore` isn't available outside a
  real device) -- flagged as a real instrumented-test gap rather than
  faked with a mock that wouldn't test real encryption.

## v0.22.7 (2026-10-08)

- Phase 19's final slice: Dashboard is now the app's start destination
  (Browse is still one tap away in the drawer, just no longer first --
  the drawer itself now leads with Dashboard too). This exposed a real
  coupling that needed fixing first: Search's "open location" action
  shared `BrowseViewModel` by looking up Browse's own backstack entry,
  which only ever worked because Browse used to be the start
  destination and so was always on the stack. `BrowseViewModel` is now
  hoisted once in `EfmApp` (Activity-scoped, the same pattern
  `AppThemeViewModel` already uses one level up in `EfmRoot`) and
  passed down explicitly, so it's there even when Search is reached
  straight from Dashboard without ever visiting Browse first. That
  also surfaced a related bug search already had since Dashboard
  gained its own search icon in slice 1: tapping a result used to
  just pop the back stack, landing back on whichever screen opened
  search (often Dashboard) instead of showing the location just
  navigated to -- it now explicitly navigates to Browse, the same
  navigate-and-restore pattern the drawer itself already uses. Phase
  19 is now complete.

## v0.22.6 (2026-10-08)

- Phase 19 slice 7: renaming and deleting a saved layout. Each row
  in the "Saved layouts" section of the list menu now also has a
  rename and a delete icon, next to its name -- rename reuses the
  shared name-input dialog (pre-filled with the current name), delete
  goes through the same "are you sure" confirmation every other
  destructive action in this app does. Deleting a saved layout only
  removes that snapshot (`dashboard_layouts`/`dashboard_layout_widgets`
  rows) -- it never touches the widgets currently on screen. Only the
  start-destination promotion remains open for Phase 19.

## v0.22.5 (2026-10-05)

- Phase 19 slice 6: named saved layouts. The same list icon that
  opens built-in templates now also offers "Save current as…"
  (a name dialog) and, once you have any, a "Saved layouts" section
  listing them by name -- tapping one rewrites the dashboard to match,
  exactly like applying a template. Two new Room tables
  (`dashboard_layouts`/`dashboard_layout_widgets`) hold the snapshots;
  `DashboardLayoutRepository.saveCurrentAsLayout()`/`applySavedLayout()`
  are the save/load inverse of each other. Renaming and deleting a
  saved layout are deliberately left for a follow-up -- this slice is
  save + switch only. Only the start-destination promotion remains
  open for Phase 19.

## v0.22.4 (2026-10-05)

- Phase 19 slice 5: three built-in templates -- "Cleanup focus"
  (duplicates/insights front and center), "Quick access" (favorites
  first), and "At a glance" (storage only) -- reachable from a new
  list icon in edit mode's top bar. Applying one rewrites every
  widget's enabled/order/size in one go, exactly as if each change
  had been made by hand, so the result stays freely customizable
  afterward -- not a locked layout. A unit test checks every template
  lists every catalog widget type exactly once, so a future catalog
  addition can't silently leave a template's rewrite incomplete. Only
  named saved layouts and the start-destination promotion remain
  open for Phase 19.

## v0.22.3 (2026-10-05)

- Phase 19 slice 4: per-widget sizing. A new size toggle in the edit
  mode's row (next to the drag handle and the visibility switch) lets
  each widget switch between its full content and a trimmed-down
  COMPACT form -- storage used drops its file-count line, duplicates
  drops its reclaimable-space line (newly shown at all, in detailed
  size), and favorites collapses its name list down to just a count.
  Insights has nothing left to trim, so its size toggle is a no-op by
  design. Persisted in `dashboard_widgets` (a new `size` column),
  seeded DETAILED so nothing changes until the user actually resizes
  something. This was the last per-widget customization planned for
  Phase 19 -- only templates and saved layouts remain, see
  docs/PLAN.md.

## v0.22.2 (2026-10-05)

- Phase 19 slice 3: Dashboard widgets can now be reordered, via a
  drag handle in edit mode -- the exact same drag-crosses-a-row-then-
  swap technique Favorites already uses for its own collections, not
  a new drag-and-drop implementation. The edit-mode list is now a
  compact, fixed-height row per widget (name + switch + drag handle)
  rather than the full card, since a consistent row height is what
  makes the drag threshold feel right -- also a more scannable way to
  manage several widgets at once than the full cards were. Position
  persists in the same `dashboard_widgets` table (a new `sortOrder`
  column). Still no resizing, templates, or saved layouts -- see
  docs/PLAN.md for what's still open.

## v0.22.1 (2026-10-05)

- Phase 19 slice 2: the Dashboard screen's first real customization
  primitive -- a pencil-icon edit mode where each widget's chevron
  becomes a visibility switch, so you can hide the cards you don't
  want. Persisted in a new `dashboard_widgets` Room table (one row per
  `DashboardWidgetType`, seeded enabled on first run and never
  overwritten once a row exists), so a hidden widget stays hidden
  across app restarts. Still no reordering, resizing, templates, or
  saved layouts -- see docs/PLAN.md for what's still open.

## v0.22.0 (2026-10-05)

- Phase 19 starts: a new "Dashboard" screen (own nav-drawer entry,
  appended last -- Browse stays the app's start destination for now)
  shows four fixed cards -- storage used, duplicates, insights, and a
  favorites quick-access list -- every one reusing an already-live
  data source with zero new scanning, same principle Phase 18's own
  dashboard follows. No widget catalog, customization, templates, or
  saved layouts yet -- this is slice 1 of a multi-slice phase; see
  docs/PLAN.md for what's still open.

## v0.21.9 (2026-10-05)

- Phase 18's eighth and final widget lands: "Most populated folders,"
  the top 5 folders by file count across every granted tree. Folder
  names are resolved via `DocumentFile` (one new SAF lookup per
  granted root; every other folder's name was already in hand from
  its own entity before this phase's walk ever recurses into it), and
  cached the same way the other top-N widgets already are
  (`stats_cache_folders`). This widget deliberately has no
  tap-to-drill-down yet: Browse's nav route carries no
  folder-targeting argument today, and the shared-ViewModel trick
  Search's own drill-down relies on doesn't carry over to a drawer
  destination (Statistics pops Browse off the back stack on the way
  in). Phase 18 is now feature-complete against its own spec.

## v0.21.8 (2026-10-05)

- Phase 18 gets a seventh widget: "Recently modified," showing the 5
  most-recently-touched files across every granted tree, same card
  shape as Largest files. Tapping it opens the same global file-list
  drill-down, now generalized with a `GlobalFilesSort` (SIZE/RECENT) so
  one screen serves both orderings instead of a second screen. The
  5-file list is cached the same way the largest-files one already is
  (`stats_cache_recent_files`), so a cache hit still populates it
  without re-walking. Only the most-populated-folders widget remains
  open for Phase 18.

## v0.21.7 (2026-10-05)

- Phase 18's trend sparkline now keeps growing even on a day the user
  never opens Statistics: `DailyInsightsWorker` (Phase 17's existing
  once-a-day job) now also calls `StatisticsRepository
  .computeStorageStats()` on every run, regardless of the "Run daily
  insights" toggle, since the storage-trend snapshot is a separate
  dashboard feature, not part of Insights. This piggybacks on v0.21.6's
  incremental cache, so a day with nothing changed is still cheap.
  Only the most/least-recently-modified and most-populated-folder
  widgets remain open for Phase 18.

## v0.21.6 (2026-10-05)

- Phase 18's last plumbing leftover lands: the Statistics dashboard's
  full-tree walk is now skipped entirely when nothing's changed since
  the last time it ran. A new `stats_cache`/`stats_cache_largest_files`
  Room table pair caches the walk's result (total size, file count, top
  5 largest files) alongside the `changeVersion` it was computed at --
  the same cheap "has the file index changed" signal Phase 17's daily
  job already uses -- and a cache hit rebuilds the by-type breakdown
  from the most recent day's own `storage_snapshots` rows instead of
  re-walking just to get the same numbers back. Phase 18 is now fully
  done.

## v0.21.5 (2026-10-04)

- Phase 14's adaptive-layout leftover lands: at tablet/unfolded-foldable
  width (Material's own 840dp "Expanded" breakpoint, read straight off
  `LocalConfiguration` with no new Gradle dependency), the nav drawer is
  now always visible (`PermanentNavigationDrawer`) instead of needing a
  hamburger tap; phone widths keep today's `ModalNavigationDrawer`
  unchanged. The switch is isolated entirely to `EfmApp.kt` -- every
  screen's own drawer-open callback keeps working as a harmless no-op in
  the new layout, so no screen needed to change. Phase 14's remaining
  item (an actual RTL/font-scale/tablet-layout visual pass) and Phase
  15's remaining item (the GitHub-Releases update checker) both still
  need a real device or a real published release, neither of which this
  sandbox has.

## v0.21.4 (2026-10-04)

- Phase 18 continues: the storage-used widget now shows a trend
  sparkline once there's at least two days of history. A new
  `storage_snapshots` Room table gets one row appended every time
  the widget's own underlying scan runs (manual refresh or opening
  the screen), forward-built from today rather than backfilled,
  since Android has no retroactive history of past storage state to
  query. Automatically recording a snapshot once a day even if the
  user never opens Statistics (piggybacking on Phase 17's daily job)
  is still open.

## v0.21.3 (2026-10-04)

- Phase 18 continues: the by-type and largest-files widgets now
  drill down too, closing the "no dead-end numbers" goal for every
  widget. A new global file list (tap a type-breakdown row or
  segment for that category sorted by size, or the largest-files
  card for every file sorted by size) reuses the same lightweight
  full-tree walk the Statistics screen's own widgets already use --
  no new scanning, no new screen architecture, just a filtered,
  sorted view over data this phase already computes. Tapping a file
  opens Preview, same as everywhere else in the app.

## v0.21.2 (2026-10-04)

- Phase 18 continues: the by-file-type widget now shows a horizontal
  stacked bar above its legend, one fixed color per category (the
  same color always means "Images," never reassigned by rank), with
  a color swatch next to each legend row tying bar segments to their
  labels. Deliberately a stacked bar, not a donut: a donut shows
  every slice at once and needs every pair of colors to stay
  distinguishable, which a 7-category palette can't guarantee,
  whereas a stacked bar only needs touching segments to be
  distinguishable -- validated with the dataviz skill's palette
  checker before shipping either way.

## v0.21.1 (2026-10-04)

- Phase 18 continues: three of Statistics' six widgets are now
  tappable -- Duplicates, Insights, and Operations each drill down
  straight into the screen that owns that data (Duplicates/Insights/
  Audit), matching the spec's "no dead-end numbers" goal for the
  widgets that already had a single owning screen to jump to. The
  by-file-type and largest-files widgets still don't drill down --
  doing so needs a global, filtered+sorted file view that doesn't
  exist anywhere in the app yet (Search requires a text query to
  show anything; Browse is scoped to one folder at a time), so that's
  left for a dedicated slice rather than bolted on here.

## v0.21.0 (2026-10-04)

- Phase 18 (statistics dashboard) has started: a new "Statistics"
  screen (own nav-drawer entry) shows a few basic widgets --
  storage used across your granted folders, a by-file-type size
  breakdown, the largest files, duplicate-group count + reclaimable
  space, how many files Insights has flagged, and a total
  operations-recorded count -- all reusing data that already exists
  elsewhere in the app, plus one new lightweight (no hashing) scan
  for the storage-used/by-type/largest-files numbers, since nothing
  else already tracks those. Charts, tap-to-drill-down into the
  owning screen, and the incremental `stats_cache`/snapshot-history
  pieces are still open.

## v0.20.3 (2026-10-04)

- Phase 17 continues: "Storage Advisor" is now "Insights" everywhere
  it showed to the user -- nav drawer entry, screen title, and the
  daily notification's wording -- and Phase 6's duplicate-scan
  results now show up as a new "Duplicates" category right in that
  same screen, alongside Large/Junk/Temporary/Unclear-extension, with
  the same multi-select delete/dismiss actions. The daily summary
  notification's count now includes already-cached duplicates too,
  so it's genuinely "one notification, not two separate systems."
  Still open: having the daily job trigger a fresh (changeVersion-
  throttled) duplicate scan itself, rather than only reading whatever
  Phase 6's own scan last cached.

## v0.20.2 (2026-10-03)

- Phase 17 continues: Settings gained a new "Insights" section with
  two switches -- "Run daily insights" (stops the daily background
  job entirely) and "Notify me" (keeps the job running so Storage
  Advisor stays fresh, just suppresses its summary notification).
  Turning "Notify me" on, on Android 13+, now triggers the real
  POST_NOTIFICATIONS runtime-permission request. Duplicate-folding,
  the incremental changeVersion-based scan skip, and the
  Advisor-to-Insights rename are still open.

## v0.20.1 (2026-10-03)

- Phase 17 continues: a new daily background job now re-runs the
  Storage Advisor scan once a day and, if anything's worth a look,
  posts one summary notification ("Worth a look -- N item(s) found
  by Storage Advisor") via its own notification channel -- tapping
  it opens straight into the Storage Advisor screen. The real
  runtime notification-permission request (with its in-app
  rationale) is still a Settings toggle away from landing, so this
  silently stays quiet on Android 13+ until that permission is
  granted some other way. Duplicate-folding into the same screen,
  the incremental changeVersion-based scan skip, and the Settings
  section are still open.

## v0.20.0 (2026-10-03)

- Phase 17 (daily insights & smart notifications) started: the
  Storage Advisor screen gained a new "Unclear file extension"
  category, flagging files with an unrecognized extension or a
  suspicious double extension (like "invoice.pdf.exe") as worth a
  look -- a hygiene nudge, never a security verdict. The daily
  background job, notification, and the rest of this phase's
  infrastructure are still to come.

## v0.19.1 (2026-10-03)

- Phase 16 is now complete: Browse's selection menu gained an
  "Upload to…" action that reuses the same destination picker as
  Move/Copy -- pick any folder, including a Google Drive folder
  already granted access to, and the selected files upload in the
  background (surviving the app leaving the foreground) with their
  own status banner above the file list (queued/uploading/failed,
  with Retry and Dismiss on a failure -- nothing fails silently).
  Heavily logged under the hood for diagnosing anything that comes up
  during manual testing. Rolling Share out to every other screen is
  still a follow-up.

## v0.19.0 (2026-10-03)

- Phase 16 (cloud upload & sharing) started with Share / Send: a new
  "Share" item in Browse's selection menu opens the system share
  sheet for one or more selected files (any installed app -- email,
  messaging, Drive, Dropbox, etc. -- can receive it). Rolling this out
  to Duplicates/Favorites/Search/Preview, a real Drive-upload
  destination, and upload-status markers are still open.

## v0.18.0 (2026-10-03)

- Phase 15 (versioning & updates) started with a "What's New" sheet:
  shown once after an upgrade (never on a fresh install), summarizing
  what changed since the version you last had open. This release's
  entry catches you up on the Logs and Audit screens, since neither
  had an in-app announcement until now. The GitHub-Releases update
  checker is deferred to a later slice.

## v0.17.0 (2026-10-03)

- Phase 14 (localization & responsiveness) started: a full code-level
  RTL/localization audit across every screen found the app already in
  good shape (consistent AutoMirrored icons, start/end-only padding,
  no absolute positioning, Locale.getDefault() everywhere, correct sp
  font units), with one real fix -- the Vault export folder picker's
  breadcrumb bar now uses the same mirrored chevron icon Browse's own
  breadcrumb bar already uses, instead of a plain "/". Adaptive
  (foldable/tablet) layouts and a real visual RTL/font-scale pass are
  still open.

## v0.16.3 (2026-10-02)

- Phase 13 — Audit trail is now complete: a new Export icon writes the
  currently filtered/searched audit entries out as plain text via the
  system "Save As" picker, the same export mechanism the Logs screen
  already uses. This closes out Phase 13.

## v0.16.2 (2026-10-02)

- Phase 13 — The Audit log screen can now be searched by file name:
  a new search icon in its top bar toggles the title into a free-text
  field, filtering the list (combined with the failed-only filter, if
  also active) as you type. Export is the only piece of this phase
  still to come.

## v0.16.1 (2026-10-02)

- Phase 13 — The Audit log screen can now be filtered to show only
  failed operations, from a new filter icon in its top bar (the same
  dropdown shape the Logs screen's priority filter already uses).
  Search and export are still to come.

## v0.16.0 (2026-10-02)

- Phase 13 — Audit trail: a new Audit log screen (its own drawer entry,
  also reachable from Settings) lists every file operation this app has
  ever made, newest first, each with its action, target, and timestamp,
  flagging any that failed. The audit log is now hash-chained under the
  hood (landed just before this slice) -- if anything in that chain is
  ever broken, the screen shows a warning banner. Filtering, search, and
  export are still to come.

## v0.15.3 (2026-10-02)

- Phase 12 — Logs: the last piece, export-to-file, has landed. A new
  Export icon in the Logs screen's top bar opens the system "Save As"
  picker and writes the currently-filtered entries out as plain text.
  This closes out Phase 12.

## v0.15.2 (2026-10-02)

- Phase 12 — Logs can now be filtered by priority (All / Info and above /
  Warnings and above / Errors only) from a new filter icon in the Logs
  screen's top bar. Export is the only piece of this phase still to come.

## v0.15.1 (2026-10-02)

- Phase 12 — Logs also gained a weekly auto-purge (entries older than 7
  days are removed automatically in the background, keeping the log from
  growing unbounded) and a "Clear logs" action in the Logs screen's top
  bar, with the same "are you sure" confirmation every other destructive
  action in the app uses. Filtering and export are still to come.

## v0.15.0 (2026-10-02)

- Phase 12 — Logs: a new Logs screen (its own drawer entry, and also
  reachable from Settings) shows the app's diagnostic log, newest first --
  every entry `Timber` writes anywhere in the app, starting with every
  failed file operation. Still to come: filtering, export, a "clear now"
  action, and the weekly auto-purge worker.

## v0.14.7 (2026-10-02)

- Phase 11 (fifth slice, and the phase's last) — reviewed ProGuard rules
  beyond the crash-readability baseline, the one item this phase still
  had open. CI's `assembleRelease` -- the real R8/ProGuard shrink-and-
  obfuscate pass that's run on every push since this phase's second slice
  -- completes with zero missing-class warnings using only the existing
  baseline rule, so per `proguard-rules.pro`'s own stated approach (rules
  grown deliberately as real failures are hit, not speculatively) nothing
  more was added. That closes Phase 11. The Kotlin/AGP modernization the
  previous slice kept running into isn't part of this phase's scope and
  stays tracked separately, to be picked up deliberately rather than
  forced through blind CI iteration.

## v0.14.6 (2026-10-02)

- Phase 11 (fourth slice) — responded to the 68 Dependabot alerts GitHub
  flagged for this repo (no tool in this sandbox can list them directly, so
  this was a version audit instead): `androidx.security:security-crypto`
  moved off a years-old alpha onto the now-stable `1.1.0` -- the library
  guarding the Vault itself -- alongside `room`, `work`,
  `datastore-preferences`, and `appcompat`. `mockk` turned out to have the
  same Kotlin-metadata problem as the rest of this list one test-classpath
  layer down (1.14.x is compiled against Kotlin 2.1/2.2), so it stayed on
  its original `1.13.13` too. `media3` landed at
  `1.10.1` rather than its very latest `1.11.1`, for two independent
  reasons: even `1.10.1` already needs `compileSdk` 36 (past what this
  project's `compileSdk = 35`/AGP 8.7.2 could reach, so both went up too --
  AGP to `8.9.1`, the minimum that supports it, and `compileSdk` to `36`,
  a small, bounded step confirmed by CI's own error message, not AGP 9),
  and `1.11.0` on top of that upgraded media3's own internal Kotlin to
  2.2.0, unreadable by this project's Kotlin 2.0.21 compiler. `hilt`
  stayed on its original `2.52` outright for the same Kotlin reason --
  2.57+ bundles a `kotlinx-metadata-jvm` upgrade with the same 2.2.x
  metadata problem, and 2.59+ needs AGP 9 besides. `core-ktx`, `lifecycle`,
  `navigation-compose`, and the Compose BOM hit a wall one rung higher
  still -- wanting `compileSdk` 37, which needs AGP 9.1+ specifically --
  so those four stayed down too. All of it (Kotlin itself, Hilt 2.57+,
  Media3 1.11+, the Compose-ecosystem four, and AGP 9) is one follow-up
  slice, since bumping any of them for real starts with bumping Kotlin.
  The AGP 8.9.1 bump brought a newer Android Lint with it, which caught a
  real, pre-existing issue in `EfmApp.kt`'s `EfmSearchDestination`: its
  `getBackStackEntry` call was `remember`ed keyed on the `NavController`
  itself rather than on a `NavBackStackEntry`, exactly what
  `UnrememberedGetBackStackEntry` exists to catch -- fixed by keying it on
  the search route's own back stack entry instead.

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
