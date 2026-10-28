# TASKS.md — build order for Claude Code (Opus)

How to use: open Claude Code in the project folder, make sure `CLAUDE.md`, `PROJECT_SPEC.md`,
`ASSETS.md` are in the repo root (or `docs/`). Then paste ONE task prompt at a time.
Do not skip ahead. After each task: build, install, test, commit.

Suggested opening prompt (paste once at the start of the session):

> Read CLAUDE.md, PROJECT_SPEC.md, ASSETS.md and TASKS.md completely. Confirm you understood the goal,
> list the risks you see, then start with Task 0 only. Reply to me in Bengali with technical terms in English.

---

## Task 0 — Environment and upstream build
Prompt:
> Task 0. Check my machine for JDK 17+, Android SDK, adb, git. Tell me what is missing and how to install it.
> Then clone Lawnchair branch `15-dev` with submodules into `./cyberhome` (keep the upstream git history, add my own
> remote later). Read the README/docs, find the correct Gradle debug variant, and build the UNMODIFIED project to
> prove the toolchain works. Report the exact build command and the APK output path. Do not change any code yet.

Done when: unmodified upstream debug APK builds.

## Task 1 — Rebrand and safe isolation
Prompt:
> Task 1. Rebrand per CLAUDE.md rule 1: app name `APP_NAME`, applicationId `APP_ID`, new launcher icon (simple placeholder
> I can replace), remove user-visible "Lawnchair" strings. Make sure the app installs side by side with the original
> Lawnchair (different package). Add LICENSE, NOTICE.md, docs/UPSTREAM_CHANGES.md, docs/PROGRESS.md.
> Disable or remove anything in the build that needs upstream secrets or signing keys. Build and install.

Done when: renamed app installs and runs as a normal Lawnchair-like launcher.

## Task 2 — Design system and wallpaper
Prompt:
> Task 2. Create the design tokens from PROJECT_SPEC.md section 2 (colors, dimens, text styles) under the `cyber_` resource
> prefix. Add the open-license display font from ASSETS.md. Generate the dot-grid dark wallpaper (vector or generated bitmap,
> no copyrighted art) and bundle it. Apply a dark HUD theme to the Settings/Compose theme where cheap. Build and show me a screenshot plan.

Done when: tokens, font, wallpaper exist and are used by a test screen.

## Task 3 — First widgets: Block Clock + Date (W1, W2)
Prompt:
> Task 3. Implement W1/W2 as an AppWidgetProvider in package `APP_ID.cyber.widgets.clock`. Big stacked HH/MM digits,
> diagonal slash ornament, date line, system 12/24h. Use TextClock inside RemoteViews. Add widget preview image and
> sizing info. Test by adding it from the widget picker. Explain how you render the slash/ornaments.

## Task 4 — Default layout + silent-bind risk check
Prompt:
> Task 4. Investigate how this Lawnchair branch loads default workspace layouts and grids. Add a custom grid
> (4x8) and a default layout XML that places the Block Clock widget and a few `<favorite>` shortcuts.
> Test on a fresh install (clear data). CRITICAL: verify whether widgets from our own package can be bound
> silently from the default layout on Android 12–15. If not, design and implement the first-run binding flow
> described in PROJECT_SPEC.md 3.2. Report findings before continuing.

Done when: fresh install shows the clock widget in place without manual adding (or via the documented fallback).

## Task 5 — Calendar, Battery, Weather (W3, W4, W6)
Prompt:
> Task 5. Implement W3 Month Calendar, W4 Battery strip, W6 Weather tile (Open-Meteo, WorkManager, manual city, cache,
> graceful offline state). No permission unless strictly needed; document each in docs/PERMISSIONS.md. Add to default layout.

## Task 6 — Cards and counters (W7, W8, W9)
Prompt:
> Task 6. Implement the three Character card styles (blue/pink/teal) with frame, corner brackets and caption, using the
> placeholder art from ASSETS.md and a config activity to pick an image from the gallery (downscale to stay within
> RemoteViews bitmap limits). Implement the Day counter pill and the Water reminder tile (tap logs a glass,
> optional interval notification via WorkManager). Add to default layout.

## Task 7 — Music strip + dial button + shortcut tiles (W5, W10, W11)
Prompt:
> Task 7. Implement the Music strip. Start with media-key dispatch (no special permission); then, as an optional
> opt-in, add MediaSession metadata via notification listener access with a clear explanation screen. Add the round dial
> button that opens the app drawer, and HUD-style icons for the Settings/Messages/4 favorite tiles in the default layout.

## Task 8 — Onboarding and one-tap setup
Prompt:
> Task 8. Implement the Onboarding screen and the "Apply Look" flow from PROJECT_SPEC.md 3.1:
> default-home role request, wallpaper apply, layout ensure, permissions. Add Settings → "Reset to default look" and a
> help entry for switching back to the previous launcher. Handle every denial path without crashing.

## Task 9 — Layout fidelity pass
Prompt:
> Task 9. Compare the home screen with PROJECT_SPEC.md section 2. List every visible difference, then fix the
> highest-impact ones (spacing, sizes, ornaments, colors). Keep a before/after list in docs/PROGRESS.md.

## Task 10 — About, license, release APK for friends
Prompt:
> Task 10. Add About screen (version, GPL v3 notice, source repo link constant, credits). Create a release signing setup
> using a local `keystore.properties` (gitignored); explain how I generate the keystore. Enable minify/shrink only if
> it does not break the build; if it does, leave it off. Produce a signed release APK, verify with `apksigner verify`,
> and give me the exact file path and install steps. Write docs/RELEASE_NOTES.md.

Done when: a signed APK installs on a friend's phone and the acceptance test in PROJECT_SPEC.md section 6 passes.

---

## Later (NOT now)
Play Store: closed testing (12 testers, 14 days for new personal accounts), privacy policy page, store listing,
premium theme packs, AdMob. Only start after friends' feedback.
