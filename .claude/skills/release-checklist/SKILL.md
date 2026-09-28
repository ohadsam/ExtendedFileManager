---
name: release-checklist
description: Run this after implementing any phase or fix in the Extended File Manager repo (ohadsam/extendedfilemanager), before considering the work done. Covers the recurring "wrap up a batch of changes" checklist — 4x persona code review (system architect, UI expert, UX expert, QA architect), version bump, What's New, docs, skills, RTL/localization check, security/permission review, CI-green gate, and merge. Invoke by name ("run the release checklist") or whenever a user asks to finish/ship/wrap up/close out a change in this repo.
---

# Release checklist

This repo (a native Android app, Kotlin + Jetpack Compose, built entirely through GitHub Actions
CI since the dev sandbox has no Android SDK and no network access to Google's Maven repo — see
`CLAUDE.md`) has one recurring closing ritual for every batch of changes — a phase from
`docs/PLAN.md`, or a standalone fix. Do not skip a step and do not reorder them — later steps
assume earlier ones are done (the version bump needs the review's fixes already applied; the merge
needs CI green).

If a step turns up nothing to do, say so explicitly and move on — don't pad an entry just to have
written something. This is a new project: don't invent war-stories or "this happened before"
claims that aren't true yet. As real incidents happen, add them to the relevant step below the
same way `system_diagram`'s own release-checklist grew concrete, specific entries over time (see
step 9) — that's how this file is supposed to accumulate value.

## 1. Code review — 4 separate persona passes, every time

Not one merged skim. Each persona looks for different things; do all four, in order:

1. **System architect.** Re-read every changed/added file. Does it respect the package layout in
   `CLAUDE.md` (data/domain/ui separation)? Does every mutating file operation route through the
   shared confirmation component and emit an audit event (once Phase 2 exists)? Any new dependency
   justified and pinned in `gradle/libs.versions.toml` (no stray version strings inline)? Any new
   background work correctly scoped to WorkManager rather than a raw thread/coroutine that dies
   with the process? Flag anything that quietly widens permissions (a new `MANAGE_EXTERNAL_STORAGE`
   request, a new broad `<uses-permission>`) without an in-app justification screen.
2. **UI expert.** Compose conventions: no business logic inside composables, state hoisted
   correctly, previews (`@Preview`) added for new non-trivial screens, Material 3 components used
   consistently with the rest of the app, dark theme and dynamic color (Android 12+) both checked.
   Icons/spacing consistent with sibling screens.
3. **UX expert.** Is the flow discoverable and forgiving? Every destructive action confirmed
   (item 9 of the product brief) with a warning that actually says what will happen, not a generic
   "Are you sure?". Empty states, loading states, and error states all present — not just the happy
   path. Touch targets sized for mobile. RTL: mirror-check any new screen in Hebrew (see step 4)
   before calling this pass done.
4. **QA architect.** Is the change actually tested (unit tests for new domain/data logic, not
   deferred to "later")? Are edge cases covered (empty folder, permission denied, huge file,
   duplicate names, file deleted externally mid-operation)? Does anything here need a security or
   performance test per `docs/PLAN.md` Phase 14's categories, even ahead of that phase, because this
   change is security/performance-sensitive on its own (hashing, encryption, protected-path checks)?

Fix everything found before moving on. If a pass finds nothing, say so and continue.

## 2. Version + What's New

- Bump `versionName`/`versionCode` in `app/build.gradle.kts` (semver: features → minor, fixes-only
  → patch; bump `versionCode` every release regardless).
- Add an entry to `docs/CHANGELOG.md` (create it on the first release if it doesn't exist yet) —
  short, user-facing highlights, not implementation detail. This is also the source for the in-app
  "What's New" sheet once Phase 13 builds it.

## 3. Docs

- `docs/PLAN.md` — tick the phase's checkbox/status, adjust later phases if this batch changed the
  plan's assumptions.
- `CLAUDE.md` — update the package layout / conventions section if this batch introduced a new
  pattern future work should follow.
- `README.md` — update if this batch is user-facing enough to change what a new contributor/user
  needs to know.
- `docs/CHANGELOG.md` — see step 2.

## 4. Localization & RTL (Hebrew + English)

Every batch checks this explicitly, even when the answer is "nothing to do":

- Any new user-facing string added to `values/strings.xml` **and** `values-he/strings.xml` — no
  hardcoded strings in composables.
- Any new screen/component visually checked (or at minimum reasoned through) in RTL layout —
  Compose's `LocalLayoutDirection` should mirror automatically for standard layouts, but anything
  using explicit `start`/`end` vs. `left`/`right`, custom icons implying direction (arrows, chevrons),
  or manual offsets needs an explicit check.
- Once Phase 12 lands full RTL infrastructure, this step also covers running its verification
  routine.

## 5. Security & permissions review

- Any new file-system access path checked against the protected-path rules (once Phase 9 exists) —
  or explicitly flagged as needing Phase 9 if this batch predates it and touches raw file I/O.
- Any new data written to disk (prefs, cache, DB) — does it belong in `EncryptedSharedPreferences`/
  Keystore-backed storage, or is plaintext genuinely fine for this data?
- Any new dependency checked for known vulnerabilities (once the Phase 9/14 CI scanning job exists,
  confirm it actually ran clean for this change).

## 6. Skills — including a self-review of this checklist

Check whether `.claude/skills/` need updating given the change. **Then, explicitly and every time,
ask whether this checklist itself needs updating** — did this batch teach it something new (a real
gotcha, a recurring pattern, a step that was missing or wrong)? If so, add a concrete section the
way this file's steps are written (specific, not generic). If not, say so explicitly.

## 7. Tests

- Add/extend unit tests for any new domain/data logic (MockK + Turbine + coroutines-test, per
  `CLAUDE.md`).
- Note in the PR/summary which parts still need instrumented/E2E coverage (Phase 14) if this batch
  can't get it yet.
- **This sandbox cannot run any Gradle task locally** (no Android SDK, no network to Google's Maven
  repo — see `CLAUDE.md`). "Tests pass" means CI (`android-ci.yml`) is green on the pushed branch,
  not a local run. Push, then watch/wait for CI rather than claiming success from a local build.

## 8. Merge

This repo's convention: feature branch → push → open (or update) a PR → CI green → merge into
`main`. Do not push directly to `main` except for the very first bootstrap commit that established
it (the repo started empty). If `main` has moved since the branch was cut and a merge conflict
appears, resolve it for real — don't force-push over someone else's work.

## Done means

- All four persona review passes ran and every finding was fixed, not just noted.
- Version bumped, changelog updated.
- All doc surfaces reviewed (even if some needed no change — say so).
- Localization/RTL checked for any new string or screen.
- Security/permissions review done for any new file-system or storage access.
- Skills reviewed, including this checklist itself.
- New logic has unit tests; CI (`android-ci.yml`) is green on the pushed branch — not just "would
  probably pass locally."
- Branch pushed, PR opened/updated (or merged, per the convention above).
